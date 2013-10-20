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

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.threads.ModifiableThreadPoolConfig;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolConfig;
import org.apache.sling.commons.threads.ThreadPoolConfig.ThreadPriority;
import org.apache.sling.commons.threads.ThreadPoolManager;


/**
 * The configurable eventing thread pool.
 */
@Component(label="%event.pool.name",
        description="%event.pool.description",
        metatype=true)
@Service(value=EventingThreadPool.class)
public class EventingThreadPool implements ThreadPool {

    @Reference
    private ThreadPoolManager threadPoolManager;

    /** The real thread pool used. */
    private org.apache.sling.commons.threads.ThreadPool threadPool;

    private static final int DEFAULT_POOL_SIZE = 35;

    @Property(intValue=DEFAULT_POOL_SIZE)
    private static final String PROPERTY_POOL_SIZE = "minPoolSize";

    public EventingThreadPool() {
        // default constructor
    }

    public EventingThreadPool(final ThreadPoolManager tpm, final int poolSize) {
        this.threadPoolManager = tpm;
    }

    public void release() {
        this.deactivate();
    }

    /**
     * Activate this component.
     */
    @Activate
    @Modified
    protected void activate(final Map<String, Object> props) {
        final int maxPoolSize = PropertiesUtil.toInteger(props.get(PROPERTY_POOL_SIZE), DEFAULT_POOL_SIZE);
        this.configure(maxPoolSize);
    }

    private void configure(final int maxPoolSize) {
        final ModifiableThreadPoolConfig config = new ModifiableThreadPoolConfig();
        config.setMinPoolSize(maxPoolSize);
        config.setMaxPoolSize(config.getMinPoolSize());
        config.setQueueSize(-1); // unlimited
        config.setShutdownGraceful(true);
        config.setPriority(ThreadPriority.NORM);
        config.setDaemon(true);
        this.threadPool = threadPoolManager.create(config, "Apache Sling Eventing Thread Pool");
    }

    /**
     * Deactivate this component.
     */
    @Deactivate
    protected void deactivate() {
        this.threadPoolManager.release(this.threadPool);
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPool#execute(java.lang.Runnable)
     */
    @Override
    public void execute(final Runnable runnable) {
        threadPool.execute(runnable);
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPool#getConfiguration()
     */
    @Override
    public ThreadPoolConfig getConfiguration() {
        return threadPool.getConfiguration();
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPool#getName()
     */
    @Override
    public String getName() {
        return threadPool.getName();
    }
}
