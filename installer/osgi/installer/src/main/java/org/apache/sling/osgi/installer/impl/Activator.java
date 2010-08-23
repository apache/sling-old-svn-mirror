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

import org.apache.sling.osgi.installer.OsgiInstaller;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The activator sets up the logging and registers the
 * OSGi installer service.
 */
public class Activator implements BundleActivator {

    /** Interface of the log service */
    private static String LOG_SERVICE_NAME = "org.osgi.service.log.LogService";

    /** Vendor of all registered services. */
    private static final String VENDOR = "The Apache Software Foundation";

    private OsgiInstallerImpl osgiControllerService;
    private ServiceRegistration osgiControllerServiceReg;

    /** Tracker for the log service. */
    private ServiceTracker logServiceTracker;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) throws Exception {
        // setup logging
        this.logServiceTracker = new ServiceTracker(context, LOG_SERVICE_NAME, null);
        this.logServiceTracker.open();
        Logger.setTracker(this.logServiceTracker);

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
    public void stop(BundleContext context) {
        // stop osgi installer service
        if ( this.osgiControllerService != null ) {
            this.osgiControllerService.deactivate();
            Logger.logInfo("Waiting for installer thread to stop");
            try {
                this.osgiControllerService.join();
            } catch (InterruptedException e) {
                // we simply ignore this
            }

            Logger.logWarn(OsgiInstaller.class.getName()
                    + " service deactivated - this warning can be ignored if system is shutting down");
            this.osgiControllerService = null;
        }
        // unregister service
        if ( this.osgiControllerServiceReg != null ) {
            this.osgiControllerServiceReg.unregister();
            this.osgiControllerServiceReg = null;
        }
        // stop logging
        Logger.setTracker(null);
    	if ( this.logServiceTracker != null ) {
    	    this.logServiceTracker.close();
    	    this.logServiceTracker = null;
    	}
    }
}
