/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.event.impl.jobs;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.QuerySyntaxException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager.QueueInfo;
import org.apache.sling.event.impl.jobs.queues.AbstractJobQueue;
import org.apache.sling.event.impl.jobs.queues.QueueManager;
import org.apache.sling.event.impl.jobs.stats.StatisticsManager;
import org.apache.sling.event.impl.jobs.topology.TopologyAware;
import org.apache.sling.event.impl.jobs.topology.TopologyCapabilities;
import org.apache.sling.event.impl.jobs.topology.TopologyHandler;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.impl.support.ScheduleInfoImpl;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.JobsIterator;
import org.apache.sling.event.jobs.NotificationConstants;
import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.apache.sling.event.jobs.Statistics;
import org.apache.sling.event.jobs.TopicStatistics;
import org.apache.sling.event.jobs.jmx.QueuesMBean;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of the job manager.
 */
@Component(immediate=true)
@Service(value={JobManager.class, EventHandler.class, Runnable.class})
@Properties({
    @Property(name="scheduler.period", longValue=60),
    @Property(name="scheduler.concurrent", boolValue=false),
    @Property(name=EventConstants.EVENT_TOPIC,
              value={SlingConstants.TOPIC_RESOURCE_ADDED,
                     SlingConstants.TOPIC_RESOURCE_CHANGED,
                     SlingConstants.TOPIC_RESOURCE_REMOVED,
                     Utility.TOPIC_STOP,
                     ResourceHelper.BUNDLE_EVENT_STARTED,
                     ResourceHelper.BUNDLE_EVENT_UPDATED})
})
public class JobManagerImpl
    implements JobManager, EventHandler, Runnable, TopologyAware {

    /** Default logger. */
    private final Logger logger = new TestLogger(LoggerFactory.getLogger(this.getClass()));

    @Reference
    private TopologyHandler topologyHandler;

    @Reference
    private EventAdmin eventAdmin;

    @Reference
    private Scheduler scheduler;

    @Reference
    private JobConsumerManager jobConsumerManager;

    @Reference
    private QueuesMBean queuesMBean;

    @Reference
    private ThreadPoolManager threadPoolManager;

    /** The job manager configuration. */
    @Reference
    private JobManagerConfiguration configuration;

    @Reference
    private QueueConfigurationManager queueManager;

    @Reference
    private StatisticsManager statisticsManager;

    @Reference QueueManager qManager;

    private volatile TopologyCapabilities topologyCapabilities;

    private MaintenanceTask maintenanceTask;

    /** We count the scheduler runs. */
    private volatile long schedulerRuns;

    /** Job Scheduler. */
    private JobSchedulerImpl jobScheduler;

    /**
     * Activate this component.
     * @param props Configuration properties
     */
    @Activate
    protected void activate(final Map<String, Object> props) throws LoginException {
        this.jobScheduler = new JobSchedulerImpl(this.configuration, this.scheduler, this);
        this.maintenanceTask = new MaintenanceTask(this.configuration);

        this.topologyHandler.addListener(this);
        logger.info("Apache Sling Job Manager started on instance {}", Environment.APPLICATION_ID);
    }

    /**
     * Deactivate this component.
     */
    @Deactivate
    protected void deactivate() {
        logger.info("Apache Sling Job Manager stopping on instance {}", Environment.APPLICATION_ID);
        this.topologyHandler.removeListener(this);

        this.jobScheduler.deactivate();

        this.maintenanceTask = null;
        logger.info("Apache Sling Job Manager stopped on instance {}", Environment.APPLICATION_ID);
    }

    /**
     * This method is invoked periodically by the scheduler.
     * It searches for idle queues and stops them after a timeout. If a queue
     * is idle for two consecutive clean up calls, it is removed.
     * @see java.lang.Runnable#run()
     */
    private void maintain() {
        this.schedulerRuns++;
        logger.debug("Job manager maintenance: Starting #{}", this.schedulerRuns);

        // invoke maintenance task
        final MaintenanceTask task = this.maintenanceTask;
        if ( task != null ) {
            task.run(this.topologyCapabilities, this.schedulerRuns - 1);
        }
        logger.debug("Job manager maintenance: Finished #{}", this.schedulerRuns);
    }

    /**
     * This method is invoked periodically by the scheduler.
     * In the default configuration every minute
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        this.maintain();
    }

    /**
     * Helper method which just logs the exception in debug mode.
     * @param e
     */
    private void ignoreException(final Exception e) {
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("Ignored exception " + e.getMessage(), e);
        }
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#restart()
     */
    @Override
    public void restart() {
        // TODO reset statistics
        // TODO reload queues?
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#isJobProcessingEnabled()
     */
    @Override
    public boolean isJobProcessingEnabled() {
        return true;
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    @Override
    public void handleEvent(final Event event) {
        if ( SlingConstants.TOPIC_RESOURCE_ADDED.equals(event.getTopic()) ) {
            this.jobScheduler.handleEvent(event);
        } else if ( Utility.TOPIC_STOP.equals(event.getTopic()) ) {
            if ( !EventUtil.isLocal(event) ) {
                final String jobId = (String) event.getProperty(Utility.PROPERTY_ID);
                this.stopJobById(jobId, false);
            }
        } else if ( ResourceHelper.BUNDLE_EVENT_STARTED.equals(event.getTopic())
                 || ResourceHelper.BUNDLE_EVENT_UPDATED.equals(event.getTopic()) ) {
            this.jobScheduler.handleEvent(event);
        } else if ( SlingConstants.TOPIC_RESOURCE_CHANGED.equals(event.getTopic())
                 || SlingConstants.TOPIC_RESOURCE_REMOVED.equals(event.getTopic()) ) {
            this.jobScheduler.handleEvent(event);
        }
    }

    private void stopProcessing() {
        this.topologyCapabilities = null;
    }

    private void startProcessing(final TopologyCapabilities caps) {
        // create new capabilities and update view
        this.topologyCapabilities = caps;
    }

    @Override
    public void topologyChanged(final TopologyCapabilities caps) {
        if ( caps == null ) {
            this.stopProcessing();
        } else {
            this.startProcessing(caps);
        }
        this.jobScheduler.topologyChanged(caps);
    }

    /**
     * Return our internal statistics object.
     *
     * @see org.apache.sling.event.jobs.JobManager#getStatistics()
     */
    @Override
    public synchronized Statistics getStatistics() {
        return this.statisticsManager.getOverallStatistics();
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#getTopicStatistics()
     */
    @Override
    public Iterable<TopicStatistics> getTopicStatistics() {
        return this.statisticsManager.getTopicStatistics().values();
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#getQueue(java.lang.String)
     */
    @Override
    public Queue getQueue(final String name) {
        return qManager.getQueue(ResourceHelper.filterQueueName(name));
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#getQueues()
     */
    @Override
    public Iterable<Queue> getQueues() {
        return qManager.getQueues();
    }

    @Override
    public JobsIterator queryJobs(final QueryType type, final String topic, final Map<String, Object>... templates) {
        return this.queryJobs(type, topic, -1, templates);
    }

    @Override
    public JobsIterator queryJobs(final QueryType type, final String topic,
            final long limit,
            final Map<String, Object>... templates) {
        final Collection<Job> list = this.findJobs(type, topic, limit, templates);
        final Iterator<Job> iter = list.iterator();
        return new JobsIterator() {

            private int index;

            @Override
            public Iterator<Event> iterator() {
                return this;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Event next() {
                index++;
                final Job job = iter.next();
                return Utility.toEvent(job);
            }

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public void skip(final long skipNum) {
                long m = skipNum;
                while ( m > 0 && this.hasNext() ) {
                    this.next();
                    m--;
                }
            }

            @Override
            public long getSize() {
                return list.size();
            }

            @Override
            public long getPosition() {
                return index;
            }
        };
    }

    @Override
    public Event findJob(final String topic, final Map<String, Object> template) {
        final Job job = this.getJob(topic, template);
        if ( job != null ) {
            return Utility.toEvent(job);
        }
        return null;
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#removeJob(java.lang.String)
     */
    @Override
    public boolean removeJob(final String jobId) {
        return this.internalRemoveJobById(jobId, false);
    }

    /**
     * Remove a job.
     * If the job is already in the storage area, it's removed forever.
     * Otherwise it's moved to the storage area.
     */
    private boolean internalRemoveJobById(final String jobId, final boolean forceRemove) {
        logger.debug("Trying to remove job {}", jobId);
        boolean result = true;
        final JobImpl job = (JobImpl)this.getJobById(jobId);
        if ( job != null ) {
            logger.debug("Found removal job: {}", job);
            // currently running?
            if ( !forceRemove && job.getProcessingStarted() != null ) {
                logger.debug("Unable to remove job - job is started: {}", job);
                result = false;
            } else {
                final boolean isHistoryJob = this.configuration.isStoragePath(job.getResourcePath());
                // if history job, simply remove - otherwise move to history!
                if ( isHistoryJob ) {
                    final ResourceResolver resolver = this.configuration.createResourceResolver();
                    try {
                        final Resource jobResource = resolver.getResource(job.getResourcePath());
                        if ( jobResource != null ) {
                            resolver.delete(jobResource);
                            resolver.commit();
                            logger.debug("Removed job with id: {}", jobId);
                        } else {
                            logger.debug("Unable to remove job with id - resource already removed: {}", jobId);
                        }
                        Utility.sendNotification(this.eventAdmin, NotificationConstants.TOPIC_JOB_REMOVED, job, null);
                    } catch ( final PersistenceException pe) {
                        this.ignoreException(pe);
                        result = false;
                    } finally {
                        resolver.close();
                    }
                } else {
                    this.finishJob(job, Job.JobState.DROPPED, true, -1);
                }
            }
        } else {
            logger.debug("Job for removal does not exist (anymore): {}", jobId);
        }
        return result;
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#forceRemoveJob(java.lang.String)
     */
    @Override
    public void forceRemoveJob(final String jobId) {
        this.internalRemoveJobById(jobId, true);
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#addJob(java.lang.String, java.util.Map)
     */
    @Override
    public Job addJob(String topic, Map<String, Object> properties) {
        return this.addJob(topic, null, properties);
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#addJob(java.lang.String, java.lang.String, java.util.Map)
     */
    @Override
    public Job addJob(final String topic, final String name, final Map<String, Object> properties) {
        return this.addJob(topic, name, properties, null);
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#getJobByName(java.lang.String)
     */
    @Override
    public Job getJobByName(final String name) {
        final ResourceResolver resolver = this.configuration.createResourceResolver();
        try {
            final StringBuilder buf = new StringBuilder(64);

            buf.append("//element(*,");
            buf.append(ResourceHelper.RESOURCE_TYPE_JOB);
            buf.append(")[@");
            buf.append(ISO9075.encode(ResourceHelper.PROPERTY_JOB_NAME));
            buf.append(" = '");
            buf.append(name);
            buf.append("']");
            final Iterator<Resource> result = resolver.findResources(buf.toString(), "xpath");

            while ( result.hasNext() ) {
                final Resource jobResource = result.next();
                // sanity check for the path
                if ( this.configuration.isJob(jobResource.getPath()) ) {
                    final JobImpl job = Utility.readJob(logger, jobResource);
                    if ( job != null ) {
                        return job;
                    }
                }
            }
        } catch (final QuerySyntaxException qse) {
            this.ignoreException(qse);
        } finally {
            resolver.close();
        }
        return null;
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#getJobById(java.lang.String)
     */
    @Override
    public Job getJobById(final String id) {
        logger.debug("Getting job by id: {}", id);
        final ResourceResolver resolver = this.configuration.createResourceResolver();
        try {
            final StringBuilder buf = new StringBuilder(64);

            buf.append("//element(*,");
            buf.append(ResourceHelper.RESOURCE_TYPE_JOB);
            buf.append(")[@");
            buf.append(ResourceHelper.PROPERTY_JOB_ID);
            buf.append(" = '");
            buf.append(id);
            buf.append("']");
            if ( logger.isDebugEnabled() ) {
                logger.debug("Exceuting query: {}", buf.toString());
            }
            final Iterator<Resource> result = resolver.findResources(buf.toString(), "xpath");

            while ( result.hasNext() ) {
                final Resource jobResource = result.next();
                // sanity check for the path
                if ( this.configuration.isJob(jobResource.getPath()) ) {
                    final JobImpl job = Utility.readJob(logger, jobResource);
                    if ( job != null ) {
                        if ( logger.isDebugEnabled() ) {
                            logger.debug("Found job with id {} = {}", id, job);
                        }
                        return job;
                    }
                }
            }
        } catch (final QuerySyntaxException qse) {
            this.ignoreException(qse);
        } finally {
            resolver.close();
        }
        logger.debug("Job not found with id: {}", id);
        return null;
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#getJob(java.lang.String, java.util.Map)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Job getJob(final String topic, final Map<String, Object> template) {
        final Iterable<Job> iter;
        if ( template == null ) {
            iter = this.findJobs(QueryType.ALL, topic, 1, (Map<String, Object>[])null);
        } else {
            iter = this.findJobs(QueryType.ALL, topic, 1, template);
        }
        final Iterator<Job> i = iter.iterator();
        if ( i.hasNext() ) {
            return i.next();
        }
        return null;
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#removeJobById(java.lang.String)
     */
    @Override
    public boolean removeJobById(final String jobId) {
        return this.internalRemoveJobById(jobId, true);
    }

    private enum Operation {
        LESS,
        LESS_OR_EQUALS,
        EQUALS,
        GREATER_OR_EQUALS,
        GREATER
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#findJobs(org.apache.sling.event.jobs.JobManager.QueryType, java.lang.String, long, java.util.Map<java.lang.String,java.lang.Object>[])
     */
    @Override
    public Collection<Job> findJobs(final QueryType type,
            final String topic,
            final long limit,
            final Map<String, Object>... templates) {
        final boolean isHistoryQuery = type == QueryType.HISTORY
                                       || type == QueryType.SUCCEEDED
                                       || type == QueryType.CANCELLED
                                       || type == QueryType.DROPPED
                                       || type == QueryType.ERROR
                                       || type == QueryType.GIVEN_UP
                                       || type == QueryType.STOPPED;
        final List<Job> result = new ArrayList<Job>();
        final ResourceResolver resolver = this.configuration.createResourceResolver();
        try {
            final StringBuilder buf = new StringBuilder(64);

            buf.append("//element(*,");
            buf.append(ResourceHelper.RESOURCE_TYPE_JOB);
            buf.append(")[@");
            buf.append(ISO9075.encode(ResourceHelper.PROPERTY_JOB_TOPIC));
            buf.append(" = '");
            buf.append(topic);
            buf.append("'");

            // restricting on the type - history or unfinished
            if ( isHistoryQuery ) {
                buf.append(" and @");
                buf.append(ISO9075.encode(JobImpl.PROPERTY_FINISHED_STATE));
                if ( type == QueryType.SUCCEEDED || type == QueryType.DROPPED || type == QueryType.ERROR || type == QueryType.GIVEN_UP || type == QueryType.STOPPED ) {
                    buf.append(" = '");
                    buf.append(type.name());
                    buf.append("'");
                } else if ( type == QueryType.CANCELLED ) {
                    buf.append(" and (@");
                    buf.append(ISO9075.encode(JobImpl.PROPERTY_FINISHED_STATE));
                    buf.append(" = '");
                    buf.append(QueryType.DROPPED.name());
                    buf.append("' or @");
                    buf.append(ISO9075.encode(JobImpl.PROPERTY_FINISHED_STATE));
                    buf.append(" = '");
                    buf.append(QueryType.ERROR.name());
                    buf.append("' or @");
                    buf.append(ISO9075.encode(JobImpl.PROPERTY_FINISHED_STATE));
                    buf.append(" = '");
                    buf.append(QueryType.GIVEN_UP.name());
                    buf.append("' or @");
                    buf.append(ISO9075.encode(JobImpl.PROPERTY_FINISHED_STATE));
                    buf.append(" = '");
                    buf.append(QueryType.STOPPED.name());
                    buf.append("')");
                }
            } else {
                buf.append(" and not(@");
                buf.append(ISO9075.encode(JobImpl.PROPERTY_FINISHED_STATE));
                buf.append(")");
                if ( type == QueryType.ACTIVE ) {
                    buf.append(" and @");
                    buf.append(ISO9075.encode(Job.PROPERTY_JOB_STARTED_TIME));
                } else if ( type == QueryType.QUEUED ) {
                    buf.append(" and not(@");
                    buf.append(ISO9075.encode(Job.PROPERTY_JOB_STARTED_TIME));
                    buf.append(")");
                }
            }

            if ( templates != null && templates.length > 0 ) {
                int index = 0;
                for (final Map<String,Object> template : templates) {
                    // skip empty templates
                    if ( template.size() == 0 ) {
                        continue;
                    }
                    if ( index == 0 ) {
                        buf.append(" and (");
                    } else {
                        buf.append(" or ");
                    }
                    buf.append('(');
                    final Iterator<Map.Entry<String, Object>> i = template.entrySet().iterator();
                    boolean first = true;
                    while ( i.hasNext() ) {
                        final Map.Entry<String, Object> current = i.next();
                        final String key = ISO9075.encode(current.getKey());
                        final char firstChar = key.length() > 0 ? key.charAt(0) : 0;
                        final String propName;
                        final Operation op;
                        if ( firstChar == '=' ) {
                            propName = key.substring(1);
                            op  = Operation.EQUALS;
                        } else if ( firstChar == '<' ) {
                            final char secondChar = key.length() > 1 ? key.charAt(1) : 0;
                            if ( secondChar == '=' ) {
                                op = Operation.LESS_OR_EQUALS;
                                propName = key.substring(2);
                            } else {
                                op = Operation.LESS;
                                propName = key.substring(1);
                            }
                        } else if ( firstChar == '>' ) {
                            final char secondChar = key.length() > 1 ? key.charAt(1) : 0;
                            if ( secondChar == '=' ) {
                                op = Operation.GREATER_OR_EQUALS;
                                propName = key.substring(2);
                            } else {
                                op = Operation.GREATER;
                                propName = key.substring(1);
                            }
                        } else {
                            propName = key;
                            op  = Operation.EQUALS;
                        }

                        if ( first ) {
                            first = false;
                            buf.append('@');
                        } else {
                            buf.append(" and @");
                        }
                        buf.append(propName);
                        buf.append(' ');
                        switch ( op ) {
                            case EQUALS : buf.append('=');break;
                            case LESS : buf.append('<'); break;
                            case LESS_OR_EQUALS : buf.append("<="); break;
                            case GREATER : buf.append('>'); break;
                            case GREATER_OR_EQUALS : buf.append(">="); break;
                        }
                        buf.append(" '");
                        buf.append(current.getValue());
                        buf.append("'");
                    }
                    buf.append(')');
                    index++;
                }
                if ( index > 0 ) {
                    buf.append(')');
                }
            }
            buf.append("] order by @");
            if ( isHistoryQuery ) {
                buf.append(JobImpl.PROPERTY_FINISHED_DATE);
                buf.append(" descending");
            } else {
                buf.append(Job.PROPERTY_JOB_CREATED);
                buf.append(" ascending");
            }
            final Iterator<Resource> iter = resolver.findResources(buf.toString(), "xpath");
            long count = 0;

            while ( iter.hasNext() && (limit < 1 || count < limit) ) {
                final Resource jobResource = iter.next();
                // sanity check for the path
                if ( this.configuration.isJob(jobResource.getPath()) ) {
                    final JobImpl job = Utility.readJob(logger, jobResource);
                    if ( job != null ) {
                        count++;
                        result.add(job);
                    }
                }
             }
        } catch (final QuerySyntaxException qse) {
            this.ignoreException(qse);
        } finally {
            resolver.close();
        }
        return result;
    }

    /**
     * Finish a job
     * @param info  The job handler
     * @param state The state of the processing
     */
    public void finishJob(final JobImpl job,
                          final Job.JobState state,
                          final boolean keepJobInHistory,
                          final long duration) {
        final boolean isSuccess = (state == Job.JobState.SUCCEEDED);
        final ResourceResolver resolver = this.configuration.createResourceResolver();
        try {
            final Resource jobResource = resolver.getResource(job.getResourcePath());
            if ( jobResource != null ) {
                try {
                    String newPath = null;
                    if ( keepJobInHistory ) {
                        final ValueMap vm = ResourceHelper.getValueMap(jobResource);
                        newPath = this.configuration.getStoragePath(job, isSuccess);
                        final Map<String, Object> props = new HashMap<String, Object>(vm);
                        props.put(JobImpl.PROPERTY_FINISHED_STATE, state.name());
                        if ( isSuccess ) {
                            // we set the finish date to start date + duration
                            final Date finishDate = new Date();
                            finishDate.setTime(job.getProcessingStarted().getTime().getTime() + duration);
                            final Calendar finishCal = Calendar.getInstance();
                            finishCal.setTime(finishDate);
                            props.put(JobImpl.PROPERTY_FINISHED_DATE, finishCal);
                        } else {
                            // current time is good enough
                            props.put(JobImpl.PROPERTY_FINISHED_DATE, Calendar.getInstance());
                        }
                        if ( job.getProperty(Job.PROPERTY_RESULT_MESSAGE) != null ) {
                            props.put(Job.PROPERTY_RESULT_MESSAGE, job.getProperty(Job.PROPERTY_RESULT_MESSAGE));
                        }
                        ResourceHelper.getOrCreateResource(resolver, newPath, props);
                    }
                    resolver.delete(jobResource);
                    resolver.commit();

                    if ( keepJobInHistory && logger.isDebugEnabled() ) {
                        if ( isSuccess ) {
                            logger.debug("Kept successful job {} at {}", Utility.toString(job), newPath);
                        } else {
                            logger.debug("Moved cancelled job {} to {}", Utility.toString(job), newPath);
                        }
                    }
                } catch ( final PersistenceException pe ) {
                    this.ignoreException(pe);
                } catch (final InstantiationException ie) {
                    // something happened with the resource in the meantime
                    this.ignoreException(ie);
                }
            }
        } finally {
            resolver.close();
        }
    }

    /**
     * Reschedule a job.
     *
     * Update the retry count and remove the started time.
     * @param job The job
     * @return true if the job could be updated.
     */
    public boolean reschedule(final JobImpl job) {
        final ResourceResolver resolver = this.configuration.createResourceResolver();
        try {
            final Resource jobResource = resolver.getResource(job.getResourcePath());
            if ( jobResource != null ) {
                final ModifiableValueMap mvm = jobResource.adaptTo(ModifiableValueMap.class);
                mvm.put(Job.PROPERTY_JOB_RETRY_COUNT, job.getProperty(Job.PROPERTY_JOB_RETRY_COUNT));
                if ( job.getProperty(Job.PROPERTY_RESULT_MESSAGE) != null ) {
                    mvm.put(Job.PROPERTY_RESULT_MESSAGE, job.getProperty(Job.PROPERTY_RESULT_MESSAGE));
                }
                mvm.remove(Job.PROPERTY_JOB_STARTED_TIME);
                try {
                    resolver.commit();
                    return true;
                } catch ( final PersistenceException pe ) {
                    ignoreException(pe);
                }
            }
        } finally {
            resolver.close();
        }

        return false;
    }

    /**
     * Try to get a "lock" for a resource
     */
    private boolean lock(final String jobTopic, final String id) {
        if ( logger.isDebugEnabled() ) {
            logger.debug("Trying to get lock for {}", id);
        }
        boolean hasLock = false;
        final ResourceResolver resolver = this.configuration.createResourceResolver();
        try {
            final String lockName = ResourceHelper.filterName(id);
            final StringBuilder sb = new StringBuilder(this.configuration.getLocksPath());
            sb.append('/');
            sb.append(jobTopic.replace('/', '.'));
            sb.append('/');
            sb.append(lockName);
            final String path = sb.toString();

            Resource lockResource = resolver.getResource(path);
            if ( lockResource == null ) {
                resolver.refresh();
                try {
                    final Map<String, Object> props = new HashMap<String, Object>();
                    props.put(Utility.PROPERTY_LOCK_CREATED, Calendar.getInstance());
                    props.put(Utility.PROPERTY_LOCK_CREATED_APP, Environment.APPLICATION_ID);
                    props.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, Utility.RESOURCE_TYPE_LOCK);

                    lockResource = ResourceHelper.getOrCreateResource(resolver,
                            path,
                            props);

                    // check if lock resource has correct name (SNS)
                    if ( !lockResource.getName().equals(lockName) ) {
                        if ( logger.isDebugEnabled() ) {
                            logger.debug("Created SNS lock resource on instance {} - discarding", Environment.APPLICATION_ID);
                        }
                        resolver.delete(lockResource);
                        resolver.commit();
                    } else {
                        final ValueMap vm = lockResource.adaptTo(ValueMap.class);
                        if ( logger.isDebugEnabled() ) {
                            logger.debug("Got lock resource on instance {} with {}", Environment.APPLICATION_ID, vm.get(Utility.PROPERTY_LOCK_CREATED_APP));
                        }
                        if ( vm.get(Utility.PROPERTY_LOCK_CREATED_APP).equals(Environment.APPLICATION_ID) ) {
                            hasLock = true;
                        }
                    }
                } catch (final PersistenceException ignore) {
                    // ignore
                    this.ignoreException(ignore);
                }
            }
        } finally {
            resolver.close();
        }
        if ( logger.isDebugEnabled() ) {
            logger.debug("Lock for {} = {}", id, hasLock);
        }
        return hasLock;
    }

    /**
     * Persist the job in the resource tree
     * @param jobTopic The required job topic
     * @param jobName The optional job name
     * @param jobProperties The optional job properties
     * @return The persisted job or <code>null</code>.
     */
    private Job addJobInteral(final String jobTopic,
            final String jobName,
            final Map<String, Object> jobProperties,
            final List<String> errors) {
        final QueueInfo info = this.queueManager.getQueueInfo(jobTopic);
        if ( info.queueConfiguration.getType() == QueueConfiguration.Type.DROP ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug("Dropping job due to configuration of queue {} : {}", info.queueName, Utility.toString(jobTopic, jobName, jobProperties));
            }
            Utility.sendNotification(this.eventAdmin, NotificationConstants.TOPIC_JOB_CANCELLED, jobTopic, jobName, jobProperties, null);
        } else {
            // check for unique jobs
            if ( jobName != null && !this.lock(jobTopic, jobName) ) {
                logger.debug("Discarding duplicate job {}", Utility.toString(jobTopic, jobName, jobProperties));
                return null;
            } else {
                if ( info.queueConfiguration.getType() != QueueConfiguration.Type.IGNORE ) {
                    final TopologyCapabilities caps = this.topologyCapabilities;
                    info.targetId = (caps == null ? null : caps.detectTarget(jobTopic, jobProperties, info));
                }
                if ( logger.isDebugEnabled() ) {
                    if ( info.targetId != null ) {
                        logger.debug("Persisting job {} into queue {}, target={}", new Object[] {Utility.toString(jobTopic, jobName, jobProperties), info.queueName, info.targetId});
                    } else {
                        logger.debug("Persisting job {} into queue {}", Utility.toString(jobTopic, jobName, jobProperties), info.queueName);
                    }
                }
                final ResourceResolver resolver = this.configuration.createResourceResolver();
                try {
                    final JobImpl job = this.writeJob(resolver,
                            jobTopic,
                            jobName,
                            jobProperties,
                            info);
                    return job;
                } catch (final PersistenceException re ) {
                    // something went wrong, so let's log it
                    this.logger.error("Exception during persisting new job '" + Utility.toString(jobTopic, jobName, jobProperties) + "'", re);
                } finally {
                    resolver.close();
                }
                if ( errors != null ) {
                    errors.add("Unable to persist new job.");
                }
            }
        }
        return null;
    }

    /**
     * Write a job to the resource tree.
     * @param resolver The resolver resolver
     * @param event The event
     * @param info The queue information (queue name etc.)
     * @throws PersistenceException
     */
    private JobImpl writeJob(final ResourceResolver resolver,
            final String jobTopic,
            final String jobName,
            final Map<String, Object> jobProperties,
            final QueueInfo info)
    throws PersistenceException {
        final String jobId = this.configuration.getUniqueId(jobTopic);
        final String path = this.configuration.getUniquePath(info.targetId, jobTopic, jobId, jobProperties);

        // create properties
        final Map<String, Object> properties = new HashMap<String, Object>();

        if ( jobProperties != null ) {
            for(final Map.Entry<String, Object> entry : jobProperties.entrySet() ) {
                final String propName = entry.getKey();
                if ( !ResourceHelper.ignoreProperty(propName) ) {
                    properties.put(propName, entry.getValue());
                }
            }
        }

        properties.put(ResourceHelper.PROPERTY_JOB_ID, jobId);
        properties.put(ResourceHelper.PROPERTY_JOB_TOPIC, jobTopic);
        if ( jobName != null ) {
            properties.put(ResourceHelper.PROPERTY_JOB_NAME, jobName);
        }
        properties.put(Job.PROPERTY_JOB_QUEUE_NAME, info.queueConfiguration.getName());
        properties.put(Job.PROPERTY_JOB_RETRY_COUNT, 0);
        properties.put(Job.PROPERTY_JOB_RETRIES, info.queueConfiguration.getMaxRetries());

        properties.put(Job.PROPERTY_JOB_CREATED, Calendar.getInstance());
        properties.put(Job.PROPERTY_JOB_CREATED_INSTANCE, Environment.APPLICATION_ID);
        if ( info.targetId != null ) {
            properties.put(Job.PROPERTY_JOB_TARGET_INSTANCE, info.targetId);
        } else {
            properties.remove(Job.PROPERTY_JOB_TARGET_INSTANCE);
        }

        // create path and resource
        properties.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, ResourceHelper.RESOURCE_TYPE_JOB);
        if ( logger.isDebugEnabled() ) {
            logger.debug("Storing new job {} at {}", properties, path);
        }
        ResourceHelper.getOrCreateResource(resolver,
                path,
                properties);

        // update property types - priority, add path and create job
        properties.put(JobImpl.PROPERTY_RESOURCE_PATH, path);
        return new JobImpl(jobTopic, jobName, jobId, properties);
    }

    public void reassign(final JobImpl job) {
        final QueueInfo queueInfo = queueManager.getQueueInfo(job.getTopic());
        final InternalQueueConfiguration config = queueInfo.queueConfiguration;

        // Sanity check if queue configuration has changed
        if ( config.getType() == QueueConfiguration.Type.DROP ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug("Dropping job due to configuration of queue {} : {}", queueInfo.queueName, Utility.toString(job));
            }
            this.finishJob(job, Job.JobState.DROPPED, false, -1); // DROP means complete removal
        } else {
            String targetId = null;
            if ( config.getType() != QueueConfiguration.Type.IGNORE ) {
                final TopologyCapabilities caps = this.topologyCapabilities;
                targetId = (caps == null ? null : caps.detectTarget(job.getTopic(), job.getProperties(), queueInfo));
            }
            this.maintenanceTask.reassignJob(job, targetId);
        }
    }

    /**
     * Get the current capabilities
     */
    public TopologyCapabilities getTopologyCapabilities() {
        return this.topologyCapabilities;
    }

    /**
     * Update the property of a job in the resource tree
     */
    public boolean persistJobProperties(final JobImpl job, final String... propNames) {
        final ResourceResolver resolver = this.configuration.createResourceResolver();
        try {
            final Resource jobResource = resolver.getResource(job.getResourcePath());
            if ( jobResource != null ) {
                final ModifiableValueMap mvm = jobResource.adaptTo(ModifiableValueMap.class);
                for(final String propName : propNames) {
                    final Object val = job.getProperty(propName);
                    if ( val != null ) {
                        if ( val.getClass().isEnum() ) {
                            mvm.put(propName, val.toString());
                        } else {
                            mvm.put(propName, val);
                        }
                    } else {
                        mvm.remove(propName);
                    }
                }
                resolver.commit();

                return true;
            } else {
                logger.debug("No job resource found at {}", job.getResourcePath());
            }
        } catch ( final PersistenceException ignore ) {
            this.ignoreException(ignore);
        } finally {
            resolver.close();
        }
        return false;
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#stopJobById(java.lang.String)
     */
    @Override
    public void stopJobById(final String jobId) {
        this.stopJobById(jobId, true);
    }

    private void stopJobById(final String jobId, final boolean forward) {
        final JobImpl job = (JobImpl)this.getJobById(jobId);
        if ( job != null && !this.configuration.isStoragePath(job.getResourcePath()) ) {
            // get the queue configuration
            final QueueInfo queueInfo = this.queueManager.getQueueInfo(job.getTopic());
            final AbstractJobQueue queue = (AbstractJobQueue)this.qManager.getQueue(queueInfo.queueName);

            boolean stopped = false;
            if ( queue != null ) {
                stopped = queue.stopJob(job);
            }
            if ( forward && !stopped ) {
                // TODO why not remove the resource?
                // send remote event
                final Map<String, Object> props = new HashMap<String, Object>();
                props.put(Utility.PROPERTY_ID, jobId);
                props.put(EventUtil.PROPERTY_DISTRIBUTE, "");
                this.eventAdmin.sendEvent(new Event(Utility.TOPIC_STOP, props));
            }
        }
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#createJob(java.lang.String)
     */
    @Override
    public JobBuilder createJob(final String topic) {
        return new JobBuilderImpl(this, topic);
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#getScheduledJobs()
     */
    @Override
    public Collection<ScheduledJobInfo> getScheduledJobs() {
        return this.jobScheduler.getScheduledJobs(null, -1, (Map<String, Object>[])null);
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#getScheduledJobs()
     */
    @Override
    public Collection<ScheduledJobInfo> getScheduledJobs(final String topic,
            final long limit,
            final Map<String, Object>... templates) {
        return this.jobScheduler.getScheduledJobs(topic, limit, templates);
    }

    public ScheduledJobInfo addScheduledJob(final String topic,
            final String jobName,
            final Map<String, Object> properties,
            final String scheduleName,
            final boolean isSuspended,
            final List<ScheduleInfoImpl> scheduleInfos,
            final List<String> errors) {
        final List<String> msgs = new ArrayList<String>();
        if ( scheduleName == null || scheduleName.length() == 0 ) {
            msgs.add("Schedule name not specified");
        }
        final String errorMessage = Utility.checkJob(topic, properties);
        if ( errorMessage != null ) {
            msgs.add(errorMessage);
        }
        if ( scheduleInfos.size() == 0 ) {
            msgs.add("No schedule defined for " + scheduleName);
        }
        for(final ScheduleInfoImpl info : scheduleInfos) {
            info.check(msgs);
        }
        if ( msgs.size() == 0 ) {
            try {
                final ScheduledJobInfo info = this.jobScheduler.writeJob(topic, jobName, properties, scheduleName, isSuspended, scheduleInfos);
                if ( info != null ) {
                    return info;
                }
                msgs.add("Unable to persist scheduled job.");
            } catch ( final PersistenceException pe) {
                msgs.add("Unable to persist scheduled job: " + scheduleName);
                logger.warn("Unable to persist scheduled job", pe);
            }
        } else {
            for(final String msg : msgs) {
                logger.warn(msg);
            }
        }
        if ( errors != null ) {
            errors.addAll(msgs);
        }
        return null;
    }

    /**
     * Internal method to add a job
     */
    public Job addJob(final String topic, final String name,
            final Map<String, Object> properties,
            final List<String> errors) {
        final String errorMessage = Utility.checkJob(topic, properties);
        if ( errorMessage != null ) {
            logger.warn("{}", errorMessage);
            if ( errors != null ) {
                errors.add(errorMessage);
            }
            return null;
        }
        if ( name != null ) {
            logger.info("Job is using deprecated name feature: {}", Utility.toString(topic, name, properties));
        }
        Job result = this.addJobInteral(topic, name, properties, errors);
        if ( result == null && name != null ) {
            result = this.getJobByName(name);
        }
        return result;
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#retryJobById(java.lang.String)
     */
    @Override
    public Job retryJobById(final String jobId) {
        final JobImpl job = (JobImpl)this.getJobById(jobId);
        if ( job != null && this.configuration.isStoragePath(job.getResourcePath()) ) {
            this.internalRemoveJobById(jobId, true);
            return this.addJob(job.getTopic(), job.getName(), job.getProperties());
        }
        return null;
    }
}
