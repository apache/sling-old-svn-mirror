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
package org.apache.sling.threads.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.sling.threads.ThreadPool;
import org.apache.sling.threads.ThreadPoolManager;
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

    /** Should we wait for running jobs to terminate on shutdown ? */
    protected final boolean shutdownGraceful;

    /** How long to wait for running jobs to terminate on disposition */
    protected final int shutdownWaitTimeMs;

    /**
     * Create a new thread pool.
     * @param name - The name of the thread pool. If null {@link DefaultThreadPoolManager#DEFAULT_THREADPOOL_NAME}
     *               is used
     */
    public DefaultThreadPool(final String name,
                             int   minPoolSize,
                             int   maxPoolSize,
                             final int queueSize,
                             long  keepAliveTime,
                             ThreadPoolManager.ThreadPoolPolicy blockPolicy,
                             final boolean shutdownGraceful,
                             final int shutdownWaitTimeMs,
                             final ThreadFactory factory,
                             final int   priority,
                             final boolean isDaemon) {
        this.logger.info("ThreadPool [{}] initializing ...", name);

        // name
        if ( name != null ) {
            this.name = name;
        } else {
            this.name = DefaultThreadPoolManager.DEFAULT_THREADPOOL_NAME;
        }

        // factory
        final ThreadFactory threadFactory;
        if (factory == null) {
            logger.warn("No ThreadFactory is configured. Will use a "
                + DefaultThreadFactory.class.getName());
            threadFactory = new DefaultThreadFactory();
        } else {
            threadFactory = factory;
        }

        // Min pool size
        // make sure we have enough threads for the default thread pool as we
        // need one for ourself
        if (DefaultThreadPoolManager.DEFAULT_THREADPOOL_NAME.equals(name)
            && ((minPoolSize > 0) && (minPoolSize < DefaultThreadPoolManager.DEFAULT_MIN_POOL_SIZE))) {
            minPoolSize = DefaultThreadPoolManager.DEFAULT_MIN_POOL_SIZE;
        } else if (minPoolSize < 1) {
            minPoolSize = 1;
            this.logger.warn("min-pool-size < 1 for pool \"" + name + "\". Set to 1");
        }
        // Max pool size
        maxPoolSize = (maxPoolSize < 0) ? Integer.MAX_VALUE : maxPoolSize;

        // Set priority and daemon if the factory is an extended factory
        if ( threadFactory instanceof ExtendedThreadFactory ) {
            final ExtendedThreadFactory extTF = (ExtendedThreadFactory)threadFactory;
            extTF.setPriority(priority);
            extTF.setDaemon(isDaemon);
        } else {
            if ( priority != Thread.NORM_PRIORITY ) {
                this.logger.warn("ThreadFactory " + threadFactory + " does not support setting the priority or daemon setting.");
            }
            if ( isDaemon != DefaultThreadPoolManager.DEFAULT_DAEMON_MODE ) {
                this.logger.warn("ThreadFactory " + threadFactory + " does not support setting the daemon mode.");
            }
        }

        // Keep alive time
        if (keepAliveTime < 0) {
            keepAliveTime = 1000;
            this.logger.warn("keep-alive-time-ms < 0 for pool \"" + name + "\". Set to 1000");
        }

        // Queue
        final BlockingQueue<Runnable> queue;
        if (queueSize != 0) {
            if (queueSize > 0) {
                queue = new java.util.concurrent.ArrayBlockingQueue<Runnable>(queueSize);
            } else {
                queue = new LinkedBlockingQueue<Runnable>();
            }
        } else {
            queue = new SynchronousQueue<Runnable>();
        }

        if ( blockPolicy == null ) {
            blockPolicy = DefaultThreadPoolManager.DEFAULT_BLOCK_POLICY;
        }
        RejectedExecutionHandler handler = null;
        switch (blockPolicy) {
            case ABORT :
                handler = new ThreadPoolExecutor.AbortPolicy();
                break;
            case DISCARD :
                handler = new ThreadPoolExecutor.AbortPolicy();
                break;
            case DISCARDOLDEST :
                handler = new ThreadPoolExecutor.AbortPolicy();
                break;
            case RUN :
                handler = new ThreadPoolExecutor.AbortPolicy();
                break;
        }
        this.shutdownGraceful = shutdownGraceful;
        this.shutdownWaitTimeMs = shutdownWaitTimeMs;
        this.executor = new ThreadPoolExecutor(minPoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.MILLISECONDS,
                queue,
                threadFactory,
                handler);
        this.logger.info("ThreadPool [{}] initialized.", name);
    }

    /**
     * @see org.apache.sling.threads.ThreadPool#getName()
     */
    public String getName() {
	    return name;
    }

    /**
     * @see org.apache.sling.threads.ThreadPool#getMaxPoolSize()
     */
    public int getMaxPoolSize() {
        return this.executor.getMaximumPoolSize();
    }

    /**
     * @see org.apache.sling.threads.ThreadPool#execute(java.lang.Runnable)
     */
    public void execute(Runnable runnable) {
        if ( this.executor == null ) {
            throw new IllegalStateException("Thread pool " + this.name + " is already shutdown.");
        }
        if ( runnable != null ) {
            this.logger.debug("Executing runnable: {},pool={}", runnable, this.name);

        	this.executor.execute(runnable);
        }
    }

    /**
     * @see org.apache.sling.threads.ThreadPool#shutdown()
     */
    public void shutdown() {
        if ( this.executor != null ) {
            if (shutdownGraceful) {
                this.executor.shutdown();
            } else {
                this.executor.shutdownNow();
            }

            try {
                if (this.shutdownWaitTimeMs > 0) {
                    if (!this.executor.awaitTermination(this.shutdownWaitTimeMs, TimeUnit.MILLISECONDS)) {
                        logger.warn("running commands have not terminated within "
                            + this.shutdownWaitTimeMs
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
