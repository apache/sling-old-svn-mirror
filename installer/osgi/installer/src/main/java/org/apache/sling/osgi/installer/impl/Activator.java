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
package org.apache.sling.osgi.installer.impl;

import java.util.Hashtable;

import org.apache.sling.osgi.installer.InstallableResourceFactory;
import org.apache.sling.osgi.installer.OsgiInstaller;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator, FrameworkListener, BundleListener {

    private static final String VENDOR = "The Apache Software Foundation";

    private OsgiInstallerImpl osgiControllerService;
    private ServiceRegistration osgiControllerServiceReg;
    private ServiceRegistration factoryServiceReg;

    private static long eventsCount;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        // listen to framework and bundle events
        context.addFrameworkListener(this);
        context.addBundleListener(this);

        // register OsgiController service
        {
            final Hashtable<String, String> props = new Hashtable<String, String>();
            props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Install Controller Service");
            props.put(Constants.SERVICE_VENDOR, VENDOR);

            this.osgiControllerService = new OsgiInstallerImpl(context);
            final String [] serviceInterfaces = {
                    OsgiInstaller.class.getName()
            };
            osgiControllerServiceReg = context.registerService(serviceInterfaces, osgiControllerService, props);
        }

        // register installable resource factory service
        {
            final Hashtable<String, String> props = new Hashtable<String, String>();
            props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Installable Resource Factory");
            props.put(Constants.SERVICE_VENDOR,VENDOR);

            factoryServiceReg = context.registerService(InstallableResourceFactory.class.getName(),
                    new InstallableResourceFactoryImpl(), props);
        }

        // register EventsCounter service
        {
            final Hashtable<String, String> props = new Hashtable<String, String>();
            props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling EventsCounter Service");
            props.put(Constants.SERVICE_VENDOR, VENDOR);
        }
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
    	context.removeBundleListener(this);
    	context.removeFrameworkListener(this);

    	if ( this.factoryServiceReg != null ) {
    	    this.factoryServiceReg.unregister();
    	    this.factoryServiceReg = null;
    	}
        if ( this.osgiControllerServiceReg != null ) {
            this.osgiControllerServiceReg.unregister();
            this.osgiControllerServiceReg = null;
        }
        if ( this.osgiControllerService != null ) {
            this.osgiControllerService.deactivate();
            this.osgiControllerService = null;
        }
    }

    /** Used for tasks that wait for a framework or bundle event before retrying their operations */
    public static long getTotalEventsCount() {
        return eventsCount;
    }

    public void frameworkEvent(FrameworkEvent arg0) {
        eventsCount++;
    }

    public void bundleChanged(BundleEvent arg0) {
        eventsCount++;
    }
}
