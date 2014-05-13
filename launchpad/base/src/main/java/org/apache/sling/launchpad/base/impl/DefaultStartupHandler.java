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
package org.apache.sling.launchpad.base.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.framework.Logger;
import org.apache.sling.launchpad.api.StartupHandler;
import org.apache.sling.launchpad.api.StartupListener;
import org.apache.sling.launchpad.api.StartupMode;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The installation listener is listening for
 * - framework events
 * - events from other services
 *
 * It notifies listeners about the finish of the startup and
 * about an approximate progress.
 *
 * @see StartupHandler
 * @see StartupListener
 * @since 2.4.0
 */
public class DefaultStartupHandler
    implements StartupHandler, BundleListener, FrameworkListener, Runnable {

    /** Logger. */
    private final Logger logger;

    /** Marker for finished. */
    private final AtomicBoolean finished = new AtomicBoolean(false);

    /** Marker if startup should wait */
    private final AtomicInteger startupShouldWait = new AtomicInteger(0);

    /** The queue for increasing the start level. */
    private final BlockingQueue<Boolean> queue = new LinkedBlockingQueue<Boolean>();

    /** The start level service. */
    private final StartLevel startLevelService;

    /** The target start level. */
    private final long targetStartLevel;

    /** The startup mode. */
    private final StartupMode startupMode;

    /** Service tracker for startup listeners. */
    private final ServiceTracker listenerTracker;

    /** Expected bundle counts. */
    private final int expectedBundlesCount;

    /** Active bundle set. */
    private final Set<String> activeBundles = new HashSet<String>();

    /** Bundle Context. */
    private final BundleContext bundleContext;

    /** Use incremental start level handling. */
    private final boolean useIncremental;

    /**
     * Constructor.
     * @param context Bundle context
     * @param logger  Logger
     * @param manager The startup manager
     */
    public DefaultStartupHandler(final BundleContext context, final Logger logger, final StartupManager manager) {
        this.logger = logger;
        this.bundleContext = context;
        this.startupMode = manager.getMode();
        this.targetStartLevel = manager.getTargetStartLevel();

        this.listenerTracker = new ServiceTracker(context, StartupListener.class.getName(),
                new ServiceTrackerCustomizer() {

                    public void removedService(final ServiceReference reference, final Object service) {
                        context.ungetService(reference);
                    }

                    public void modifiedService(final ServiceReference reference, final Object service) {
                        // nothing to do
                    }

                    public Object addingService(final ServiceReference reference) {
                        final StartupListener listener = (StartupListener) context.getService(reference);
                        if ( listener != null ) {
                            listener.inform(startupMode, finished.get());
                        }
                        return listener;
                    }
                });
        this.listenerTracker.open();
        this.startLevelService = (StartLevel)context.getService(context.getServiceReference(StartLevel.class.getName()));
        context.addFrameworkListener(this);

        this.useIncremental = this.startupMode != StartupMode.RESTART && manager.isIncrementalStartupEnabled();

        if ( !this.useIncremental ) {
            final Bundle[] bundles = context.getBundles();
            this.expectedBundlesCount = (bundles != null && bundles.length > 0 ? bundles.length : 10);

            context.addBundleListener(this);
        } else {
            this.expectedBundlesCount = 10;
        }

        this.bundleContext.registerService(StartupHandler.class.getName(), this, null);
        logger.log(Logger.LOG_INFO, "Started startup handler with target start level="
               + String.valueOf(this.targetStartLevel) + ", and expected bundle count=" + String.valueOf(this.expectedBundlesCount));
        final Thread t = new Thread(this);
        t.start();
    }

    /**
     * @see org.apache.sling.launchpad.api.StartupHandler#getMode()
     */
    public StartupMode getMode() {
        return this.startupMode;
    }

    /**
     * @see org.apache.sling.launchpad.api.StartupHandler#isFinished()
     */
    public boolean isFinished() {
        return this.finished.get();
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        while ( !this.finished.get() ) {
            Boolean doInc = null;
            try {
                doInc = this.queue.take();
            } catch (final InterruptedException e) {
                // ignore
            }
            if ( doInc != null && doInc ) {
                // if the installer is idle we first wait
                // we have to do this to give the installer or plugins for the installer,
                // time to start after the start level has changed
                if ( this.startupShouldWait.get() == 0 ) {
                    this.sleep(2000L);
                }
                // now we wait until the installer is idle
                while ( this.startupShouldWait.get() != 0 ) {
                    this.sleep(50L);
                }
                this.incStartLevel();
            }
        }
    }

    /**
     * Increment the current start level by one
     */
    private void incStartLevel() {
        final int newLevel = this.startLevelService.getStartLevel() + 1;
        logger.log(Logger.LOG_DEBUG, "Increasing start level to " + String.valueOf(newLevel));
        this.startLevelService.setStartLevel(newLevel);
    }

    /**
     * @see org.apache.sling.launchpad.api.StartupHandler#waitWithStartup(boolean)
     */
    public void waitWithStartup(final boolean flag) {
        logger.log(Logger.LOG_DEBUG, "Wait with startup " + flag);
        if ( flag ) {
            this.startupShouldWait.incrementAndGet();
        } else {
            this.startupShouldWait.decrementAndGet();
        }
    }

    /**
     * Sleep a little bit
     * @param time Sleeping time
     */
    private void sleep(final long time) {
        try {
            Thread.sleep(time);
        } catch ( final InterruptedException e) {
            // ignore
        }
    }

    /**
     * Put a task in the queue
     * @param info Add new boolean information to queue
     */
    private void enqueue(final boolean info) {
        try {
            this.queue.put(info);
        } catch (final InterruptedException e) {
            // ignore
        }
    }

    /**
     * @see org.osgi.framework.FrameworkListener#frameworkEvent(org.osgi.framework.FrameworkEvent)
     */
    public void frameworkEvent(final FrameworkEvent event) {
        if ( finished.get() ) {
            return;
        }
        logger.log(Logger.LOG_DEBUG, "Received framework event " + event);

        if ( !this.useIncremental ) {
            // restart
            if ( event.getType() == FrameworkEvent.STARTED ) {
                this.startupFinished();
            }

        } else {
            // first startup or update
            if ( event.getType() == FrameworkEvent.STARTED ) {
                this.enqueue(true);

            } else if ( event.getType() == FrameworkEvent.STARTLEVEL_CHANGED ) {
                if ( this.startLevelService.getStartLevel() >= this.targetStartLevel ) {
                    this.startupFinished();
                } else {
                    this.enqueue(true);
                    final int startLevel = this.startLevelService.getStartLevel();
                    logger.log(Logger.LOG_INFO, "Startup progress " + String.valueOf(startLevel) + '/' + String.valueOf(targetStartLevel));
                    final float ratio = (float) startLevel / (float) targetStartLevel;
                    this.startupProgress(ratio);
                }
            }
        }
    }

    /**
     * Notify finished startup
     */
    private void startupFinished() {
        logger.log(Logger.LOG_INFO, "Startup finished.");
        this.finished.set(true);

        final Object[] listeners = this.listenerTracker.getServices();
        if ( listeners != null ) {
            for(final Object l : listeners) {
                if ( l instanceof StartupListener ) {
                    ((StartupListener) l).startupFinished(this.startupMode);
                }
            }
        }
        // stop the queue
        this.enqueue(false);

        // clear bundle set
        this.activeBundles.clear();

        // unregister listeners
        if ( !this.useIncremental ) {
            this.bundleContext.removeBundleListener(this);
        }
        this.bundleContext.removeFrameworkListener(this);
    }

    /**
     * Notify startup progress
     * @param ratio ratio
     */
    private void startupProgress(final float ratio) {
        final Object[] listeners = this.listenerTracker.getServices();
        if ( listeners != null ) {
            for(final Object l : listeners) {
                if ( l instanceof StartupListener ) {
                    ((StartupListener) l).startupProgress(ratio);
                }
            }
        }
    }

    /**
     * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
     */
    public void bundleChanged(final BundleEvent event) {
        if (!finished.get()) {
            logger.log(Logger.LOG_DEBUG, "Received bundle event " + event);

            if (event.getType() == BundleEvent.RESOLVED || event.getType() == BundleEvent.STARTED) {
                // Add (if not existing) bundle to active bundles and refresh progress bar
                activeBundles.add(event.getBundle().getSymbolicName());

                logger.log(Logger.LOG_INFO, "Startup progress " + String.valueOf(activeBundles.size()) + '/' + String.valueOf(expectedBundlesCount));
                final float ratio = (float) activeBundles.size() / (float) expectedBundlesCount;
                this.startupProgress(ratio);
            } else if (event.getType() == BundleEvent.STOPPED) {
                // Only remove bundle from active bundles,
                // but do not refresh progress bar, to prevent progress bar from going back
                activeBundles.remove(event.getBundle().getSymbolicName());
            }
        }
    }
}
