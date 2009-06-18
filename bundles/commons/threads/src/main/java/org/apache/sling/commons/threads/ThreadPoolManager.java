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


/**
 * The <cod>ThreadPoolManager</code> manages thread pools.
 *
 * @version $Id$
 */
public interface ThreadPoolManager {

    /** The default thread pool name */
    String DEFAULT_THREADPOOL_NAME = "default";

    /**
     * Add a new pool.
     * If a pool with the same name already exists, the new pool is not added
     * and false is returned.
     * @param pool The pool.
     * @return True if the pool could be added, false otherwise.
     */
    boolean add(ThreadPool pool);

    /**
     * Get a thread pool.
     * If there is no thread pool with the given name, the default thread
     * pool is returned.
     * @param name The name of the thread pool or null for the default pool.
     */
    ThreadPool get(String name);

    /**
     * Create a new thread pool.
     * If a pool with the same name already exists, no new pool is created
     * and <code>null</code> is returned.
     * @param name Name must not be null.
     * @param config The thread pool configuration.
     */
    ThreadPool create(String name,
                      ThreadPoolConfig config);
}
