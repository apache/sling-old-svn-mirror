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
package org.apache.sling.installer.factories.subsystems.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator {

    /** The service tracker for the root subsystem */
    private ServiceTracker<Subsystem, Subsystem> rootSubsystemTracker;

    /** The service registration for the installer service. */
    private ServiceRegistration<?> serviceReg;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) throws Exception {
        this.rootSubsystemTracker = new ServiceTracker<Subsystem, Subsystem>(context,
            context.createFilter("(&(" + Constants.OBJECTCLASS + "=" + Subsystem.class.getName() + ")" +
             "(" + SubsystemConstants.SUBSYSTEM_ID_PROPERTY + "=0))"),

             new ServiceTrackerCustomizer<Subsystem, Subsystem>() {

                public Subsystem addingService(final ServiceReference<Subsystem> reference) {
                    final Subsystem service = context.getService(reference);
                    if ( service != null ) {
                        registerInstaller(context, service);
                    }
                    return service;
                }

                public void modifiedService(final ServiceReference<Subsystem> reference, final Subsystem service) {
                    // nothing to do
                }

                public void removedService(final ServiceReference<Subsystem> reference, final Subsystem service) {
                    unregisterInstaller();
                }

        });
        this.rootSubsystemTracker.open();
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context) throws Exception {
        if ( this.rootSubsystemTracker != null ) {
            this.rootSubsystemTracker.close();
            this.rootSubsystemTracker = null;
        }
        this.unregisterInstaller();
    }

    private void registerInstaller(final BundleContext context, final Subsystem rootSubsystem) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Installer Support for Subsystems");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        this.serviceReg = context.registerService(
                new String[] {ResourceTransformer.class.getName(), InstallTaskFactory.class.getName()},
                new SubsystemInstaller(rootSubsystem, context), props);
    }

    private void unregisterInstaller() {
        if ( serviceReg != null ) {
            serviceReg.unregister();
            serviceReg = null;
        }
    }
}
