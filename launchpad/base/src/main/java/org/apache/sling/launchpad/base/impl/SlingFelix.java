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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.Util;
import org.apache.sling.launchpad.base.shared.Loader;
import org.apache.sling.launchpad.base.shared.Notifiable;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;


public class SlingFelix extends Felix {

    private final Notifiable notifiable;

    private Thread notifierThread;

    // see getBundle(Class) below
    private Method getBundleMethod;

    public SlingFelix(final Notifiable notifiable, @SuppressWarnings("rawtypes") final Map props) throws Exception {
        super(getPropsAndDefaultProps(props));
        this.notifiable = notifiable;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Map getPropsAndDefaultProps(final Map props) {
        final Logger logger = (Logger)props.get(FelixConstants.LOG_LOGGER_PROP);
        if ( logger != null ) {
            final Properties fullProps = new Properties();
            final Properties defaultProps = Util.loadDefaultProperties(logger);
            fullProps.putAll(defaultProps);
            fullProps.putAll(props);

            // replace variables
            for(final Object name : defaultProps.keySet()) {
                if ( !props.containsKey(name) ) {
                    final String value = (String)fullProps.get(name);
                    final String substValue = Util.substVars(value, name.toString(), null, fullProps);
                    fullProps.put(name, substValue);
                }
            }

            return fullProps;
        }
        return props;
    }

    @Override
    public void update() throws BundleException {
        update(null);
    }

    @Override
    public void update(final InputStream is) throws BundleException {
        // get the update file and make sure, the stream is closed
        try {
            startNotifier(true, is);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignore) {
                }
            }
        }

        // just stop the framework now
        super.stop();
    }

    public void restart() throws BundleException {
        super.stop();
    }

    @Override
    public void stop() throws BundleException {
        startNotifier(false, null);
        super.stop();
    }

    @Override
    public void stop(final int status) throws BundleException {
        startNotifier(false, null);
        super.stop(status);
    }

    private synchronized void startNotifier(final boolean restart, final InputStream ins) {
        if (notifierThread == null) {
            notifierThread = new Thread(new Notifier(restart, ins),
                "Sling Notifier");
            notifierThread.setDaemon(false);
            notifierThread.start();
        }
    }

    /**
     * Returns the bundle from which the given class has been loaded or
     * <code>null</code> if the class has not been loaded through any
     * of the bundles in this framework.
     * <p>
     * This method delegates to Felix.getBundle(Class) to support the
     * URLHandlers service implementation. See SLING-2554 for details.
     *
     * @param clazz The class to check
     *
     * @return The Bundle or <code>null</code> if the class has not been
     *      loaded through any of the bundles in this framework.
     */
    public Bundle getBundle(Class<?> clazz) {
        Method getBundleMethod = this.getBundleMethod;
        if (getBundleMethod == null) {
            Class<?> provider = Felix.class; // super class actually
            try {
                getBundleMethod = provider.getDeclaredMethod("getBundle", Class.class);
                getBundleMethod.setAccessible(true);
                this.getBundleMethod = getBundleMethod;
            } catch (Exception e) {
                throw new NoSuchMethodError("getBundle");
            }
        }

        try {
            return (Bundle) getBundleMethod.invoke(this, clazz);
        } catch (IllegalArgumentException e) {
            // we don't expect this, we checked everything
        } catch (IllegalAccessException e) {
            // we don't expect this, because we set the method accessible
        } catch (InvocationTargetException e) {
            // unpack and rethrow
            Throwable t = e.getCause();
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }

        return null;
    }

    private class Notifier implements Runnable {

        private final boolean restart;

        private final File updateFile;

        private Notifier(final boolean restart, final InputStream ins) {
            this.restart = restart;

            if (ins != null) {
                File tmpFile;
                try {
                    tmpFile = File.createTempFile("slingupdate", ".jar");
                    Loader.spool(ins, tmpFile);
                } catch (IOException ioe) {
                    // TOOD: log
                    tmpFile = null;
                }
                this.updateFile = tmpFile;
            } else {
                this.updateFile = null;
            }
        }

        @Override
        public void run() {

            try {
                SlingFelix.this.waitForStop(0);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            if (restart) {
                SlingFelix.this.notifiable.updated(updateFile);
            } else {
                SlingFelix.this.notifiable.stopped();
            }
        }
    }
}
