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
package org.apache.sling.installer.provider.file.impl;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.installer.api.UpdateHandler;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ServicesListener</code> listens for the required services
 * and starts/stops the scanners based on the availability of the
 * services.
 */
public class ServicesListener {

    /** The logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The name of the installer service. */
    private static final String INSTALLER_SERVICE_NAME = OsgiInstaller.class.getName();

    /** The name of the settings service. */
    private static final String SETTINGS_SERVICE_NAME = SlingSettingsService.class.getName();

    /** The bundle context. */
    private final BundleContext bundleContext;

    /** The listener for the installer. */
    private final Listener installerListener;

    /** The listener for the settings service. */
    private final Listener settingsListener;

    /** The file installer. */
    private final FileInstaller installer;

    /** Service registration. */
    private ServiceRegistration registration;

    private boolean running = false;

    public ServicesListener(final BundleContext bundleContext,
            final List<ScanConfiguration> configs) {
        this.bundleContext = bundleContext;
        boolean writeBack = true;
        final Object writeBackObj = Activator.getProp(this.bundleContext, Activator.KEY_WRITEBACK);
        if ( writeBackObj != null && "false".equalsIgnoreCase(writeBackObj.toString())) {
            writeBack = false;
        }
        this.installer = new FileInstaller(configs, writeBack);
        this.installerListener = new Listener(INSTALLER_SERVICE_NAME);
        this.settingsListener = new Listener(SETTINGS_SERVICE_NAME);
        this.installerListener.start();
        this.settingsListener.start();
    }

    public synchronized void notifyChange() {
        final boolean shouldRun = this.installer.hasConfigurations();
        if ( (shouldRun && !running) || (!shouldRun && running) ) {
            final OsgiInstaller installer = (OsgiInstaller)this.installerListener.getService();
            final SlingSettingsService settings = (SlingSettingsService)this.settingsListener.getService();
            if ( installer != null && settings != null && !running ) {
                logger.debug("Starting scanner");
                this.startScanner(installer, settings);
            } else if ( running && (installer == null || settings == null) ) {
                logger.debug("Stopping scanner");
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

    /** Vendor of all registered services. */
    public static final String VENDOR = "The Apache Software Foundation";

    private void startScanner(final OsgiInstaller installer, final SlingSettingsService settings) {
        if ( !running ) {
            this.installer.start(installer, settings);
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling File Installer Controller Service");
            props.put(Constants.SERVICE_VENDOR, VENDOR);
            props.put(UpdateHandler.PROPERTY_SCHEMES, this.installer.getSchemes());

            this.registration = this.bundleContext.registerService(UpdateHandler.class.getName(),
                    this.installer, props);
            running = true;
        }
    }

    private void stopScanner() {
        if ( running ) {
            if ( this.registration != null ) {
                this.registration.unregister();
                this.registration = null;
            }
            this.installer.stop();
            running = false;
        }
    }

    protected final class Listener implements ServiceListener {

        private final String serviceName;

        private ServiceReference reference;
        private Object service;

        public Listener(final String serviceName) {
            this.serviceName = serviceName;
        }

        public void start() {
            this.retainService();
            try {
                bundleContext.addServiceListener(this, "("
                        + Constants.OBJECTCLASS + "=" + serviceName + ")");
            } catch (final InvalidSyntaxException ise) {
                // this should really never happen
                throw new RuntimeException("Unexpected exception occured.", ise);
            }
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
}
