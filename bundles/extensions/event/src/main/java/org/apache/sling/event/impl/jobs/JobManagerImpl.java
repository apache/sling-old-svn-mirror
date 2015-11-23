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
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.QuerySyntaxException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager.QueueInfo;
import org.apache.sling.event.impl.jobs.config.TopologyCapabilities;
import org.apache.sling.event.impl.jobs.notifications.NotificationUtility;
import org.apache.sling.event.impl.jobs.queues.JobQueueImpl;
import org.apache.sling.event.impl.jobs.queues.QueueManager;
import org.apache.sling.event.impl.jobs.scheduling.JobSchedulerImpl;
import org.apache.sling.event.impl.jobs.stats.StatisticsManager;
import org.apache.sling.event.impl.jobs.tasks.CleanUpTask;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.Job.JobState;
import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.NotificationConstants;
import org.apache.sling.event.jobs.Queue;
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
                     ResourceHelper.BUNDLE_EVENT_STARTED,
                     ResourceHelper.BUNDLE_EVENT_UPDATED})
})
public class JobManagerImpl
    implements JobManager, EventHandler, Runnable {

    /** Default logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
    private StatisticsManager statisticsManager;

    @Reference
    private QueueManager qManager;

    private volatile CleanUpTask maintenanceTask;

    /** Job Scheduler. */
    private org.apache.sling.event.impl.jobs.scheduling.JobSchedulerImpl jobScheduler;

    /**
     * Activate this component.
     * @param props Configuration properties
     */
    @Activate
    protected void activate(final Map<String, Object> props) throws LoginException {
        this.jobScheduler = new org.apache.sling.event.impl.jobs.scheduling.JobSchedulerImpl(this.configuration, this.scheduler, this);
        this.maintenanceTask = new CleanUpTask(this.configuration, this.jobScheduler);

        logger.info("Apache Sling Job Manager started on instance {}", Environment.APPLICATION_ID);
    }

    /**
     * Deactivate this component.
     */
    @Deactivate
    protected void deactivate() {
        logger.debug("Apache Sling Job Manager stopping on instance {}", Environment.APPLICATION_ID);

        this.jobScheduler.deactivate();

        this.maintenanceTask = null;
        logger.info("Apache Sling Job Manager stopped on instance {}", Environment.APPLICATION_ID);
    }

    /**
     * This method is invoked periodically by the scheduler.
     * In the default configuration every minute
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        // invoke maintenance task
        final CleanUpTask task = this.maintenanceTask;
        if ( task != null ) {
            task.run();
        }
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    @Override
    public void handleEvent(final Event event) {
        this.jobScheduler.handleEvent(event);
    }

    /**
     * Return our internal statistics object.
     *
     * @see org.apache.sling.event.jobs.JobManager#getStatistics()
     */
    @Override
    public synchronized Statistics getStatistics() {
        return this.statisticsManager.getGlobalStatistics();
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

    /**
     * Remove a job.
     * If the job is already in the storage area, it's removed forever.
     * Otherwise it's moved to the storage area.
     */
    private boolean internalRemoveJobById(final String jobId, final boolean forceRemove) {
        logger.debug("Trying to remove job {}", jobId);
        boolean result = true;
        JobImpl job = (JobImpl)this.getJobById(jobId);
        if ( job != null ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug("Found removal job: {}", Utility.toString(job));
            }
            final JobImpl retryJob = (JobImpl)this.configuration.getJobFromRetryList(jobId);
            if ( retryJob != null ) {
                job = retryJob;
            }
            // currently running?
            if ( !forceRemove && job.getProcessingStarted() != null ) {
                if ( logger.isDebugEnabled() ) {
                    logger.debug("Unable to remove job - job is started: {}", Utility.toString(job));
                }
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
                        NotificationUtility.sendNotification(this.eventAdmin, NotificationConstants.TOPIC_JOB_REMOVED, job, null);
                    } catch ( final PersistenceException pe) {
                        logger.warn("Unable to remove job at " + job.getResourcePath(), pe);
                        result = false;
                    } finally {
                        resolver.close();
                    }
                } else {
                    final JobHandler jh = new JobHandler(job, null, this.configuration);
                    jh.finished(Job.JobState.DROPPED, true, null);
                }
                this.configuration.getAuditLogger().debug("REMOVE OK : {}", jobId);
            }
        } else {
            logger.debug("Job for removal does not exist (anymore): {}", jobId);
        }
        return result;
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#addJob(java.lang.String, java.util.Map)
     */
    @Override
    public Job addJob(String topic, Map<String, Object> properties) {
        return this.addJob(topic, properties, null);
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#getJobById(java.lang.String)
     */
    @Override
    public Job getJobById(final String id) {
        logger.debug("Getting job by id: {}", id);
        final ResourceResolver resolver = this.configuration.createResourceResolver();
        final StringBuilder buf = new StringBuilder(64);
        try {

            buf.append("/jcr:root");
            buf.append(this.configuration.getJobsBasePathWithSlash());
            buf.append("/element(*,");
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
                            logger.debug("Found job with id {} = {}", id, Utility.toString(job));
                        }
                        return job;
                    }
                }
            }
        } catch (final QuerySyntaxException qse) {
            logger.warn("Query syntax wrong " + buf.toString(), qse);
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
     * @see org.apache.sling.event.jobs.JobManager#findJobs(org.apache.sling.event.jobs.JobManager.QueryType, java.lang.String, long, java.util.Map[])
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
        final StringBuilder buf = new StringBuilder(64);
        try {

            buf.append("/jcr:root");
            buf.append(this.configuration.getJobsBasePathWithSlash());
            buf.append("/element(*,");
            buf.append(ResourceHelper.RESOURCE_TYPE_JOB);
            buf.append(")[@");
            buf.append(ISO9075.encode(ResourceHelper.PROPERTY_JOB_TOPIC));
            if (topic != null) {
                buf.append(" = '");
                buf.append(topic);
                buf.append("'");
            }

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
            logger.warn("Query syntax wrong " + buf.toString(), qse);
        } finally {
            resolver.close();
        }
        return result;
    }

    /**
     * Persist the job in the resource tree
     * @param jobTopic The required job topic
     * @param jobName The optional job name
     * @param passedJobProperties The optional job properties
     * @return The persisted job or <code>null</code>.
     */
    private Job addJobInteral(final String jobTopic,
            final Map<String, Object> jobProperties,
            final List<String> errors) {
        final QueueInfo info = this.configuration.getQueueConfigurationManager().getQueueInfo(jobTopic);

        final TopologyCapabilities caps = this.configuration.getTopologyCapabilities();
        info.targetId = (caps == null ? null : caps.detectTarget(jobTopic, jobProperties, info));

        if ( logger.isDebugEnabled() ) {
            if ( info.targetId != null ) {
                logger.debug("Persisting job {} into queue {}, target={}", new Object[] {Utility.toString(jobTopic, jobProperties), info.queueName, info.targetId});
            } else {
                logger.debug("Persisting job {} into queue {}", Utility.toString(jobTopic, jobProperties), info.queueName);
            }
        }
        final ResourceResolver resolver = this.configuration.createResourceResolver();
        try {
            final JobImpl job = this.writeJob(resolver,
                    jobTopic,
                    jobProperties,
                    info);
            if ( info.targetId != null ) {
                this.configuration.getAuditLogger().debug("ASSIGN OK {} : {}",
                        info.targetId, job.getId());
            } else {
                this.configuration.getAuditLogger().debug("UNASSIGN OK : {}",
                        job.getId());
            }
            return job;
        } catch (final PersistenceException re ) {
            // something went wrong, so let's log it
            this.logger.error("Exception during persisting new job '" + Utility.toString(jobTopic, jobProperties) + "'", re);
        } finally {
            resolver.close();
        }
        if ( errors != null ) {
            errors.add("Unable to persist new job.");
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
        properties.put(Job.PROPERTY_JOB_QUEUE_NAME, info.queueConfiguration.getName());
        properties.put(Job.PROPERTY_JOB_RETRY_COUNT, 0);
        properties.put(Job.PROPERTY_JOB_RETRIES, info.queueConfiguration.getMaxRetries());

        properties.put(Job.PROPERTY_JOB_CREATED, Calendar.getInstance());
        properties.put(JobImpl.PROPERTY_JOB_QUEUED, Calendar.getInstance());
        properties.put(Job.PROPERTY_JOB_CREATED_INSTANCE, Environment.APPLICATION_ID);
        if ( info.targetId != null ) {
            properties.put(Job.PROPERTY_JOB_TARGET_INSTANCE, info.targetId);
        } else {
            properties.remove(Job.PROPERTY_JOB_TARGET_INSTANCE);
        }

        // create path and resource
        properties.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, ResourceHelper.RESOURCE_TYPE_JOB);
        if ( logger.isDebugEnabled() ) {
            logger.debug("Storing new job {} at {}", Utility.toString(jobTopic, properties), path);
        }
        ResourceHelper.getOrCreateResource(resolver,
                path,
                properties);

        // update property types - priority, add path and create job
        properties.put(JobImpl.PROPERTY_RESOURCE_PATH, path);
        return new JobImpl(jobTopic, jobId, properties);
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
            final QueueInfo queueInfo = this.configuration.getQueueConfigurationManager().getQueueInfo(job.getTopic());
            final JobQueueImpl queue = (JobQueueImpl)this.qManager.getQueue(queueInfo.queueName);

            boolean stopped = false;
            if ( queue != null ) {
                stopped = queue.stopJob(job);
            }
            if ( forward && !stopped ) {
                // mark the job as stopped
                final JobHandler jh = new JobHandler(job, null, this.configuration);
                jh.finished(JobState.STOPPED, true, null);
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

    /**
     * Internal method to add a job
     */
    public Job addJob(final String topic,
            final Map<String, Object> properties,
            final List<String> errors) {
        final String errorMessage = Utility.checkJob(topic, properties);
        if ( errorMessage != null ) {
            logger.warn("{}", errorMessage);
            if ( errors != null ) {
                errors.add(errorMessage);
            }
            this.configuration.getAuditLogger().debug("ADD FAILED topic={}, properties={} : {}",
                    new Object[] {topic,
                                  properties,
                                  errorMessage});
            return null;
        }
        final List<String> errorList = new ArrayList<String>();
        Job result = this.addJobInteral(topic, properties, errorList);
        if ( errors != null ) {
            errors.addAll(errorList);
        }
        if ( result == null ) {
            this.configuration.getAuditLogger().debug("ADD FAILED topic={}, properties={} : {}",
                    new Object[] {topic,
                                  properties,
                                  errorList});
        } else {
            this.configuration.getAuditLogger().debug("ADD OK topic={}, properties={} : {}",
                    new Object[] {topic,
                                  properties,
                                  result.getId()});
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
            return this.addJob(job.getTopic(), job.getProperties());
        }
        return null;
    }

    public JobSchedulerImpl getJobScheduler() {
        return this.jobScheduler;
    }
}
