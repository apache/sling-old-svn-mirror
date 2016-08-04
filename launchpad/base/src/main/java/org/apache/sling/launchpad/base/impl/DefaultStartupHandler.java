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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.framework.Logger;
import org.apache.sling.launchpad.api.StartupHandler;
import org.apache.sling.launchpad.api.StartupListener;
import org.apache.sling.launchpad.api.StartupMode;
import org.apache.sling.launchpad.api.StartupService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
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
@SuppressWarnings("deprecation")
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
    private final ServiceTracker<StartupListener, StartupListener> listenerTracker;

    /** Expected bundle counts. */
    private final int expectedBundlesCount;

    /** Active bundle set. */
    private final Set<String> activeBundles = new HashSet<String>();

    /** Bundle Context. */
    private final BundleContext bundleContext;

    /** Use incremental start level handling. */
    private final boolean useIncremental;

    /** MBean startup listener. */
    private final StartupListener mbeanStartupListener;

    /** The started time. */
    private final long startedAt;

    private volatile Object[] logService;

    /**
     * Constructor.
     * @param context Bundle context
     * @param logger  Logger
     * @param manager The startup manager
     */
    public DefaultStartupHandler(final BundleContext context,
            final Logger logger,
            final StartupManager manager,
            final long startedAt) {
        this.logger = logger;
        this.bundleContext = context;
        this.startedAt = startedAt;
        this.startupMode = manager.getMode();
        this.targetStartLevel = manager.getTargetStartLevel();

        StartupListener listener = null;
        try {
            listener = new MBeanStartupListener();
        } catch ( final Exception ignore ) {
            // ignore
        }
        this.mbeanStartupListener = listener;
        this.listenerTracker = new ServiceTracker<StartupListener, StartupListener>(context, StartupListener.class,
                new ServiceTrackerCustomizer<StartupListener, StartupListener>() {

                    @Override
                    public void removedService(final ServiceReference<StartupListener> reference, final StartupListener service) {
                        context.ungetService(reference);
                    }

                    @Override
                    public void modifiedService(final ServiceReference<StartupListener> reference, final StartupListener service) {
                        // nothing to do
                    }

                    @Override
                    public StartupListener addingService(final ServiceReference<StartupListener> reference) {
                        final StartupListener listener = context.getService(reference);
                        if (listener != null) {
                            try {
                                listener.inform(startupMode, finished.get());
                            } catch (final Throwable t) {
                                logger.log(Logger.LOG_ERROR, "Error calling StartupListener " + listener, t);
                            }
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
        this.log(Logger.LOG_INFO, "Started startup handler with target start level="
               + String.valueOf(this.targetStartLevel) + ", and expected bundle count=" + String.valueOf(this.expectedBundlesCount));
        final Thread t = new Thread(this);
        t.start();
    }

    /**
     * @see org.apache.sling.launchpad.api.StartupHandler#getMode()
     */
    @Override
    public StartupMode getMode() {
        return this.startupMode;
    }

    /**
     * @see org.apache.sling.launchpad.api.StartupHandler#isFinished()
     */
    @Override
    public boolean isFinished() {
        return this.finished.get();
    }

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
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
        this.log(Logger.LOG_DEBUG, "Increasing start level to " + String.valueOf(newLevel));
        this.startLevelService.setStartLevel(newLevel);
    }

    /**
     * @see org.apache.sling.launchpad.api.StartupHandler#waitWithStartup(boolean)
     */
    @Override
    public void waitWithStartup(final boolean flag) {
        this.log(Logger.LOG_DEBUG, "Wait with startup " + flag);
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
    @Override
    public void frameworkEvent(final FrameworkEvent event) {
        if ( finished.get() ) {
            return;
        }
        this.log(Logger.LOG_DEBUG, "Received framework event " + event);

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
                    this.log(Logger.LOG_DEBUG, "Startup progress " + String.valueOf(startLevel) + '/' + String.valueOf(targetStartLevel));
                    final float ratio = (float) startLevel / (float) targetStartLevel;
                    this.startupProgress(ratio);
                }
            }
        }
    }

    private void log(final int level, final String msg) {
        log(null, level, msg, null);
    }

    private void log(final int level, final String msg, final Throwable t) {
        log(null, level, msg, t);
    }

    private void log(final ServiceReference<?> sRef, final int level, final String msg, final Throwable t) {
        boolean loggedWithService = false;
        if ( this.logService == null ) {
            final ServiceReference<?> ref = this.bundleContext.getServiceReference("org.osgi.service.log.LogService");
            if ( ref != null ) {
                final Object ls = this.bundleContext.getService(ref);
                if ( ls != null ) {
                    final Class<?>[] formalParams = {
                            ServiceReference.class,
                            Integer.TYPE,
                            String.class,
                            Throwable.class
                        };

                    try {
                        final Method logMethod = ls.getClass().getMethod("log", formalParams);
                        logMethod.setAccessible(true);
                        logService = new Object[] { ls, logMethod };
                    } catch (final NoSuchMethodException ex) {
                        // no need to log
                    }
                }
            }
        }
        if ( this.logService != null ) {
            final Object[] params = {sRef, new Integer(level), msg, t};
            try {
                ((Method) this.logService[1]).invoke(this.logService[0], params);
                loggedWithService = true;
            } catch (final InvocationTargetException ex) {
                // no need to log
            } catch (final IllegalAccessException ex) {
                // no need to log
            }
        }
        if ( !loggedWithService ) {
            logger.log(level, msg);
        }
    }

    /**
     * Notify finished startup
     */
    private void startupFinished() {
        this.log(Logger.LOG_INFO, "Startup finished in " + String.valueOf(System.currentTimeMillis() - this.startedAt) + "ms");
        this.finished.set(true);

        for (final StartupListener listener : this.listenerTracker.getServices(new StartupListener[0])) {
            try {
                listener.startupFinished(this.startupMode);
            } catch (Throwable t) {
                this.log(Logger.LOG_ERROR, "Error calling StartupListener " + listener, t);
            }
        }
        if ( this.mbeanStartupListener != null ) {
            this.mbeanStartupListener.startupFinished(this.startupMode);
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

        // register startup service
        final Dictionary<String, Object> serviceProps = new Hashtable<String, Object>();
        serviceProps.put(StartupMode.class.getName(), this.startupMode.name());
        serviceProps.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Startup Service");
        serviceProps.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        this.bundleContext.registerService(StartupService.class, new StartupService() {

            @Override
            public StartupMode getStartupMode() {
                return startupMode;
            }

            }, serviceProps);
    }

    /**
     * Notify startup progress
     * @param ratio ratio
     */
    private void startupProgress(final float ratio) {
        for (final StartupListener listener : this.listenerTracker.getServices(new StartupListener[0])) {
            try {
                listener.startupProgress(ratio);
            } catch (final Throwable t) {
                this.log(Logger.LOG_ERROR, "Error calling StartupListener " + listener, t);
            }
        }
        if ( this.mbeanStartupListener != null ) {
            this.mbeanStartupListener.startupProgress(ratio);
        }
    }

    /**
     * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
     */
    @Override
    public void bundleChanged(final BundleEvent event) {
        if (!finished.get()) {
            this.log(Logger.LOG_DEBUG, "Received bundle event " + event);

            if (event.getType() == BundleEvent.RESOLVED || event.getType() == BundleEvent.STARTED) {
                // Add (if not existing) bundle to active bundles and refresh progress bar
                activeBundles.add(event.getBundle().getSymbolicName());

                this.log(Logger.LOG_DEBUG, "Startup progress " + String.valueOf(activeBundles.size()) + '/' + String.valueOf(expectedBundlesCount));
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
