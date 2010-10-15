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
        UNORDERED,
        ORDERED,
        TOPIC_ROUND_ROBIN,
        IGNORE
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
     * Is this a local running queue (= processing only
     * jobs started on the same instance.)
     */
    boolean isLocalQueue();

    /**
     * Application ids - returns an array of application
     * ids if this queue is bound to some cluster nodes.
     */
    String[] getApplicationIds();

    /**
     * The list of topics this queue is bound to.
     */
    String[] getTopics();

    /**
     * Get the ranking of this configuration.
     */
    int getRanking();
}
