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
package org.apache.sling.launchpad.installer.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.installer.api.event.InstallationListener;
import org.apache.sling.launchpad.api.LaunchpadContentProvider;
import org.apache.sling.launchpad.api.StartupHandler;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * The <code>ServicesListener</code> listens for the required services
 * and starts the installation support if all services are available.
 */
public class ServicesListener {

    /** The bundle context. */
    private final BundleContext bundleContext;

    /** The listener for the installer. */
    private final Listener installerListener;

    /** The listener for the provider. */
    private final Listener providerListener;

    /** The listener for the startup handler. */
    private final Listener startupListener;

    /** The listener for the settings service. */
    private final Listener settingsListener;

    /** The registration of the launchpad listener. */
    private ServiceRegistration launchpadListenerReg;

    /** Boolean marker to not reprocess things. */
    private volatile boolean installed = false;

    private LaunchpadListener launchpadListener;

    /**
     * Start listeners
     */
    public ServicesListener(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.installerListener = new Listener(OsgiInstaller.class.getName());
        this.providerListener = new Listener(LaunchpadContentProvider.class.getName());
        this.startupListener = new Listener(StartupHandler.class.getName());
        this.settingsListener = new Listener(SlingSettingsService.class.getName());
        this.startupListener.start();
        this.installerListener.start();
        this.providerListener.start();
        this.settingsListener.start();
    }

    /**
     * Notify of service changes from the listeners.
     * If all services are available, register listener and pass resources
     * to the OSGi installer.
     */
    public synchronized void notifyChange() {
        // check if all services are available
        final OsgiInstaller installer = (OsgiInstaller)this.installerListener.getService();
        final LaunchpadContentProvider lcp = (LaunchpadContentProvider)this.providerListener.getService();
        final StartupHandler handler = (StartupHandler)this.startupListener.getService();
        final SlingSettingsService settings = (SlingSettingsService)this.settingsListener.getService();
        if ( installer != null && lcp != null && handler != null && settings != null ) {
            if ( !this.installed ) {
                this.installed = true;
                this.launchpadListener = new LaunchpadListener(handler);
                final Dictionary<String, Object> props = new Hashtable<String, Object>();
                props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Launchpad Startup Listener");
                props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
                this.launchpadListenerReg = this.bundleContext.registerService(InstallationListener.class.getName(), launchpadListener, props);
                LaunchpadConfigInstaller.install(installer, lcp, settings.getRunModes());
            }
        }
    }

    /**
     * Deactivate this listener.
     */
    public void deactivate() {
        this.installerListener.deactivate();
        this.providerListener.deactivate();
        this.startupListener.deactivate();
        this.settingsListener.deactivate();
        if ( this.launchpadListenerReg != null ) {
            this.launchpadListener.stop();
            this.launchpadListenerReg.unregister();
            this.launchpadListenerReg = null;
        }
        this.launchpadListener = null;
    }

    /**
     * Helper class listening for service events for a defined service.
     */
    protected final class Listener implements ServiceListener {

        /** The name of the service. */
        private final String serviceName;

        /** The service reference. */
        private volatile ServiceReference reference;

        /** The service. */
        private volatile Object service;

        /**
         * Constructor
         */
        public Listener(final String serviceName) {
            this.serviceName = serviceName;
        }

        /**
         * Start the listener.
         * First register a service listener and then check for the service.
         */
        public void start() {
            try {
                bundleContext.addServiceListener(this, "("
                        + Constants.OBJECTCLASS + "=" + serviceName + ")");
            } catch (final InvalidSyntaxException ise) {
                // this should really never happen
                throw new RuntimeException("Unexpected exception occured.", ise);
            }
            this.retainService();
        }

        /**
         * Unregister the listener.
         */
        public void deactivate() {
            bundleContext.removeServiceListener(this);
        }

        /**
         * Return the service (if available)
         */
        public synchronized Object getService() {
            return this.service;
        }

        /**
         * Try to get the service and notify the change.
         */
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

        /**
         * Try to release the service and notify the change.
         */
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
            if (event.getType() == ServiceEvent.REGISTERED) {
                this.retainService();
            } else if ( event.getType() == ServiceEvent.UNREGISTERING ) {
                this.releaseService();
            }
        }
    }
}
