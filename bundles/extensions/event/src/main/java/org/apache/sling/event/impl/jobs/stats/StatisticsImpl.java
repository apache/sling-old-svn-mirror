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
    @Override
    public synchronized long getStartTime() {
        return startTime;
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getNumberOfProcessedJobs()
     */
    @Override
    public synchronized long getNumberOfProcessedJobs() {
        return getNumberOfCancelledJobs() + getNumberOfFailedJobs() + getNumberOfFinishedJobs();
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getNumberOfActiveJobs()
     */
    @Override
    public synchronized long getNumberOfActiveJobs() {
        return activeJobs;
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getNumberOfQueuedJobs()
     */
    @Override
    public synchronized long getNumberOfQueuedJobs() {
        return queuedJobs;
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getNumberOfJobs()
     */
    @Override
    public synchronized long getNumberOfJobs() {
        return activeJobs + queuedJobs;
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getAverageWaitingTime()
     */
    @Override
    public synchronized long getAverageWaitingTime() {
        return averageWaitingTime;
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getAverageProcessingTime()
     */
    @Override
    public synchronized long getAverageProcessingTime() {
        return averageProcessingTime;
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getNumberOfFinishedJobs()
     */
    @Override
    public synchronized long getNumberOfFinishedJobs() {
        return finishedJobs;
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getNumberOfCancelledJobs()
     */
    @Override
    public synchronized long getNumberOfCancelledJobs() {
        return cancelledJobs;
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getNumberOfFailedJobs()
     */
    @Override
    public synchronized long getNumberOfFailedJobs() {
        return failedJobs;
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getLastActivatedJobTime()
     */
    @Override
    public synchronized long getLastActivatedJobTime() {
        return this.lastActivated;
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#getLastFinishedJobTime()
     */
    @Override
    public synchronized long getLastFinishedJobTime() {
        return this.lastFinished;
    }

    /**
     * Add a finished job
     * @param jobTime The processing time for this job.
     */
    public synchronized void finishedJob(final long jobTime) {
        this.lastFinished = System.currentTimeMillis();
        this.processingTime += jobTime;
        this.processingCount++;
        this.averageProcessingTime = this.processingTime / this.processingCount;
        this.finishedJobs++;
        this.activeJobs--;
    }

    /**
     * Add a failed job.
     */
    public synchronized void failedJob() {
        this.failedJobs++;
        this.activeJobs--;
        this.queuedJobs++;
    }

    /**
     * Add a cancelled job.
     */
    public synchronized void cancelledJob() {
        this.cancelledJobs++;
        this.activeJobs--;
    }

    /**
     * New job in the queue
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
     * @param queueTime The time the job stayed in the queue.
     */
    public synchronized void addActive(final long queueTime) {
        this.queuedJobs--;
        this.activeJobs++;
        this.waitingCount++;
        this.waitingTime += queueTime;
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
            if ( this.waitingCount > 0 ) {
                this.averageWaitingTime = this.waitingTime / this.waitingCount;
            }
            this.processingTime += other.processingTime;
            this.processingCount += other.processingCount;
            if ( this.processingCount > 0 ) {
                this.averageProcessingTime = this.processingTime / this.processingCount;
            }
            this.finishedJobs += other.finishedJobs;
            this.failedJobs += other.failedJobs;
            this.cancelledJobs += other.cancelledJobs;
            this.activeJobs += other.activeJobs;
        }
    }

    /**
     * Create a new statistics object with exactly the same values.
     */
    public void copyFrom(final StatisticsImpl other) {
        final long localQueuedJobs;
        final long localLastActivated;
        final long localLastFinished;
        final long localAverageWaitingTime;
        final long localAverageProcessingTime;
        final long localWaitingTime;
        final long localProcessingTime;
        final long localWaitingCount;
        final long localProcessingCount;
        final long localFinishedJobs;
        final long localFailedJobs;
        final long localCancelledJobs;
        final long localActiveJobs;
        synchronized ( other ) {
            localQueuedJobs = other.queuedJobs;
            localLastActivated = other.lastActivated;
            localLastFinished = other.lastFinished;
            localAverageWaitingTime = other.averageWaitingTime;
            localAverageProcessingTime = other.averageProcessingTime;
            localWaitingTime = other.waitingTime;
            localProcessingTime = other.processingTime;
            localWaitingCount = other.waitingCount;
            localProcessingCount = other.processingCount;
            localFinishedJobs = other.finishedJobs;
            localFailedJobs = other.failedJobs;
            localCancelledJobs = other.cancelledJobs;
            localActiveJobs = other.activeJobs;
        }
        synchronized ( this ) {
            this.queuedJobs = localQueuedJobs;
            this.lastActivated = localLastActivated;
            this.lastFinished = localLastFinished;
            this.averageWaitingTime = localAverageWaitingTime;
            this.averageProcessingTime = localAverageProcessingTime;
            this.waitingTime = localWaitingTime;
            this.processingTime = localProcessingTime;
            this.waitingCount = localWaitingCount;
            this.processingCount = localProcessingCount;
            this.finishedJobs = localFinishedJobs;
            this.failedJobs = localFailedJobs;
            this.cancelledJobs = localCancelledJobs;
            this.activeJobs = localActiveJobs;
        }
    }

    /**
     * @see org.apache.sling.event.jobs.Statistics#reset()
     */
    @Override
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
