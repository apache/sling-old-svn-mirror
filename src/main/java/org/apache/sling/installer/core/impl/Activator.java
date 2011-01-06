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

import java.util.Hashtable;

import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.core.impl.config.ConfigTaskCreator;
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

    private OsgiInstallerImpl osgiControllerService;
    private ServiceRegistration osgiControllerServiceReg;

    private BundleTaskCreator bundleTaskFactory;
    private ServiceRegistration bundleTaskFactoryReg;

    private ConfigTaskCreator configTaskFactory;
    private ServiceRegistration configTaskFactoryReg;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) throws Exception {
        // register install task factories
        this.registerFactories(context);

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
        // unregister service
        if ( this.osgiControllerServiceReg != null ) {
            this.osgiControllerServiceReg.unregister();
            this.osgiControllerServiceReg = null;
        }
        if ( this.bundleTaskFactoryReg != null ) {
            this.bundleTaskFactoryReg.unregister();
            this.bundleTaskFactoryReg = null;
        }
        if ( this.bundleTaskFactory != null ) {
            this.bundleTaskFactory.deactivate();
            this.bundleTaskFactory = null;
        }
        if ( this.configTaskFactoryReg != null ) {
            this.configTaskFactoryReg.unregister();
            this.configTaskFactoryReg = null;
        }
        if ( this.configTaskFactory != null ) {
            this.configTaskFactory.deactivate();
            this.configTaskFactory = null;
        }
    }

    private void registerFactories(final BundleContext context) {
        final String [] serviceInterfaces = {
                InstallTaskFactory.class.getName()
        };

        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Bundle Install Task Factory");
        props.put(Constants.SERVICE_VENDOR, VENDOR);

        this.bundleTaskFactory = new BundleTaskCreator(context);
        this.bundleTaskFactoryReg = context.registerService(serviceInterfaces,
                bundleTaskFactory, props);

        props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Configuration Install Task Factory");
        props.put(Constants.SERVICE_VENDOR, VENDOR);

        this.configTaskFactory = new ConfigTaskCreator(context);
        this.configTaskFactoryReg = context.registerService(serviceInterfaces,
                configTaskFactory, props);
    }
}
