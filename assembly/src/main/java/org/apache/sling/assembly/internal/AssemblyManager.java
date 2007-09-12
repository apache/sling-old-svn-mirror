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
package org.apache.sling.assembly.internal;

import java.io.PrintStream;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.sling.assembly.installer.Installer;
import org.apache.sling.assembly.installer.InstallerService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.log.LogService;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The <code>AssemblyManager</code> class implements the Assembly Manager
 * keeping track of all Assembly Bundles installed in the framework and calling
 * lifecycle methods on the {@link Assembly} instances representing the Assembly
 * Bundles.
 * <p>
 * All bundle events are handled asynchronously in a separate thread, which is
 * started by the {@link #start()} method and terminated by the {@link #stop()}
 * method. The {@link #run()} method continuosly waits for
 * <code>BundleEvent</code>s to arrive until an event is received whose type
 * is <code>-1</code> indicating termination of service of this Assembly
 * Manager.
 */
public class AssemblyManager implements Runnable {

    /**
     * The special bundle event type value used to terminate the background
     * thread (value is -1).
     */
    private static final int TERMINATION_EVENT = -1;

    /**
     * An array of log flag strings used by the
     * {@link #log(int, String, Throwable)} method to mark log messages when the
     * log service is not available.
     */
    private static final String[] LOG_FLAGS = { "*NULL *", "*ERROR*",
        "*WARN *", "*INFO *", "*DEBUG*" };

    /**
     * The <code>BundleContext</code> of the bundle to which this Assembly
     * Manager belongs.
     */
    private BundleContext context;

    /**
     * The {@link InstallerService} implemented and used by this bundle.
     */
    private InstallerService installerService;

    /**
     * The background thread handling bundle events asychronously.
     */
    private Thread queueHandler;

    /**
     * The queue of bundle events posted by the {@link #put(BundleEvent)} method
     * and retrieved from by the {@link #take()} method.
     */
    private LinkedList queue = new LinkedList();

    /**
     * The map of {@link Assembly} instances indexed by the bundle location of
     * the respective Assembly Bundle.
     */
    private Map assemblies = new HashMap();

    /**
     * The map of {@link InstalledBundle} instances indexed by the bundle
     * specification common location. This map contains all installed bundles
     * installed by all assemblies.
     */
    private Map installedBundles = new HashMap();

    /**
     * The service tracker keeping the <code>StartLevel</code> service.
     */
    private ServiceTracker startLevelTracker;

    /**
     * The service tracker keeping the <code>LogService</code>.
     */
    private ServiceTracker logServiceTracker;

    /**
     * Creates the Assembly Manager using the <code>BundleContext</code> to
     * access foreign service from the framework. To activate the Assembly
     * Manager, the {@link #start()} method must be called.
     *
     * @param context The <code>BundleContext</code> of the bundle to which
     *            this Assembly Manager belongs.
     */
    AssemblyManager(BundleContext context, InstallerService installerService) {
        this.context = context;
        this.installerService = installerService;

        this.startLevelTracker = new ServiceTracker(context,
            StartLevel.class.getName(), null);
        this.logServiceTracker = new ServiceTracker(context,
            LogService.class.getName(), null);
    }

    /**
     * Starts this Assembly Manager by first finding all Assembly Bundles
     * already present in the framework and the starting the thread to handle
     * bundle events.
     */
    void start() {
        this.startLevelTracker.open();
        this.logServiceTracker.open();

        // block queue while we fill it with initial events
        synchronized (this.queue) {
            // handle existing bundles
            Bundle[] bundles = this.context.getBundles();
            for (int i = 0; i < bundles.length; i++) {
                Bundle bundle = bundles[i];

                // ignore non-assembly bundles
                if (bundle.getHeaders().get(Assembly.ASSEMBLY_BUNDLES) == null) {
                    continue;
                }

                switch (bundle.getState()) {
                    case Bundle.INSTALLED:
                        this.put(new BundleEvent(BundleEvent.INSTALLED, bundle));
                        break;

                    case Bundle.RESOLVED:
                        this.put(new BundleEvent(BundleEvent.RESOLVED, bundle));
                        break;

                    case Bundle.ACTIVE:
                        this.put(new BundleEvent(BundleEvent.STARTED, bundle));
                        break;

                    case Bundle.UNINSTALLED:
                        this.put(new BundleEvent(BundleEvent.UNINSTALLED, bundle));
                        break;
                }
            }
        }

        // queue is unblocked now, start the thread now
        if (this.queueHandler == null) {
            this.queueHandler = new Thread(this, "AssemblyManager Queue Handler");
            this.queueHandler.start();
        }
    }

    /**
     * Stops this Assembly Manager by posting a dummy bundle event to the
     * background thread to terminate it and cleaing up all service trackers.
     */
    void stop() {
        // not started, don't do anything
        if (this.queueHandler == null) {
            return;
        }

        // Append a pseudo event terminating the event handling loop
        this.put(new BundleEvent(TERMINATION_EVENT, this.context.getBundle()));

        try {
            // wait at most 10 seconds for the thread to terminate
            this.queueHandler.join(10L * 1000L);
        } catch (InterruptedException ie) {
            this.log(LogService.LOG_DEBUG,
                "Interrupted waiting for queue handler to terminate", ie);
        }

        if (this.queueHandler.isAlive()) {
            this.log(LogService.LOG_ERROR, "Queue handler is still alive !");
        }

        this.queueHandler = null;

        this.startLevelTracker.close();
        this.logServiceTracker.close();
    }

    // ---------- Runnable -----------------------------------------------------

    /**
     * Main method of the background thread continuously listening for bundle
     * events to arrive and handle.
     */
    public void run() {
        for (;;) {
            BundleEvent event = this.take();

            // terminate thread if no event or event has no bundle
            if (event.getType() == TERMINATION_EVENT) {
                return;
            }

            // handle the event
            try {
                this.handleEvent(event.getType(), event.getBundle());
            } catch (IllegalStateException ise) {
                // bundle has already been uninstalled
                this.log(LogService.LOG_ERROR, "Cannot handle event: "
                    + ise.getMessage());
            } catch (Throwable t) {
                // unexpected !
                this.log(LogService.LOG_ERROR, "Problem handling event "
                    + event.getType() + " for bundle "
                    + event.getBundle().getSymbolicName(), t);
            }
        }
    }

    // ---------- Event Queue --------------------------------------------------

    /**
     * Posts a bundle event to the queue for processing by the background
     * thread. New events are append to the end of the queue.
     * <p>
     * This method is intended to only be used by the
     * {@link AssemblyActivator#bundleChanged(BundleEvent)} method to forward
     * bundle events.
     */
    void put(BundleEvent event) {
        if (event != null) {
            synchronized (this.queue) {
                this.queue.addLast(event);
                this.queue.notifyAll();
            }
        }
    }

    /**
     * Removes a bundle event from the queue and waits (indefinitely) if no
     * bundle events are currently available. Events are taken off of the front
     * of the queue.
     * <p>
     * This method is only used internally by the {@link #run()} method.
     */
    private BundleEvent take() {
        synchronized (this.queue) {
            while (this.queue.isEmpty()) {
                try {
                    this.queue.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }

            return (BundleEvent) this.queue.removeFirst();
        }
    }

    // ---------- implementation -----------------------------------------------

    /**
     * Forwards bundle events to the {@link Assembly} instance representing the
     * given <code>Bundle</code>.
     */
    private void handleEvent(int type, Bundle bundle) {
        // handle the bundle
        switch (type) {
            case BundleEvent.INSTALLED:
                this.getOrCreateAssembly(bundle).install();
                break;
            case BundleEvent.STARTED:
                this.getOrCreateAssembly(bundle).start();
                break;
            case BundleEvent.STOPPED:
                this.getOrCreateAssembly(bundle).stop();
                break;
            case BundleEvent.UNINSTALLED:
                this.getOrCreateAssembly(bundle).uninstall();
                this.removeAssembly(bundle);
                break;
            case BundleEvent.UPDATED:
                this.getOrCreateAssembly(bundle).update();
                break;
            default:
                // don't care
                // - resolved and unresolved need no handling
                // - starting and stopping are not expected
        }
    }

    // ---------- InstalledBundle registry -------------------------------------

    InstalledBundle putInstalledBundle(String key, InstalledBundle bundle) {
        return (InstalledBundle) this.installedBundles.put(key, bundle);
    }

    InstalledBundle getInstalledBundle(String key) {
        return (InstalledBundle) this.installedBundles.get(key);
    }

    InstalledBundle removeInstalledBundle(String key) {
        return (InstalledBundle) this.installedBundles.remove(key);
    }

    // ---------- internal helpers ---------------------------------------------

    void log(int level, String message) {
        this.log(level, message, null);
    }

    void log(int level, String message, Throwable exception) {
        LogService log = (LogService) this.logServiceTracker.getService();
        if (log == null) {
            String prefix = new Date() + " ";
            if (level >= 0 && level < LOG_FLAGS.length) {
                prefix += LOG_FLAGS[level];
            } else {
                prefix += "*" + level + "*";
            }

            PrintStream ps = (level == LogService.LOG_ERROR)
                    ? System.err
                    : System.out;

            ps.print(prefix);
            ps.println(message);

            if (exception != null) {
                ps.print(prefix);
                exception.printStackTrace(ps);
            }
        } else {
            log.log(level, message, exception);
        }
    }

    // ---------- internal helpers ---------------------------------------------

    private Assembly getOrCreateAssembly(Bundle bundle) {
        Assembly assembly = (Assembly) this.assemblies.get(bundle.getLocation());
        if (assembly == null) {
            assembly = new Assembly(this, bundle);
            this.assemblies.put(bundle.getLocation(), assembly);
        }
        return assembly;
    }

    private void removeAssembly(Bundle bundle) {
        this.assemblies.remove(bundle.getLocation());
    }

    /**
     * Returns the <code>BundleContext</code> of the Assembly Manager bundle.
     */
    BundleContext getBundleContext() {
        return this.context;
    }

    /**
     * Returns the framework StartLevel service or <code>null</code> if none
     * is available.
     */
    StartLevel getStartLevel() {
        return (StartLevel) this.startLevelTracker.getService();
    }

    /**
     * Returns an {@link Installer} instance for installation tasks. The
     * installer returned is not thread safe.
     */
    Installer getInstaller() {
        return this.installerService.getInstaller();
    }
}
