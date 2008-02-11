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
 * This is an extension to the {@link ThreadFactory}.
 *
 * @version $Id$
 */
public interface ExtendedThreadFactory
    extends ThreadFactory {

    /**
     * Set the daemon mode of created <code>Thread</code>s should have
     *
     * @param isDaemon Whether new {@link Thread}s should run as daemons.
     */
    void setDaemon( boolean isDaemon );

    /**
     * Get the daemon mode created <code>Thread</code>s will have
     *
     * @return Whether new {@link Thread}s should run as daemons.
     */
    boolean isDaemon();

    /**
     * Set the priority newly created <code>Thread</code>s should have
     *
     * @param priority One of {@link Thread#MIN_PRIORITY}, {@link
     *        Thread#NORM_PRIORITY}, {@link Thread#MAX_PRIORITY}
     */
    void setPriority( int priority );

    /**
     * Get the priority newly created <code>Thread</code>s will have
     *
     * @return One of {@link Thread#MIN_PRIORITY}, {@link
     *         Thread#NORM_PRIORITY}, {@link Thread#MAX_PRIORITY}
     */
    int getPriority();

    /**
     * Create a new Thread for a {@link Runnable} command
     *
     * @param command The <code>Runnable</code>
     *
     * @return new <code>Thread</code>
     */
    Thread newThread( Runnable command );
}
