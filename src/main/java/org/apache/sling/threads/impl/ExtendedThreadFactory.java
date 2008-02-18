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

import java.util.concurrent.ThreadFactory;


/**
 * This class is responsible to create new Thread instances.
 * It's a very basic implementation.
 *
 * @version $Id$
 */
public final class ExtendedThreadFactory implements ThreadFactory {

    /** The daemon mode */
    private final boolean isDaemon;

    /** The priority of newly created Threads */
    private final int priority;

    /** The real factory. */
    private final ThreadFactory factory;

    /**
     * Create a new wrapper for a thread factory handling the
     *
     * @param priority One of {@link Thread#MIN_PRIORITY}, {@link
     *        Thread#NORM_PRIORITY}, {@link Thread#MAX_PRIORITY}
     * @param isDaemon Whether new {@link Thread}s should run as daemons.
     */
    public ExtendedThreadFactory(final ThreadFactory factory,
                                final int priority,
                                final boolean isDaemon) {
        this.isDaemon = isDaemon;
        if( ( Thread.MAX_PRIORITY == priority ) ||
                ( Thread.MIN_PRIORITY == priority ) ||
                ( Thread.NORM_PRIORITY == priority ) ) {
                this.priority = priority;
            } else {
                throw new IllegalStateException("Unknown priority " + priority);
            }
        this.factory = factory;
    }

    /**
     * Invoke the thread factory and set the daemon flag and priority.
     * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
     */
    public Thread newThread( final Runnable command ) {
        final Thread thread = this.factory.newThread(command);
        thread.setPriority( this.priority );
        thread.setDaemon( this.isDaemon );

        return thread;
    }
}
