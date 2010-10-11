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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.event.impl.EnvironmentComponent;
import org.apache.sling.event.impl.jobs.JobEvent;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;

/**
 * The default parallel job queue processing the entries FIFO.
 * Failing jobs are rescheduled and put at the end of the queue.
 */
public final class ParallelJobQueue extends AbstractParallelJobQueue {

    /** The queue. */
    private final BlockingQueue<JobEvent> queue = new LinkedBlockingQueue<JobEvent>();

    public ParallelJobQueue(final String name,
                           final InternalQueueConfiguration config,
                           final EnvironmentComponent env,
                           final Scheduler scheduler) {
        super(name, config, env, scheduler);
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

    /**
     * @see org.apache.sling.event.jobs.Queue#clear()
     */
    public void clear() {
        this.queue.clear();
        super.clear();
    }
}

