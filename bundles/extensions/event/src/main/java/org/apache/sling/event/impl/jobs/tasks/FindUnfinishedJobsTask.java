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
package org.apache.sling.event.impl.jobs.tasks;

import java.util.Calendar;
import java.util.Iterator;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.impl.jobs.JobTopicTraverser;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.jobs.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This task is executed when the job handling starts.
 * It checks for unfinished jobs from a previous start and corrects their state.
 */
public class FindUnfinishedJobsTask {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Job manager configuration. */
    private final JobManagerConfiguration configuration;

    /**
     * Constructor
     * @param config the configuration
     */
    public FindUnfinishedJobsTask(final JobManagerConfiguration config) {
        this.configuration = config;
    }

    public void run() {
        this.initialScan();
    }

    /**
     * Scan the resource tree for unfinished jobs from previous runs
     */
    private void initialScan() {
        logger.debug("Scanning repository for unfinished jobs...");
        final ResourceResolver resolver = configuration.createResourceResolver();
        if ( resolver == null ) {
            return;
        }
        try {
            final Resource baseResource = resolver.getResource(configuration.getLocalJobsPath());

            // sanity check - should never be null
            if ( baseResource != null ) {
                final Iterator<Resource> topicIter = baseResource.listChildren();
                while ( topicIter.hasNext() ) {
                    final Resource topicResource = topicIter.next();
                    logger.debug("Found topic {}", topicResource.getName());

                    // init topic
                    initTopic(topicResource);
                }
            }
        } finally {
            resolver.close();
        }
    }

    /**
     * Initialize a topic and update all jobs from that topic.
     * Reset started time and increase retry count of unfinished jobs
     * @param topicResource The topic resource
     */
    private void initTopic(final Resource topicResource) {
        logger.debug("Initializing topic {}...", topicResource.getName());

        JobTopicTraverser.traverse(logger, topicResource, new JobTopicTraverser.JobCallback() {

            @Override
            public boolean handle(final JobImpl job) {
                if ( job.getProcessingStarted() != null ) {
                    logger.debug("Found unfinished job {}", job.getId());
                    job.retry();
                    try {
                        final Resource jobResource = topicResource.getResourceResolver().getResource(job.getResourcePath());
                        // sanity check
                        if ( jobResource != null ) {
                            final ModifiableValueMap mvm = jobResource.adaptTo(ModifiableValueMap.class);
                            mvm.remove(Job.PROPERTY_JOB_STARTED_TIME);
                            mvm.put(Job.PROPERTY_JOB_RETRY_COUNT, job.getRetryCount());
                            if ( job.getProperty(JobImpl.PROPERTY_JOB_QUEUED, Calendar.class) == null) {
                                mvm.put(JobImpl.PROPERTY_JOB_QUEUED, Calendar.getInstance());
                            }
                            jobResource.getResourceResolver().commit();
                        }
                    } catch ( final PersistenceException ignore) {
                        logger.error("Unable to update unfinished job " + job, ignore);
                    }
                } else if ( job.getProperty(JobImpl.PROPERTY_JOB_QUEUED, Calendar.class) == null) {
                    logger.debug("Found job without queued date {}", job.getId());
                    try {
                        final Resource jobResource = topicResource.getResourceResolver().getResource(job.getResourcePath());
                        // sanity check
                        if ( jobResource != null ) {
                            final ModifiableValueMap mvm = jobResource.adaptTo(ModifiableValueMap.class);
                            mvm.put(JobImpl.PROPERTY_JOB_QUEUED, Calendar.getInstance());
                            jobResource.getResourceResolver().commit();
                        }
                    } catch ( final PersistenceException ignore) {
                        logger.error("Unable to update queued date for job " + job.getId(), ignore);
                    }
                }

                return true;
            }
        });
        logger.debug("Topic {} initialized", topicResource.getName());
    }
}
