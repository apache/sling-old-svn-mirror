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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.impl.jobs.JobConsumerManager;
import org.apache.sling.event.impl.jobs.JobHandler;
import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.impl.jobs.JobTopicTraverser;
import org.apache.sling.event.impl.jobs.Utility;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.impl.jobs.stats.StatisticsManager;
import org.apache.sling.event.jobs.Job.JobState;
import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.apache.sling.event.jobs.QueueConfiguration.Type;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The queue job cache caches jobs per queue based on the topics the queue is actively
 * processing.
 */
public class QueueJobCache {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The maximum of pre loaded jobs for a topic. */
    private final int maxPreloadLimit = 10;

    /** The job manager configuration. */
    private final JobManagerConfiguration configuration;

    /** The set of topics handled by this queue. */
    private final Set<String> topics;

    /** The set of new topics to scan. */
    private final Set<String> topicsWithNewJobs = new HashSet<String>();

    /** The cache of current objects. */
    private final List<JobImpl> cache = new ArrayList<JobImpl>();

    /** The queue type. */
    private final QueueConfiguration.Type queueType;

    /** Block the cache - for ordered queues only. */
    private final AtomicBoolean queueIsBlocked = new AtomicBoolean(false);

    /**
     * Create a new queue job cache
     * @param configuration Current job manager configuration
     * @param queueType The queue type
     * @param topics The topics handled by this queue.
     */
    public QueueJobCache(final JobManagerConfiguration configuration,
            final String queueName,
            final StatisticsManager statisticsManager,
            final QueueConfiguration.Type queueType,
            final Set<String> topics) {
        this.configuration = configuration;
        this.queueType = queueType;
        this.topics = new ConcurrentSkipListSet<String>(topics);
        this.fillCache(queueName, statisticsManager);
    }

    /**
     * All topics of this queue.
     * @return The topics.
     */
    public Set<String> getTopics() {
        return this.topics;
    }

    /**
     * Check whether there are jobs for this queue
     * @return {@code true} if there is any job outstanding.
     */
    public boolean isEmpty() {
        boolean result = true;
        synchronized ( this.cache ) {
            result = this.cache.isEmpty();
        }
        if ( result ) {
            synchronized ( this.topicsWithNewJobs ) {
                result = this.topicsWithNewJobs.isEmpty();
            }
        }
        return result;
    }

    public void setIsBlocked(final boolean value) {
        this.queueIsBlocked.set(value);
    }

    /**
     * Fill the cache.
     * No need to sync as this is called from the constructor.
     */
    private void fillCache(final String queueName, final StatisticsManager statisticsManager) {
        final Set<String> checkingTopics = new HashSet<String>();
        checkingTopics.addAll(this.topics);
        if ( !checkingTopics.isEmpty() ) {
            this.loadJobs(queueName, checkingTopics, statisticsManager);
        }
    }

    /**
     * Get the next job.
     * This method is potentially called concurrently, and
     * {@link #reschedule(JobHandler)} and {@link #handleNewTopics(Set)}
     * can be called concurrently.
     */
    public JobHandler getNextJob(final JobConsumerManager jobConsumerManager,
            final StatisticsManager statisticsManager,
            final Queue queue,
            final boolean doFull) {
        JobHandler handler = null;

        if ( !this.queueIsBlocked.get() ) {
            synchronized ( this.cache ) {
                boolean retry;
                do {
                    retry = false;
                    if ( this.cache.isEmpty() ) {
                        final Set<String> checkingTopics = new HashSet<String>();
                        synchronized ( this.topicsWithNewJobs ) {
                            checkingTopics.addAll(this.topicsWithNewJobs);
                            this.topicsWithNewJobs.clear();
                        }
                        if ( doFull ) {
                            checkingTopics.addAll(this.topics);
                        }
                        if ( !checkingTopics.isEmpty() ) {
                            this.loadJobs(queue.getName(), checkingTopics, statisticsManager);
                        }
                    }

                    if ( !this.cache.isEmpty() ) {
                        final JobImpl job = this.cache.remove(0);
                        final JobExecutor consumer = jobConsumerManager.getExecutor(job.getTopic());

                        handler = new JobHandler(job, consumer, this.configuration);
                        if ( consumer != null ) {
                            if ( !handler.startProcessing(queue) ) {
                                statisticsManager.jobDequeued(queue.getName(), handler.getJob().getTopic());
                                if ( logger.isDebugEnabled() ) {
                                    logger.debug("Discarding removed job {}", Utility.toString(job));
                                }
                                handler = null;
                                retry = true;
                            }
                        } else {
                            statisticsManager.jobDequeued(queue.getName(), handler.getJob().getTopic());
                            // no consumer on this instance, assign to another instance
                            handler.reassign();

                            handler = null;
                            retry = true;
                        }

                    }
                } while ( handler == null && retry);
            }
        }
        return handler;
    }

