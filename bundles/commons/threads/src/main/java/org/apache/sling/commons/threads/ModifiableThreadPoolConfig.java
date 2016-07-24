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

import org.osgi.annotation.versioning.ProviderType;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * This is a modifiable thread pool configuration that can be instantiated
 * and then configured to create a new thread pool.
 *
 * The default values for this configuration are:
 * - min pool size: 5
 * - max pool size: 5
 * - queue size: -1
 * - keep alive time: 60000
 * - block policy: RUN
 * - shutdown graceful: true
 * - shutdown wait time: -1
 * - priority: NORM
 * - daemon: false
 * - factory: null (= default jvm thread factory)
 */
@ProviderType
public final class ModifiableThreadPoolConfig implements ThreadPoolConfig {

    /** Configuration property for the min pool size. */
    public static final String PROPERTY_MIN_POOL_SIZE = "minPoolSize";
    /** Configuration property for the max pool size. */
    public static final String PROPERTY_MAX_POOL_SIZE = "maxPoolSize";
    /** Configuration property for the queue size. */
    public static final String PROPERTY_QUEUE_SIZE = "queueSize";
    /** Configuration property for the max thread age. */
    public static final String PROPERTY_MAX_THREAD_AGE = "maxThreadAge";
    /** Configuration property for the keep alive time. */
    public static final String PROPERTY_KEEP_ALIVE_TIME = "keepAliveTime";
    /** Configuration property for the block policy. */
    public static final String PROPERTY_BLOCK_POLICY = "blockPolicy";
    /** Configuration property for the shutdown graceful flag. */
    public static final String PROPERTY_SHUTDOWN_GRACEFUL = "shutdownGraceful";
    /** Configuration property for the shutdown wait time. */
    public static final String PROPERTY_SHUTDOWN_WAIT_TIME = "shutdownWaitTime";
    /** Configuration property for the priority. */
    public static final String PROPERTY_PRIORITY = "priority";
    /** Configuration property for the daemon flag. */
    public static final String PROPERTY_DAEMON = "daemon";
    /** Configuration property for the thread pool name. */
    public static final String PROPERTY_NAME = "name";

    /** The min pool size. */
    private int minPoolSize = 5;

    /** The max pool size. */
    private int maxPoolSize = 5;

    /** The queue size */
    private int queueSize = -1;

    /** Max age of a thread in milliseconds */
    private long maxThreadAge = TimeUnit.MINUTES.toMillis(5);

    /** The keep alive time. */
    private long  keepAliveTime = 60000L;

    /** The thread pool policy. Default is RUN. */
    private ThreadPoolPolicy blockPolicy = ThreadPoolPolicy.RUN;

    /** Try to shutdown gracefully? */
    private  boolean shutdownGraceful = true;

    /** Wait time during shutdown. */
    private  int shutdownWaitTimeMs = -1;

    /** Optional thread factory. */
    private  ThreadFactory factory;

    /** Thread priority. */
    private  ThreadPriority priority = ThreadPriority.NORM;

    /** Create daemon threads? */
    private  boolean isDaemon = false;

    /**
     * Create a new default configuration.
     */
    public ModifiableThreadPoolConfig() {
        // nothing to do
    }

