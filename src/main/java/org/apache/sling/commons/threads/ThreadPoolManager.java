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


import aQute.bnd.annotation.ProviderType;

/**
 * The <code>ThreadPoolManager</code> manages thread pools.
 *
 */
@ProviderType
public interface ThreadPoolManager {

    /** The default thread pool name */
    String DEFAULT_THREADPOOL_NAME = "default";

    /**
     * Get a thread pool.
     * If there is no thread pool with the given name, a new thread pool with
     * the default configuration is created and returned.
     * A thread pool must be released when not used anymore with the
     * {@link #release(ThreadPool)} method.
     * @param name The name of the thread pool or null for the default pool.
     * @return A thread pool.
     */
    ThreadPool get(String name);

    /**
     * Create a new thread pool with this configuration.
     * A thread pool must be released when not used anymore with the
     * {@link #release(ThreadPool)} method.
     * @param config The thread pool configuration.
     * @return A new thread pool.
     */
    ThreadPool create(ThreadPoolConfig config);

    /**
     * Create a new thread pool with this configuration.
     * A thread pool must be released when not used anymore with the
     * {@link #release(ThreadPool)} method.
     * @param config The thread pool configuration.
     * @param label  An optional label for the thread pool. The label
     *               will be appended to the name of the pool.
     * @return A new thread pool.
     * @since 3.1
     */
    ThreadPool create(ThreadPoolConfig config, String label);

    /**
     * Release the thread pool again.
     * @param pool The thread pool to release
     */
    void release(ThreadPool pool);
}
