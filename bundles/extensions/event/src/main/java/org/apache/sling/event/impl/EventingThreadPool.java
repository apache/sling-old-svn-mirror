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
package org.apache.sling.event.impl;

import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.commons.threads.ThreadPoolConfig;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.event.ThreadPool;
import org.osgi.service.component.ComponentContext;


/**
 * The configurable eventing thread pool.
 * @scr.component label="%event.pool.name" description="%event.pool.description"
 * @scr.service interface="org.apache.sling.event.ThreadPool"
 *
 * @scr.property nameRef="PROPERTY_MIN_POOL_SIZE" valueRef="DEFAULT_MIN_POOL_SIZE"
 * @scr.property nameRef="PROPERTY_MAX_POOL_SIZE" valueRef="DEFAULT_MAX_POOL_SIZE"
 * @scr.property nameRef="PROPERTY_QUEUEL_SIZE" valueRef="DEFAULT_QUEUE_SIZE"
 */
public class EventingThreadPool implements ThreadPool {

    /** @scr.reference */
    protected ThreadPoolManager threadPoolManager;

    /** The real thread pool used. */
    private org.apache.sling.commons.threads.ThreadPool threadPool;

    private static final String PROPERTY_MIN_POOL_SIZE = "minPoolSize";
    private static final String PROPERTY_MAX_POOL_SIZE = "maxPoolSize";
    private static final String PROPERTY_QUEUEL_SIZE = "queueSize";

    private static final int DEFAULT_MIN_POOL_SIZE = 20; // this is sufficient for all threads + approx 10 job queues
    private static final int DEFAULT_MAX_POOL_SIZE = 30;
    private static final int DEFAULT_QUEUE_SIZE = 50; // queue upto 50 threads

    /**
     * Activate this component.
     * @param context
     */
    protected void activate(final ComponentContext ctx) throws Exception {
        // start background threads
        if ( this.threadPoolManager == null ) {
            throw new Exception("No ThreadPoolManager found.");
        }
        final ThreadPoolConfig config = new ThreadPoolConfig();
        config.setMinPoolSize(OsgiUtil.toInteger(ctx.getProperties().get(PROPERTY_MIN_POOL_SIZE), DEFAULT_MIN_POOL_SIZE));
        config.setMaxPoolSize(OsgiUtil.toInteger(ctx.getProperties().get(PROPERTY_MAX_POOL_SIZE), DEFAULT_MAX_POOL_SIZE));
        config.setQueueSize(OsgiUtil.toInteger(ctx.getProperties().get(PROPERTY_QUEUEL_SIZE), DEFAULT_QUEUE_SIZE));
        config.setShutdownGraceful(true);
        threadPoolManager.create(EventHelper.THREAD_POOL_NAME, config);

        this.threadPool = threadPoolManager.get(EventHelper.THREAD_POOL_NAME);
        if ( this.threadPool == null ) {
            throw new Exception("No thread pool with name " + EventHelper.THREAD_POOL_NAME + " found.");
        }
    }

    /**
     * Deactivate this component.
     * @param context
     */
    protected void deactivate(final ComponentContext context) {
        this.threadPool = null;
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPool#execute(java.lang.Runnable)
     */
    public void execute(Runnable runnable) {
        threadPool.execute(runnable);
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPool#getConfiguration()
     */
    public ThreadPoolConfig getConfiguration() {
        return threadPool.getConfiguration();
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPool#getName()
     */
    public String getName() {
        return threadPool.getName();
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPool#shutdown()
     */
    public void shutdown() {
        threadPool.shutdown();
    }
}
