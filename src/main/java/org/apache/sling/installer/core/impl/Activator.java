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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.installer.api.ResourceChangeListener;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.RetryHandler;
import org.apache.sling.installer.core.impl.console.OsgiInstallerWebConsolePlugin;
import org.apache.sling.installer.core.impl.tasks.BundleTaskCreator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * The activator sets up the logging and registers the
 * OSGi installer service.
 */
public class Activator implements BundleActivator {

    /** Vendor of all registered services. */
    public static final String VENDOR = "The Apache Software Foundation";

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
        // create osgi installer
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Installer Controller Service");
        props.put(Constants.SERVICE_VENDOR, VENDOR);

        this.osgiControllerService = new OsgiInstallerImpl(context);
        this.osgiControllerService.setDaemon(true);

        // register internal services
        this.registerServices(context);

        // start and register osgi installer service
        this.osgiControllerService.start();
        final String [] serviceInterfaces = {
                OsgiInstaller.class.getName(),
                ResourceChangeListener.class.getName(),
                RetryHandler.class.getName()
        };
        osgiControllerServiceReg = context.registerService(serviceInterfaces, osgiControllerService, props);

        // register service factory for the web console plugin
        final Hashtable<String, Object> consoleProps = new Hashtable<String, Object>();
        consoleProps.put("felix.webconsole.label", "osgi-installer");
        consoleProps.put("felix.webconsole.title", "OSGi Installer");
        consoleProps.put("felix.webconsole.configprinter.modes", new String[] {"zip", "txt"});
        consoleProps.put(Constants.SERVICE_VENDOR, Activator.VENDOR);
        consoleProps.put(Constants.SERVICE_DESCRIPTION,
            "OSGi Installer Web Console Plugin");
        this.webReg = context.registerService("javax.servlet.Servlet",
                new ServiceFactory() {

                    public void ungetService(final Bundle bundle,
                            final ServiceRegistration reg,
                            final Object consoleObject) {
                        // nothing to do
                    }

                    public Object getService(final Bundle bundle,
                            final ServiceRegistration reg) {
                        return new OsgiInstallerWebConsolePlugin(osgiControllerService);
                    }
                }, consoleProps);
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
            DefaultTransformer.class
        };
        for(final Class<?> serviceClass : serviceClasses) {
            final InternalService service = (InternalService) serviceClass.newInstance();

            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(Constants.SERVICE_DESCRIPTION, service.getDescription());
            props.put(Constants.SERVICE_VENDOR, VENDOR);
            props.put(Constants.SERVICE_RANKING, new Integer(-100));

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
                service.init(context, this.osgiControllerService, this.osgiControllerService);
                this.registrations.add(context.registerService(
                        serviceInterfaces, service, props));
            }
        }
    }
}
