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

import org.osgi.annotation.versioning.ProviderType;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * The thread pool interface allows to start runnables by
 * getting threads from a managed pool.
 */
@ProviderType
public interface ThreadPool extends Executor {

    /**
     * Submits a callable for execution
     * @param callable The {@link Callable} to submit
     * @param <T> The generic type for the callable.
     * @return A {@link Future} representing pending completion of the {@link Callable}
     * @since 3.2
     */
    <T> Future<T> submit(Callable<T> callable);

    /**
     * Submits a runnable for execution
     * @param runnable The {@link Runnable} to submit
     * @return A {@link Future} representing pending completion of the {@link Runnable}
     * @since 3.2
     */
    Future<?> submit(Runnable runnable);

    /**
     * The name of the thread pool.
     * @return The thread pool name.
     */
    String getName();

    /**
     * The thread pool configuration.
     * @return The thread pool configuration
     */
    ThreadPoolConfig getConfiguration();
}
