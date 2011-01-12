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
package org.apache.sling.installer.core.impl;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.core.impl.config.ConfigTaskCreator;
import org.apache.sling.installer.core.impl.console.OsgiInstallerWebConsolePlugin;
import org.apache.sling.installer.core.impl.tasks.BundleTaskCreator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * The activator sets up the logging and registers the
 * OSGi installer service.
 */
public class Activator implements BundleActivator {

    /** Vendor of all registered services. */
    private static final String VENDOR = "The Apache Software Foundation";

    private List<InternalService> services = new ArrayList<InternalService>();
    private List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();

    private OsgiInstallerImpl osgiControllerService;
    private ServiceRegistration osgiControllerServiceReg;

    /** Registration for the web console plugin. */
    private ServiceRegistration webReg;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) throws Exception {
        // register internal services
        this.registerServices(context);

        // register osgi installer service
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Install Controller Service");
        props.put(Constants.SERVICE_VENDOR, VENDOR);

        this.osgiControllerService = new OsgiInstallerImpl(context);
        this.osgiControllerService.setDaemon(true);
        this.osgiControllerService.start();
        final String [] serviceInterfaces = {
                OsgiInstaller.class.getName()
        };
        osgiControllerServiceReg = context.registerService(serviceInterfaces, osgiControllerService, props);

        try {
            this.webReg = OsgiInstallerWebConsolePlugin.register(context,
                    this.osgiControllerService);
        } catch (final Throwable ignore) {
            // ignore this
        }
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context) {
        // stop web console plugin
        if ( this.webReg != null ) {
            this.webReg.unregister();
            this.webReg = null;
        }
        // stop osgi installer service
        if ( this.osgiControllerService != null ) {
            this.osgiControllerService.deactivate();
            this.osgiControllerService = null;
        }
        if ( this.osgiControllerServiceReg != null ) {
            this.osgiControllerServiceReg.unregister();
            this.osgiControllerServiceReg = null;
        }
        // unregister services
        for(final ServiceRegistration reg : this.registrations) {
            reg.unregister();
        }
        this.registrations.clear();
        // stop services
        for(final InternalService service : this.services) {
            service.deactivate();
        }
        this.services.clear();
    }

    /**
     * Register internal services.
     */
    private void registerServices(final BundleContext context) throws Exception {

        final Class<?>[] serviceClasses = new Class<?>[] {
            BundleTaskCreator.class,
            ConfigTaskCreator.class,
            DefaultTransformer.class
        };
        for(final Class<?> serviceClass : serviceClasses) {
            final InternalService service = (InternalService) serviceClass.newInstance();

            final Hashtable<String, String> props = new Hashtable<String, String>();
            props.put(Constants.SERVICE_DESCRIPTION, service.getDescription());
            props.put(Constants.SERVICE_VENDOR, VENDOR);

            final String[] serviceInterfaces;
            if ( service instanceof ResourceTransformer && service instanceof InstallTaskFactory ) {
                serviceInterfaces = new String[] {
                        ResourceTransformer.class.getName(),
                        InstallTaskFactory.class.getName()
                };
            } else if ( service instanceof ResourceTransformer ) {
                serviceInterfaces = new String[] {
                        ResourceTransformer.class.getName()
                };

            } else if ( service instanceof InstallTaskFactory ) {
                serviceInterfaces = new String[] {
                        InstallTaskFactory.class.getName()
                };
            } else {
                serviceInterfaces = null;
            }
            if ( serviceInterfaces != null ) {
                this.services.add(service);
                service.init(context);
                this.registrations.add(context.registerService(
                        serviceInterfaces, service, props));
            }
        }
    }
}
