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
package org.apache.sling.event.jobs;

import aQute.bnd.annotation.ProviderType;

/**
 * Statistic information.
 * This information is not preserved between restarts of the service.
 * Once a service is restarted, the counters start at zero!
 * @since 3.0
 */
@ProviderType
public interface Statistics {

    /**
     * The time this service has been started
     */
    long getStartTime();

    /**
     * Number of successfully finished jobs.
     */
    long getNumberOfFinishedJobs();

    /**
     * Number of permanently failing or cancelled jobs.
     */
    long getNumberOfCancelledJobs();

    /**
     * Number of failing jobs.
     */
    long getNumberOfFailedJobs();

    /**
     * Number of already processed jobs. This adds
     * {@link #getNumberOfFinishedJobs()}, {@link #getNumberOfCancelledJobs()}
     * and {@link #getNumberOfFailedJobs()}
     */
    long getNumberOfProcessedJobs();

    /**
     * Number of jobs currently in processing.
     */
    long getNumberOfActiveJobs();

    /**
     * Number of jobs currently waiting in a queue.
     */
    long getNumberOfQueuedJobs();

    /**
     * This just adds {@link #getNumberOfActiveJobs()} and {@link #getNumberOfQueuedJobs()}
     */
    long getNumberOfJobs();

    /**
     * The time a job has been started last.
     */
    long getLastActivatedJobTime();

    /**
     * The time a job has been finished/failed/cancelled last.
     */
    long getLastFinishedJobTime();

    /**
     * The average waiting time of a job in the queue.
     */
    long getAverageWaitingTime();

    /**
     * The average processing time of a job - this only counts finished jobs.
     */
    long getAverageProcessingTime();

    /**
     * Clear all collected statistics and set the starting time to the current time.
     * Note that not all fields are cleared, last waiting time or number of active and queued
     * jobs is not cleared as these are currently used.
     */
    void reset();
}
