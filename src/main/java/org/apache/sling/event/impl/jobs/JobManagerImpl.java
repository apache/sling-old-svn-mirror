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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.apache.sling.commons.threads.ThreadPoolManager;
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
import org.apache.sling.event.impl.jobs.jmx.QueuesMBeanImpl;
import org.apache.sling.event.impl.jobs.queues.AbstractJobQueue;
import org.apache.sling.event.impl.jobs.queues.OrderedJobQueue;
import org.apache.sling.event.impl.jobs.queues.ParallelJobQueue;
import org.apache.sling.event.impl.jobs.queues.TopicRoundRobinJobQueue;
import org.apache.sling.event.impl.jobs.stats.StatisticsImpl;
import org.apache.sling.event.impl.jobs.stats.TopicStatisticsImpl;
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
import org.apache.sling.event.jobs.consumer.JobExecutor;
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
@Component(immediate=true, metatype=true,
           label="Apache Sling Job Manager",
           description="This is the central service of the job handling.",
           name="org.apache.sling.event.impl.jobs.jcr.PersistenceHandler")
@Service(value={JobManager.class, EventHandler.class, TopologyEventListener.class, Runnable.class})
@Properties({
    @Property(name=JobManagerConfiguration.PROPERTY_DISABLE_DISTRIBUTION,
            boolValue=JobManagerConfiguration.DEFAULT_DISABLE_DISTRIBUTION,
            label="Disable Distribution",
            description="If the distribution is disabled, all jobs will be processed on the leader only! Please use this switch " +
                        "with care."),
    @Property(name=JobManagerConfiguration.PROPERTY_REPOSITORY_PATH,
             value=JobManagerConfiguration.DEFAULT_REPOSITORY_PATH, propertyPrivate=true),
    @Property(name=JobManagerConfiguration.PROPERTY_SCHEDULED_JOBS_PATH,
             value=JobManagerConfiguration.DEFAULT_SCHEDULED_JOBS_PATH, propertyPrivate=true),
    @Property(name=JobManagerConfiguration.PROPERTY_BACKGROUND_LOAD_DELAY,
             longValue=JobManagerConfiguration.DEFAULT_BACKGROUND_LOAD_DELAY, propertyPrivate=true),
    @Property(name="scheduler.period", longValue=60, propertyPrivate=true),
    @Property(name="scheduler.concurrent", boolValue=false, propertyPrivate=true),
    @Property(name=EventConstants.EVENT_TOPIC,
              value={SlingConstants.TOPIC_RESOURCE_ADDED,
                     SlingConstants.TOPIC_RESOURCE_CHANGED,
                     SlingConstants.TOPIC_RESOURCE_REMOVED,
                     "org/apache/sling/event/notification/job/*",
                     Utility.TOPIC_STOP,
                     ResourceHelper.BUNDLE_EVENT_STARTED,
                     ResourceHelper.BUNDLE_EVENT_UPDATED}, propertyPrivate=true)
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

    @Reference
    private QueuesMBean queuesMBean;

    @Reference
    private ThreadPoolManager threadPoolManager;

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

    /** Set of paths directly added as jobs - these will be ignored during observation handling. */
    private final Set<String> directlyAddedPaths = new HashSet<String>();

    /** Job Scheduler. */
    private JobSchedulerImpl jobScheduler;

    /**
     * Activate this component.
     * @param props Configuration properties
     */
    @Activate
    protected void activate(final Map<String, Object> props) throws LoginException {
        this.configuration = new JobManagerConfiguration(props);
        this.jobScheduler = new JobSchedulerImpl(this.configuration, this.resourceResolverFactory, this.scheduler, this);
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
        this.configuration.update(props);
        final TopologyCapabilities caps = this.topologyCapabilities;
        if ( caps != null ) {
            caps.update(this.configuration.disableDistribution());
        }
    }

    /**
     * Deactivate this component.
     */
    @Deactivate
    protected void deactivate() {
        logger.info("Apache Sling Job Manager stopping on instance {}", Environment.APPLICATION_ID);
        this.jobScheduler.deactivate();

        this.backgroundLoader.deactivate();
        this.backgroundLoader = null;

        this.maintenanceTask = null;
        this.configuration = null;
        final Iterator<AbstractJobQueue> i = this.queues.values().iterator();
        while ( i.hasNext() ) {
            final AbstractJobQueue jbq = i.next();
            jbq.close();
            // update mbeans
            ((QueuesMBeanImpl)queuesMBean).sendEvent(new QueueStatusEvent(null, jbq));
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
                    if ( jbq.tryToClose() ) {
                        logger.debug("Removing idle job queue {}", jbq);
                        // copy statistics
                        this.baseStatistics.add(jbq);
                        // remove
                        i.remove();
                        // update mbeans
                        ((QueuesMBeanImpl)queuesMBean).sendEvent(new QueueStatusEvent(null, jbq));
                    }
                }
            }
        }
        // invoke maintenance task
        final MaintenanceTask task = this.maintenanceTask;
        if ( task != null ) {
            task.run(this.topologyCapabilities, this.queueConfigManager, this.schedulerRuns - 1);
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
        // check if we still are able to process this job
        final JobExecutor consumer = this.jobConsumerManager.getExecutor(job.getTopic());
        boolean reassign = false;
        String reassignTargetId = null;
        if ( consumer == null && (!job.isBridgedEvent() || !this.jobConsumerManager.supportsBridgedEvents())) {
            reassign = true;
        }

        // get the queue configuration
        final QueueInfo queueInfo = queueConfigManager.getQueueInfo(job.getTopic());
        final InternalQueueConfiguration config = queueInfo.queueConfiguration;

        // Sanity check if queue configuration has changed
        if ( config.getType() == QueueConfiguration.Type.DROP ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug("Dropping job due to configuration of queue {} : {}", queueInfo.queueName, Utility.toString(job));
            }
            this.finishJob(job, Job.JobState.DROPPED, false, -1);
        } else if ( config.getType() == QueueConfiguration.Type.IGNORE ) {
            if ( !reassign ) {
                if ( logger.isDebugEnabled() ) {
                    logger.debug("Ignoring job due to configuration of queue {} : {}", queueInfo.queueName, Utility.toString(job));
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
                            queue = new OrderedJobQueue(queueInfo.queueName, config, this.jobConsumerManager, this.threadPoolManager, this.eventAdmin);
                        } else if ( config.getType() == QueueConfiguration.Type.UNORDERED ) {
                            queue = new ParallelJobQueue(queueInfo.queueName, config, this.jobConsumerManager, this.threadPoolManager, this.eventAdmin, this.scheduler);
                        } else if ( config.getType() == QueueConfiguration.Type.TOPIC_ROUND_ROBIN ) {
                            queue = new TopicRoundRobinJobQueue(queueInfo.queueName, config, this.jobConsumerManager, this.threadPoolManager, this.eventAdmin, this.scheduler);
                        }
                        if ( queue == null ) {
                            // this is just a sanity check, actually we can never get here
                            logger.warn("Ignoring event due to unknown queue type of queue {} : {}", queueInfo.queueName, Utility.toString(job));
                            this.finishJob(job, Job.JobState.DROPPED, false, -1);
                        } else {
                            queues.put(queueInfo.queueName, queue);
                            ((QueuesMBeanImpl)queuesMBean).sendEvent(new QueueStatusEvent(queue, null));
                            queue.start();
                        }
                    }
                }

                // and put job
                if ( queue != null ) {
                    job.updateQueueInfo(queue);
                    final JobHandler handler = new JobHandler(job, this);

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
        // check for main queue
        final String oldName = ResourceHelper.filterQueueName(queue.getName());
        this.queues.remove(oldName);
        // check if we can close or have to rename
        if ( queue.tryToClose() ) {
            // copy statistics
            this.baseStatistics.add(queue);
            // update mbeans
            ((QueuesMBeanImpl)queuesMBean).sendEvent(new QueueStatusEvent(null, queue));
        } else {
            queue.outdate();
            // readd with new name
            String newName = ResourceHelper.filterName(queue.getName());
            int index = 0;
            while ( this.queues.containsKey(newName) ) {
                newName = ResourceHelper.filterName(queue.getName()) + '$' + String.valueOf(index++);
            }
            this.queues.put(newName, queue);
            // update mbeans
            ((QueuesMBeanImpl)queuesMBean).sendEvent(new QueueStatusEvent(queue, queue));
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
                synchronized ( this.directlyAddedPaths ) {
                    if ( directlyAddedPaths.remove(path) ) {
                        return;
                    }
                }
                this.backgroundLoader.loadJob(path);
            }
            this.jobScheduler.handleEvent(event);
        } else if ( Utility.TOPIC_STOP.equals(event.getTopic()) ) {
            if ( !EventUtil.isLocal(event) ) {
                final String jobId = (String) event.getProperty(Utility.PROPERTY_ID);
                this.stopJobById(jobId, false);
            }
        } else if ( ResourceHelper.BUNDLE_EVENT_STARTED.equals(event.getTopic())
                 || ResourceHelper.BUNDLE_EVENT_UPDATED.equals(event.getTopic()) ) {
            this.backgroundLoader.tryToReloadUnloadedJobs();
            this.jobScheduler.handleEvent(event);
        } else if ( SlingConstants.TOPIC_RESOURCE_CHANGED.equals(event.getTopic())
                 || SlingConstants.TOPIC_RESOURCE_REMOVED.equals(event.getTopic()) ) {
            this.jobScheduler.handleEvent(event);
        } else {
            if ( EventUtil.isLocal(event) ) {
                // job notifications
                final String topic = (String)event.getProperty(NotificationConstants.NOTIFICATION_PROPERTY_JOB_TOPIC);
                if ( topic != null ) { // this is just a sanity check
                    TopicStatisticsImpl ts = (TopicStatisticsImpl)this.topicStatistics.get(topic);
                    if ( ts == null ) {
                        this.topicStatistics.putIfAbsent(topic, new TopicStatisticsImpl(topic));
                        ts = (TopicStatisticsImpl)this.topicStatistics.get(topic);
                    }
                    if ( event.getTopic().equals(NotificationConstants.TOPIC_JOB_CANCELLED) ) {
                        ts.addCancelled();
                    } else if ( event.getTopic().equals(NotificationConstants.TOPIC_JOB_FAILED) ) {
                        ts.addFailed();
                    } else if ( event.getTopic().equals(NotificationConstants.TOPIC_JOB_FINISHED) ) {
                        final Long time = (Long)event.getProperty(Utility.PROPERTY_TIME);
                        ts.addFinished(time == null ? -1 : time);
                    } else if ( event.getTopic().equals(NotificationConstants.TOPIC_JOB_STARTED) ) {
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
                final String errorMessage = Utility.checkJobTopic(vm.get(ResourceHelper.PROPERTY_JOB_TOPIC));
                final String jobId = vm.get(ResourceHelper.PROPERTY_JOB_ID, String.class);
                if ( errorMessage == null && jobId != null ) {
                    final String topic = vm.get(ResourceHelper.PROPERTY_JOB_TOPIC, String.class);
                    final Map<String, Object> jobProperties = ResourceHelper.cloneValueMap(vm);

                    jobProperties.put(JobImpl.PROPERTY_RESOURCE_PATH, resource.getPath());
                    // convert to integers (JCR supports only long...)
                    jobProperties.put(Job.PROPERTY_JOB_RETRIES, vm.get(Job.PROPERTY_JOB_RETRIES, Integer.class));
                    jobProperties.put(Job.PROPERTY_JOB_RETRY_COUNT, vm.get(Job.PROPERTY_JOB_RETRY_COUNT, Integer.class));
                    if ( vm.get(Job.PROPERTY_JOB_PROGRESS_STEPS) != null ) {
                        jobProperties.put(Job.PROPERTY_JOB_PROGRESS_STEPS, vm.get(Job.PROPERTY_JOB_PROGRESS_STEPS, Integer.class));
                    }
                    if ( vm.get(Job.PROPERTY_JOB_PROGRESS_STEP) != null ) {
                        jobProperties.put(Job.PROPERTY_JOB_PROGRESS_STEP, vm.get(Job.PROPERTY_JOB_PROGRESS_STEP, Integer.class));
                    }
                    @SuppressWarnings("unchecked")
                    final List<Exception> readErrorList = (List<Exception>) jobProperties.get(ResourceHelper.PROPERTY_MARKER_READ_ERROR_LIST);
                    if ( readErrorList != null ) {
                        for(final Exception e : readErrorList) {
                            logger.warn("Unable to read job from " + resource.getPath(), e);
                        }
                    }
                    job = new JobImpl(topic,
                            (String)jobProperties.get(ResourceHelper.PROPERTY_JOB_NAME),
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
        this.jobScheduler.handleTopologyEvent(event);
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
        return this.queues.get(ResourceHelper.filterQueueName(name));
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
                    ResourceResolver resolver = null;
                    try {
                        resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
                        final Resource jobResource = resolver.getResource(job.getResourcePath());
                        if ( jobResource != null ) {
                            resolver.delete(jobResource);
                            resolver.commit();
                            logger.debug("Removed job with id: {}", jobId);
                        } else {
                            logger.debug("Unable to remove job with id - resource already removed: {}", jobId);
                        }
                        Utility.sendNotification(this.eventAdmin, NotificationConstants.TOPIC_JOB_REMOVED, job, null);
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
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);

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
        @SuppressWarnings("unchecked")
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
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);

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
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
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
        } catch (final LoginException ignore) {
            this.ignoreException(ignore);
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
     * @param job The job
     * @return true if the job could be updated.
     */
    public boolean reschedule(final JobImpl job) {
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
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
     * Try to get a "lock" for a resource
     */
    private boolean lock(final String jobTopic, final String id) {
        if ( logger.isDebugEnabled() ) {
            logger.debug("Trying to get lock for {}", id);
        }
        boolean hasLock = false;
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
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
        final QueueInfo info = this.queueConfigManager.getQueueInfo(jobTopic);
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
                ResourceResolver resolver = null;
                try {
                    resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);

                    final JobImpl job = this.writeJob(resolver,
                            jobTopic,
                            jobName,
                            jobProperties,
                            info);
                    if ( job != null ) {
                        if ( configuration.isLocalJob(job.getResourcePath()) ) {
                            this.backgroundLoader.addJob(job);
                        }
                        return job;
                    }
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
        synchronized ( this.directlyAddedPaths ) {
            this.directlyAddedPaths.add(path);
        }
        ResourceHelper.getOrCreateResource(resolver,
                path,
                properties);

        // update property types - priority, add path and create job
        properties.put(JobImpl.PROPERTY_RESOURCE_PATH, path);
        return new JobImpl(jobTopic, jobName, jobId, properties);
    }

    public void reassign(final JobImpl job) {
        final QueueInfo queueInfo = queueConfigManager.getQueueInfo(job.getTopic());
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
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
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
            final QueueInfo queueInfo = queueConfigManager.getQueueInfo(job.getTopic());
            final AbstractJobQueue queue;
            synchronized ( queuesLock ) {
                queue = this.queues.get(queueInfo.queueName);
            }
            boolean stopped = false;
            if ( queue != null ) {
                stopped = queue.stopJob(job);
            }
            if ( forward && !stopped ) {
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

    public JobManagerConfiguration getConfiguration() {
        return this.configuration;
    }
}
