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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
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
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.impl.EnvironmentComponent;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager.QueueInfo;
import org.apache.sling.event.impl.jobs.jmx.QueueStatusEvent;
import org.apache.sling.event.impl.jobs.queues.AbstractJobQueue;
import org.apache.sling.event.impl.jobs.queues.OrderedJobQueue;
import org.apache.sling.event.impl.jobs.queues.ParallelJobQueue;
import org.apache.sling.event.impl.jobs.queues.TopicRoundRobinJobQueue;
import org.apache.sling.event.impl.jobs.stats.StatisticsImpl;
import org.apache.sling.event.impl.jobs.stats.TopicStatisticsImpl;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.JobUtil;
import org.apache.sling.event.jobs.JobUtil.JobPriority;
import org.apache.sling.event.jobs.JobsIterator;
import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.apache.sling.event.jobs.Statistics;
import org.apache.sling.event.jobs.TopicStatistics;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of the job manager.
 */
@Component(immediate=true,
           name="org.apache.sling.event.impl.jobs.jcr.PersistenceHandler")
@Service(value={JobManager.class, EventHandler.class, TopologyEventListener.class, Runnable.class})
@Properties({
    @Property(name=JobManagerConfiguration.CONFIG_PROPERTY_REPOSITORY_PATH,
          value=JobManagerConfiguration.DEFAULT_REPOSITORY_PATH,
          propertyPrivate=true),
    @Property(name="scheduler.period", longValue=60, propertyPrivate=true),
    @Property(name="scheduler.concurrent", boolValue=false, propertyPrivate=true),
    @Property(name=EventConstants.EVENT_TOPIC, propertyPrivate=true,
              value={SlingConstants.TOPIC_RESOURCE_ADDED,
                     "org/apache/sling/event/notification/job/*",
                     ResourceHelper.BUNDLE_EVENT_STARTED,
                     ResourceHelper.BUNDLE_EVENT_UPDATED})
})
public class JobManagerImpl
    extends StatisticsImpl
    implements JobManager, EventHandler, TopologyEventListener, Runnable {

    /** Default logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The environment component. */
    @Reference
    private EnvironmentComponent environment;

    @Reference
    private EventAdmin eventAdmin;

    /** The configuration manager. */
    @Reference
    private QueueConfigurationManager queueConfigManager;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Scheduler scheduler;

    @Reference
    private JobConsumerManager jobConsumerManager;

    /** The job manager configuration. */
    private JobManagerConfiguration configuration;

    private volatile TopologyCapabilities topologyCapabilities;

    private MaintenanceTask maintenanceTask;

    private BackgroundLoader backgroundLoader;

    /** Lock object for the queues map - we don't want to sync directly on the concurrent map. */
    private final Object queuesLock = new Object();

    /** All active queues. */
    private final Map<String, AbstractJobQueue> queues = new ConcurrentHashMap<String, AbstractJobQueue>();

    /** We count the scheduler runs. */
    private volatile long schedulerRuns;

    /** Current statistics. */
    private final StatisticsImpl baseStatistics = new StatisticsImpl();

    /** Statistics per topic. */
    private final ConcurrentMap<String, TopicStatistics> topicStatistics = new ConcurrentHashMap<String, TopicStatistics>();

    /**
     * Activate this component.
     * @param props Configuration properties
     */
    @Activate
    protected void activate(final Map<String, Object> props) throws LoginException {
        this.configuration = new JobManagerConfiguration(props);
        this.maintenanceTask = new MaintenanceTask(this.configuration, this.resourceResolverFactory);
        this.backgroundLoader = new BackgroundLoader(this, this.configuration, this.resourceResolverFactory);

        // create initial resources
        final ResourceResolver resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
        try {
            ResourceHelper.getOrCreateBasePath(resolver, this.configuration.getLocalJobsPath());
            ResourceHelper.getOrCreateBasePath(resolver, this.configuration.getUnassignedJobsPath());
            ResourceHelper.getOrCreateBasePath(resolver, this.configuration.getLocksPath());
        } catch ( final PersistenceException pe ) {
            this.ignoreException(pe);
        } finally {
            resolver.close();
        }

        logger.info("Apache Sling Job Manager started on instance {}", Environment.APPLICATION_ID);
    }

    /**
     * Configure this component.
     * @param props Configuration properties
     */
    @Modified
    protected void update(final Map<String, Object> props) {
        // nothing to do
    }

    /**
     * Deactivate this component.
     */
    @Deactivate
    protected void deactivate() {
        logger.info("Apache Sling Job Manager stopping on instance {}", Environment.APPLICATION_ID);
        this.backgroundLoader.deactivate();
        this.backgroundLoader = null;

        this.maintenanceTask = null;
        this.configuration = null;
        final Iterator<AbstractJobQueue> i = this.queues.values().iterator();
        while ( i.hasNext() ) {
            final AbstractJobQueue jbq = i.next();
            // update mbeans
            eventAdmin.sendEvent(new QueueStatusEvent(null, jbq));
            jbq.close();
            // update mbeans
            eventAdmin.sendEvent(new QueueStatusEvent(null, jbq));
        }
        this.queues.clear();
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

        // check for unprocessed jobs first
        logger.debug("Checking for unprocessed jobs...");
        for(final AbstractJobQueue jbq : this.queues.values() ) {
            jbq.checkForUnprocessedJobs();
        }

        // we only do a full clean up on every fifth run
        final boolean doFullCleanUp = (schedulerRuns % 5 == 0);

        if ( doFullCleanUp ) {
            // check for idle queue
            logger.debug("Checking for idle queues...");

           // we synchronize to avoid creating a queue which is about to be removed during cleanup
            synchronized ( queuesLock ) {
                final Iterator<Map.Entry<String, AbstractJobQueue>> i = this.queues.entrySet().iterator();
                while ( i.hasNext() ) {
                    final Map.Entry<String, AbstractJobQueue> current = i.next();
                    final AbstractJobQueue jbq = current.getValue();
                    if ( jbq.isMarkedForRemoval() ) {
                        logger.debug("Removing idle Job Queue {}", jbq);
                        // close
                        jbq.close();
                        // copy statistics
                        this.baseStatistics.add(jbq);
                        // remove
                        i.remove();
                        // update mbeans
                        eventAdmin.sendEvent(new QueueStatusEvent(null, jbq));
                    } else {
                        // mark to be removed during next cycle
                        jbq.markForRemoval();
                    }
                }
            }
        }
        // invoke maintenance task
        final MaintenanceTask task = this.maintenanceTask;
        if ( task != null ) {
            task.run(this.topologyCapabilities, this.queueConfigManager, this.schedulerRuns);
        }
        logger.debug("Job manager maintenance: Finished #{}", this.schedulerRuns);
    }

    /**
     * Process a new job
     * This method first searches the corresponding queue - if such a queue
     * does not exist yet, it is created and started.
     *
     * @param job The job
     */
    void process(final JobImpl job) {
        final JobHandler handler = new JobHandler(job, this);

        // check if we still are able to process this job
        final JobConsumer consumer = this.jobConsumerManager.getConsumer(job.getTopic());
        boolean reassign = false;
        String reassignTargetId = null;
        if ( consumer == null && (!job.isBridgedEvent() || !this.jobConsumerManager.supportsBridgedEvents())) {
            reassign = true;
        }

        // get the queue configuration
        final QueueInfo queueInfo = queueConfigManager.getQueueInfo(handler.getJob().getTopic());
        final InternalQueueConfiguration config = queueInfo.queueConfiguration;

        // Sanity check if queue configuration has changed
        if ( config.getType() == QueueConfiguration.Type.DROP ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug("Dropping job due to configuration of queue {} : {}", queueInfo.queueName, Utility.toString(handler.getJob()));
            }
            handler.remove();
        } else if ( config.getType() == QueueConfiguration.Type.IGNORE ) {
            if ( !reassign ) {
                if ( logger.isDebugEnabled() ) {
                    logger.debug("Ignoring job due to configuration of queue {} : {}", queueInfo.queueName, Utility.toString(handler.getJob()));
                }
            }
        } else {

            if ( reassign ) {
                final TopologyCapabilities caps = this.topologyCapabilities;
                reassignTargetId = (caps == null ? null : caps.detectTarget(job.getTopic(), job.getProperties(), queueInfo));

            } else {
                // get or create queue
                AbstractJobQueue queue = null;
                // we synchronize to avoid creating a queue which is about to be removed during cleanup
                synchronized ( queuesLock ) {
                    queue = this.queues.get(queueInfo.queueName);
                    // check for reconfiguration, we really do an identity check here(!)
                    if ( queue != null && queue.getConfiguration() != config ) {
                        this.outdateQueue(queue);
                        // we use a new queue with the configuration
                        queue = null;
                    }
                    if ( queue == null ) {
                        if ( config.getType() == QueueConfiguration.Type.ORDERED ) {
                            queue = new OrderedJobQueue(queueInfo.queueName, config, this.jobConsumerManager, this.eventAdmin);
                        } else if ( config.getType() == QueueConfiguration.Type.UNORDERED ) {
                            queue = new ParallelJobQueue(queueInfo.queueName, config, this.jobConsumerManager, this.eventAdmin, this.scheduler);
                        } else if ( config.getType() == QueueConfiguration.Type.TOPIC_ROUND_ROBIN ) {
                            queue = new TopicRoundRobinJobQueue(queueInfo.queueName, config, this.jobConsumerManager, this.eventAdmin, this.scheduler);
                        }
                        if ( queue == null ) {
                            // this is just a sanity check, actually we can never get here
                            logger.warn("Ignoring event due to unknown queue type of queue {} : {}", queueInfo.queueName, Utility.toString(handler.getJob()));
                            handler.remove();
                        } else {
                            queues.put(queueInfo.queueName, queue);
                            eventAdmin.sendEvent(new QueueStatusEvent(queue, null));
                            queue.start();
                        }
                    }
                }

                // and put job
                if ( queue != null ) {
                    handler.getJob().updateQueue(queue);
                    queue.process(handler);
                }
            }
        }
        if ( reassign ) {
            this.maintenanceTask.reassignJob(job, reassignTargetId);
        }
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

    private void outdateQueue(final AbstractJobQueue queue) {
        // remove the queue with the old name
        this.queues.remove(queue.getName());
        // check if we can close or have to rename
        queue.markForRemoval();
        if ( queue.isMarkedForRemoval() ) {
            // close
            queue.close();
            // copy statistics
            this.baseStatistics.add(queue);
            // update mbeans
            eventAdmin.sendEvent(new QueueStatusEvent(null, queue));
        } else {
            if ( !queue.getName().contains("<outdated>") ) {
                // notify queue
                queue.rename(queue.getName() + "<outdated>(" + queue.hashCode() + ")");
            }
            // readd with new name
            this.queues.put(queue.getName(), queue);
            // update mbeans
            eventAdmin.sendEvent(new QueueStatusEvent(queue, queue));
        }
    }

    /**
     * @see org.apache.sling.event.impl.jobs.stats.StatisticsImpl#reset()
     * Reset this statistics and all queues.
     */
    @Override
    public synchronized void reset() {
        this.baseStatistics.reset();
        for(final AbstractJobQueue jq : this.queues.values() ) {
            jq.reset();
        }
        this.topicStatistics.clear();
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#restart()
     */
    @Override
    public void restart() {
        // let's rename/close all queues and clear them
        synchronized ( queuesLock ) {
            final List<AbstractJobQueue> queues = new ArrayList<AbstractJobQueue>(this.queues.values());
            for(final AbstractJobQueue queue : queues ) {
                queue.clear();
                this.outdateQueue(queue);
            }
        }
        // reset statistics
        this.reset();
        // and now load again
        this.backgroundLoader.restart();
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
            final String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
            final String rt = (String) event.getProperty(SlingConstants.PROPERTY_RESOURCE_TYPE);
            if ( (rt == null || ResourceHelper.RESOURCE_TYPE_JOB.equals(rt)) &&
                 this.configuration.isLocalJob(path) ) {
                this.backgroundLoader.loadJob(path);
            }
        } else if ( ResourceHelper.BUNDLE_EVENT_STARTED.equals(event.getTopic())
                 || ResourceHelper.BUNDLE_EVENT_UPDATED.equals(event.getTopic()) ) {
            this.backgroundLoader.tryToReloadUnloadedJobs();
        } else {
            if ( EventUtil.isLocal(event) ) {
                // job notifications
                final String topic = (String)event.getProperty(JobUtil.NOTIFICATION_PROPERTY_JOB_TOPIC);
                if ( topic != null ) { // this is just a sanity check
                    TopicStatisticsImpl ts = (TopicStatisticsImpl)this.topicStatistics.get(topic);
                    if ( ts == null ) {
                        this.topicStatistics.putIfAbsent(topic, new TopicStatisticsImpl(topic));
                        ts = (TopicStatisticsImpl)this.topicStatistics.get(topic);
                    }
                    if ( event.getTopic().equals(JobUtil.TOPIC_JOB_CANCELLED) ) {
                        ts.addCancelled();
                    } else if ( event.getTopic().equals(JobUtil.TOPIC_JOB_FAILED) ) {
                        ts.addFailed();
                    } else if ( event.getTopic().equals(JobUtil.TOPIC_JOB_FINISHED) ) {
                        final Long time = (Long)event.getProperty(Utility.PROPERTY_TIME);
                        ts.addFinished(time == null ? -1 : time);
                    } else if ( event.getTopic().equals(JobUtil.TOPIC_JOB_STARTED) ) {
                        final Long time = (Long)event.getProperty(Utility.PROPERTY_TIME);
                        ts.addActivated(time == null ? -1 : time);
                    }
                }
            }
        }
    }

    /**
     * Read a job
     */
    JobImpl readJob(final Resource resource) {
        JobImpl job = null;
        if ( resource != null ) {
            try {
                final ValueMap vm = ResourceHelper.getValueMap(resource);

                // check job topic and job id
                final String errorMessage = Utility.checkJobTopic(vm.get(JobUtil.PROPERTY_JOB_TOPIC));
                final String jobId = vm.get(JobUtil.JOB_ID, String.class);
                if ( errorMessage == null && jobId != null ) {
                    final String topic = vm.get(JobUtil.PROPERTY_JOB_TOPIC, String.class);
                    final Map<String, Object> jobProperties = ResourceHelper.cloneValueMap(vm);

                    jobProperties.put(JobImpl.PROPERTY_RESOURCE_PATH, resource.getPath());
                    // convert to integers (JCR supports only long...)
                    jobProperties.put(Job.PROPERTY_JOB_RETRIES, vm.get(Job.PROPERTY_JOB_RETRIES, Integer.class));
                    jobProperties.put(Job.PROPERTY_JOB_RETRY_COUNT, vm.get(Job.PROPERTY_JOB_RETRY_COUNT, Integer.class));
                    jobProperties.put(Job.PROPERTY_JOB_PRIORITY, JobPriority.valueOf(vm.get(Job.PROPERTY_JOB_PRIORITY, JobPriority.NORM.name())));

                    @SuppressWarnings("unchecked")
                    final List<Exception> readErrorList = (List<Exception>) jobProperties.get(ResourceHelper.PROPERTY_MARKER_READ_ERROR_LIST);
                    if ( readErrorList != null ) {
                        for(final Exception e : readErrorList) {
                            logger.warn("Unable to read job from " + resource.getPath(), e);
                        }
                    }
                    job = new JobImpl(topic,
                            (String)jobProperties.get(JobUtil.PROPERTY_JOB_NAME),
                            jobId,
                            jobProperties);
                } else {
                    if ( errorMessage != null ) {
                        logger.warn("{} : {}", errorMessage, resource.getPath());
                    } else if ( jobId == null ) {
                        logger.warn("Discarding job - no job id found : {}", resource.getPath());
                    }
                    // remove the job as the topic is invalid anyway
                    try {
                        resource.getResourceResolver().delete(resource);
                        resource.getResourceResolver().commit();
                    } catch ( final PersistenceException ignore) {
                        this.ignoreException(ignore);
                    }
                }
            } catch (final InstantiationException ie) {
                // something happened with the resource in the meantime
                this.ignoreException(ie);
            }

        }
        return job;
    }

    private void stopProcessing() {
        this.backgroundLoader.stop();

        // let's rename/close all queues and clear them
        synchronized ( queuesLock ) {
            final List<AbstractJobQueue> queues = new ArrayList<AbstractJobQueue>(this.queues.values());
            for(final AbstractJobQueue queue : queues ) {
                queue.clear();
                this.outdateQueue(queue);
            }
        }

        // deactivate old capabilities - this stops all background processes
        if ( this.topologyCapabilities != null ) {
            this.topologyCapabilities.deactivate();
        }
        this.topologyCapabilities = null;
    }

    private void startProcessing(final TopologyView view) {
        // create new capabilities and update view
        this.topologyCapabilities = new TopologyCapabilities(view, this.configuration.disableDistribution());

        this.backgroundLoader.start();
    }

    /**
     * @see org.apache.sling.discovery.TopologyEventListener#handleTopologyEvent(org.apache.sling.discovery.TopologyEvent)
     */
    @Override
    public void handleTopologyEvent(final TopologyEvent event) {
        this.logger.info("Received topology event {}", event);

        // check if there is a change of properties which doesn't affect us
        if ( event.getType() == Type.PROPERTIES_CHANGED ) {
            final Map<String, String> newAllInstances = TopologyCapabilities.getAllInstancesMap(event.getNewView());
            if ( this.topologyCapabilities != null && this.topologyCapabilities.isSame(newAllInstances) ) {
                logger.info("No changes in capabilities - ignoring event");
                return;
            }
        }

        if ( event.getType() == Type.TOPOLOGY_CHANGING ) {
           this.stopProcessing();

        } else if ( event.getType() == Type.TOPOLOGY_INIT
            || event.getType() == Type.TOPOLOGY_CHANGED
            || event.getType() == Type.PROPERTIES_CHANGED ) {

            this.stopProcessing();

            this.startProcessing(event.getNewView());
        }
    }

    /**
     * Return our internal statistics object.
     *
     * @see org.apache.sling.event.jobs.JobManager#getStatistics()
     */
    @Override
    public synchronized Statistics getStatistics() {
        this.copyFrom(this.baseStatistics);
        for(final AbstractJobQueue jq : this.queues.values() ) {
            this.add(jq);
        }

        return this;
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#getTopicStatistics()
     */
    @Override
    public Iterable<TopicStatistics> getTopicStatistics() {
        return topicStatistics.values();
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#getQueue(java.lang.String)
     */
    @Override
    public Queue getQueue(final String name) {
        return this.queues.get(name);
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#getQueues()
     */
    @Override
    public Iterable<Queue> getQueues() {
        final Iterator<AbstractJobQueue> jqI = this.queues.values().iterator();
        return new Iterable<Queue>() {

            @Override
            public Iterator<Queue> iterator() {
                return new Iterator<Queue>() {

                    @Override
                    public boolean hasNext() {
                        return jqI.hasNext();
                    }

                    @Override
                    public Queue next() {
                        return jqI.next();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
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
        logger.debug("Trying to remove job {}", jobId);
        boolean result = true;
        final Job job = this.getJobById(jobId);
        logger.debug("Found removal job: {}", job);
        if ( job != null ) {
            // currently running?
            if ( job.getProcessingStarted() != null ) {
                logger.debug("Unable to remove job - job is started: {}", job);
                result = false;
            } else {
                ResourceResolver resolver = null;
                try {
                    resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
                    final Resource jobResource = resolver.getResource(((JobImpl)job).getResourcePath());
                    if ( jobResource != null ) {
                        resolver.delete(jobResource);
                        resolver.commit();
                        logger.debug("Removed job with id: {}", jobId);
                    } else {
                        logger.debug("Unable to remove job with id - resource already removed: {}", jobId);
                    }
                } catch ( final LoginException le ) {
                    this.ignoreException(le);
                    result = false;
                } catch ( final PersistenceException pe) {
                    this.ignoreException(pe);
                    result = false;
                } finally {
                    if ( resolver != null ) {
                        resolver.close();
                    }
                }
            }
        }
        return result;
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#forceRemoveJob(java.lang.String)
     */
    @Override
    public void forceRemoveJob(final String jobId) {
        this.removeJobById(jobId);
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#addJob(java.lang.String, java.lang.String, java.util.Map)
     */
    @Override
    public Job addJob(final String topic, final String name, final Map<String, Object> properties) {
        final String errorMessage = Utility.checkJobTopic(topic);
        if ( errorMessage != null ) {
            logger.warn("{}", errorMessage);
            return null;
        }
        Job result = this.addJobInteral(topic, name, properties);
        if ( result == null && name != null ) {
            result = this.getJobByName(name);
        }
        return result;
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#getJobByName(java.lang.String)
     */
    @Override
    public Job getJobByName(final String name) {
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);

            final StringBuilder buf = new StringBuilder(64);

            buf.append("//element(*,");
            buf.append(ResourceHelper.RESOURCE_TYPE_JOB);
            buf.append(")[@");
            buf.append(ISO9075.encode(JobUtil.PROPERTY_JOB_NAME));
            buf.append(" = '");
            buf.append(name);
            buf.append("']");
            final Iterator<Resource> result = resolver.findResources(buf.toString(), "xpath");

            while ( result.hasNext() ) {
                final Resource jobResource = result.next();
                // sanity check for the path
                if ( this.configuration.isJob(jobResource.getPath()) ) {
                    final JobImpl job = this.readJob(jobResource);
                    if ( job != null ) {
                        return job;
                    }
                }
            }
        } catch (final QuerySyntaxException qse) {
            this.ignoreException(qse);
        } catch (final LoginException le) {
            this.ignoreException(le);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
        return null;
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#getJobById(java.lang.String)
     */
    @Override
    public Job getJobById(final String id) {
        logger.debug("Getting job by id: {}", id);
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);

            final StringBuilder buf = new StringBuilder(64);

            buf.append("//element(*,");
            buf.append(ResourceHelper.RESOURCE_TYPE_JOB);
            buf.append(")[@");
            buf.append(JobUtil.JOB_ID);
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
                    final JobImpl job = this.readJob(jobResource);
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
        } catch (final LoginException le) {
            this.ignoreException(le);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
        logger.debug("Job not found with id: {}", id);
        return null;
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#getJob(java.lang.String, java.util.Map)
     */
    @Override
    public Job getJob(final String topic, final Map<String, Object> template) {
        final Iterable<Job> iter = this.findJobs(QueryType.ALL, topic, 1, template);
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
    public boolean removeJobById(String jobId) {
        boolean result = true;
        final Job job = this.getJobById(jobId);
        if ( job != null ) {
            ResourceResolver resolver = null;
            try {
                resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
                final Resource jobResource = resolver.getResource(((JobImpl)job).getResourcePath());
                if ( jobResource != null ) {
                    resolver.delete(jobResource);
                    resolver.commit();
                }
            } catch ( final LoginException le ) {
                this.ignoreException(le);
                result = false;
            } catch ( final PersistenceException pe) {
                this.ignoreException(pe);
                result = false;
            } finally {
                if ( resolver != null ) {
                    resolver.close();
                }
            }
        }
        return result;
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#findJobs(org.apache.sling.event.jobs.JobManager.QueryType, java.lang.String, long, java.util.Map<java.lang.String,java.lang.Object>[])
     */
    @Override
    public Collection<Job> findJobs(final QueryType type,
            final String topic,
            final long limit,
            final Map<String, Object>... templates) {
        final List<Job> result = new ArrayList<Job>();
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);

            final StringBuilder buf = new StringBuilder(64);

            buf.append("//element(*,");
            buf.append(ResourceHelper.RESOURCE_TYPE_JOB);
            buf.append(")[@");
            buf.append(ISO9075.encode(JobUtil.PROPERTY_JOB_TOPIC));
            buf.append(" = '");
            buf.append(topic);
            buf.append("'");
            if ( type == QueryType.ACTIVE ) {
                buf.append(" and @");
                buf.append(ISO9075.encode(Job.PROPERTY_JOB_STARTED_TIME));
            } else if ( type == QueryType.QUEUED ) {
                buf.append(" and not(@");
                buf.append(ISO9075.encode(Job.PROPERTY_JOB_STARTED_TIME));
                buf.append(")");
            }
            if ( templates != null && templates.length > 0 ) {
                buf.append(" and (");
                int index = 0;
                for (final Map<String,Object> template : templates) {
                    if ( index > 0 ) {
                        buf.append(" or ");
                    }
                    buf.append('(');
                    final Iterator<Map.Entry<String, Object>> i = template.entrySet().iterator();
                    boolean first = true;
                    while ( i.hasNext() ) {
                        final Map.Entry<String, Object> current = i.next();
                        final String propName = ISO9075.encode(current.getKey());
                        if ( first ) {
                            first = false;
                            buf.append('@');
                        } else {
                            buf.append(" and @");
                        }
                        buf.append(propName);
                        buf.append(" = '");
                        buf.append(current.getValue());
                        buf.append("'");
                    }
                    buf.append(')');
                    index++;
                }
                buf.append(')');
            }
            buf.append("] order by @");
            buf.append(Job.PROPERTY_JOB_CREATED);
            buf.append(" ascending");
            final Iterator<Resource> iter = resolver.findResources(buf.toString(), "xpath");
            long count = 0;

            while ( iter.hasNext() && (limit < 1 || count < limit) ) {
                final Resource jobResource = iter.next();
                // sanity check for the path
                if ( this.configuration.isJob(jobResource.getPath()) ) {
                    final JobImpl job = readJob(jobResource);
                    if ( job != null ) {
                        count++;
                        result.add(job);
                    }
                }
             }
        } catch (final QuerySyntaxException qse) {
            this.ignoreException(qse);
        } catch (final LoginException le) {
            this.ignoreException(le);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
        return result;
    }

    public void finished(final JobHandler info) {
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
            final Resource jobResource = resolver.getResource(info.getJob().getResourcePath());
            if ( jobResource != null ) {
                try {
                    resolver.delete(jobResource);
                    resolver.commit();
                } catch ( final PersistenceException pe ) {
                    // ignore
                }
            }
        } catch ( final LoginException ignore ) {
            // ignore
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
    }

    /**
     * Reschedule a job.
     *
     * Update the retry count and remove the started time.
     * @param info The job info
     * @return true if the job could be updated.
     */
    public boolean reschedule(final JobHandler info) {
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
            final Resource jobResource = resolver.getResource(info.getJob().getResourcePath());
            if ( jobResource != null ) {
                final ModifiableValueMap mvm = jobResource.adaptTo(ModifiableValueMap.class);
                mvm.put(Job.PROPERTY_JOB_RETRY_COUNT, info.getJob().getProperty(Job.PROPERTY_JOB_RETRY_COUNT));
                mvm.remove(Job.PROPERTY_JOB_STARTED_TIME);
                try {
                    resolver.commit();
                    return true;
                } catch ( final PersistenceException pe ) {
                    // ignore
                }
            }
        } catch ( final LoginException ignore ) {
            // ignore
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }

        return false;
    }

    /**
     * Remove the job.
     * @param info
     * @return
     */
    public boolean remove(final JobHandler info) {
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
            final Resource jobResource = resolver.getResource(info.getJob().getResourcePath());
            if ( jobResource != null ) {
                Utility.sendNotification(this.eventAdmin, JobUtil.TOPIC_JOB_CANCELLED, info.getJob(), null);
                try {
                    resolver.delete(jobResource);
                    resolver.commit();
                } catch ( final PersistenceException pe ) {
                    // ignore
                }
            }
        } catch ( final LoginException ignore ) {
            // ignore
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }

        return true;
    }

    /**
     * Try to start the job
     */
    public boolean start(final JobHandler info) {
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
            final Resource jobResource = resolver.getResource(info.getJob().getResourcePath());
            if ( jobResource != null ) {
                final ModifiableValueMap mvm = jobResource.adaptTo(ModifiableValueMap.class);
                mvm.put(Job.PROPERTY_JOB_STARTED_TIME, Calendar.getInstance());
                mvm.put(Job.PROPERTY_JOB_QUEUE_NAME, info.getJob().getQueueName());
                mvm.put(Job.PROPERTY_JOB_RETRIES, info.getJob().getNumberOfRetries());
                mvm.put(Job.PROPERTY_JOB_PRIORITY, info.getJob().getJobPriority().name());
                resolver.commit();

                return true;
            }
        } catch ( final PersistenceException ignore ) {
            this.ignoreException(ignore);
        } catch ( final LoginException ignore ) {
            this.ignoreException(ignore);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }

        return false;
    }

    /**
     * Try to get a "lock" for a resource
     */
    private boolean lock(final String id) {
        if ( logger.isDebugEnabled() ) {
            logger.debug("Trying to get lock for {}", id);
        }
        boolean hasLock = false;
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
            final StringBuilder sb = new StringBuilder(this.configuration.getLocksPath());
            sb.append('/');
            sb.append(ResourceHelper.filterName(id));
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

                    final ValueMap vm = lockResource.adaptTo(ValueMap.class);
                    if ( logger.isDebugEnabled() ) {
                        logger.debug("Got lock resource on instance {} with {}", Environment.APPLICATION_ID, vm.get(Utility.PROPERTY_LOCK_CREATED_APP));
                    }
                    if ( vm.get(Utility.PROPERTY_LOCK_CREATED_APP).equals(Environment.APPLICATION_ID) ) {
                        hasLock = true;
                    }
                } catch (final PersistenceException ignore) {
                    // ignore
                    this.ignoreException(ignore);
                }
            }
        } catch (final LoginException ignore) {
            this.ignoreException(ignore);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
        if ( logger.isDebugEnabled() ) {
            logger.debug("Lock for {} = {}", id, hasLock);
        }
        return hasLock;
    }



    private Job addJobInteral(final String jobTopic, final String jobName, final Map<String, Object> jobProperties) {
        final QueueInfo info = this.queueConfigManager.getQueueInfo(jobTopic);
        if ( info.queueConfiguration.getType() == QueueConfiguration.Type.DROP ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug("Dropping job due to configuration of queue {} : {}", info.queueName, Utility.toString(jobTopic, jobName, jobProperties));
            }
            Utility.sendNotification(this.eventAdmin, JobUtil.TOPIC_JOB_CANCELLED, jobTopic, jobName, jobProperties, null);
        } else {
            // check for unique jobs
            if ( jobName != null && !this.lock(jobName) ) {
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
                ResourceResolver resolver = null;
                try {
                    resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);

                    return this.writeJob(resolver,
                            jobTopic,
                            jobName,
                            jobProperties,
                            info);
                } catch (final PersistenceException re ) {
                    // something went wrong, so let's log it
                    this.logger.error("Exception during persisting new job '" + Utility.toString(jobTopic, jobName, jobProperties) + "'", re);
                } catch (final LoginException le) {
                    // there is nothing we can do except log!
                    this.logger.error("Exception during persisting new job '" + Utility.toString(jobTopic, jobName, jobProperties) + "'", le);
                } finally {
                    if ( resolver != null ) {
                        resolver.close();
                    }
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
    private Job writeJob(final ResourceResolver resolver,
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

        properties.put(JobUtil.JOB_ID, jobId);
        properties.put(JobUtil.PROPERTY_JOB_TOPIC, jobTopic);
        if ( jobName != null ) {
            properties.put(JobUtil.PROPERTY_JOB_NAME, jobName);
        }
        properties.put(Job.PROPERTY_JOB_QUEUE_NAME, info.queueConfiguration.getName());
        properties.put(Job.PROPERTY_JOB_RETRY_COUNT, 0);
        properties.put(Job.PROPERTY_JOB_RETRIES, info.queueConfiguration.getMaxRetries());
        properties.put(Job.PROPERTY_JOB_PRIORITY, info.queueConfiguration.getPriority().name());

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

        // update property types - priority and create job
        properties.put(Job.PROPERTY_JOB_PRIORITY, info.queueConfiguration.getPriority());
        return new JobImpl(jobTopic, jobName, jobId, properties);
    }

    public void reassign(final JobHandler handler) {
        final JobImpl job = handler.getJob();
        final QueueInfo queueInfo = queueConfigManager.getQueueInfo(job.getTopic());
        final InternalQueueConfiguration config = queueInfo.queueConfiguration;

        // Sanity check if queue configuration has changed
        if ( config.getType() == QueueConfiguration.Type.DROP ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug("Dropping job due to configuration of queue {} : {}", queueInfo.queueName, Utility.toString(handler.getJob()));
            }
            handler.remove();
        } else {
            String targetId = null;
            if ( config.getType() != QueueConfiguration.Type.IGNORE ) {
                final TopologyCapabilities caps = this.topologyCapabilities;
                targetId = (caps == null ? null : caps.detectTarget(job.getTopic(), job.getProperties(), queueInfo));
            }
            this.maintenanceTask.reassignJob(handler.getJob(), targetId);
        }
    }
}
