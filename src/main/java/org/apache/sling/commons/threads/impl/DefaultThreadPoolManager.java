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

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolConfig;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The DefaultThreadPoolManager implements the {@link ThreadPoolManager} interface
 * and is responsible to manage {@link ThreadPool}s.
 *
 * @scr.component metatype="no"
 * @scr.service interface="org.apache.sling.commons.threads.ThreadPoolManager"
 *
 * @version $Id$
 */
public class DefaultThreadPoolManager implements ThreadPoolManager {

    /** By default we use the logger for this class. */
    protected Logger logger = LoggerFactory.getLogger(getClass());

    /** The managed thread pools */
    protected final Map<String, ThreadPool> pools = new HashMap<String, ThreadPool>();

    /**
     * Activate this component.
     */
    protected void activate(ComponentContext context) throws Exception {
        this.logger.info("Starting thread pool manager.");
        final ThreadPool defaultPool = new DefaultThreadPool(
                    DEFAULT_THREADPOOL_NAME,
                    new ThreadPoolConfig());
        synchronized ( this.pools ) {
            this.pools.put(defaultPool.getName(), defaultPool);
        }
        this.logger.info("Thread pool manager startet with default pool.");
    }

    /**
     * Deactivate this component.
     */
    protected void deactivate(ComponentContext context) throws Exception {
        this.logger.info("Stopping thread pool manager.");
        this.logger.debug("Disposing all thread pools");

        synchronized ( this.pools ) {
            for (ThreadPool pool : this.pools.values()) {
                this.logger.debug("Shutting down thread pool {}", pool.getName());

                pool.shutdown();

                this.logger.debug("Thread pool " + pool.getName() + " is shut down.");
            }
            this.pools.clear();
        }
        this.logger.info("Thread pool manager stopped.");
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPoolManager#add(org.apache.sling.commons.threads.ThreadPool)
     */
    public boolean add(ThreadPool pool) {
        synchronized ( this.pools ) {
            if (null != pools.get(pool.getName())) {
                return false;
            }
            pools.put(pool.getName(), pool);
        }
        return true;
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPoolManager#get(java.lang.String)
     */
    public ThreadPool get(String name) {
        if ( name == null ) {
            name = DEFAULT_THREADPOOL_NAME;
        }
        synchronized (this.pools) {
            ThreadPool pool = this.pools.get(name);
            if ( pool == null && !(name.equals(DEFAULT_THREADPOOL_NAME))) {
                this.logger.info("Requested pool {} is not available, returning default pool.", name);
                pool = this.pools.get(DEFAULT_THREADPOOL_NAME);
            }
            return pool;
        }
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPoolManager#create(java.lang.String, org.apache.sling.commons.threads.ThreadPoolConfig)
     */
    public ThreadPool create(String name,
                             ThreadPoolConfig config) {
        if ( name == null ) {
            throw new IllegalArgumentException("Name must not be null.");
        }
        if ( config == null ) {
            throw new IllegalArgumentException("Config must not be null.");
        }
        synchronized ( this.pools ) {
            ThreadPool pool = this.pools.get(name);
            if ( pool != null ) {
                // pool already exists
                return null;
            }
            pool = new DefaultThreadPool(name, config);
            this.pools.put(name, pool);
            return pool;
        }
    }

}
