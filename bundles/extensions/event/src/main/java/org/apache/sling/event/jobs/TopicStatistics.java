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
 * Statistic information about a topic.
 * This information is not preserved between restarts of the service.
 * Once a service is restarted, the counters start at zero!
 * @since 3.0
 */
@ProviderType
public interface TopicStatistics {

    /**
     * The topic this statistics is about.
     */
    String getTopic();

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
}
