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


/**
 * The configuration of a queue.
 * @since 3.0
 */
public interface QueueConfiguration {

    /** The queue type. */
    static enum Type {
        UNORDERED,          // unordered, parallel processing (push)
        ORDERED,            // ordered, FIFO (push)
        TOPIC_ROUND_ROBIN,  // unordered, parallel processing, executed based on topic (push)
        IGNORE,             // ignore job, but do not remove
        DROP                // drop job without processing!
    }

    /**
     * Return the retry delay in ms
     */
    long getRetryDelayInMs();

    /**
     * Return the max number of retries, -1 for endless retry!
     */
    int getMaxRetries();

    /**
     * Return the queue type.
     */
    Type getType();

    /**
     * Return the thread priority for the job thread.
     */
    JobUtil.JobPriority getPriority();

    /**
     * Return the max number of parallel processes.
     */
    int getMaxParallel();

    /**
     * @deprecated This information is not used anymore
     */
    @Deprecated
    boolean isLocalQueue();

    /**
     * @deprecated This information is not used anymore
     */
    @Deprecated
    String[] getApplicationIds();

    /**
     * The list of topics this queue is bound to.
     */
    String[] getTopics();

    /**
     * Get the ranking of this configuration.
     */
    int getRanking();

    /**
     * Wait for async job consumers
     * @since 1.2
     */
    boolean waitForAsyncJobConsumers();
}
