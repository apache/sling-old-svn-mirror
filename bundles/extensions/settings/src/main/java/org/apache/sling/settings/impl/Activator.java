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
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * This is the bundle activator.
 * It registers the SlingSettingsService.
 *
 */
public class Activator implements BundleActivator, BundleListener {

    /** The service registration */
    private ServiceRegistration serviceRegistration;

    /** The bundle context. */
    private BundleContext bundleContext;

    /** The settings service. */
    private SlingSettingsServiceImpl settingsService;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        this.bundleContext = context;
        this.settingsService = new SlingSettingsServiceImpl(context);
        // for compatibility, we might have to wait for the engine bundle
        // to be started to get the Sling ID

        if ( this.settingsService.isDelayedStart() ) {
            this.bundleContext.addBundleListener(this);
        } else {
            this.startService();
        }
    }

    /**
     * @param event
     */
    public void bundleChanged(BundleEvent event) {
        if ( SlingSettingsServiceImpl.ENGINE_SYMBOLIC_NAME.equals(event.getBundle().getSymbolicName())) {
            this.settingsService.initDelayed(this.bundleContext);
            if ( !this.settingsService.isDelayedStart() ) {
                this.bundleContext.removeBundleListener(this);
                this.startService();
            }
        }

    }

    private void startService() {
        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_PID, this.settingsService.getClass().getName());
        props.put(Constants.SERVICE_DESCRIPTION,
            "Apache Sling Settings Service");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        serviceRegistration = this.bundleContext.registerService(new String[] {
                                               SlingSettingsService.class.getName()},
                                               this.settingsService, props);
        try {
            SlingPropertiesPrinter.initPlugin(this.bundleContext);
        } catch (Throwable ignore) {
            // we just ignore this
        }
        try {
            SlingSettingsPrinter.initPlugin(this.bundleContext, this.settingsService);
        } catch (Throwable ignore) {
            // we just ignore this
        }
        try {
            RunModeCommand.initPlugin(this.bundleContext, this.settingsService.getRunModes());
        } catch (Throwable ignore) {
            // we just ignore this
        }

    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        this.bundleContext.removeBundleListener(this);
        try {
            RunModeCommand.destroyPlugin();
        } catch (Throwable ignore) {
            // we just ignore this
        }
        try {
            SlingSettingsPrinter.destroyPlugin();
        } catch (Throwable ignore) {
            // we just ignore this
        }
        try {
            SlingPropertiesPrinter.destroyPlugin();
        } catch (Throwable ignore) {
            // we just ignore this
        }
        if ( serviceRegistration != null ) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
        this.settingsService = null;
    }
}
