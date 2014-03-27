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

import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This activator registers the dynamic class loader manager.
 * It listens for bundle events and reregisters the class loader manager
 * if a bundle event for a used bundle occurs.
 */
public class Activator implements SynchronousBundleListener, BundleActivator {

    /** Package admin service name */
    private static String PACKAGE_ADMIN_NAME = PackageAdmin.class.getName();

    /** A service tracker for the package admin. */
    private ServiceTracker packageAdminTracker;

    /** The service registration for the dynamic class loader manager. */
    private ServiceRegistration serviceReg;

    /** The dynamic class loader service factory. */
    private DynamicClassLoaderManagerFactory service;

    /** The bundle context. */
    private BundleContext bundleContext;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) {
        this.bundleContext = context;

        this.packageAdminTracker = new ServiceTracker(this.bundleContext, PACKAGE_ADMIN_NAME, null);
        this.packageAdminTracker.open();

        // register service
        this.registerManagerFactory();
        this.bundleContext.addBundleListener(this);
    }

    /**
     * Register the dynamic class loader manager factory.
     */
    protected void registerManagerFactory() {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Dynamic Class Loader Service");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        this.service = new DynamicClassLoaderManagerFactory(this.bundleContext,
                (PackageAdmin)this.packageAdminTracker.getService());
        this.serviceReg = this.bundleContext.registerService(new String[] {DynamicClassLoaderManager.class.getName()}, service, props);
    }

    /**
     * Unregister the dynamic class loader manager factory.
     */
    protected void unregisterManagerFactory() {
        if ( this.serviceReg != null ) {
            this.serviceReg.unregister();
            this.serviceReg = null;
        }
        if ( this.service != null ) {
            this.service = null;
        }
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context) {
        context.removeBundleListener(this);
        this.unregisterManagerFactory();
        if ( this.packageAdminTracker != null ) {
            this.packageAdminTracker.close();
            this.packageAdminTracker = null;
        }
        this.bundleContext = null;
    }

    /**
     * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
     */
    public void bundleChanged(final BundleEvent event) {
        synchronized ( this ) {
            final boolean lazyBundle = event.getBundle().getHeaders().get( Constants.BUNDLE_ACTIVATIONPOLICY ) != null;

            final boolean reload;
            if ( ( event.getType() == BundleEvent.STARTED && !lazyBundle)
                 || (event.getType() == BundleEvent.STARTING && lazyBundle) ) {
                reload = this.service.hasUnresolvedPackages(event.getBundle());
            } else if ( event.getType() == BundleEvent.UNRESOLVED || event.getType() == BundleEvent.RESOLVED ) {
                reload = this.service.isBundleUsed(event.getBundle().getBundleId());
            } else {
                reload = false;
            }
            if ( reload ) {
                this.unregisterManagerFactory();
                this.registerManagerFactory();
            }
        }
    }
}
