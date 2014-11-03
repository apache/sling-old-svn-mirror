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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;

/**
 * The job topic traverser is an utility class to traverse all jobs
 * of a specific topic in order of creation.
 *
 * The traverser can be used with two different callbacks,
 * the resource callback is called with a resource object,
 * the job callback with a job object created from the
 * resource.
 */
public class JobTopicTraverser {

    /**
     * Callback called for each found job.
     */
    public interface JobCallback {

        /**
         * Callback handle for a job.
         * If the callback signals to stop traversing, the current minute is still
         * processed completely (to ensure correct ordering of jobs).
         * @param job The job to handle
         * @return <code>true</code> If processing should continue, <code>false</code> otherwise.
         */
        boolean handle(final JobImpl job);
    }

    /**
     * Callback called for each found resource.
     */
    public interface ResourceCallback {

        /**
         * Callback handle for a resource.
         * The callback is called in sorted order on a minute base, all resources within a minute
         * are not necessarily called in correct time order!
         * If the callback signals to stop traversing, the traversal is stopped
         * immediately.
         * @param rsrc The resource to handle
         * @return <code>true</code> If processing should continue, <code>false</code> otherwise.
         */
        boolean handle(final Resource rsrc);
    }

    /**
     * Traverse the topic and call the callback for each found job.
     *
     * Once the callback notifies to stop traversing by returning false, the current minute
     * will be processed completely (to ensure correct ordering of jobs) and then the
     * traversal stops.
     *
     * @param logger        The logger to use for debug logging
     * @param topicResource The topic resource
     * @param handler       The callback
     */
    public static void traverse(final Logger logger,
            final Resource topicResource,
            final JobCallback handler) {
        traverse(logger, topicResource, handler, null);
    }

    /**
     * Traverse the topic and call the callback for each found resource.
     *
     * Once the callback notifies to stop traversing by returning false, the
     * traversal stops.
     *
     * @param logger        The logger to use for debug logging
     * @param topicResource The topic resource
     * @param handler       The callback
     */
    public static void traverse(final Logger logger,
            final Resource topicResource,
            final ResourceCallback handler) {
        traverse(logger, topicResource, null, handler);
    }

    /**
     * Internal method for traversal
     * @param logger        The logger to use for debug logging
     * @param topicResource The topic resource
     * @param jobHandler    The job callback
     * @param resourceHandler    The resource callback
     */
    private static void traverse(final Logger logger,
            final Resource topicResource,
            final JobCallback jobHandler,
            final ResourceCallback resourceHandler) {
        logger.debug("Processing topic {}", topicResource.getName().replace('.', '/'));
        // now years
        for(final Resource yearResource: Utility.getSortedChildren(logger, "year", topicResource)) {
            logger.debug("Processing year {}", yearResource.getName());

            // now months
            for(final Resource monthResource: Utility.getSortedChildren(logger, "month", yearResource)) {
                logger.debug("Processing month {}", monthResource.getName());

                // now days
                for(final Resource dayResource: Utility.getSortedChildren(logger, "day", monthResource)) {
                    logger.debug("Processing day {}", dayResource.getName());

                    // now hours
                    for(final Resource hourResource: Utility.getSortedChildren(logger, "hour", dayResource)) {
                        logger.debug("Processing hour {}", hourResource.getName());

                        // now minutes
                        for(final Resource minuteResource: Utility.getSortedChildren(logger, "minute", hourResource)) {
                            logger.debug("Processing minute {}", minuteResource.getName());

                            // now jobs
                            final List<JobImpl> jobs = new ArrayList<JobImpl>();
                            // we use an iterator to skip removed entries
                            // see SLING-4073
                            final Iterator<Resource> jobIter = minuteResource.listChildren();
                            while ( jobIter.hasNext() ) {
                                final Resource jobResource = jobIter.next();
                                if ( resourceHandler != null ) {
                                    if ( !resourceHandler.handle(jobResource) ) {
                                        return;
                                    }
                                } else {
                                    final JobImpl job = Utility.readJob(logger, jobResource);
                                    if ( job != null ) {
                                        logger.debug("Found job {}", jobResource.getName());
                                        jobs.add(job);
                                    }
                                }
                            }

                            if ( jobHandler != null ) {
                                Collections.sort(jobs);

                                boolean stop = false;
                                for(final JobImpl job : jobs) {
                                    if ( !jobHandler.handle(job) ) {
                                        stop = true;
                                    }
                                }
                                if ( stop ) {
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
