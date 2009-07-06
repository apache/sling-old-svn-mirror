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
package org.apache.sling.commons.classloader.impl;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This activator registers the dynamic class loader manager.
 */
public class Activator implements BundleActivator {

    /** Package admin service name */
    private static String PACKAGE_ADMIN_NAME = PackageAdmin.class.getName();

    /** A service tracker for the package admin. */
    private ServiceTracker packageAdminTracker;

    /** The service registration for the dynamic class loader manager. */
    private ServiceRegistration serviceReg;

    /** The dynamic class loader service. */
    private DynamicClassLoaderManagerImpl service;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        this.packageAdminTracker = new ServiceTracker(context, PACKAGE_ADMIN_NAME, null);
        this.packageAdminTracker.open();

        // register service
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Dynamic Class Loader Service");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        this.service = new DynamicClassLoaderManagerImpl(context,
                (PackageAdmin)this.packageAdminTracker.getService());
        this.serviceReg = context.registerService(new String[] {DynamicClassLoaderManagerImpl.class.getName()}, service, props);
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        if ( this.serviceReg != null ) {
            this.serviceReg.unregister();
            this.serviceReg = null;
        }
        if ( this.service != null ) {
            this.service.deactivate();
            this.service = null;
        }
        if ( this.packageAdminTracker != null ) {
            this.packageAdminTracker.close();
            this.packageAdminTracker = null;
        }
    }
}
