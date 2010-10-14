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

import org.apache.sling.event.jobs.Statistics;

/**
 * Implementation of the statistics.
 */
public class StatisticsImpl implements Statistics {

    private volatile long startTime;

    private volatile long activeJobs;

    private volatile long queuedJobs;

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

    public StatisticsImpl() {
        this.startTime = System.currentTimeMillis();
    }

    public StatisticsImpl(final long startTime) {
        this.startTime = startTime;
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getStartTime()
     */
    public synchronized long getStartTime() {
        return startTime;
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getNumberOfProcessedJobs()
     */
    public synchronized long getNumberOfProcessedJobs() {
        return getNumberOfCancelledJobs() + getNumberOfFailedJobs() + getNumberOfFinishedJobs();
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getNumberOfActiveJobs()
     */
    public synchronized long getNumberOfActiveJobs() {
        return activeJobs;
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getNumberOfQueuedJobs()
     */
    public synchronized long getNumberOfQueuedJobs() {
        return queuedJobs;
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getNumberOfJobs()
     */
    public synchronized long getNumberOfJobs() {
        return activeJobs + queuedJobs;
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getAverageWaitingTime()
     */
    public synchronized long getAverageWaitingTime() {
        return averageWaitingTime;
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getAverageProcessingTime()
     */
    public synchronized long getAverageProcessingTime() {
        return averageProcessingTime;
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getNumberOfFinishedJobs()
     */
    public synchronized long getNumberOfFinishedJobs() {
        return finishedJobs;
    }

    public synchronized long getNumberOfCancelledJobs() {
        return cancelledJobs;
    }

    public synchronized long getNumberOfFailedJobs() {
        return failedJobs;
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getLastActivatedJobTime()
     */
    public synchronized long getLastActivatedJobTime() {
        return this.lastActivated;
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getLastFinishedJobTime()
     */
    public synchronized long getLastFinishedJobTime() {
        return this.lastFinished;
    }

    /**
     * Add a finished job
     */
    public synchronized void finishedJob(final long time) {
        this.lastFinished = System.currentTimeMillis();
        this.processingTime += time;
        this.processingCount++;
        this.averageProcessingTime = this.processingTime / this.processingCount;
        this.finishedJobs++;
        this.activeJobs--;
    }

    public synchronized void failedJob() {
        this.failedJobs++;
        this.activeJobs--;
        this.queuedJobs++;
    }

    public synchronized void cancelledJob() {
        this.cancelledJobs++;
        this.activeJobs--;
    }

    /**
     * New job in the qeue
     */
    public synchronized void incQueued() {
        this.queuedJobs++;
    }

    /**
     * Job not processed by us
     */
    public synchronized void decQueued() {
        this.queuedJobs--;
    }

    /**
     * Clear all queued
     */
    public synchronized void clearQueued() {
        this.queuedJobs = 0;
    }

    /**
     * Add a job from the queue to status active
     * @param time The time the job stayed in the queue.
     */
    public synchronized void addActive(final long time) {
        this.queuedJobs--;
        this.activeJobs++;
        this.waitingCount++;
        this.waitingTime += time;
        this.averageWaitingTime = this.waitingTime / this.waitingCount;
        this.lastActivated = System.currentTimeMillis();
    }

    /**
     * Add another statistics information.
     */
    public synchronized void add(final StatisticsImpl other) {
        synchronized ( other ) {
            if ( other.lastActivated > this.lastActivated ) {
                this.lastActivated = other.lastActivated;
            }
            if ( other.lastFinished > this.lastFinished ) {
                this.lastFinished = other.lastFinished;
            }
            this.queuedJobs += other.queuedJobs;
            this.waitingTime += other.waitingTime;
            this.waitingCount += other.waitingCount;
            this.averageWaitingTime = this.waitingTime / this.waitingCount;
            this.processingTime += other.processingTime;
            this.processingCount += other.processingCount;
            this.averageProcessingTime = this.processingTime / this.processingCount;
            this.finishedJobs += other.finishedJobs;
            this.failedJobs += other.failedJobs;
            this.cancelledJobs += other.cancelledJobs;
            this.activeJobs += other.activeJobs;
        }
    }

    /**
     * Create a new statistics object with exactly the same values.
     */
    public synchronized void copyFrom(final StatisticsImpl other) {
        this.queuedJobs = other.queuedJobs;
        this.lastActivated = other.lastActivated;
        this.lastFinished = other.lastFinished;
        this.averageWaitingTime = other.averageWaitingTime;
        this.averageProcessingTime = other.averageProcessingTime;
        this.waitingTime = other.waitingTime;
        this.processingTime = other.processingTime;
        this.waitingCount = other.waitingCount;
        this.processingCount = other.processingCount;
        this.finishedJobs = other.finishedJobs;
        this.failedJobs = other.failedJobs;
        this.cancelledJobs = other.cancelledJobs;
        this.activeJobs = other.activeJobs;
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#reset()
     */
    public synchronized void reset() {
        this.startTime = System.currentTimeMillis();
        this.lastActivated = -1;
        this.lastFinished = -1;
        this.averageWaitingTime = 0;
        this.averageProcessingTime = 0;
        this.waitingTime = 0;
        this.processingTime = 0;
        this.waitingCount = 0;
        this.processingCount = 0;
        this.finishedJobs = 0;
        this.failedJobs = 0;
        this.cancelledJobs = 0;
    }
}
