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
package org.apache.sling.event.impl.jobs.queues;

import java.util.Date;

import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.event.impl.EnvironmentComponent;
import org.apache.sling.event.impl.jobs.JobEvent;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.apache.sling.event.jobs.JobUtil;

/**
 * Abstract base class for a parallel processing job queue.
 */
public abstract class AbstractParallelJobQueue extends AbstractJobQueue {

    protected volatile int jobCount;

    /** The scheduler for rescheduling. */
    private final Scheduler scheduler;

    private final Object syncLock = new Object();

    public AbstractParallelJobQueue(final String name,
                           final InternalQueueConfiguration config,
                           final EnvironmentComponent env,
                           final Scheduler scheduler) {
        super(name, config, env);
        this.scheduler = scheduler;
    }

    @Override
    public String getStateInfo() {
        return super.getStateInfo() + ", jobCount=" + this.jobCount;
    }

    @Override
    protected JobEvent start(final JobEvent processInfo) {
        // acquire a slot
        this.acquireSlot();

        if ( !this.executeJob(processInfo) ) {
            this.freeSlot();
        }
        return null;
    }

    /**
     * Acquire a processing slot.
     * This method is called if the queue is not ordered.
     */
    private void acquireSlot() {
        synchronized ( this.syncLock ) {
            if ( jobCount >= this.configuration.getMaxParallel() ) {
                this.isWaiting = true;
                this.logger.debug("Job queue {} is processing {} jobs - waiting for a free slot.", this.queueName, jobCount);
                while ( this.isWaiting ) {
                    try {
                        this.syncLock.wait();
                    } catch (final InterruptedException e) {
                        this.ignoreException(e);
                    }
                }
                this.logger.debug("Job queue {} is continuing.", this.queueName);
            }
            jobCount++;
        }
    }

    /**
     * Free a slot when a job processing is finished.
     */
    private void freeSlot() {
        synchronized ( this.syncLock ) {
            jobCount--;
            if ( this.isWaiting ) {
                this.logger.debug("Notifying job queue {} to continue processing.", this.queueName);
                this.isWaiting = false;
                this.syncLock.notify();
            }
        }
    }

    @Override
    protected boolean canBeMarkedForRemoval() {
        boolean result = super.canBeMarkedForRemoval();
        if ( result ) {
            result = this.jobCount == 0;
        }
        return result;
    }

    @Override
    protected void notifyFinished(final JobEvent rescheduleInfo) {
        this.freeSlot();
    }

    @Override
    protected JobEvent reschedule(final JobEvent info) {
        // we just sleep for the delay time - if none, we continue and retry
        // this job again
        long delay = this.configuration.getRetryDelayInMs();
        if ( info.event.getProperty(JobUtil.PROPERTY_JOB_RETRY_DELAY) != null ) {
            delay = (Long)info.event.getProperty(JobUtil.PROPERTY_JOB_RETRY_DELAY);
        }
        if ( delay > 0 ) {
            final Date fireDate = new Date();
            fireDate.setTime(System.currentTimeMillis() + delay);

            final String jobName = "Waiting:" + queueName + ":" + info.hashCode();
            final Runnable t = new Runnable() {
                public void run() {
                    put(info);
                }
            };
            try {
                scheduler.fireJobAt(jobName, t, null, fireDate);
            } catch (Exception e) {
                // we ignore the exception and just put back the job in the queue
                ignoreException(e);
                t.run();
            }
        } else {
            // put directly into queue
            put(info);
        }
        return null;
    }
}

