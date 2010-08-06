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
package org.apache.sling.scripting.jsp;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;

/**
 * This activator registers the tag lib cache
 * It listens for bundle events and reregisters the cache
 * if a bundle event for a used bundle occurs.
 */
public class Activator implements SynchronousBundleListener, BundleActivator {

    /** The service registration for the taglib cache */
    private ServiceRegistration serviceReg;

    /** The bundle context. */
    private BundleContext bundleContext;

    /** The service. */
    private SlingTldLocationsCache service;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) {
        this.bundleContext = context;

        // register service
        this.registerCache();
        this.bundleContext.addBundleListener(this);
    }

    /**
     * Register the taglib cache
     */
    protected void registerCache() {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling JSP Scripting TagLib Cache");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        this.service = new SlingTldLocationsCache(this.bundleContext);
        this.serviceReg = this.bundleContext.registerService(new String[] {TaglibCache.class.getName()}, service, props);
    }

    /**
     * Unregister the taglib cache.
     */
    protected void unregisterCache() {
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
        this.unregisterCache();
        this.bundleContext = null;
    }

    /**
     * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
     */
    public void bundleChanged(final BundleEvent event) {
        if ( event.getType() == BundleEvent.RESOLVED ) {
            this.service.addBundle(event.getBundle());
        } else if ( event.getType() == BundleEvent.UNRESOLVED ) {
            if ( this.service.isBundleUsed(event.getBundle()) ) {
                this.unregisterCache();
                this.registerCache();
            }
        }
    }
}
