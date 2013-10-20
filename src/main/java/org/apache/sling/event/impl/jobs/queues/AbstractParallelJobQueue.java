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
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.event.impl.jobs.JobConsumerManager;
import org.apache.sling.event.impl.jobs.JobHandler;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.osgi.service.event.EventAdmin;

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
                           final JobConsumerManager jobConsumerManager,
                           final ThreadPoolManager threadPoolManager,
                           final EventAdmin eventAdmin,
                           final Scheduler scheduler) {
        super(name, config, jobConsumerManager, threadPoolManager, eventAdmin);
        this.scheduler = scheduler;
    }

    @Override
    public String getStateInfo() {
        return super.getStateInfo() + ", jobCount=" + this.jobCount;
    }

    @Override
    protected JobHandler start(final JobHandler processInfo) {
        // acquire a slot
        this.acquireSlot();

        // check if we got outdated in the meantime
        if ( this.isOutdated() ) {
            this.freeSlot();
            return null;
        }
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
    protected boolean canBeClosed() {
        boolean result = super.canBeClosed();
        if ( result ) {
            result = this.jobCount == 0;
        }
        return result;
    }

    @Override
    protected void notifyFinished(final JobHandler rescheduleInfo) {
        this.freeSlot();
    }

    @Override
    protected JobHandler reschedule(final JobHandler info) {
        // we just sleep for the delay time - if none, we continue and retry
        // this job again
        final long delay = this.getRetryDelay(info);
        if ( delay > 0 ) {
            final Date fireDate = new Date();
            fireDate.setTime(System.currentTimeMillis() + delay);

            final String jobName = "Waiting:" + queueName + ":" + info.hashCode();
            final Runnable t = new Runnable() {
                @Override
                public void run() {
                    put(info);
                }
            };
            scheduler.schedule(t, scheduler.AT(fireDate).name(jobName));
        } else {
            // put directly into queue
            put(info);
        }
        return null;
    }
}

