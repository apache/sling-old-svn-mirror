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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolConfig;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The DefaultThreadPool class implements the {@link ThreadPool} interface.
 * Instances of this class are managed by the {@link ThreadPoolManager}.
 *
 * @version $Id$
 */
public class DefaultThreadPool
    implements ThreadPool {

    /** By default we use the logger for this class. */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The name of this thread pool */
    protected final String name;

    /** The executor. */
    protected ThreadPoolExecutor executor;

    protected final ThreadPoolConfig configuration;

    /**
     * Create a new thread pool.
     * @param name - The name of the thread pool. If null {@link DefaultThreadPoolManager#DEFAULT_THREADPOOL_NAME}
     *               is used
     */
    public DefaultThreadPool(final String name,
                             ThreadPoolConfig origConfig) {
        this.logger.info("ThreadPool [{}] initializing ...", name);

        // name
        if ( name != null ) {
            this.name = name;
        } else {
            this.name = DefaultThreadPoolManager.DEFAULT_THREADPOOL_NAME;
        }

        this.configuration = new ThreadPoolConfig(origConfig);

        // factory
        final ThreadFactory delegateThreadFactory;
        if (this.configuration.getFactory() == null) {
            logger.warn("No ThreadFactory is configured. Will use JVM default thread factory."
                + ExtendedThreadFactory.class.getName());
            delegateThreadFactory = Executors.defaultThreadFactory();
        } else {
            delegateThreadFactory = this.configuration.getFactory();
        }
        // Min pool size
        if (this.configuration.getMinPoolSize() < 1) {
            this.configuration.setMinPoolSize(1);
            this.logger.warn("min-pool-size < 1 for pool \"" + name + "\". Set to 1");
        }
        // Max pool size
        if ( this.configuration.getMaxPoolSize() < 0 ) {
            this.configuration.setMaxPoolSize(Integer.MAX_VALUE);
        }

        // Set priority and daemon flag
        final ExtendedThreadFactory threadFactory = new ExtendedThreadFactory(delegateThreadFactory, this.configuration.getPriority(), this.configuration.isDaemon());

        // Keep alive time
        if (this.configuration.getKeepAliveTime() < 0) {
            this.configuration.setKeepAliveTime(1000);
            this.logger.warn("keep-alive-time-ms < 0 for pool \"" + name + "\". Set to 1000");
        }

        // Queue
        final BlockingQueue<Runnable> queue;
        if (this.configuration.getQueueSize() != 0) {
            if (this.configuration.getQueueSize() > 0) {
                queue = new java.util.concurrent.ArrayBlockingQueue<Runnable>(this.configuration.getQueueSize());
            } else {
                queue = new LinkedBlockingQueue<Runnable>();
            }
        } else {
            queue = new SynchronousQueue<Runnable>();
        }

        RejectedExecutionHandler handler = null;
        switch (this.configuration.getBlockPolicy()) {
            case ABORT :
                handler = new ThreadPoolExecutor.AbortPolicy();
                break;
            case DISCARD :
                handler = new ThreadPoolExecutor.DiscardPolicy();
                break;
            case DISCARDOLDEST :
                handler = new ThreadPoolExecutor.DiscardOldestPolicy();
                break;
            case RUN :
                handler = new ThreadPoolExecutor.CallerRunsPolicy();
                break;
        }
        this.executor = new ThreadPoolExecutor(this.configuration.getMinPoolSize(),
                this.configuration.getMaxPoolSize(),
                this.configuration.getKeepAliveTime(),
                TimeUnit.MILLISECONDS,
                queue,
                threadFactory,
                handler);
        this.configuration.makeReadOnly();
        this.logger.info("ThreadPool [{}] initialized.", name);
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPool#getName()
     */
    public String getName() {
	    return name;
    }


    /**
     * @see org.apache.sling.commons.threads.ThreadPool#getConfiguration()
     */
    public ThreadPoolConfig getConfiguration() {
        return this.configuration;
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPool#execute(java.lang.Runnable)
     */
    public void execute(Runnable runnable) {
        if ( this.executor == null ) {
            throw new IllegalStateException("Thread pool " + this.name + " is already shutdown.");
        }
        if ( runnable != null ) {
            if ( this.logger.isDebugEnabled() ) {
                this.logger.debug("Executing runnable: {}, pool={}, corePoolSize={}, maxPoolSize={}, queueSize={}",
                        new Object[] {runnable,
                                      this.name,
                                      this.executor.getCorePoolSize(),
                                      this.executor.getMaximumPoolSize(),
                                      this.executor.getQueue().size()});
            }

        	this.executor.execute(runnable);
        }
    }

    /**
     * @see org.apache.sling.commons.threads.ThreadPool#shutdown()
     */
    public void shutdown() {
        if ( this.executor != null ) {
            if (this.configuration.isShutdownGraceful()) {
                this.executor.shutdown();
            } else {
                this.executor.shutdownNow();
            }

            try {
                if (this.configuration.getShutdownWaitTimeMs() > 0) {
                    if (!this.executor.awaitTermination(this.configuration.getShutdownWaitTimeMs(), TimeUnit.MILLISECONDS)) {
                        logger.warn("running commands have not terminated within "
                            + this.configuration.getShutdownWaitTimeMs()
                            + "ms. Will shut them down by interruption");
                        this.executor.shutdownNow();
                    }
                }
            } catch (final InterruptedException ie) {
                this.logger.error("Cannot shutdown ThreadPool", ie);
            }
            this.executor = null;
        }
    }
}
