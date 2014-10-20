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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.impl.jobs.JobHandler;
import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.impl.jobs.JobTopicTraverser;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.apache.sling.event.jobs.QueueConfiguration.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The queue job cache caches jobs per queue based on the topics the queue is actively
 * processing.
 *
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

    /**
     * Create a new queue job cache
     * @param configuration Current job manager configuration
     * @param info The queue info
     * @param topics The topics handled by this queue.
     */
    public QueueJobCache(final JobManagerConfiguration configuration,
            final QueueConfiguration.Type queueType,
            final Set<String> topics) {
        this.configuration = configuration;
        this.queueType = queueType;
        this.topics = new ConcurrentSkipListSet<String>(topics);
        this.topicsWithNewJobs.addAll(topics);
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

    /**
     * Get the next job.
     * This method is not called concurrently, however
     * {@link #reschedule(JobImpl)} and {@link #handleNewJob(String)}
     * can be called concurrently.
     */
    public JobImpl getNextJob(final boolean doFull) {
        JobImpl result = null;

        synchronized ( this.cache ) {
            if ( this.cache.isEmpty() ) {
                final Set<String> checkingTopics = new HashSet<String>();
                synchronized ( this.topicsWithNewJobs ) {
                    if ( doFull ) {
                        logger.debug("Doing full cache update {} : {}", this.topics, this.topicsWithNewJobs);
                        checkingTopics.addAll(this.topics);
                    }
                    checkingTopics.addAll(this.topicsWithNewJobs);
                    this.topicsWithNewJobs.clear();
                }
                if ( !checkingTopics.isEmpty() ) {
                    this.loadJobs(checkingTopics);
                }
            }

            if ( !this.cache.isEmpty() ) {
                result = this.cache.remove(0);
            }
        }

        return result;
    }

    /**
     * Load the next N x numberOf(topics) jobs
     */
    private void loadJobs( final Set<String> checkingTopics) {
        logger.debug("Starting jobs loading...");

        final Map<String, List<JobImpl>> topicCache = new HashMap<String, List<JobImpl>>();

        final ResourceResolver resolver = this.configuration.createResourceResolver();
        try {
            for(final String topic : checkingTopics) {
                final Resource baseResource = resolver.getResource(this.configuration.getLocalJobsPath());

                final List<JobImpl> list = new ArrayList<JobImpl>();
                topicCache.put(topic, list);

                // sanity check - should never be null
                if ( baseResource != null ) {
                    final Resource topicResource = baseResource.getChild(topic.replace('/', '.'));
                    if ( topicResource != null ) {
                        loadJobs(topic, topicResource, list);
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
     * Load the next N x numberOf(topics) jobs
     */
    private void loadJobs(final String topic, final Resource topicResource, final List<JobImpl> list) {
        logger.debug("Loading jobs from topic {}", topic);

        JobTopicTraverser.traverse(logger, topicResource, new JobTopicTraverser.JobCallback() {

            @Override
            public boolean handle(final JobImpl job) {
                if ( job.getProcessingStarted() == null && !job.hasReadErrors() ) {
                    list.add(job);
                } else {
                    logger.debug("Discarding job because {} or {}", job.getProcessingStarted(), job.hasReadErrors());
                }
                return list.size() < maxPreloadLimit;
            }
        });
        logger.debug("Caching {} jobs for topic {}", list.size(), topic);
    }

    /**
     * Mark the topic to contain new jobs.
     * @param topic The topic
     */
    public void handleNewJob(final Set<String> topics) {
        logger.debug("Update cache to handle new event for topics {}", topics);
        synchronized ( this.topicsWithNewJobs ) {
            this.topicsWithNewJobs.addAll(topics);
        }
        this.topics.addAll(topics);
    }

    public void reschedule(final JobHandler handler) {
        synchronized ( this.cache ) {
            if ( handler.reschedule() ) {
                if ( this.queueType == Type.ORDERED ) {
                    this.cache.add(0, handler.getJob());
                } else {
                    this.cache.add(handler.getJob());
                }
            }
        }
    }
}