    /**
     * Load the next N x numberOf(topics) jobs
     * @param checkingTopics The set of topics to check.
     */
    private void loadJobs( final String queueName, final Set<String> checkingTopics,
            final StatisticsManager statisticsManager) {
        logger.debug("Starting jobs loading from {}...", checkingTopics);

        final Map<String, List<JobImpl>> topicCache = new HashMap<String, List<JobImpl>>();

        final ResourceResolver resolver = this.configuration.createResourceResolver();
        try {
            final Resource baseResource = resolver.getResource(this.configuration.getLocalJobsPath());
            // sanity check - should never be null
            if ( baseResource != null ) {
                for(final String topic : checkingTopics) {

                    final Resource topicResource = baseResource.getChild(topic.replace('/', '.'));
                    if ( topicResource != null ) {
                        topicCache.put(topic, loadJobs(queueName, topic, topicResource, statisticsManager));
                    }
                }
            }
        } finally {
            resolver.close();
        }
        orderTopics(topicCache);

        logger.debug("Finished jobs loading {}", this.cache.size());
    }

    /**
     * Order the topics based on the queue type and put them in the cache.
     * @param topicCache The topic based cache
     */
    private void orderTopics(final Map<String, List<JobImpl>> topicCache) {
        if ( this.queueType == Type.ORDERED
             || this.queueType == Type.UNORDERED) {
            for(final List<JobImpl> list : topicCache.values()) {
                this.cache.addAll(list);
            }
            Collections.sort(this.cache);
        } else {
            // topic round robin
            boolean done = true;
            do {
                done = true;
                for(final Map.Entry<String, List<JobImpl>> entry : topicCache.entrySet()) {
                    if ( !entry.getValue().isEmpty() ) {
                        this.cache.add(entry.getValue().remove(0));
                        if ( !entry.getValue().isEmpty() ) {
                            done = false;
                        }
                    }
                }
            } while ( !done ) ;
        }
    }

    /**
     * Load the next N x numberOf(topics) jobs.
     * @param topic The topic
     * @param topicResource The parent resource of the jobs
     * @return The cache which will be filled with the jobs.
     */
    private List<JobImpl> loadJobs(final String queueName, final String topic,
            final Resource topicResource,
            final StatisticsManager statisticsManager) {
        logger.debug("Loading jobs from topic {}", topic);
        final List<JobImpl> list = new ArrayList<JobImpl>();

        final AtomicBoolean scanTopic = new AtomicBoolean(false);

        JobTopicTraverser.traverse(logger, topicResource, new JobTopicTraverser.JobCallback() {

            @Override
            public boolean handle(final JobImpl job) {
                if ( job.getProcessingStarted() == null && !job.hasReadErrors() ) {
                    list.add(job);
                    statisticsManager.jobQueued(queueName, topic);
                    if ( list.size() == maxPreloadLimit ) {
                        scanTopic.set(true);
                    }
                } else if ( job.getProcessingStarted() != null ) {
                    logger.debug("Ignoring job {} - processing already started.", job);
                } else {
                    // error reading job
                    scanTopic.set(true);
                    if ( job.isReadErrorRecoverable() ) {
                        logger.debug("Ignoring job {} due to recoverable read errors.", job);
                    } else {
                        logger.debug("Failing job {} due to unrecoverable read errors.", job);
                        final JobHandler handler = new JobHandler(job, null, configuration);
                        handler.finished(JobState.ERROR, true, null);
                    }
                }
                return list.size() < maxPreloadLimit;
            }
        });
        if ( scanTopic.get() ) {
            synchronized ( this.topicsWithNewJobs ) {
                this.topicsWithNewJobs.add(topic);
            }
        }
        logger.debug("Caching {} jobs for topic {}", list.size(), topic);

        return list;
    }

    /**
     * Inform the queue cache about topics containing new jobs
     * @param topics The set of topics to scan
     */
    public void handleNewTopics(final Set<String> topics) {
        logger.debug("Update cache to handle new event for topics {}", topics);
        synchronized ( this.topicsWithNewJobs ) {
            this.topicsWithNewJobs.addAll(topics);
        }
        this.topics.addAll(topics);
    }

    /**
     * Reschedule a job
     * Reschedule the job and add it back into the cache.
     * @param handler The job handler
     */
    public void reschedule(final String queueName, final JobHandler handler, final StatisticsManager statisticsManager) {
        synchronized ( this.cache ) {
            if ( handler.reschedule() ) {
                if ( this.queueType == Type.ORDERED ) {
                    this.cache.add(0, handler.getJob());
                } else {
                    this.cache.add(handler.getJob());
                }
                statisticsManager.jobQueued(queueName, handler.getJob().getTopic());
            }
        }
    }
}
