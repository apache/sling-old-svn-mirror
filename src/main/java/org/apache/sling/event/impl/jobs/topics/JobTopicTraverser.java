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
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.impl.jobs.Utility;
import org.slf4j.Logger;

public class JobTopicTraverser {

    public interface Handler {
        boolean handle(final JobImpl job);
    }

    public static void traverse(final Logger logger,
            final Resource topicResource,
            final Handler handler) {
        logger.debug("Processing topic {}", topicResource.getName());
        // now years
        for(final Resource yearResource: Utility.getSortedChildren(logger, "year", topicResource)) {
            final int year = Integer.valueOf(yearResource.getName());
            logger.debug("Processing year {}", year);

            // now months
            for(final Resource monthResource: Utility.getSortedChildren(logger, "month", yearResource)) {
                final int month = Integer.valueOf(monthResource.getName());
                logger.debug("Processing month {}", month);

                // now days
                for(final Resource dayResource: Utility.getSortedChildren(logger, "day", monthResource)) {
                    final int day = Integer.valueOf(dayResource.getName());
                    logger.debug("Processing day {}", day);

                    // now hours
                    for(final Resource hourResource: Utility.getSortedChildren(logger, "hour", dayResource)) {
                        final int hour = Integer.valueOf(hourResource.getName());
                        logger.debug("Processing hour {}", hour);

                        // now minutes
                        for(final Resource minuteResource: Utility.getSortedChildren(logger, "minute", hourResource)) {
                            final int minute = Integer.valueOf(minuteResource.getName());
                            logger.debug("Processing minute {}", minute);

                            // now jobs
                            final List<JobImpl> jobs = new ArrayList<JobImpl>();
                            final Iterator<Resource> jobIter = minuteResource.listChildren();
                            while ( jobIter.hasNext() ) {
                                final Resource jobResource = jobIter.next();

                                final JobImpl job = Utility.readJob(logger, jobResource);
                                if ( job != null ) {
                                    logger.debug("Found job {}", jobResource.getName());
                                    jobs.add(job);
                                }
                            }

                            Collections.sort(jobs);

                            boolean stop = false;
                            for(final JobImpl job : jobs) {
                                if ( !handler.handle(job) ) {
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
