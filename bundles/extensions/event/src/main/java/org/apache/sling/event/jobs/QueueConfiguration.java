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
 * The configuration of a queue.
 * @since 3.0
 */
@ProviderType
public interface QueueConfiguration {

    /** The queue type. */
    static enum Type {
        UNORDERED,          // unordered, parallel processing (push)
        ORDERED,            // ordered, FIFO (push)
        TOPIC_ROUND_ROBIN   // unordered, parallel processing, executed based on topic (push)
    }

    /**
     * The thread priority.
     * @since 1.3
     */
    static enum ThreadPriority {
        NORM,
        MIN,
        MAX
    }

    /**
     * Return the retry delay in ms
     * @return The retry delay
     */
    long getRetryDelayInMs();

    /**
     * Return the max number of retries, -1 for endless retry!
     * @return Max number of retries
     */
    int getMaxRetries();

    /**
     * Return the queue type.
     * @return The queue type
     */
    Type getType();

    /**
     * Return the thread priority for the job thread
     * @return Thread priority
     */
    ThreadPriority getThreadPriority();

    /**
     * Return the max number of parallel processes.
     * @return Max parallel processes
     */
    int getMaxParallel();

    /**
     * The list of topics this queue is bound to.
     * @return All topics for this queue.
     */
    String[] getTopics();

    /**
     * Whether successful jobs are kept for a complete history
     * @return <code>true</code> if successful jobs are kept.
     * @since 1.3
     */
    boolean isKeepJobs();

    /**
     * Return the size for the optional thread pool for this queue.
     * @return A positive number or <code>0</code> if the default thread pool
     *         should be used.
     * @since 1.3
     */
    int getOwnThreadPoolSize();

    /**
     * Get the ranking of this configuration.
     * @return The ranking
     */
    int getRanking();

    /**
     * Prefer to run the job on the same instance it was created on.
     * @return {@code true} if running on the creation instance is preferred.
     * @since 1.4
     */
    boolean isPreferRunOnCreationInstance();
}
