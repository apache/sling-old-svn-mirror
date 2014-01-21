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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.event.impl.jobs.JobConsumerManager;
import org.apache.sling.event.impl.jobs.JobHandler;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.osgi.service.event.EventAdmin;

/**
 * An ordered job queue is processing the queue FIFO in a serialized
 * way. If a job fails it is rescheduled and the reschedule is processed
 * next - this basically means that failing jobs block the queue
 * until they are finished!
 */
public final class OrderedJobQueue extends AbstractJobQueue {

    /** The job handler for rescheduling. */
    private volatile JobHandler jobHandler;

    /** Lock and status object for handling the sleep phase. */
    private final SleepLock sleepLock = new SleepLock();

    /** The queue - we use a set which is sorted by job creation date. */
    private final Set<JobHandler> queue = new TreeSet<JobHandler>(new Comparator<JobHandler>() {

        @Override
        public int compare(final JobHandler o1, final JobHandler o2) {
            if ( o1.getJob() == null ) {
                if ( o2.getJob() == null ) {
                    return 0;
                }
                return -1;
            }
            if ( o2.getJob() == null ) {
                return 1;
            }
            int result = o1.getJob().getCreated().compareTo(o2.getJob().getCreated());
            if (result == 0 ) {
                result = o1.getJob().getId().compareTo(o2.getJob().getId());
            }
            return result;
        }
    });

    private final Object syncLock = new Object();

    public OrderedJobQueue(final String name,
                           final InternalQueueConfiguration config,
                           final JobConsumerManager jobConsumerManager,
                           final ThreadPoolManager threadPoolManager,
                           final EventAdmin eventAdmin) {
        super(name, config, jobConsumerManager, threadPoolManager, eventAdmin);
    }

    @Override
    public String getStateInfo() {
        return super.getStateInfo() + ", isSleepingUntil=" + this.sleepLock.sleepingSince;
    }

    @Override
    protected JobHandler start(final JobHandler handler) {
        JobHandler rescheduleHandler = null;

        // if we are ordered we simply wait for the finish
        synchronized ( this.syncLock ) {
            if ( this.executeJob(handler) ) {
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
                rescheduleHandler = this.jobHandler;
                this.jobHandler = null;
            }
        }
        return rescheduleHandler;
    }

    private void wakeUp(final boolean discardJob) {
        synchronized ( this.sleepLock ) {
            if ( this.sleepLock.sleepingSince != -1 ) {
                if ( discardJob ) {
                    this.sleepLock.jobHandler = null;
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
    protected void put(final JobHandler handler) {
        synchronized ( this.queue ) {
            this.queue.add(handler);
            this.queue.notify();
            this.isWaitingForNext = false;
        }
    }

    @Override
    protected JobHandler take() {
        synchronized ( this.queue ) {
            while ( this.queue.isEmpty() ) {
                this.isWaitingForNext = true;
                try {
                    this.queue.wait();
                } catch (final InterruptedException e) {
                    this.ignoreException(e);
                }
                this.isWaitingForNext = false;
            }
            // get the first element and remove it
            final Iterator<JobHandler> i = this.queue.iterator();
            final JobHandler result = i.next();
            i.remove();
            return result;
        }
    }

    @Override
    protected boolean isEmpty() {
        synchronized ( this.queue ) {
            return this.queue.isEmpty();
        }
    }

    @Override
    protected void notifyFinished(final JobHandler rescheduleHandler) {
        this.jobHandler = rescheduleHandler;
        this.logger.debug("Notifying job queue {} to continue processing.", this.queueName);
        synchronized ( this.syncLock ) {
            this.isWaiting = false;
            this.syncLock.notify();
        }
    }

    @Override
    protected JobHandler reschedule(final JobHandler handler) {
        // we just sleep for the delay time - if none, we continue and retry
        // this job again
        final long delay = this.getRetryDelay(handler);
        if ( delay > 0 ) {
            synchronized ( this.sleepLock ) {
                this.sleepLock.sleepingSince = System.currentTimeMillis();
                this.sleepLock.jobHandler = handler;
                this.logger.debug("Job queue {} is sleeping for {}ms.", this.queueName, delay);
                try {
                    this.sleepLock.wait(delay);
                } catch (final InterruptedException e) {
                    this.ignoreException(e);
                }
                this.sleepLock.sleepingSince = -1;
                final JobHandler result = this.sleepLock.jobHandler;
                this.sleepLock.jobHandler = null;

                if ( result == null ) {
                    handler.cancel();
                }
                return result;
            }
        }
        return handler;
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#clear()
     */
    @Override
    public void clear() {
        synchronized ( this.queue ) {
            this.queue.clear();
        }
        super.clear();
    }

    @Override
    public synchronized void removeAll() {
        // remove all remaining jobs first
        super.removeAll();
        this.jobHandler = null;
        this.wakeUp(true);
    }

    @Override
    protected Collection<JobHandler> removeAllJobs() {
        final List<JobHandler> events = new ArrayList<JobHandler>();
        synchronized ( this.queue ) {
            events.addAll(this.queue);
            this.queue.clear();
        }
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
        public volatile JobHandler jobHandler;
    }
}

