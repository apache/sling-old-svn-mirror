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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.threads.ModifiableThreadPoolConfig;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolConfig;
import org.apache.sling.commons.threads.ThreadPoolConfig.ThreadPriority;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.osgi.service.component.ComponentContext;


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

    @Property(value="NORM",
            options={@PropertyOption(name="NORM",value="Norm"),
                     @PropertyOption(name="MIN",value="Min"),
                     @PropertyOption(name="MAX",value="Max")})
    private static final String PROPERTY_PRIORITY = "priority";

    /**
     * Activate this component.
     * @param context
     */
    protected void activate(final ComponentContext ctx) {
        final ModifiableThreadPoolConfig config = new ModifiableThreadPoolConfig();
        config.setMinPoolSize(PropertiesUtil.toInteger(ctx.getProperties().get(PROPERTY_POOL_SIZE), DEFAULT_POOL_SIZE));
        config.setMaxPoolSize(config.getMinPoolSize());
        config.setQueueSize(-1); // unlimited
        config.setShutdownGraceful(true);
        config.setPriority(ThreadPriority.valueOf(PropertiesUtil.toString(ctx.getProperties().get(PROPERTY_PRIORITY), "NORM")));
        config.setDaemon(true);
        this.threadPool = threadPoolManager.create(config, "Apache Sling Eventing Thread Pool");
    }

    /**
     * Deactivate this component.
     * @param context
     */
    protected void deactivate(final ComponentContext context) {
        this.threadPoolManager.release(this.threadPool);
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
}
