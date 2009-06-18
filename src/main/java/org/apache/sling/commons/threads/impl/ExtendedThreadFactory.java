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

import java.util.concurrent.ThreadFactory;

import org.apache.sling.commons.threads.ThreadPoolConfig;


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
     * @param priority A non null value.
     * @param isDaemon Whether new {@link Thread}s should run as daemons.
     */
    public ExtendedThreadFactory(final ThreadFactory factory,
                                final ThreadPoolConfig.ThreadPriority priority,
                                final boolean isDaemon) {
        this.isDaemon = isDaemon;
        if ( priority == null ) {
            throw new IllegalStateException("Prioriy must not be null.");
        }
        switch ( priority ) {
            case NORM : this.priority = Thread.NORM_PRIORITY;
                        break;
            case MIN  : this.priority = Thread.MIN_PRIORITY;
                        break;
            case MAX  : this.priority = Thread.MAX_PRIORITY;
                        break;
            default: // this can never happen
                        this.priority = Thread.NORM_PRIORITY;
                        break;
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
