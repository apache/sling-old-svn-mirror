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
package org.apache.sling.commons.threaddump.internal;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator, UncaughtExceptionHandler {

    private static final String LOG_NAME = "org.apache.sling.commons.threaddump.internal.UncaughtExceptionHandler";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(LOG_NAME);

    private UncaughtExceptionHandler oldHandler;

    public void start(BundleContext bundleContext) {

        // install handler for uncaught exceptions
        oldHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);

        // install thread handler shell command
        register(bundleContext,
            new String[] { "org.apache.felix.shell.Command" },
            new ServiceFactory() {

                public void ungetService(final Bundle bundle,
                    final ServiceRegistration reg,
                    final Object consoleObject) {
                    // nothing to do
                }

                public Object getService(final Bundle bundle,
                    final ServiceRegistration reg) {
                     return new ThreadDumpCommand();
                }
            }, null);

        // install Web Console configuration printer
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("felix.webconsole.label", "slingthreads");
        props.put("felix.webconsole.title", "Threads");
        props.put("felix.webconsole.configprinter.modes", "always");

        final ThreadDumperPanel tdp = new ThreadDumperPanel();

        register(bundleContext, new String[] {
            tdp.getClass().getName() }, tdp, props);
    }

    public void stop(BundleContext bundleContext) {
        Thread.setDefaultUncaughtExceptionHandler(oldHandler);
    }

    private void register(final BundleContext context,
            final String[] serviceNames,
            final Object service,
            final Dictionary<String, Object> properties) {

        final Dictionary<String, Object> props =
                (properties == null ? new Hashtable<String, Object>() : properties);

        // default settings
        props.put(Constants.SERVICE_DESCRIPTION, "Thread Dumper ("
            + serviceNames[0] + ")");
        props.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");

        context.registerService(serviceNames, service, props);
    }

    // ---------- UncaughtExceptionHandler

    /**
     * Logs the uncaught exception for the thread at level ERROR and chains to
     * the old handler, which was installed before this handler has been
     * installed.
     *
     * @param t The <code>Thread</code> which got the exception but did not
     *            handle it.
     * @param e The uncaught <code>Throwable</code> causing the thread to die.
     */
    public void uncaughtException(Thread t, Throwable e) {
        if (e instanceof ThreadDeath) {
            log.error("Thread " + t + " has just been killed", e);
        } else {
            log.error("Uncaught exception in Thread " + t, e);
        }

        // chain to original handler
        if (oldHandler != null) {
            oldHandler.uncaughtException(t, e);
        }
    }

}
