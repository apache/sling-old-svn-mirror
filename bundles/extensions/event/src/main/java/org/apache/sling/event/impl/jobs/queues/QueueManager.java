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
package org.apache.sling.event.impl.jobs.queues;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.event.impl.EventingThreadPool;
import org.apache.sling.event.impl.jobs.JobConsumerManager;
import org.apache.sling.event.impl.jobs.JobHandler;
import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.impl.jobs.config.ConfigurationChangeListener;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager.QueueInfo;
import org.apache.sling.event.impl.jobs.jmx.QueueStatusEvent;
import org.apache.sling.event.impl.jobs.jmx.QueuesMBeanImpl;
import org.apache.sling.event.impl.jobs.stats.StatisticsManager;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.NotificationConstants;
import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.jmx.QueuesMBean;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of the queue manager.
 */
@Component(immediate=true)
@Service(value={Runnable.class, QueueManager.class, EventHandler.class})
@Properties({
    @Property(name=Scheduler.PROPERTY_SCHEDULER_PERIOD, longValue=60),
    @Property(name=Scheduler.PROPERTY_SCHEDULER_CONCURRENT, boolValue=false),
    @Property(name=EventConstants.EVENT_TOPIC, value=NotificationConstants.TOPIC_JOB_ADDED)
})
public class QueueManager
    implements Runnable, EventHandler, ConfigurationChangeListener {

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

    /**
     * Our thread pool.
     */
    @Reference(referenceInterface=EventingThreadPool.class)
    private ThreadPool threadPool;

    /** The job manager configuration. */
    @Reference
    private JobManagerConfiguration configuration;

    @Reference
    private StatisticsManager statisticsManager;

    /** Lock object for the queues map - we don't want to sync directly on the concurrent map. */
    private final Object queuesLock = new Object();

    /** All active queues. */
    private final Map<String, JobQueueImpl> queues = new ConcurrentHashMap<String, JobQueueImpl>();

    /** We count the scheduler runs. */
    private volatile long schedulerRuns;

    /** Flag whether the manager is active or suspended. */
    private final AtomicBoolean isActive = new AtomicBoolean(false);

    /** The queue services. */
    private volatile QueueServices queueServices;

    /**
     * Activate this component.
     * @param props Configuration properties
     */
    @Activate
    protected void activate(final Map<String, Object> props) {
        logger.info("Apache Sling Queue Manager started on instance {}", Environment.APPLICATION_ID);
        this.queueServices = new QueueServices();
        queueServices.configuration = this.configuration;
        queueServices.eventAdmin = this.eventAdmin;
        queueServices.jobConsumerManager = this.jobConsumerManager;
        queueServices.scheduler = this.scheduler;
        queueServices.threadPoolManager = this.threadPoolManager;
        queueServices.statisticsManager = statisticsManager;
        queueServices.eventingThreadPool = this.threadPool;
        this.configuration.addListener(this);
    }

    /**
     * Deactivate this component.
     */
    @Deactivate
    protected void deactivate() {
        logger.debug("Apache Sling Queue Manager stopping on instance {}", Environment.APPLICATION_ID);

        this.configuration.removeListener(this);
        final Iterator<JobQueueImpl> i = this.queues.values().iterator();
        while ( i.hasNext() ) {
            final JobQueueImpl jbq = i.next();
            jbq.close();
            // update mbeans
            ((QueuesMBeanImpl)queuesMBean).sendEvent(new QueueStatusEvent(null, jbq));
        }
        this.queues.clear();
        this.queueServices = null;
        logger.info("Apache Sling Queue Manager stopped on instance {}", Environment.APPLICATION_ID);
    }

    /**
     * This method is invoked periodically by the scheduler.
     * It searches for idle queues and stops them after a timeout. If a queue
     * is idle for two consecutive clean up calls, it is removed.
     * @see java.lang.Runnable#run()
     */
    private void maintain() {
        this.schedulerRuns++;
        logger.debug("Queue manager maintenance: Starting #{}", this.schedulerRuns);

        // queue maintenance
        if ( this.isActive.get() ) {
            for(final JobQueueImpl jbq : this.queues.values() ) {
                jbq.maintain();
            }
        }

        // full topic scan is done every third run
        if ( schedulerRuns % 3 == 0 && this.isActive.get() ) {
            this.fullTopicScan();
        }

        // we only do a full clean up on every fifth run
        final boolean doFullCleanUp = (schedulerRuns % 5 == 0);

        if ( doFullCleanUp ) {
            // check for idle queue
            logger.debug("Checking for idle queues...");

           // we synchronize to avoid creating a queue which is about to be removed during cleanup
            synchronized ( queuesLock ) {
                final Iterator<Map.Entry<String, JobQueueImpl>> i = this.queues.entrySet().iterator();
                while ( i.hasNext() ) {
                    final Map.Entry<String, JobQueueImpl> current = i.next();
                    final JobQueueImpl jbq = current.getValue();
                    if ( jbq.tryToClose() ) {
                        logger.debug("Removing idle job queue {}", jbq);
                        // remove
                        i.remove();
                        // update mbeans
                        ((QueuesMBeanImpl)queuesMBean).sendEvent(new QueueStatusEvent(null, jbq));
                    }
                }
            }
        }
        logger.debug("Queue manager maintenance: Finished #{}", this.schedulerRuns);
    }

    /**
     * Start a new queue
     * This method first searches the corresponding queue - if such a queue
     * does not exist yet, it is created and started.
     *
     * @param queueInfo The queue info
     * @param topics The topics
     */
    private void start(final QueueInfo queueInfo,
                       final Set<String> topics) {
        final InternalQueueConfiguration config = queueInfo.queueConfiguration;
        // get or create queue
        boolean isNewQueue = false;
        JobQueueImpl queue = null;
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
                queue = JobQueueImpl.createQueue(queueInfo.queueName, config, queueServices, topics);
                // on startup the queue might be empty and we get null back from createQueue
                if ( queue != null ) {
                    isNewQueue = true;
                    queues.put(queueInfo.queueName, queue);
                    ((QueuesMBeanImpl)queuesMBean).sendEvent(new QueueStatusEvent(queue, null));
                }
            }
        }
        if ( queue != null ) {
            if ( !isNewQueue ) {
                queue.wakeUpQueue(topics);
            }
            queue.startJobs();
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

    private void outdateQueue(final JobQueueImpl queue) {
        // remove the queue with the old name
        // check for main queue
        final String oldName = ResourceHelper.filterQueueName(queue.getName());
        this.queues.remove(oldName);
        // check if we can close or have to rename
        if ( queue.tryToClose() ) {
            // copy statistics
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
     * Outdate all queues.
     */
    private void restart() {
        // let's rename/close all queues and clear them
        synchronized ( queuesLock ) {
            final List<JobQueueImpl> queues = new ArrayList<JobQueueImpl>(this.queues.values());
            for(final JobQueueImpl queue : queues ) {
                this.outdateQueue(queue);
            }
        }
        // check if we're still active
        final JobManagerConfiguration config = this.configuration;
        if ( config != null ) {
            final List<Job> rescheduleList = this.configuration.clearJobRetryList();
            for(final Job j : rescheduleList) {
                final JobHandler jh = new JobHandler((JobImpl)j, null, this.configuration);
                jh.reschedule();
            }
        }
    }

    /**
     * @param name The queue name
     * @return The queue or {@code null}.
     * @see org.apache.sling.event.jobs.JobManager#getQueue(java.lang.String)
     */
    public Queue getQueue(final String name) {
        return this.queues.get(name);
    }

    /**
     * @return An iterator for the available queues.
     * @see org.apache.sling.event.jobs.JobManager#getQueues()
     */
    public Iterable<Queue> getQueues() {
        final Iterator<JobQueueImpl> jqI = this.queues.values().iterator();
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

    /**
     * This method is called whenever the topology or queue configurations change.
     * @param active Whether the job handling is active atm.
     */
    @Override
    public void configurationChanged(final boolean active) {
        // are we still active?
        if ( this.configuration != null ) {
            logger.debug("Topology changed {}", active);
            this.isActive.set(active);
            if ( active ) {
                fullTopicScan();
            } else {
                this.restart();
            }
        }
    }

    private void fullTopicScan() {
        logger.debug("Scanning repository for existing topics...");
        final Set<String> topics = this.scanTopics();
        final Map<QueueInfo, Set<String>> mapping = this.updateTopicMapping(topics);
        // start queues
        for(final Map.Entry<QueueInfo, Set<String>> entry : mapping.entrySet() ) {
            this.start(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Scan the resource tree for topics.
     */
    private Set<String> scanTopics() {
        final Set<String> topics = new HashSet<String>();

        final ResourceResolver resolver = this.configuration.createResourceResolver();
        try {
            final Resource baseResource = resolver.getResource(this.configuration.getLocalJobsPath());

            // sanity check - should never be null
            if ( baseResource != null ) {
                final Iterator<Resource> topicIter = baseResource.listChildren();
                while ( topicIter.hasNext() ) {
                    final Resource topicResource = topicIter.next();
                    final String topic = topicResource.getName().replace('.', '/');
                    logger.debug("Found topic {}", topic);
                    topics.add(topic);
                }
            }
        } finally {
            resolver.close();
        }
        return topics;
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    @Override
    public void handleEvent(final Event event) {
        final String topic = (String)event.getProperty(NotificationConstants.NOTIFICATION_PROPERTY_JOB_TOPIC);
        if ( this.isActive.get() && topic != null ) {
            final QueueInfo info = this.configuration.getQueueConfigurationManager().getQueueInfo(topic);
            this.start(info, Collections.singleton(topic));
        }
    }

    /**
     * Get the latest mapping from queue name to topics
     */
    private Map<QueueInfo, Set<String>> updateTopicMapping(final Set<String> topics) {
        final Map<QueueInfo, Set<String>> mapping = new HashMap<QueueConfigurationManager.QueueInfo, Set<String>>();
        for(final String topic : topics) {
            final QueueInfo queueInfo = this.configuration.getQueueConfigurationManager().getQueueInfo(topic);
            Set<String> queueTopics = mapping.get(queueInfo);
            if ( queueTopics == null ) {
                queueTopics = new HashSet<String>();
                mapping.put(queueInfo, queueTopics);
            }
            queueTopics.add(topic);
        }

        this.logger.debug("Established new topic mapping: {}", mapping);
        return mapping;
    }

    protected void bindThreadPool(final EventingThreadPool etp) {
        this.threadPool = etp;
    }

    protected void unbindThreadPool(final EventingThreadPool etp) {
        if ( this.threadPool == etp ) {
            this.threadPool = null;
        }
    }
}
