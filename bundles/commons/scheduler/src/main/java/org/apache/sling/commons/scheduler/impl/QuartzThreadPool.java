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
package org.apache.sling.commons.scheduler.impl;

import org.apache.sling.commons.threads.ThreadPool;

public class QuartzThreadPool implements org.quartz.spi.ThreadPool {

    /** Our executor thread pool */
    private ThreadPool executor;

    /**
     * Create a new wrapper implementation for Quartz.
     */
    public QuartzThreadPool(final ThreadPool executor) {
        this.executor = executor;
    }

    /**
     * @see org.quartz.spi.QuartzThreadPool#getPoolSize()
     */
    @Override
    public int getPoolSize() {
        return this.executor.getConfiguration().getMaxPoolSize();
    }

    /**
     * @see org.quartz.spi.QuartzThreadPool#initialize()
     */
    @Override
    public void initialize() {
        // nothing to do
    }

    /**
     * @see org.quartz.spi.ThreadPool#setInstanceId(java.lang.String)
     */
    @Override
    public void setInstanceId(final String id) {
        // we ignore this
    }

    /**
     * @see org.quartz.spi.ThreadPool#setInstanceName(java.lang.String)
     */
    @Override
    public void setInstanceName(final String name) {
        // we ignore this
    }

    /**
     * @see org.quartz.spi.QuartzThreadPool#runInThread(java.lang.Runnable)
     */
    @Override
    public boolean runInThread(final Runnable job) {
        this.executor.execute(job);

        return true;
    }

    /**
     * @see org.quartz.spi.ThreadPool#blockForAvailableThreads()
     */
    @Override
    public int blockForAvailableThreads() {
        return this.executor.getConfiguration().getMaxPoolSize() - this.executor.getConfiguration().getQueueSize();
    }

    /**
     * @see org.quartz.spi.QuartzThreadPool#shutdown(boolean)
     */
    @Override
    public void shutdown(final boolean waitForJobsToComplete) {
        // the pool is managed by the thread pool manager,
        // so we can just return
        this.executor = null;
    }
}