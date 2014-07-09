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

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.installer.api.ResourceChangeListener;
import org.apache.sling.installer.api.event.InstallationListener;
import org.apache.sling.installer.api.info.InfoProvider;
import org.apache.sling.installer.api.jmx.InstallerMBean;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.RetryHandler;
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
    public static final String VENDOR = "The Apache Software Foundation";

    private List<InternalService> services = new ArrayList<InternalService>();
    private List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();

    private OsgiInstallerImpl osgiControllerService;
    private ServiceRegistration osgiControllerServiceReg;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) throws Exception {
        // create osgi installer
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Installer Controller Service");
        props.put(Constants.SERVICE_VENDOR, VENDOR);

        this.osgiControllerService = new OsgiInstallerImpl(context);

        // register internal services
        this.registerServices(context);

        // start and register osgi installer service
        this.osgiControllerService.start();
        final String [] serviceInterfaces = {
                OsgiInstaller.class.getName(),
                InfoProvider.class.getName(),
                ResourceChangeListener.class.getName(),
                RetryHandler.class.getName()
        };
        osgiControllerServiceReg = context.registerService(serviceInterfaces, osgiControllerService, props);

        registerJmxBean(context);
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context) {
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

    private void registerJmxBean(BundleContext context) throws MalformedObjectNameException {
        Hashtable<String, String> jmxProps = new Hashtable<String, String>();
        jmxProps.put("type", "Installer");
        jmxProps.put("name", "Sling OSGi Installer");

        final Hashtable<String, Object> mbeanProps = new Hashtable<String, Object>();
        mbeanProps.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Installer Controller Service");
        mbeanProps.put(Constants.SERVICE_VENDOR, VENDOR);
        mbeanProps.put("jmx.objectname", new ObjectName("org.apache.sling.installer", jmxProps));
        ServiceRegistration mbeanReg = context.registerService(new String[] {InstallerMBean.class.getName(),
                InstallationListener.class.getName()}, new InstallerMBeanImpl(osgiControllerService), mbeanProps);
        registrations.add(mbeanReg);
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
