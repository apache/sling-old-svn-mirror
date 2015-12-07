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

import java.util.Locale;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.commons.threads.ThreadPoolConfig;


/**
 * This class is responsible to create new Thread instances.
 * It's a very basic implementation.
 *
 */
public final class ExtendedThreadFactory implements ThreadFactory {

    /** Template for thread names, for use with String#format() */
    private static final String THREAD_NAME_TEMPLATE = "sling-%s-%d";

    /** The real factory. */
    private final ThreadFactory factory;

    /** The name of the thread pool */
    private final String name;

    /** The priority of newly created Threads */
    private final int priority;

    /** The daemon mode */
    private final boolean isDaemon;

    /** Thread counter for use in thread name */
    private final AtomicInteger threadCounter;

    /**
     * Create a new wrapper for a thread factory handling the
     *
     * @param name The name of the thread pool.
     * @param priority A non null value.
     * @param isDaemon Whether new {@link Thread}s should run as daemons.
     */
    public ExtendedThreadFactory(final ThreadFactory factory,
                                 final String name,
                                 final ThreadPoolConfig.ThreadPriority priority,
                                 final boolean isDaemon) {
        this.factory = factory;
        this.name = normalizeName(name);
        this.priority = convertPriority(priority);
        this.isDaemon = isDaemon;
        this.threadCounter = new AtomicInteger(1);
    }

    private String normalizeName(final String name) {
        final String n = name.toLowerCase(Locale.ENGLISH).replaceAll("\\s+", "-");
        return stripPrefixes(n, "apache-sling-", "sling-");
    }

    private int convertPriority(final ThreadPoolConfig.ThreadPriority priority) {
        if (priority == null) {
            throw new IllegalStateException("Prioriy must not be null.");
        }
        switch (priority) {
            case MIN  :
                return Thread.MIN_PRIORITY;
            case MAX  :
                return Thread.MAX_PRIORITY;
            case NORM :
            default: // this can never happen
                return Thread.NORM_PRIORITY;
        }
    }

    /**
     * Invoke the thread factory and set the daemon flag and priority.
     * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
     */
    public Thread newThread( final Runnable command ) {
        final Thread thread = this.factory.newThread(command);
        thread.setName(nextThreadName());
        thread.setPriority( this.priority );
        thread.setDaemon( this.isDaemon );

        return thread;
    }

    private String nextThreadName() {
        return String.format(THREAD_NAME_TEMPLATE, this.name, this.threadCounter.getAndIncrement());
    }

    private static String stripPrefixes(final String name, final String... prefixes) {
        for (final String prefix : prefixes) {
            if (name.startsWith(prefix)) {
                return name.substring(prefix.length());
            }
        }
        return name;
    }
}
