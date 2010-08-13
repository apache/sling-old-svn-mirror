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
package org.apache.sling.installer.file.impl;

import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import org.apache.sling.osgi.installer.OsgiInstaller;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * The <code>ServicesListener</code> listens for the required services
 * and starts/stops the scanners based on the availability of the
 * services.
 */
public class ServicesListener {

    /** The name of the installer service. */
    private static final String INSTALLER_SERVICE_NAME = OsgiInstaller.class.getName();

    /** The bundle context. */
    private final BundleContext bundleContext;

    /** The listener for the installer. */
    private final Listener installerListener;

    /** All active scan configurations. */
    private final List<ScanConfiguration> scanConfigurations = new ArrayList<ScanConfiguration>();

    /** All monitors. */
    private final List<FileMonitor> monitors = new ArrayList<FileMonitor>();

    private boolean running = false;

    public ServicesListener(final BundleContext bundleContext,
            final List<ScanConfiguration> configs)
    throws InvalidSyntaxException{
        if ( configs != null ) {
            scanConfigurations.addAll(configs);
        }
        this.bundleContext = bundleContext;
        this.installerListener = new Listener(INSTALLER_SERVICE_NAME);
    }

    public synchronized void notifyChange() {
        final boolean shouldRun = !this.scanConfigurations.isEmpty();
        if ( (shouldRun && !running) || (!shouldRun && running) ) {
            final OsgiInstaller installer = (OsgiInstaller)this.installerListener.getService();

            if ( installer != null&& !running ) {
                this.startScanner(installer);
            } else if ( running && installer == null ) {
                this.stopScanner();
            }
        }
    }

    /**
     * Deactivate this listener.
     */
    public void deactivate() {
        this.installerListener.deactivate();
        this.stopScanner();
    }

    private void startScanner(final OsgiInstaller installer) {
        if ( !running ) {
            for(final ScanConfiguration config : this.scanConfigurations) {
                this.monitors.add(new FileMonitor(new File(config.directory),
                        config.scanInterval, new Installer(installer, hash(config.directory))));
            }
            running = true;
        }
    }

    private void stopScanner() {
        if ( running ) {
            for(final FileMonitor monitor : this.monitors) {
                monitor.stop();
            }
            this.monitors.clear();
            running = false;
        }
    }

    protected final class Listener implements ServiceListener {

        private final String serviceName;

        private ServiceReference reference;
        private Object service;

        public Listener(final String serviceName) throws InvalidSyntaxException {
            this.serviceName = serviceName;
            bundleContext.addServiceListener(this, "("
                    + Constants.OBJECTCLASS + "=" + serviceName + ")");
            this.getService();
        }

        public void deactivate() {
            bundleContext.removeServiceListener(this);
        }

        public synchronized Object getService() {
            return this.service;
        }
        private synchronized void retainService() {
            if ( this.reference == null ) {
                this.reference = bundleContext.getServiceReference(this.serviceName);
                if ( this.reference != null ) {
                    this.service = bundleContext.getService(this.reference);
                    if ( this.service == null ) {
                        this.reference = null;
                    } else {
                        notifyChange();
                    }
                }
            }
        }

        private synchronized void releaseService() {
            if ( this.reference != null ) {
                this.service = null;
                bundleContext.ungetService(this.reference);
                this.reference = null;
                notifyChange();
            }
        }

        /**
         * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
         */
        public void serviceChanged(ServiceEvent event) {
            if (event.getType() == ServiceEvent.REGISTERED && this.service == null ) {
                this.retainService();
            } else if ( event.getType() == ServiceEvent.UNREGISTERING && this.service != null ) {
                this.releaseService();
            }
        }
    }

    /**
     * Hash the string
     */
    private static String hash(String value) {
        try {
            final MessageDigest d = MessageDigest.getInstance("MD5");
            d.update(value.getBytes("UTF-8"));
            final BigInteger bigInt = new BigInteger(1, d.digest());
            return new String(bigInt.toString(16));
        } catch (Exception ignore) {
            // if anything goes wrong we just return the value
            return value;
        }
    }
}
