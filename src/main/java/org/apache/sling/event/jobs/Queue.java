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
package org.apache.sling.event.jobs;

import aQute.bnd.annotation.ProviderType;


/**
 * This is a job queue processing job events.
 * @since 3.0
 */
@ProviderType
public interface Queue {

    /**
     * Get the queue name.
     */
    String getName();

    /**
     * Return statistics information about this queue.
     */
    Statistics getStatistics();

    /**
     * Get the corresponding configuration.
     */
    QueueConfiguration getConfiguration();

    /**
     * Suspend the queue - when a queue is suspended it stops processing
     * jobs - however already started jobs are finished (but not rescheduled).
     * Depending on the queue implementation, the queue is only suspended
     * for a specific time.
     * A queue can be resumed with {@link #resume()}.
     */
    void suspend();

    /**
     * Resume a suspended queue. {@link #suspend()}. If the queue is not
     * suspended, calling this method has no effect.
     * Depending on the queue implementation, if a job failed a job queue might
     * sleep for a configured time, before a new job is processed. By calling this
     * method, the job queue can be woken up and force an immediate reprocessing.
     * This feature is only supported by ordered queues at the moment. If a queue
     * does not support this feature, calling this method has only an effect if
     * the queue is really suspended.
     */
    void resume();

    /**
     * Is the queue currently suspended?
     */
    boolean isSuspended();

    /**
     * Remove all outstanding jobs from the queue. This does not delete
     * the jobs. The jobs are either processed by a different cluster node
     * or on restart.
     */
    void clear();

    /**
     * Remove all outstanding jobs and delete them. This actually cancels
     * all outstanding jobs.
     */
    void removeAll();

    /**
     * Return some information about the current state of the queue. This
     * method is meant to see the internal state of the queue for debugging
     * or monitoring purposes.
     */
    String getStateInfo();

    /**
     * For monitoring purposes and possible extensions from the different
     * queue types. This method allows to query state information.
     */
    Object getState(final String key);
}
