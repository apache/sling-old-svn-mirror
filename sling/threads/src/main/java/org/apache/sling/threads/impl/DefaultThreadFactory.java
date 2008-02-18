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


/**
 * This class is responsible to create new Thread instances.
 * It's a very basic implementation.
 *
 * @version $Id$
 */
public class DefaultThreadFactory
    implements ExtendedThreadFactory {

    /** The daemon mode */
    private boolean isDaemon = DefaultThreadPoolManager.DEFAULT_DAEMON_MODE;

    /** The priority of newly created Threads */
    private int priority = DefaultThreadPoolManager.DEFAULT_THREAD_PRIORITY;

    /**
     * @see org.apache.sling.threads.impl.ExtendedThreadFactory#setDaemon(boolean)
     */
    public void setDaemon( boolean isDaemon ) {
        this.isDaemon = isDaemon;
    }

    /**
     * @see org.apache.sling.threads.impl.ExtendedThreadFactory#isDaemon()
     */
    public boolean isDaemon() {
        return this.isDaemon;
    }

    /**
     * @see org.apache.sling.threads.impl.ExtendedThreadFactory#setPriority(int)
     */
    public void setPriority( final int priority ) {
        if( ( Thread.MAX_PRIORITY == priority ) ||
            ( Thread.MIN_PRIORITY == priority ) ||
            ( Thread.NORM_PRIORITY == priority ) ) {
            this.priority = priority;
        } else {
            throw new IllegalStateException("Unknown priority " + priority);
        }
    }

    /**
     * @see org.apache.sling.threads.impl.ExtendedThreadFactory#getPriority()
     */
    public int getPriority() {
        return this.priority;
    }

    /**
     * @see org.apache.sling.threads.impl.ExtendedThreadFactory#newThread(java.lang.Runnable)
     */
    public Thread newThread( final Runnable command ) {
        final Thread thread = new Thread( command );
        thread.setPriority( this.priority );
        thread.setDaemon( this.isDaemon );

        return thread;
    }
}
