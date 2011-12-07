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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.sling.event.impl.EnvironmentComponent;
import org.apache.sling.event.impl.jobs.JobEvent;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.apache.sling.event.jobs.JobUtil;

/**
 * An ordered job queue is processing the queue FIFO in a serialized
 * way. If a job fails it is rescheduled and the reschedule is processed
 * next - this basically means that failing jobs block the queue
 * until they are finished!
 */
public final class OrderedJobQueue extends AbstractJobQueue {

    /** The job event for rescheduling. */
    private volatile JobEvent jobEvent;

    /** Lock and status object for handling the sleep phase. */
    private final SleepLock sleepLock = new SleepLock();

    /** The queue. */
    private final BlockingQueue<JobEvent> queue = new LinkedBlockingQueue<JobEvent>();

    private final Object syncLock = new Object();

    public OrderedJobQueue(final String name,
                           final InternalQueueConfiguration config,
                           final EnvironmentComponent env) {
        super(name, config, env);
    }

    @Override
    public String getStateInfo() {
        return super.getStateInfo() + ", isSleepingUntil=" + this.sleepLock.sleepingSince;
    }

    @Override
    protected JobEvent start(final JobEvent processInfo) {
        JobEvent rescheduleInfo = null;

        // if we are ordered we simply wait for the finish
        synchronized ( this.syncLock ) {
            if ( this.executeJob(processInfo) ) {
                this.isWaiting = true;
                this.logger.debug("Job queue {} is waiting for finish.", this.queueName);
                while ( this.isWaiting ) {
                    try {
                        this.syncLock.wait();
                    } catch (InterruptedException e) {
                        this.ignoreException(e);
                    }
                }
                this.logger.debug("Job queue {} is continuing.", this.queueName);
                rescheduleInfo = this.jobEvent;
                this.jobEvent = null;
            }
        }
        return rescheduleInfo;
    }

    private void wakeUp(final boolean discardJob) {
        synchronized ( this.sleepLock ) {
            if ( this.sleepLock.sleepingSince != -1 ) {
                if ( discardJob ) {
                    this.sleepLock.jobEvent = null;
                }
                this.sleepLock.notify();
            }
        }
    }

    @Override
    public void resume() {
        this.wakeUp(false);
        super.resume();
    }

    @Override
    protected void put(final JobEvent event) {
        try {
            this.queue.put(event);
        } catch (final InterruptedException e) {
            // this should never happen
            this.ignoreException(e);
        }
    }

    @Override
    protected JobEvent take() {
        try {
            return this.queue.take();
        } catch (final InterruptedException e) {
            // this should never happen
            this.ignoreException(e);
        }
        return null;
    }

    @Override
    protected boolean isEmpty() {
        return this.queue.isEmpty();
    }

    @Override
    protected void notifyFinished(final JobEvent rescheduleInfo) {
        this.jobEvent = rescheduleInfo;
        this.logger.debug("Notifying job queue {} to continue processing.", this.queueName);
        synchronized ( this.syncLock ) {
            this.isWaiting = false;
            this.syncLock.notify();
        }
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
            synchronized ( this.sleepLock ) {
                this.sleepLock.sleepingSince = System.currentTimeMillis();
                this.sleepLock.jobEvent = info;
                this.logger.debug("Job queue {} is sleeping for {}ms.", this.queueName, delay);
                try {
                    this.sleepLock.wait(delay);
                } catch (final InterruptedException e) {
                    this.ignoreException(e);
                }
                this.sleepLock.sleepingSince = -1;
                final JobEvent result = this.sleepLock.jobEvent;
                this.sleepLock.jobEvent = null;

                if ( result == null ) {
                    info.remove();
                }
                return result;
            }
        }
        return info;
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#clear()
     */
    public void clear() {
        this.queue.clear();
        super.clear();
    }

    @Override
    public synchronized void removeAll() {
        // remove all remaining jobs first
        super.removeAll();
        this.jobEvent = null;
        this.wakeUp(true);
    }

    @Override
    protected Collection<JobEvent> removeAllJobs() {
        final List<JobEvent> events = new ArrayList<JobEvent>();
        this.queue.drainTo(events);
        return events;
    }

    @Override
    public Object getState(final String key) {
        if ( "isSleepingUntil".equals(key) ) {
            return this.sleepLock.sleepingSince;
        }
        return super.getState(key);
    }

    private static final class SleepLock {

        /** Marker indicating that this queue is currently sleeping. */
        public volatile long sleepingSince = -1;

        /** The job event to be returned after sleeping. */
        public volatile JobEvent jobEvent;
    }
}

