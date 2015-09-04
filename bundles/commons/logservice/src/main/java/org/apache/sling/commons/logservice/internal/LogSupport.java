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
package org.apache.sling.commons.logservice.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogService;
import org.osgi.service.startlevel.StartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>LogReaderServiceFactory</code> TODO
 */
public class LogSupport implements SynchronousBundleListener, ServiceListener,
        FrameworkListener {

    /**
     * The service property name of the component name (value is
     * "component.name"). Note: We use a private constant here to not create a
     * unneded dependency on the org.osgi.service.component package.
     */
    private static final String COMPONENT_NAME = ComponentConstants.COMPONENT_NAME; // "component.name";

    /**
     * The empty enumeration currently returned on the {@link #getLog()} call
     * because we do not currently record the log events.
     */
    private final Enumeration<?> EMPTY = Collections.enumeration(Collections.emptyList());

    // The registered LogListeners
    private LogListenerProxy[] listeners;

    // The lock used to guard concurrent access to the listeners array
    private final Object listenersLock = new Object();

    // The loggers by bundle id used for logging messages originated from
    // specific bundles
    @SuppressWarnings("serial")
    private final Map<Long, Logger> loggers = new LinkedHashMap<Long, Logger>(16,
        0.75f, true) {
        private static final int MAX_SIZE = 50;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Logger> eldest) {
            return size() > MAX_SIZE;
        }
    };

    // the worker thread actually sending LogEvents to LogListeners
    private final LogEntryDispatcher logEntryDispatcher;

    private final StartLevel startLevelService;

    /* package */LogSupport(final StartLevel startLevelService) {
        logEntryDispatcher = new LogEntryDispatcher(this);
        logEntryDispatcher.start();
        this.startLevelService = startLevelService;
    }

    /* package */void shutdown() {

        // terminate the dispatcher and wait for its termination here
        logEntryDispatcher.terminate();
        try {
            logEntryDispatcher.join(1000L);
        } catch (InterruptedException ie) {
            // don't care
        }

        // drop all listeners
        synchronized (listenersLock) {
            listeners = null;
        }
    }

    // ---------- LogReaderService interface -----------------------------------

    /* package */void addLogListener(Bundle bundle, LogListener listener) {
        synchronized (listenersLock) {
            LogListenerProxy llp = new LogListenerProxy(bundle, listener);
            if (listeners == null) {
                listeners = new LogListenerProxy[] { llp };
            } else if (getListener(listener) < 0) {
                LogListenerProxy[] newListeners = new LogListenerProxy[listeners.length + 1];
                System.arraycopy(listeners, 0, newListeners, 0,
                    listeners.length);
                newListeners[listeners.length] = llp;
                listeners = newListeners;
            }
        }
    }

    /* package */void removeLogListener(LogListener listener) {
        synchronized (listenersLock) {
            // no listeners registered, nothing to do
            if (listeners == null) {
                return;
            }

            // listener is not registered, nothing to do
            int idx = getListener(listener);
            if (idx < 0) {
                return;
            }

            LogListenerProxy[] newListeners = new LogListenerProxy[listeners.length - 1];
            if (idx > 0) {
                System.arraycopy(listeners, 0, newListeners, 0, idx);
            }
            if (idx < listeners.length) {
                System.arraycopy(listeners, idx + 1, newListeners, 0,
                    newListeners.length - idx);
            }
            listeners = newListeners;
        }
    }

    /**
     * Removes all registered LogListeners belonging to the given bundle. This
     * is the task required by the specification from a Log Service
     * implemenation:
     * <p>
     * <blockquote> When a bundle which registers a LogListener object is
     * stopped or otherwise releases the Log Reader Service, the Log Reader
     * Service must remove all of the bundle's listeners.</blockquote>
     * <p>
     *
     * @param bundle The bundle whose listeners are to be removed.
     */
    /* package */void removeLogListeners(Bundle bundle) {
        // grab an immediate copy of the array
        LogListenerProxy[] current = getListeners();
        if (current == null) {
            return;
        }

        // check for listeners by bundle
        for (int i = 0; i < current.length; i++) {
            if (current[i].hasBundle(bundle)) {
                removeLogListener(current[i]);
            }
        }
    }

    private int getListener(LogListener listener) {
        if (listeners != null) {
            for (int i = 0; i < listeners.length; i++) {
                if (listeners[i].isSame(listener)) {
                    return i;
                }
            }
        }

        // fall back to not found
        return -1;
    }

    /**
     * Returns the currently registered LogListeners
     */
    private LogListenerProxy[] getListeners() {
        synchronized (listenersLock) {
            return listeners;
        }
    }

    /**
     * Returns an empty enumeration for now because we do not implement log
     * entry recording for the moment.
     */
    Enumeration<?> getLog() {
        return EMPTY;
    }

    // ---------- Firing a log event -------------------------------------------

    /**
     * Logs the given log entry to the log file and enqueues for the dispatching
     * to the registered LogListeners in a separate worker thread.
     */
    /* package */void fireLogEvent(LogEntry logEntry) {

        // actually log it to SLF4J
        logOut(logEntry);

        // enqueue for asynchronous delivery
        logEntryDispatcher.enqueLogEntry(logEntry);
    }

    // ---------- BundleListener -----------------------------------------------

    /**
     * Listens for Bundle events and logs the respective events according to the
     * Log Service specification. In addition, all LogListener instances
     * registered for stopped bundles are removed by this method.
     */
    public void bundleChanged(BundleEvent event) {
        String message;
        switch (event.getType()) {
            case BundleEvent.INSTALLED:
                message = "BundleEvent INSTALLED";
                break;
            case BundleEvent.RESOLVED:
                message = "BundleEvent RESOLVED";
                break;
            case BundleEvent.STARTING:
                message = "BundleEvent STARTING";
                break;
            case BundleEvent.STARTED:
                message = "BundleEvent STARTED";
                break;
            case BundleEvent.STOPPING:
                message = "BundleEvent STOPPING";
                break;
            case BundleEvent.STOPPED:
                // this is special, as we have to fix the listener list for
                // stopped bundles
                removeLogListeners(event.getBundle());
                message = "BundleEvent STOPPED";
                break;
            case BundleEvent.UNRESOLVED:
                message = "BundleEvent UNRESOLVED";
                break;
            case BundleEvent.UPDATED:
                message = "BundleEvent UPDATED";
                break;
            case BundleEvent.UNINSTALLED:
                // remove any cached logger for the uninstalled bundle
                ungetLogger(event.getBundle());
                message = "BundleEvent UNINSTALLED";
                break;
            default:
                message = "BundleEvent " + event.getType();
        }

        LogEntry entry = new LogEntryImpl(event.getBundle(), null,
            LogService.LOG_INFO, message, null);
        fireLogEvent(entry);
    }

    // ---------- ServiceListener ----------------------------------------------

    /**
     * Listens for Service events and logs the respective events according to
     * the Log Service specification.
     */
    public void serviceChanged(ServiceEvent event) {
        int level = LogService.LOG_INFO;
        String message;
        switch (event.getType()) {
            case ServiceEvent.REGISTERED:
                message = "ServiceEvent REGISTERED";
                break;
            case ServiceEvent.MODIFIED:
                message = "ServiceEvent MODIFIED";
                level = LogService.LOG_DEBUG;
                break;
            case ServiceEvent.UNREGISTERING:
                message = "ServiceEvent UNREGISTERING";
                break;
            default:
                message = "ServiceEvent " + event.getType();
        }

        LogEntry entry = new LogEntryImpl(
            event.getServiceReference().getBundle(),
            event.getServiceReference(), level, message, null);
        fireLogEvent(entry);
    }

    // ---------- FrameworkListener --------------------------------------------

    /**
     * Listens for Framework events and logs the respective events according to
     * the Log Service specification.
     * <p>
     * In the case of a Framework ERROR which is a ClassNotFoundException for an
     * unresolved bundle, the message is logged at INFO level instead of ERROR
     * level as prescribed by the spec. This is because such a situation should
     * not really result in a Framework ERROR but the Apache Felix framework has
     * no means of controlling this at the moment (framework 1.0.4 release).
     */
    public void frameworkEvent(FrameworkEvent event) {
        int level = LogService.LOG_INFO;
        String message;
        Throwable exception = event.getThrowable();
        switch (event.getType()) {
            case FrameworkEvent.STARTED:
                message = "FrameworkEvent STARTED";
                break;
            case FrameworkEvent.ERROR:
                message = "FrameworkEvent ERROR";

                // special precaution for Felix.loadBundleClass event overkill
                // FIXME: actually, the error is ok, if the bundle failed to
                // resolve
                if (exception instanceof BundleException) {
                    StackTraceElement[] ste = exception.getStackTrace();
                    if (ste != null && ste.length > 0
                        && "loadBundleClass".equals(ste[0].getMethodName())) {
                        message += ": Class " + exception.getMessage()
                            + " not found";
                        if (event.getBundle() != null) {
                            message += " in bundle "
                                + event.getBundle().getSymbolicName() + " ("
                                + event.getBundle().getBundleId() + ")";
                        }
                        level = LogService.LOG_INFO;
                        exception = null; // don't care for a stack trace here
                        break;
                    }
                }

                level = LogService.LOG_ERROR;
                break;
            case FrameworkEvent.PACKAGES_REFRESHED:
                message = "FrameworkEvent PACKAGES REFRESHED";
                break;
            case FrameworkEvent.STARTLEVEL_CHANGED:
                message = "FrameworkEvent STARTLEVEL CHANGED to " + this.startLevelService.getStartLevel();
                break;
            case FrameworkEvent.WARNING:
                message = "FrameworkEvent WARNING";
                break;
            case FrameworkEvent.INFO:
                message = "FrameworkEvent INFO";
                break;
            default:
                message = "FrameworkEvent " + event.getType();
        }

        final LogEntry entry = new LogEntryImpl(event.getBundle(), null, level,
            message, exception);
        fireLogEvent(entry);
    }

    // ---------- Effective logging --------------------------------------------

    /**
     * Get a logger for messages orginating from the given bundle. If no bundle
     * is specified, we use the system bundle logger.
     *
     * @param bundle The bundle for which a logger is to be returned.
     * @return The Logger for the bundle.
     */
    private Logger getLogger(Bundle bundle) {
        Long bundleId = new Long((bundle == null) ? 0 : bundle.getBundleId());
        Logger log;
        synchronized (loggers) {
            log = loggers.get(bundleId);
        }
        if (log == null) {

            String name;
            if (bundle == null) {

                // if we have no bundle, use the system bundle's name
                name = Constants.SYSTEM_BUNDLE_SYMBOLICNAME;

            } else {

                // otherwise use the bundle symbolic name
                name = bundle.getSymbolicName();

                // if the bundle has no symbolic name, use the location
                if (name == null) {
                    name = bundle.getLocation();
                }

                // if the bundle also has no location, use the bundle Id
                if (name == null) {
                    name = String.valueOf(bundle.getBundleId());
                }
            }

            log = LoggerFactory.getLogger(name);
            synchronized (loggers) {
                loggers.put(bundleId, log);
            }
        }
        return log;
    }

    /**
     * Removes the cached logger for the given bundle, for example if the
     * bundle is uninstalled and thus there will be no more logs from this
     * bundle.
     */
    private void ungetLogger(Bundle bundle) {
        synchronized (loggers) {
            loggers.remove(bundle.getBundleId());
        }
    }

    /**
     * Actually logs the given log entry to the logger for the bundle recorded
     * in the log entry.
     */
    private void logOut(LogEntry logEntry) {
        // get the logger for the bundle
        Logger log = getLogger(logEntry.getBundle());
        if (logEntry.getLevel() > getLevel(log))
            // early Exit, this message will not be logged, don't do any work...
            return;

        final StringBuilder msg = new StringBuilder();

        ServiceReference sr = logEntry.getServiceReference();
        if (sr != null) {
            msg.append("Service [");
            if (sr.getProperty(Constants.SERVICE_PID) != null) {
                msg.append(sr.getProperty(Constants.SERVICE_PID)).append(',');
            } else if (sr.getProperty(COMPONENT_NAME) != null) {
                msg.append(sr.getProperty(COMPONENT_NAME)).append(',');
            } else if (sr.getProperty(Constants.SERVICE_DESCRIPTION) != null) {
                msg.append(sr.getProperty(Constants.SERVICE_DESCRIPTION)).append(
                    ',');
            }
            msg.append(sr.getProperty(Constants.SERVICE_ID))
                .append(", ")
                .append(Arrays.toString((String[]) sr.getProperty(Constants.OBJECTCLASS)))
                .append("] ");
        }

        if (logEntry.getMessage() != null) {
            msg.append(logEntry.getMessage());
        }

        Throwable exception = logEntry.getException();
        if (exception != null) {
            msg.append(" (").append(exception).append(')');
        }

        String message = msg.toString();
        switch (logEntry.getLevel()) {
            case LogService.LOG_DEBUG:
                log.debug(message, exception);
                break;
            case LogService.LOG_INFO:
                log.info(message, exception);
                break;
            case LogService.LOG_WARNING:
                log.warn(message, exception);
                break;
            case LogService.LOG_ERROR:
                log.error(message, exception);
                break;
            default:
                if (logEntry.getLevel() > LogService.LOG_DEBUG) {
                    log.trace(message, exception);
                } else if (logEntry.getLevel() < LogService.LOG_ERROR) {
                    log.error(message, exception);
                }
                break;
        }
    }

    static int getLevel(Logger log) {
        if (log.isTraceEnabled())
            return LogService.LOG_DEBUG + 1; // No constant for trace in LogService
        else if (log.isDebugEnabled())
            return LogService.LOG_DEBUG;
        else if (log.isInfoEnabled())
            return LogService.LOG_INFO;
        else if (log.isWarnEnabled())
            return LogService.LOG_WARNING;
        return LogService.LOG_ERROR;
    }

    // ---------- internal class -----------------------------------------------

    /**
     * The <code>LogListenerProxy</code> class is a proxy to the actually
     * registered <code>LogListener</code> which also records the bundle
     * registering the listener. This allows for the removal of the log
     * listeners registered by bundles which have not been removed before the
     * bundle has been stopped.
     */
    private static class LogListenerProxy implements LogListener {

        private final int runningBundle = Bundle.STARTING | Bundle.ACTIVE
            | Bundle.STOPPING;

        private final Bundle bundle;

        private final LogListener delegatee;

        public LogListenerProxy(Bundle bundle, LogListener delegatee) {
            this.bundle = bundle;
            this.delegatee = delegatee;
        }

        public void logged(LogEntry entry) {
            if ((bundle.getState() & runningBundle) != 0) {
                delegatee.logged(entry);
            }
        }

        /* package */boolean isSame(LogListener listener) {
            return listener == delegatee || listener == this;
        }

        /* package */boolean hasBundle(Bundle bundle) {
            return this.bundle == bundle;
        }
    }

    /**
     * The <code>LogEntryDispatcher</code> implements the worker thread
     * responsible for delivering log events to the log listeners.
     */
    private static class LogEntryDispatcher extends Thread {

        // provides the actual log listeners on demand
        private final LogSupport logSupport;

        // the queue of log events to be dispatched
        private final BlockingQueue<LogEntry> dispatchQueue;

        // true as long as the thread is active
        private boolean active;

        LogEntryDispatcher(LogSupport logSupport) {
            super("LogEntry Dispatcher");

            this.logSupport = logSupport;
            this.dispatchQueue = new LinkedBlockingQueue<LogEntry>();
            this.active = true;
        }

        /**
         * Add a log entry for dispatching.
         */
        void enqueLogEntry(LogEntry logEntry) {
            dispatchQueue.offer(logEntry);
        }

        /**
         * Get the next log entry for dispatching. This method blocks until an
         * event is available or the thread is interrupted.
         *
         * @return The next event to dispatch
         * @throws InterruptedException If the thread has been interrupted while
         *             waiting for a log event to dispatch.
         */
        LogEntry dequeueLogEntry() throws InterruptedException {
            return dispatchQueue.take();
        }

        /**
         * Terminates this work thread by resetting the active flag and
         * interrupting itself such that the {@link #dequeueLogEntry()} is
         * aborted for the thread to terminate.
         */
        void terminate() {
            active = false;
            interrupt();
        }

        /**
         * Runs the actual log event dispatching. This method continues to get
         * log events from the {@link #dequeueLogEntry()} method until the
         * active flag is reset.
         */
        @Override
        public void run() {
            while (active) {

                LogEntry logEntry = null;
                try {
                    logEntry = dequeueLogEntry();
                } catch (InterruptedException ie) {
                    // don't care, this is expected
                }

                // dispatch the log entry
                if (logEntry != null) {

                    // grab an immediate copy of the array
                    LogListener[] logListeners = logSupport.getListeners();

                    // fire the events outside of the listenersLock
                    if (logListeners != null) {
                        for (LogListener logListener : logListeners) {
                            try {
                                logListener.logged(logEntry);
                            } catch (Throwable t) {
                                // should we really care ??
                            }
                        }
                    }
                }
            }
        }
    }
}
