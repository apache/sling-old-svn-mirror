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
package org.apache.sling.commons.threads.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolConfig;


/**
 * This is a facade which is delivered to clients. It allows
 * to change the thread pool behind the facacde without affecting
 * the client.
 */
public final class ThreadPoolFacade implements ThreadPool {

    /** The thread pool */
    private volatile DefaultThreadPool delegatee;

    /** The name of the pool */
    private String name;

    public ThreadPoolFacade(final DefaultThreadPool pool) {
        this.delegatee = pool;
        this.name = pool.getName();
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPool#execute(java.lang.Runnable)
     */
    public void execute(final Runnable runnable) {
        this.delegatee.execute(runnable);
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPool#submit(java.util.concurrent.Callable)
     */
    public <T> Future<T> submit(final Callable<T> callable) {
        return this.delegatee.submit(callable);
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPool#submit(java.lang.Runnable)
     */
    public Future<?> submit(final Runnable runnable) {
        return this.delegatee.submit(runnable);
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPool#getConfiguration()
     */
    public ThreadPoolConfig getConfiguration() {
        return this.delegatee.getConfiguration();
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPool#getName()
     */
    public String getName() {
        return this.name;
    }

    /**
     * Shutdown this thread pool.
     */
    public void shutdown() {
        this.delegatee.shutdown();
    }

    /**
     * Update name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Update pool
     */
    public void setPool(final DefaultThreadPool pool) {
        final DefaultThreadPool oldPool = this.delegatee;
        this.delegatee = pool;
        oldPool.shutdown();
    }

    public ThreadPoolExecutor getExecutor() {
        return this.delegatee.getExecutor();
    }
}
