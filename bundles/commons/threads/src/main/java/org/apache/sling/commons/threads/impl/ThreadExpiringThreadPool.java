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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * An extension of ThreadPoolExecutor, which keeps track of the age
 * of the worker threads and expires them when they get older than
 * a specified max-age.
 * <br/>
 * To be precise, a thread is expired when it finishes processing
 * a task and its max-age has been exceeded at that time. I.e. if a
 * thread is idle past its expiry, it may still process a single
 * task before it is expired.
 */
public class ThreadExpiringThreadPool extends ThreadPoolExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadExpiringThreadPool.class);

    /**
     * Map from thread-id to the time (in milliseconds) when a thread was first used to
     * process a task. This is used to look determine when a thread is to be expired.
     */
    private final ConcurrentHashMap<Long, Long> threadStartTimes;

    /**
     * Thread max-age in milliseconds.
     */
    private final long maxThreadAge;

    /**
     * Convenience flag indicating whether threads expire or not.
     * This is equivalent to {@code maxThreadAge >= 0}.
     */
    private final boolean enableThreadExpiry;

    /**
     * Marker exception object thrown to terminate threads that have
     * reached or exceeded their max-age. This exception is intentionally
     * used for (minimal) control flow, i.e. the {@code ThreadPoolExecutor}
     * will dispose of any thread that threw an exception and create a new
     * one in its stead. This exception should never show up in any logs,
     * otherwise it is a bug.
     */
    private final RuntimeException expiredThreadException;

    public ThreadExpiringThreadPool(
            final int corePoolSize,
            final int maximumPoolSize,
            final long maxThreadAge,
            final TimeUnit maxThreadAgeUnit,
            final long keepAliveTime,
            final TimeUnit keepAliveTimeUnit,
            final BlockingQueue<Runnable> workQueue,
            final ThreadFactory threadFactory,
            final RejectedExecutionHandler handler
    ) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, keepAliveTimeUnit, workQueue, threadFactory, handler);
        this.threadStartTimes = new ConcurrentHashMap<Long, Long>(maximumPoolSize);
        this.maxThreadAge = TimeUnit.MILLISECONDS.convert(maxThreadAge, maxThreadAgeUnit);
        this.enableThreadExpiry = maxThreadAge >= 0;
        this.expiredThreadException = new RuntimeException("Kill old thread");
    }

    @Override
    protected void beforeExecute(final Thread thread, final Runnable runnable) {
        if (enableThreadExpiry) {
            recordStartTime(thread);
        }
        super.beforeExecute(thread, runnable);
    }

    private void recordStartTime(final Thread thread) {
        final long threadId = thread.getId();
        if (threadStartTimes.putIfAbsent(threadId, System.currentTimeMillis()) == null) {
            LOG.debug("{} used for the first time.", thread);

            // The uncaught exception handler makes sure that the exception
            // signalling the death of a thread is swallowed. All other
            // Throwables are handed to the originalHandler.
            final Thread.UncaughtExceptionHandler originalHandler = thread.getUncaughtExceptionHandler();
            thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(final Thread thread, final Throwable throwable) {
                    // first reset the original uncaught exception handler - just as a precaution
                    thread.setUncaughtExceptionHandler(originalHandler);

                    // ignore expected exception thrown to terminate the thread
                    if (throwable == expiredThreadException) {
                        return;
                    }

                    // delegate any other exceptions to the original uncaught exception handler
                    if (originalHandler != null) {
                        originalHandler.uncaughtException(thread, throwable);
                    }
                }
            });
        }
    }

    @Override
    protected void afterExecute(final Runnable runnable, final Throwable throwable) {
        super.afterExecute(runnable, throwable);
        if (throwable == null && enableThreadExpiry) {
            checkMaxThreadAge(Thread.currentThread());
        }
    }

    private void checkMaxThreadAge(final Thread thread) {
        final long now = System.currentTimeMillis();
        final long threadId = thread.getId();
        final Long started = threadStartTimes.get(threadId);
        if (started != null && now >= started + maxThreadAge) {
            final long delta = now - (started + maxThreadAge);
            LOG.debug("{} exceeded its max age by {}ms and will be replaced.", thread, delta);
            threadStartTimes.remove(threadId);

            // throw marker exception to kill this thread and thus trigger creation of a new one
            throw expiredThreadException;
        }
    }
}
