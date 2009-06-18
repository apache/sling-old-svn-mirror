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

import java.util.concurrent.ThreadFactory;

/**
 * The ThreadPool configuration.
 *
 * @version $Id$
 */
public final class ThreadPoolConfig {

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

    /** The min pool size. */
    private int minPoolSize = 5;

    /** The max pool size. */
    private int maxPoolSize = 5;

    /** The queue size */
    private int queueSize = -1;

    /** The keep alive time. */
    private long  keepAliveTime = 60000L;

    /** The thread pool policy. Default is RUN. */
    private ThreadPoolPolicy blockPolicy = ThreadPoolPolicy.RUN;

    private  boolean shutdownGraceful = false;

    private  int shutdownWaitTimeMs = -1;

    private  ThreadFactory factory;

    private  ThreadPriority   priority = ThreadPriority.NORM;

    private  boolean isDaemon = false;

    /** Can this configuration still be changed? */
    private boolean isWritable = true;

    /**
     * Create a new default configuration.
     */
    public ThreadPoolConfig() {
        // nothing to do
    }

    /**
     * Clone an existing configuration
     * @param copy The config to clone
     */
    public ThreadPoolConfig(ThreadPoolConfig copy) {
        this.minPoolSize = copy.minPoolSize;
        this.maxPoolSize = copy.maxPoolSize;
        this.queueSize = copy.queueSize;
        this.keepAliveTime = copy.keepAliveTime;
        this.blockPolicy = copy.blockPolicy;
        this.shutdownGraceful = copy.shutdownGraceful;
        this.shutdownWaitTimeMs = copy.shutdownWaitTimeMs;
        this.factory = copy.factory;
        this.priority = copy.priority;
        this.isDaemon = copy.isDaemon;
    }

    protected void checkWritable() {
        if ( !isWritable ) {
            throw new IllegalStateException("ThreadPoolConfig is read-only.");
        }
    }

    /**
     * Make the configuration read-only.
     */
    public void makeReadOnly() {
        this.isWritable = false;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(int minPoolSize) {
        this.checkWritable();
        this.minPoolSize = minPoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.checkWritable();
        this.maxPoolSize = maxPoolSize;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.checkWritable();
        this.queueSize = queueSize;
    }

    public long getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(long keepAliveTime) {
        this.checkWritable();
        this.keepAliveTime = keepAliveTime;
    }

    public ThreadPoolPolicy getBlockPolicy() {
        return blockPolicy;
    }

    public void setBlockPolicy(ThreadPoolPolicy blockPolicy) {
        this.checkWritable();
        this.blockPolicy = blockPolicy;
        if ( blockPolicy == null ) {
            throw new IllegalArgumentException("Policy must not be null.");
        }
    }

    public boolean isShutdownGraceful() {
        return shutdownGraceful;
    }

    public void setShutdownGraceful(boolean shutdownGraceful) {
        this.checkWritable();
        this.shutdownGraceful = shutdownGraceful;
    }

    public int getShutdownWaitTimeMs() {
        return shutdownWaitTimeMs;
    }

    public void setShutdownWaitTimeMs(int shutdownWaitTimeMs) {
        this.checkWritable();
        this.shutdownWaitTimeMs = shutdownWaitTimeMs;
    }

    public ThreadFactory getFactory() {
        return factory;
    }

    public void setFactory(ThreadFactory factory) {
        this.checkWritable();
        this.factory = factory;
    }

    public ThreadPriority getPriority() {
        return priority;
    }

    public void setPriority(ThreadPriority priority) {
        this.checkWritable();
        if ( priority == null ) {
            throw new IllegalArgumentException("Priority must not be null.");
        }
        this.priority = priority;
    }

    public boolean isDaemon() {
        return isDaemon;
    }

    public void setDaemon(boolean isDaemon) {
        this.checkWritable();
        this.isDaemon = isDaemon;
    }
}