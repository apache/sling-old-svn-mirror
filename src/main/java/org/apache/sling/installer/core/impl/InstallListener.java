/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.installer.core.impl;

import org.apache.sling.installer.api.event.InstallationEvent;
import org.apache.sling.installer.api.event.InstallationListener;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;

/**
 * Proxy component for notifying all registered {@link InstallationListener}s.
 */
public class InstallListener implements InstallationListener {

    /** Start event. */
    private static final InstallationEvent START_EVENT = new InstallationEvent() {

        public TYPE getType() { return TYPE.STARTED; }

        public Object getSource() { return null; }
    };

    /** Suspend event. */
    private static final InstallationEvent SUSPENDED_EVENT = new InstallationEvent() {

        public TYPE getType() { return TYPE.SUSPENDED; }

        public Object getSource() { return null; }
    };

    /** Service tracker for the listeners. */
    private final ServiceTracker tracker;

    /** Flag avoiding sending duplicate events. */
    private volatile boolean started = false;

    /** Logger. */
    private final Logger logger;

    /**
     * Start service tracker.
     */
    public InstallListener(final BundleContext context, final Logger logger) {
        this.logger = logger;
        this.tracker = new ServiceTracker(context, InstallationListener.class.getName(), null);
        this.tracker.open();
    }

    /**
     * Stop service tracker.
     */
    public void dispose() {
        this.tracker.close();
    }

    /**
     * @see org.apache.sling.installer.api.event.InstallationListener#onEvent(org.apache.sling.installer.api.event.InstallationEvent)
     */
    public void onEvent(final InstallationEvent event) {
        if ( this.logger.isDebugEnabled() ) {
            if ( event.getType() == InstallationEvent.TYPE.STARTED ) {
                logger.debug("onEvent(STARTED).");
            } else if ( event.getType() == InstallationEvent.TYPE.SUSPENDED ) {
                logger.debug("onEvent(SUSPENDED).");
            } else {
                final TaskResource src = (TaskResource)event.getSource();
                logger.debug("onEvent(event.getSource(): {})", src);
            }
        }
        final Object[] listeners = this.tracker.getServices();
        if ( listeners != null ) {
            for(final Object l : listeners) {
                if ( l instanceof InstallationListener ) {
                    ((InstallationListener)l).onEvent(event);
                }
            }
        }
    }

    /**
     * Send started event.
     */
    public synchronized void start() {
        if ( ! this.started ) {
            this.started = true;
            this.onEvent(START_EVENT);
        }
    }

    /**
     * Send suspended event.
     */
    public synchronized void suspend() {
        if ( this.started ) {
            this.started = false;
            this.onEvent(SUSPENDED_EVENT);
        }
    }
}
