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

import java.lang.invoke.MethodHandles;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * An extension of ThreadPoolExecutor which automatically gets rid of all {@link ThreadLocal} references before a thread is going to be reused.
 * @see ThreadLocalCleaner
 */
public class ThreadPoolExecutorCleaningThreadLocals extends ThreadPoolExecutor {
    private final ThreadLocalChangeListener listener;

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public ThreadPoolExecutorCleaningThreadLocals(int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            ThreadFactory threadFactory,
            RejectedExecutionHandler handler,
            ThreadLocalChangeListener listener) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, 
                workQueue, threadFactory, handler);
        this.listener = listener;
    }

    private static final ThreadLocal<ThreadLocalCleaner> local = new ThreadLocal<>();

    protected void beforeExecute(Thread t, Runnable r) {
        LOGGER.debug("Collecting changes to ThreadLocal for thread {} from now on...", t);
        ThreadLocalCleaner cleaner = new ThreadLocalCleaner(listener);
        local.set(cleaner);
    }

    protected void afterExecute(Runnable r, Throwable t) {
        LOGGER.debug("Cleaning up thread locals for thread {}...", Thread.currentThread());
        ThreadLocalCleaner cleaner = local.get();
        local.remove();
        cleaner.cleanup();
    }
}
