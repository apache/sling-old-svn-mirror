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

import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.event.impl.jobs.JobConsumerManager;
import org.apache.sling.event.impl.jobs.JobHandler;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.osgi.service.event.EventAdmin;

/**
 * The default parallel job queue processing the entries FIFO.
 * Failing jobs are rescheduled and put at the end of the queue.
 */
public final class ParallelJobQueue extends AbstractParallelJobQueue {

    /** The queue. */
    private final BlockingQueue<JobHandler> queue = new LinkedBlockingQueue<JobHandler>();

    public ParallelJobQueue(final String name,
                           final InternalQueueConfiguration config,
                           final JobConsumerManager jobConsumerManager,
                           final ThreadPoolManager threadPoolManager,
                           final EventAdmin eventAdmin,
                           final Scheduler scheduler) {
        super(name, config, jobConsumerManager, threadPoolManager, eventAdmin, scheduler);
    }

    @Override
    protected void put(final JobHandler event) {
        try {
            this.isWaitingForNext = false;
            this.queue.put(event);
        } catch (final InterruptedException e) {
            this.ignoreException(e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    protected JobHandler take() {
        try {
            this.isWaitingForNext = true;
            return this.queue.take();
        } catch (final InterruptedException e) {
            this.ignoreException(e);
            Thread.currentThread().interrupt();
        } finally {
            this.isWaitingForNext = false;
        }
        return null;
    }

    @Override
    protected boolean isEmpty() {
        return this.queue.isEmpty();
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#clear()
     */
    @Override
    public void clear() {
        this.queue.clear();
        super.clear();
    }

    @Override
    protected Collection<JobHandler> removeAllJobs() {
        final List<JobHandler> events = new ArrayList<JobHandler>();
        this.queue.drainTo(events);
        return events;
    }
}

