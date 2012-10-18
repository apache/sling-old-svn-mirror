/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.commons.threads.jmx;

/**
 * This is the management interface for a Sling Thread Pool.
 */
public interface ThreadPoolMBean {

    /**
     * Retrieve the block policy of the thread pool.
     * 
     * @return the block policy
     */
    String getBlockPolicy();

    /**
     * Retrieve the active count from the pool's Executor.
     * 
     * @return the active count or -1 if the thread pool does not have an Executor
     */
    int getExecutorActiveCount();

    /**
     * Retrieve the completed task count from the pool's Executor.
     * 
     * @return the completed task count or -1 if the thread pool does not have an Executor
     */
    long getExecutorCompletedTaskCount();

    /**
     * Retrieve the core pool size from the pool's Executor.
     * 
     * @return the core pool size or -1 if the thread pool does not have an Executor
     */
    int getExecutorCorePoolSize();

    /**
     * Retrieve the largest pool size from the pool's Executor.
     * 
     * @return the largest pool size or -1 if the thread pool does not have an Executor
     */
    int getExecutorLargestPoolSize();

    /**
     * Retrieve the maximum pool size from the pool's Executor.
     * 
     * @return the maximum pool size or -1 if the thread pool does not have an Executor
     */
    int getExecutorMaximumPoolSize();


    /**
     * Retrieve the pool size from the pool's Executor.
     * 
     * @return the pool size or -1 if the thread pool does not have an Executor
     */
    int getExecutorPoolSize();


    /**
     * Retrieve the task count from the pool's Executor.
     * 
     * @return the task count or -1 if the thread pool does not have an Executor
     */
    long getExecutorTaskCount();

    /**
     * Return the configured keep alive time.
     * 
     * @return The configured keep alive time.
     */
    long getKeepAliveTime();

    /**
     * Return the configured maximum pool size.
     * 
     * @return The configured maximum pool size.
     */
    int getMaxPoolSize();

    /**
     * Return the minimum pool size.
     * 
     * @return The minimum pool size.
     */
    int getMinPoolSize();

    /**
     * Return the name of the thread pool
     * 
     * @return the name
     */
    String getName();

    /**
     * Return the configuration pid of the thread pool.
     * 
     * @return the pid
     */
    String getPid();

    /**
     * Return the configured priority of the thread pool.
     * 
     * @return the priority
     */
    String getPriority();

    /**
     * Return the configured queue size.
     * 
     * @return The configured queue size.
     */
    int getQueueSize();

    /**
     * Return the configured shutdown wait time in milliseconds.
     * 
     * @return The configured shutdown wait time.
     */
    int getShutdownWaitTimeMs();

    /**
     * Return whether or not the thread pool creates daemon threads.
     * 
     * @return The daemon configuration.
     */
    boolean isDaemon();

    /**
     * Return whether or not the thread pool is configured to shutdown gracefully.
     * 
     * @return The graceful shutdown configuration.
     */
    boolean isShutdownGraceful();

    /**
     * Return whether or not the thread pool is in use.
     * 
     * @return The used state of the pool.
     */
    boolean isUsed();

}
