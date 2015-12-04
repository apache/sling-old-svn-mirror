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
 * and registers the settings service once all required services are
 * available
 */
public class ServicesListener {

    /** The bundle context. */
    private final BundleContext bundleContext;

    /** The listener for configuration admin. */
    private ConfigAdminListener configAdminListener;

    /** The listener for the shell. */
    private ShellListener shellListener;


    /** The registration of the settings service. */
    private ServiceRegistration settingsReg;

    /**
     * Start listeners
     */
    public ServicesListener(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        final SlingSettingsServiceImpl settingsService = new SlingSettingsServiceImpl(bundleContext);

        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_DESCRIPTION,
            "Apache Sling Settings Service");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        this.settingsReg = bundleContext.registerService(new String[] {
                                               SlingSettingsService.class.getName()},
                                               settingsService, props);
        SlingPropertiesPrinter.initPlugin(bundleContext);
        SlingSettingsPrinter.initPlugin(bundleContext, settingsService);

        // add config admin support
        this.configAdminListener = new ConfigAdminListener(settingsService);
        this.configAdminListener.start();

        // add shell support
        this.shellListener = new ShellListener(settingsService);
        this.shellListener.start();
    }

    /**
     * Deactivate this listener.
     */
    public void deactivate() {
        if ( this.shellListener != null ) {
            this.shellListener.deactivate();
            this.shellListener = null;
        }
        if ( this.configAdminListener != null ) {
            this.configAdminListener.deactivate();
            this.configAdminListener = null;
        }
        if ( this.settingsReg != null ) {
            this.settingsReg.unregister();
            this.settingsReg = null;
        }

        SlingSettingsPrinter.destroyPlugin();
        SlingPropertiesPrinter.destroyPlugin();
    }

    /**
     * Helper class listening for service events for a defined service.
     */
    private abstract class AbstractListener implements ServiceListener {

        /** The name of the service. */
        private final String serviceName;

        /** The service reference. */
        private volatile ServiceReference reference;

        /** The service. */
        private volatile Object service;

        /**
         * Constructor
         */
        public AbstractListener(final String serviceName) {
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
                throw new RuntimeException("Unexpected exception occurred.", ise);
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
                        serviceChanged();
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
                serviceChanged();
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

        protected abstract void serviceChanged();
    }

    /**
     * Helper class listening for service events for config admin
     */
    private final class ConfigAdminListener extends AbstractListener {

        private Object settingsServiceConfigurator;

        private final SlingSettingsServiceImpl settings;

        /**
         * Constructor
         */
        public ConfigAdminListener(final SlingSettingsServiceImpl settings) {
            super("org.osgi.service.cm.ConfigurationAdmin");
            this.settings = settings;
        }

        @Override
        protected void serviceChanged() {
            if ( this.getService() != null && this.settingsServiceConfigurator == null ) {
                this.settingsServiceConfigurator = new SettingsServiceConfigurator(bundleContext, settings);
            }
        }

        @Override
        public void deactivate() {
            super.deactivate();
            if ( settingsServiceConfigurator != null ) {
                ((SettingsServiceConfigurator)settingsServiceConfigurator).destroy();
                settingsServiceConfigurator = null;
            }
        }

    }

    /**
     * Helper class listening for service events for config admin
     */
    private final class ShellListener extends AbstractListener {

        private Object runModeCommand;

        private final SlingSettingsServiceImpl settings;

        /**
         * Constructor
         */
        public ShellListener(final SlingSettingsServiceImpl settings) {
            super("org.apache.felix.shell.ShellService");
            this.settings = settings;
        }

        @Override
        protected void serviceChanged() {
            if ( this.getService() != null && this.runModeCommand == null ) {
                this.runModeCommand = new RunModeCommand(bundleContext, settings.getRunModes());
            }
        }

        @Override
        public void deactivate() {
            super.deactivate();
            if ( runModeCommand != null ) {
                ((RunModeCommand)runModeCommand).destroy();
                runModeCommand = null;
            }
        }

    }
}
