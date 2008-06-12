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
package org.apache.sling.commons.log;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>LogReaderServiceFactory</code> TODO
 * <p>
 * <blockquote> When a bundle which registers a LogListener object is stopped or
 * otherwise releases the Log Reader Service, the Log Reader Service must remove
 * all of the bundle's listeners.</blockquote>
 * <p>
 * TODO: To support configuration, we will implement ManagedService here !
 */
public class LogSupport implements BundleListener, ServiceListener,
        FrameworkListener {

    /**
     * The service property name of the component name (value is
     * "component.name"). Note: We use a private constant here to not create a
     * unneded dependency on the org.osgi.service.component package.
     */
    private static final String COMPONENT_NAME = "component.name";

    private final Object lock = new Object();

    private LogListenerProxy[] listeners;

    /* package */LogSupport() {
    }

    /* package */void shutdown() {
        synchronized (this.lock) {
            this.listeners = null;
        }
    }

    // ---------- LogReaderService interface -----------------------------------

    /* package */void addLogListener(Bundle bundle, LogListener listener) {
        synchronized (this.lock) {
            LogListenerProxy llp = new LogListenerProxy(bundle, listener);
            if (this.listeners == null) {
                this.listeners = new LogListenerProxy[] { llp };
            } else if (this.getListener(listener) < 0) {
                LogListenerProxy[] newListeners = new LogListenerProxy[this.listeners.length + 1];
                System.arraycopy(this.listeners, 0, newListeners, 0,
                    this.listeners.length);
                newListeners[this.listeners.length] = llp;
                this.listeners = newListeners;
            }
        }
    }

    /* package */void removeLogListener(LogListener listener) {
        synchronized (this.lock) {
            // no listeners registered, nothing to do
            if (this.listeners == null) {
                return;
            }

            // listener is not registered, nothing to do
            int idx = this.getListener(listener);
            if (idx < 0) {
                return;
            }

            LogListenerProxy[] newListeners = new LogListenerProxy[this.listeners.length - 1];
            if (idx > 0) {
                System.arraycopy(this.listeners, 0, newListeners, 0, idx);
            }
            if (idx < this.listeners.length) {
                System.arraycopy(this.listeners, idx + 1, newListeners, 0,
                    newListeners.length - idx);
            }
            this.listeners = newListeners;
        }
    }

    /* package */void removeLogListeners(Bundle bundle) {
        // grab an immediate copy of the array
        LogListenerProxy[] current = this.getListeners();
        if (current == null) {
            return;
        }

        // check for listeners by bundle
        for (int i = 0; i < current.length; i++) {
            if (current[i].hasBundle(bundle)) {
                this.removeLogListener(current[i]);
            }
        }
    }

    private int getListener(LogListener listener) {
        if (this.listeners != null) {
            for (int i = 0; i < this.listeners.length; i++) {
                if (this.listeners[i].isSame(listener)) {
                    return i;
                }
            }
        }

        // fall back to not found
        return -1;
    }

    private LogListenerProxy[] getListeners() {
        synchronized (this.lock) {
            return this.listeners;
        }
    }

    @SuppressWarnings("unchecked")
    private final Enumeration EMPTY = new Enumeration() {
        public boolean hasMoreElements() {
            return false;
        }

        public Object nextElement() {
            throw new NoSuchElementException("Empty");
        }
    };

    /* package */@SuppressWarnings("unchecked")
    Enumeration getLog() {
        return this.EMPTY;
    }

    // ---------- Firing a log event -------------------------------------------

    /* package */void fireLogEvent(LogEntry entry) {
        this.logOut(entry);

        // grab an immediate copy of the array
        LogListener[] current = this.getListeners();
        if (current == null) {
            return;
        }

        // fire the events outside of the lock
        for (int i = 0; i < current.length; i++) {
            current[i].logged(entry);
        }
    }

    // ---------- BundleListener -----------------------------------------------

    public void bundleChanged(BundleEvent event) {
        String message;
        switch (event.getType()) {
            case BundleEvent.INSTALLED:
                message = "BundleEvent INSTALLED";
                break;
            case BundleEvent.STARTED:
                message = "BundleEvent STARTED";
                break;
            case BundleEvent.STOPPED:
                // this is special, as we have to fix the listener list for
                // stopped bundles
                this.removeLogListeners(event.getBundle());
                message = "BundleEvent STOPPED";
                break;
            case BundleEvent.UPDATED:
                message = "BundleEvent UPDATED";
                break;
            case BundleEvent.UNINSTALLED:
                message = "BundleEvent UNINSTALLED";
                break;
            case BundleEvent.RESOLVED:
                message = "BundleEvent RESOLVED";
                break;
            case BundleEvent.UNRESOLVED:
                message = "BundleEvent UNRESOLVED";
                break;
            default:
                message = "BundleEvent " + event.getType();
        }

        LogEntry entry = new LogEntryImpl(event.getBundle(), null,
            LogService.LOG_INFO, message, null);
        this.fireLogEvent(entry);
    }

    // ---------- ServiceListener ----------------------------------------------

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

        String s = (event.getServiceReference().getBundle() == null)
                ? null
                : "Bundle " + event.getServiceReference().getBundle();
        s = (s == null) ? message : s + " " + message;

        LogEntry entry = new LogEntryImpl(
            event.getServiceReference().getBundle(),
            event.getServiceReference(), level, message, null);
        this.fireLogEvent(entry);
    }

    // ---------- FrameworkListener --------------------------------------------

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
                message = "FrameworkEvent STARTLEVEL CHANGED";
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

        String s = (event.getBundle() == null) ? null : "Bundle "
            + event.getBundle();
        s = (s == null) ? message : s + " " + message;

        LogEntry entry = new LogEntryImpl(event.getBundle(), null, level,
            message, exception);
        this.fireLogEvent(entry);
    }

    // ---------- Effective logging --------------------------------------------

    private Map<Long, Logger> loggers = new HashMap<Long, Logger>();

    private Logger getLogger(Bundle bundle) {
        Long bundleId = new Long((bundle == null) ? 0 : bundle.getBundleId());
        Logger log = this.loggers.get(bundleId);
        if (log == null) {
            // TODO: use systembundle for bundle==null
            String name = (bundle == null)
                    ? Constants.SYSTEM_BUNDLE_SYMBOLICNAME
                    : bundle.getSymbolicName();
            log = LoggerFactory.getLogger(name);
            this.loggers.put(bundleId, log);
        }
        return log;
    }

    private void logOut(LogEntry logEntry) {
        // /* package */ void logOut(Bundle bundle, ServiceReference sr, int
        // level, String message, Throwable exception) {

        // get the logger for the bundle
        Logger log = this.getLogger(logEntry.getBundle());

        StringBuffer msg = new StringBuffer();

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
            msg.append(sr.getProperty(Constants.SERVICE_ID)).append("] ");
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

    // ---------- internal class -----------------------------------------------

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
            if ((this.bundle.getState() & this.runningBundle) != 0) {
                this.delegatee.logged(entry);
            }
        }

        /* package */boolean isSame(LogListener listener) {
            return listener == this.delegatee || listener == this;
        }

        /* package */boolean hasBundle(Bundle bundle) {
            return this.bundle == bundle;
        }
    }
}
