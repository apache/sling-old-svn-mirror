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
package org.apache.sling.event.impl.job;

import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.impl.AbstractRepositoryEventHandler.EventInfo;
import org.osgi.service.event.Event;
import org.slf4j.Logger;

/**
 * The job blocking queue extends the blocking queue by some
 * functionality for the job event handling.
 */
public final class JobBlockingQueue extends LinkedBlockingQueue<EventInfo> {

    private static final long serialVersionUID = -1874643704782461425L;

    private volatile EventInfo eventInfo;

    private final Object lock = new Object();

    private volatile boolean isWaiting = false;

    private volatile boolean markForCleanUp = false;

    private volatile boolean finished = false;

    private volatile boolean isSleeping = false;

    private volatile String schedulerJobName;
    private volatile Thread sleepingThread;

    /** The queue name. */
    private final String queueName;

    /** The logger. */
    private final Logger logger;

    /** Ordered Queue? */
    private final boolean orderedQueue;

    private volatile int jobCount;

    public JobBlockingQueue(final String name,
                            final boolean orderedQueue,
                            final Logger logger) {
        this.queueName = name;
        this.orderedQueue = orderedQueue;
        this.logger = logger;
    }

    /**
     * Wait for the job to be finished.
     * This is called if the queue is ordered.
     */
    public EventInfo waitForFinish() throws InterruptedException {
        this.isWaiting = true;
        this.markForCleanUp = false;
        this.logger.debug("Job queue {} is waiting for finish.", this.queueName);
        while ( this.isWaiting ) {
            this.lock.wait();
        }
        this.logger.debug("Job queue {} is continuing.", this.queueName);
        final EventInfo object = this.eventInfo;
        this.eventInfo = null;
        return object;
    }

    /**
     * Get the name of the job queue.
     */
    public String getName() {
        return this.queueName;
    }

    /**
     * Mark this queue for cleanup.
     */
    public void markForCleanUp() {
        if ( !this.isWaiting && jobCount == 0 ) {
            this.markForCleanUp = true;
        }
    }

    /**
     * Acquire a processing slot.
     * This method is called if the queue is not ordered.
     * @param maxJobs
     */
    public void acquireSlot(final int maxJobs) throws InterruptedException {
        if ( jobCount >= maxJobs ) {
            this.isWaiting = true;
            this.markForCleanUp = false;
            this.logger.debug("Job queue {} is processing {} job - waiting for a free slot.", this.queueName, jobCount);
            while ( this.isWaiting ) {
                this.lock.wait();
            }
            this.logger.debug("Job queue {} is continuing.", this.queueName);
        }
        jobCount++;
    }

    /**
     * Free a slot when a job processing is finished.
     */
    public void freeSlot() {
        jobCount--;
        if ( this.isWaiting ) {
            this.logger.debug("Notifying job queue {} to continue processing.", this.queueName);
            this.isWaiting = false;
            this.lock.notify();
        }
    }

    /**
     * Check if this queue is marked for cleanup
     */
    public boolean isMarkedForCleanUp() {
        return !this.isWaiting && this.markForCleanUp;
    }

    /**
     * Notify a finished job - for ordered queues
     */
    public void notifyFinish(EventInfo i) {
        this.eventInfo = i;
        this.logger.debug("Notifying job queue {} to continue processing.", this.queueName);
        this.isWaiting = false;
        this.lock.notify();
    }

    /**
     * Return the lock for this queue.
     */
    public Object getLock() {
        return lock;
    }

    public boolean isWaiting() {
        return this.isWaiting;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean flag) {
        this.finished = flag;
    }

    public void setSleeping(boolean flag) {
        this.isSleeping = flag;
        if ( !flag ) {
            this.schedulerJobName = null;
            this.sleepingThread = null;
        }
    }

    public void setSleeping(String schedulerJobName) {
        this.schedulerJobName = schedulerJobName;
        this.setSleeping(true);
    }

    public void setSleeping(Thread sleepingThread) {
        this.sleepingThread = sleepingThread;
        this.setSleeping(true);
    }

    public String getSchedulerJobName() {
        return this.schedulerJobName;
    }

    public Thread getSleepingThread() {
        return this.sleepingThread;
    }

    public boolean isSleeping() {
        return this.isSleeping;
    }

    /**
     * Is this a ordered queue?
     */
    public boolean isOrdered() {
        return orderedQueue;
    }

    /**
     * Reschedule a job.
     * If this is a ordered queue, this method will return the event info
     * which should be processed next. Otherwise null is returned.
     */
    public EventInfo reschedule(final EventInfo info, final Scheduler scheduler) {
        final Event job = info.event;
        // is this an ordered queue?
        if ( this.orderedQueue ) {
            // we just sleep for the delay time - if none, we continue and retry
            // this job again
            if ( job.getProperty(EventUtil.PROPERTY_JOB_RETRY_DELAY) != null ) {
                final long delay = (Long)job.getProperty(EventUtil.PROPERTY_JOB_RETRY_DELAY);
                setSleeping(Thread.currentThread());
                try {
                    this.logger.debug("Job queue {} is sleeping for {}ms.", this.queueName, delay);
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    this.ignoreException(e);
                } finally {
                    setSleeping(false);
                }
            }
            return info;
        }
        if ( job.getProperty(EventUtil.PROPERTY_JOB_RETRY_DELAY) != null ) {
            final long delay = (Long)job.getProperty(EventUtil.PROPERTY_JOB_RETRY_DELAY);
            final Date fireDate = new Date();
            fireDate.setTime(System.currentTimeMillis() + delay);

            final String jobName = "Waiting:" + queueName;
            final Runnable t = new Runnable() {
                public void run() {
                    setSleeping(jobName);
                    try {
                        put(info);
                    } catch (InterruptedException e) {
                        // this should never happen
                        ignoreException(e);
                    } finally {
                        setSleeping(false);
                    }
                }
            };
            if ( scheduler != null ) {
                try {
                    scheduler.fireJobAt(jobName, t, null, fireDate);
                } catch (Exception e) {
                    // we ignore the exception and just put back the job in the queue
                    ignoreException(e);
                    t.run();
                }
            } else {
                t.run();
            }
        } else {
            // put directly into queue
            try {
                put(info);
            } catch (InterruptedException e) {
                // this should never happen
                this.ignoreException(e);
            }
        }

        return null;
    }

    /**
     * Helper method which just logs the exception in debug mode.
     * @param e
     */
    protected void ignoreException(Exception e) {
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("Ignored exception " + e.getMessage(), e);
        }
    }
}

