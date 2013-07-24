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
package org.apache.sling.settings.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.launchpad.api.StartupHandler;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

/**
 * The <code>ServicesListener</code> listens for the required services
 * and registers the settings service once all required services are
 * available
 */
public class ServicesListener {

    /** The bundle context. */
    private final BundleContext bundleContext;

    /** The listener for the startup handler. */
    private final Listener startupListener;

    /** The registration of the settings service. */
    private ServiceRegistration settingsReg;

    /** The registration of the managed service. */
    private ServiceRegistration managedServiceReg;

    /**
     * Start listeners
     */
    public ServicesListener(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.startupListener = new Listener(StartupHandler.class.getName());
        this.startupListener.start();
    }

    /**
     * Notify of service changes from the listeners.
     * If all services are available, register listener and pass resources
     * to the OSGi installer.
     */
    public synchronized void notifyChange() {
        // check if all services are available
        final StartupHandler handler = (StartupHandler)this.startupListener.getService();
        if ( handler != null && this.settingsReg == null ) {
            this.activate(handler);
        }
    }

    private void activate(final StartupHandler handler) {
        final SlingSettingsServiceImpl settingsService = new SlingSettingsServiceImpl(bundleContext, handler);

        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_DESCRIPTION,
            "Apache Sling Settings Service");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        this.settingsReg = bundleContext.registerService(new String[] {
                                               SlingSettingsService.class.getName()},
                                               settingsService, props);
        SlingPropertiesPrinter.initPlugin(bundleContext);
        SlingSettingsPrinter.initPlugin(bundleContext, settingsService);
        try {
            RunModeCommand.initPlugin(bundleContext, settingsService.getRunModes());
        } catch (final Throwable ignore) {
            // we just ignore this
        }
        // setup manager service for configuration handling
        final Dictionary<String, String> msProps = new Hashtable<String, String>();
        msProps.put(Constants.SERVICE_PID, settingsService.getClass().getName());
        msProps.put(Constants.SERVICE_DESCRIPTION,
            "Apache Sling Managed Service for the Settings Service");
        msProps.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        managedServiceReg = this.bundleContext.registerService(ManagedService.class.getName(), new ServiceFactory() {

            public void ungetService(final Bundle bundle, final ServiceRegistration registration,
                    final Object service) {
                // nothing to do
            }

            public Object getService(final Bundle bundle, final ServiceRegistration registration) {
                return new SettingsServiceConfigurator(settingsService);
            }
        }, msProps);
    }
    /**
     * Deactivate this listener.
     */
    public void deactivate() {
        if ( this.managedServiceReg != null ) {
            this.managedServiceReg.unregister();
            this.managedServiceReg = null;
        }
        this.startupListener.deactivate();
        if ( this.settingsReg != null ) {
            this.settingsReg.unregister();
            this.settingsReg = null;
        }
        try {
            RunModeCommand.destroyPlugin();
        } catch (Throwable ignore) {
            // we just ignore this
        }
        SlingSettingsPrinter.destroyPlugin();
        SlingPropertiesPrinter.destroyPlugin();
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
