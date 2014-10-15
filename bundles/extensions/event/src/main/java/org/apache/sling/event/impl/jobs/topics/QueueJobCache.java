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
package org.apache.sling.event.impl.jobs.topics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.impl.jobs.JobManagerConfiguration;
import org.apache.sling.event.impl.jobs.TestLogger;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager.QueueInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO - note last scan time and not all new observation events to avoid unnecessary rescan
 */
public class QueueJobCache {

    /** Logger. */
    private final Logger logger = new TestLogger(LoggerFactory.getLogger(this.getClass()));

    /** The maximum of pre loaded jobs for a topic. */
    private final int maxPreloadLimit = 10;

    private final JobManagerConfiguration configuration;

    private final Set<String> topics;

    private final Map<String, List<JobImpl>> cache = new HashMap<String, List<JobImpl>>();

    private final QueueInfo info;

    public QueueJobCache(final JobManagerConfiguration configuration,
            final QueueInfo info,
            final Set<String> topics) {
        this.configuration = configuration;
        this.info = info;
        this.topics = topics;
        for(final String topic : topics) {
            this.cache.put(topic, new ArrayList<JobImpl>());
        }
    }

    public QueueInfo getQueueInfo() {
        return this.info;
    }

    public Set<String> getTopics() {
        return this.topics;
    }

    /**
     * Get the next job - this method is not called concurrently
     * TODO This is very expensive atm
     */
    public JobImpl getNextJob() {
        JobImpl result = null;

        // check state of cache
        this.loadJobs();

        final List<JobImpl> allJobs = new ArrayList<JobImpl>();
        for(final Map.Entry<String, List<JobImpl>> entry : this.cache.entrySet()) {
            allJobs.addAll(entry.getValue());
        }
        Collections.sort(allJobs);
        if ( allJobs.size() > 0 ) {
            result = allJobs.get(0);
        }
        return result;
    }

    /**
     * Load the next N x numberOf(topics) jobs
     */
    private void loadJobs() {
        logger.debug("Starting jobs loading...");

        ResourceResolver resolver = null;
        try {
            for(final String topic : this.topics) {
                final List<JobImpl> list = this.cache.get(topic);
                if ( list.size() < this.maxPreloadLimit ) {
                    list.clear();
                    if ( resolver == null ) {
                        resolver = this.configuration.createResourceResolver();
                    }

                    final Resource baseResource = resolver.getResource(this.configuration.getLocalJobsPath());

                    // sanity check - should never be null
                    if ( baseResource != null ) {
                        final Resource topicResource = baseResource.getChild(topic.replace('/', '.'));
                        if ( topicResource != null ) {
                            loadJobs(topic, topicResource);
                        }
                    }
                }
            }
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
        logger.debug("Finished jobs loading");
    }

    /**
     * Load the next N x numberOf(topics) jobs
     */
    private void loadJobs(final String topic, final Resource topicResource) {
        logger.debug("Loading jobs from topic {}", topic);
        final List<JobImpl> result = this.cache.get(topic);

        JobTopicTraverser.traverse(logger, topicResource, new JobTopicTraverser.Handler() {

            @Override
            public boolean handle(final JobImpl job) {
                if ( job.getProcessingStarted() == null && !job.hasReadErrors() ) {
                    result.add(job);
                } else {
                    logger.debug("Discarding job because {} or {}", job.getProcessingStarted(), job.hasReadErrors());
                }
                return result.size() < maxPreloadLimit;
            }
        });
        logger.debug("Caching {} jobs for topic {}", result.size(), topic);
    }

    public void handleNewJob(final String topic) {
        // TODO Auto-generated method stub

    }
}