    /**
     * Clone an existing configuration
     * @param copy The config to clone
     */
    public ModifiableThreadPoolConfig(final ThreadPoolConfig copy) {
        if ( copy != null ) {
            this.minPoolSize = copy.getMinPoolSize();
            this.maxPoolSize = copy.getMaxPoolSize();
            this.queueSize = copy.getQueueSize();
            this.maxThreadAge = copy.getMaxThreadAge();
            this.keepAliveTime = copy.getKeepAliveTime();
            this.blockPolicy = copy.getBlockPolicy();
            this.shutdownGraceful = copy.isShutdownGraceful();
            this.shutdownWaitTimeMs = copy.getShutdownWaitTimeMs();
            this.factory = copy.getFactory();
            this.priority = copy.getPriority();
            this.isDaemon = copy.isDaemon();
        }
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPoolConfig#getMinPoolSize()
     */
    public int getMinPoolSize() {
        return minPoolSize;
    }

    /**
     * Set the min pool size.
     * @param minPoolSize New min pool size.
     */
    public void setMinPoolSize(final int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPoolConfig#getMaxPoolSize()
     */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * Set the max pool size.
     * @param maxPoolSize New max pool size.
     */
    public void setMaxPoolSize(final int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPoolConfig#getQueueSize()
     */
    public int getQueueSize() {
        return queueSize;
    }

    /**
     * Set the queue size.
     * @param queueSize New queue size.
     */
    public void setQueueSize(final int queueSize) {
        this.queueSize = queueSize;
    }


    /**
     * @see org.apache.sling.commons.threads.ThreadPoolConfig#getMaxThreadAge()
     */
    public long getMaxThreadAge() {
        return maxThreadAge;
    }

    /**
     * Set the max thread age.
     * @param maxThreadAge New max thread age in milliseconds.
     */
    public void setMaxThreadAge(final long maxThreadAge) {
        this.maxThreadAge = maxThreadAge;
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPoolConfig#getKeepAliveTime()
     */
    public long getKeepAliveTime() {
        return keepAliveTime;
    }

    /**
     * Set the keep alive time.
     * @param keepAliveTime New keep alive time.
     */
    public void setKeepAliveTime(final long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPoolConfig#getBlockPolicy()
     */
    public ThreadPoolPolicy getBlockPolicy() {
        return blockPolicy;
    }

    /**
     * Set the block policy.
     * @param blockPolicy The new block policy.
     * @throws IllegalArgumentException If blockPolicy is null.
     */
    public void setBlockPolicy(final ThreadPoolPolicy blockPolicy) {
        this.blockPolicy = blockPolicy;
        if ( blockPolicy == null ) {
            throw new IllegalArgumentException("Policy must not be null.");
        }
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPoolConfig#isShutdownGraceful()
     */
    public boolean isShutdownGraceful() {
        return shutdownGraceful;
    }

    /**
     * Set if the pool should be shutdown graceful.
     * @param shutdownGraceful The shutdown graceful setting.
     */
    public void setShutdownGraceful(final boolean shutdownGraceful) {
        this.shutdownGraceful = shutdownGraceful;
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPoolConfig#getShutdownWaitTimeMs()
     */
    public int getShutdownWaitTimeMs() {
        return shutdownWaitTimeMs;
    }

    /**
     * Set the shutdown wait time.
     * @param shutdownWaitTimeMs The new shutdown wait time.
     */
    public void setShutdownWaitTimeMs(final int shutdownWaitTimeMs) {
        this.shutdownWaitTimeMs = shutdownWaitTimeMs;
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPoolConfig#getFactory()
     */
    public ThreadFactory getFactory() {
        return factory;
    }

    /**
     * Set the thread factory.
     * @param factory The thread factory to be used or <code>null</code> to use
     *                the default thread factory.
     */
    public void setFactory(final ThreadFactory factory) {
        this.factory = factory;
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPoolConfig#getPriority()
     */
    public ThreadPriority getPriority() {
        return priority;
    }

    /**
     * Set the thread priority.
     * @param priority The thread priority.
     * @throws IllegalArgumentException If priority is null.
     */
    public void setPriority(final ThreadPriority priority) {
        if ( priority == null ) {
            throw new IllegalArgumentException("Priority must not be null.");
        }
        this.priority = priority;
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPoolConfig#isDaemon()
     */
    public boolean isDaemon() {
        return isDaemon;
    }

    /**
     * Set the daemon handling.
     * @param isDaemon The daemon setting.
     */
    public void setDaemon(final boolean isDaemon) {
        this.isDaemon = isDaemon;
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj instanceof ModifiableThreadPoolConfig ) {
            final ModifiableThreadPoolConfig o = (ModifiableThreadPoolConfig)obj;
            return this.minPoolSize == o.minPoolSize
                && this.maxPoolSize == o.maxPoolSize
                && this.queueSize == o.queueSize
                && this.maxThreadAge == o.maxThreadAge
                && this.keepAliveTime == o.keepAliveTime
                && this.blockPolicy.equals(o.blockPolicy)
                && this.shutdownGraceful == o.shutdownGraceful
                && this.shutdownWaitTimeMs == o.shutdownWaitTimeMs
                && this.priority.equals(o.priority)
                && this.isDaemon == o.isDaemon;
        }
        return false;
    }

    @Override
    public int hashCode() {
        // we actually don't need hash code, but we don't want to violate the
        // contract with equals
        return this.blockPolicy.hashCode();
    }
}