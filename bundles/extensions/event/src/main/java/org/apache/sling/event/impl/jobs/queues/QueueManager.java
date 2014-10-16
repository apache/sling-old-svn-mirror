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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.event.impl.jobs.JobConsumerManager;
import org.apache.sling.event.impl.jobs.JobManagerConfiguration;
import org.apache.sling.event.impl.jobs.TestLogger;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager.QueueInfo;
import org.apache.sling.event.impl.jobs.jmx.QueueStatusEvent;
import org.apache.sling.event.impl.jobs.jmx.QueuesMBeanImpl;
import org.apache.sling.event.impl.jobs.stats.StatisticsManager;
import org.apache.sling.event.impl.jobs.topics.TopicManager;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.apache.sling.event.jobs.jmx.QueuesMBean;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of the job manager.
 */
@Component(immediate=true)
@Service(value={Runnable.class, QueueManager.class})
@Properties({
    @Property(name="scheduler.period", longValue=60, propertyPrivate=true),
    @Property(name="scheduler.concurrent", boolValue=false, propertyPrivate=true)
})
public class QueueManager
    implements Runnable {

    /** Default logger. */
    private final Logger logger = new TestLogger(LoggerFactory.getLogger(this.getClass()));

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
    private QueueConfigurationManager queueManager;

    /** Lock object for the queues map - we don't want to sync directly on the concurrent map. */
    private final Object queuesLock = new Object();

    /** All active queues. */
    private final Map<String, AbstractJobQueue> queues = new ConcurrentHashMap<String, AbstractJobQueue>();

    /** We count the scheduler runs. */
    private volatile long schedulerRuns;

    /**
     * Activate this component.
     * @param props Configuration properties
     */
    @Activate
    protected void activate(final Map<String, Object> props) {
        logger.info("Apache Sling Queue Manager started on instance {}", Environment.APPLICATION_ID);
    }

    /**
     * Deactivate this component.
     */
    @Deactivate
    protected void deactivate() {
        logger.debug("Apache Sling Queue Manager stopping on instance {}", Environment.APPLICATION_ID);

        final Iterator<AbstractJobQueue> i = this.queues.values().iterator();
        while ( i.hasNext() ) {
            final AbstractJobQueue jbq = i.next();
            jbq.close();
            // update mbeans
            ((QueuesMBeanImpl)queuesMBean).sendEvent(new QueueStatusEvent(null, jbq));
        }
        this.queues.clear();
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
     * @param topic The topic
     */
    public void start(final TopicManager topicManager, final QueueInfo queueInfo) {
        final InternalQueueConfiguration config = queueInfo.queueConfiguration;
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
                final QueueServices services = new QueueServices();
                services.eventAdmin = this.eventAdmin;
                services.jobConsumerManager = this.jobConsumerManager;
                services.scheduler = this.scheduler;
                services.threadPoolManager = this.threadPoolManager;
                services.topicManager = topicManager;
                services.statisticsManager = statisticsManager;
                if ( config.getType() == QueueConfiguration.Type.ORDERED ) {
                    queue = new OrderedJobQueue(queueInfo.queueName, config, services);
                } else if ( config.getType() == QueueConfiguration.Type.UNORDERED ) {
                    queue = new ParallelJobQueue(queueInfo.queueName, config, services);
                } else if ( config.getType() == QueueConfiguration.Type.TOPIC_ROUND_ROBIN ) {
                    queue = new ParallelJobQueue(queueInfo.queueName, config, services);
                }
                // this is just a sanity check, actually we always have a queue instance here
                if ( queue != null ) {
                    queues.put(queueInfo.queueName, queue);
                    ((QueuesMBeanImpl)queuesMBean).sendEvent(new QueueStatusEvent(queue, null));
                    queue.start();
                }
            }
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

    private void outdateQueue(final AbstractJobQueue queue) {
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
     * @see org.apache.sling.event.jobs.JobManager#restart()
     */
    public void restart() {
        // let's rename/close all queues and clear them
        synchronized ( queuesLock ) {
            final List<AbstractJobQueue> queues = new ArrayList<AbstractJobQueue>(this.queues.values());
            for(final AbstractJobQueue queue : queues ) {
                queue.clear();
                this.outdateQueue(queue);
            }
        }
    }

    private void stopProcessing() {
        // let's rename/close all queues and clear them
        synchronized ( queuesLock ) {
            final List<AbstractJobQueue> queues = new ArrayList<AbstractJobQueue>(this.queues.values());
            for(final AbstractJobQueue queue : queues ) {
                queue.clear();
                this.outdateQueue(queue);
            }
        }
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#getQueue(java.lang.String)
     */
    public Queue getQueue(final String name) {
        return this.queues.get(name);
    }

    /**
     * @see org.apache.sling.event.jobs.JobManager#getQueues()
     */
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
}
