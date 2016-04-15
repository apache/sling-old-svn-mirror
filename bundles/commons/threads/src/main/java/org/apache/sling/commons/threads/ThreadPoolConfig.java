/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.threads;

import aQute.bnd.annotation.ProviderType;

import java.util.concurrent.ThreadFactory;

/**
 * The thread pool configuration.
 */
@ProviderType
public interface ThreadPoolConfig {

    /** The thread pool policies. */
    public enum ThreadPoolPolicy {
        ABORT,
        DISCARD,
        DISCARDOLDEST,
        RUN
    };

    public enum ThreadPriority {
        NORM,
        MIN,
        MAX
    };

    /**
     * Return the minimum pool size.
     * @return The minimum pool size.
     */
    int getMinPoolSize();

    /**
     * Return the maximum pool size
     * @return The maximum pool size.
     */
    int getMaxPoolSize();

    /**
     * Return the queue size.
     * @return The queue size.
     */
    int getQueueSize();

    /**
     * Return the maximum age before a thread is retired.
     * @return The maximum age of a thread in milliseconds.
     */
    long getMaxThreadAge();

    /**
     * Return the keep alive time.
     * @return The keep alive time.
     */
    long getKeepAliveTime();

    /**
     * Return the block policy.
     * @return The block policy.
     */
    ThreadPoolPolicy getBlockPolicy();

    /**
     * Should this pool shutdown graceful.
     * @return <code>true</code> if the pool should shutdown graceful.
     */
    boolean isShutdownGraceful();

    /**
     * Return the shutdown wait time in ms. A value below 1 means
     * no waiting at shutdown.
     * @return The shutdown wait time in ms.
     */
    int getShutdownWaitTimeMs();

    /**
     * Return the thread pool factory. A value of null means the
     * default jvm thread pool factory is used.
     * @return The thread pool factory or <code>null</code>
     */
    ThreadFactory getFactory();

    /**
     * Return the priority for the threads.
     * @return The priority for the threads.
     */
    ThreadPriority getPriority();

    /**
     * Return if daemon threads should be created.
     * @return <code>true</code> if daemon threads should be created.
     */
    boolean isDaemon();
}