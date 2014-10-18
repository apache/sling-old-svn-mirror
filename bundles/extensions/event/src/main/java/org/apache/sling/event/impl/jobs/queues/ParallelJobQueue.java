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
import java.util.Set;

import org.apache.sling.event.impl.jobs.JobHandler;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;

/**
 * The default parallel job queue processing the entries FIFO.
 * Failing jobs are rescheduled and put at the end of the queue.
 */
public final class ParallelJobQueue extends AbstractJobQueue {

    private volatile int jobCount;

    private final Object syncLock = new Object();

    public ParallelJobQueue(final String name,
                           final InternalQueueConfiguration config,
                           final QueueServices services,
                           final Set<String> topics) {
        super(name, config, services, topics);
    }

    @Override
    public String getStateInfo() {
        return super.getStateInfo() + ", jobCount=" + this.jobCount;
    }

    @Override
    protected void start(final JobHandler processInfo) {
        // acquire a slot
        this.acquireSlot();

        // check if we got outdated in the meantime
        if ( this.isOutdated() ) {
            this.freeSlot();
            return;
        }
        if ( !this.executeJob(processInfo) ) {
            this.freeSlot();
        }
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
                        Thread.currentThread().interrupt();
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
    protected void notifyFinished(final boolean reschedule) {
        this.freeSlot();
    }

    @Override
    protected void reschedule(final JobHandler handler) {
        // we just sleep for the delay time - if none, we continue and retry
        // this job again
        final long delay = this.getRetryDelay(handler);
        if ( delay > 0 ) {
            handler.addToRetryList();
            final Date fireDate = new Date();
            fireDate.setTime(System.currentTimeMillis() + delay);

            final String jobName = "Waiting:" + queueName + ":" + handler.hashCode();
            final Runnable t = new Runnable() {
                @Override
                public void run() {
                    handler.removeFromRetryList();
                    ParallelJobQueue.super.reschedule(handler);
                }
            };
            services.scheduler.schedule(t, services.scheduler.AT(fireDate).name(jobName));
        } else {
            // put directly into queue
            super.reschedule(handler);
        }
    }
}

