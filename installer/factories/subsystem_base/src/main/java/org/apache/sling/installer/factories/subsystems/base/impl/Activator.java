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
package org.apache.sling.installer.factories.subsystems.base.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
    private ServiceTracker<SlingSettingsService, SlingSettingsService> slingSettingsTracker;

    /** The service registration for the installer service. */
    private ServiceRegistration<?> serviceReg;

    public void start(BundleContext context) throws Exception {
        slingSettingsTracker = new ServiceTracker<SlingSettingsService, SlingSettingsService>(context,
            SlingSettingsService.class, null) {
                @Override
                public SlingSettingsService addingService(ServiceReference<SlingSettingsService> reference) {
                    SlingSettingsService slingSettings = super.addingService(reference);
                    registerInstaller(context, slingSettings);
                    return slingSettings;
                }

                @Override
                public void removedService(ServiceReference<SlingSettingsService> reference, SlingSettingsService service) {
                    unregisterInstaller();
                    super.removedService(reference, service);
                }
        };
        slingSettingsTracker.open();
    }

    public void stop(BundleContext context) throws Exception {
        slingSettingsTracker.close();
        unregisterInstaller();
    }

    private void registerInstaller(BundleContext context, SlingSettingsService slingSettings) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Installer Support for subsystem-base files");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        this.serviceReg = context.registerService(ResourceTransformer.class,
                new SubsystemBaseTransformer(slingSettings), props);
    }

    private void unregisterInstaller() {
        if ( serviceReg != null ) {
            serviceReg.unregister();
            serviceReg = null;
        }
    }
}
