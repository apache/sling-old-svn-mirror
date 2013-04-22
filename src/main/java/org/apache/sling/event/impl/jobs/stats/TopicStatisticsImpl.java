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
package org.apache.sling.event.impl.jobs.stats;

import org.apache.sling.event.jobs.TopicStatistics;

/**
 * Implementation of the statistics.
 */
public class TopicStatisticsImpl implements TopicStatistics {

    private final String topic;

    private volatile long lastActivated = -1;

    private volatile long lastFinished = -1;

    private volatile long averageWaitingTime;

    private volatile long averageProcessingTime;

    private volatile long waitingTime;

    private volatile long processingTime;

    private volatile long waitingCount;

    private volatile long processingCount;

    private volatile long finishedJobs;

    private volatile long failedJobs;

    private volatile long cancelledJobs;

    /** Constructor. */
    public TopicStatisticsImpl(final String topic) {
        this.topic = topic;
    }

    /**
     * @see org.apache.sling.event.jobs.TopicStatistics#getTopic()
     */
    public String getTopic() {
        return this.topic;
    }

    /**
     * @see org.apache.sling.event.jobs.TopicStatistics#getNumberOfProcessedJobs()
     */
    public synchronized long getNumberOfProcessedJobs() {
        return getNumberOfCancelledJobs() + getNumberOfFailedJobs() + getNumberOfFinishedJobs();
    }

    /**
     * @see org.apache.sling.event.jobs.TopicStatistics#getAverageWaitingTime()
     */
    public synchronized long getAverageWaitingTime() {
        return averageWaitingTime;
    }

    /**
     * @see org.apache.sling.event.jobs.TopicStatistics#getAverageProcessingTime()
     */
    public synchronized long getAverageProcessingTime() {
        return averageProcessingTime;
    }

    /**
     * @see org.apache.sling.event.jobs.TopicStatistics#getNumberOfFinishedJobs()
     */
    public synchronized long getNumberOfFinishedJobs() {
        return finishedJobs;
    }

    /**
     * @see org.apache.sling.event.jobs.TopicStatistics#getNumberOfCancelledJobs()
     */
    public synchronized long getNumberOfCancelledJobs() {
        return cancelledJobs;
    }

    /**
     * @see org.apache.sling.event.jobs.TopicStatistics#getNumberOfFailedJobs()
     */
    public synchronized long getNumberOfFailedJobs() {
        return failedJobs;
    }

    /**
     * @see org.apache.sling.event.jobs.TopicStatistics#getLastActivatedJobTime()
     */
    public synchronized long getLastActivatedJobTime() {
        return this.lastActivated;
    }

    /**
     * @see org.apache.sling.event.jobs.TopicStatistics#getLastFinishedJobTime()
     */
    public synchronized long getLastFinishedJobTime() {
        return this.lastFinished;
    }

    /**
     * Add a finished job.
     * @param jobTime The time of the job processing.
     */
    public synchronized void addFinished(final long jobTime) {
        this.finishedJobs++;
        this.lastFinished = System.currentTimeMillis();
        if ( jobTime != -1 ) {
            this.processingTime += jobTime;
            this.processingCount++;
            this.averageProcessingTime = this.processingTime / this.processingCount;
        }
    }

    /**
     * Add a started job.
     * @param queueTime The time of the job in the queue.
     */
    public synchronized void addActivated(final long queueTime) {
        this.lastActivated = System.currentTimeMillis();
        if ( queueTime != -1 ) {
            this.waitingTime += queueTime;
            this.waitingCount++;
            this.averageWaitingTime = this.waitingTime / this.waitingCount;
        }
    }

    /**
     * Add a failed job.
     */
    public synchronized void addFailed() {
        this.failedJobs++;
    }

    /**
     * Add a cancelled job.
     */
    public synchronized void addCancelled() {
        this.cancelledJobs++;
    }
}
