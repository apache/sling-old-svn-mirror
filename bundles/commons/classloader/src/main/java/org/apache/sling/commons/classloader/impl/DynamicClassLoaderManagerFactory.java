/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.classloader.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * This is the service factory for the dynamic class loader manager.
 */
public class DynamicClassLoaderManagerFactory
    implements ServiceFactory {

    /** The package admin. */
    private final PackageAdmin pckAdmin;

    /** The bundle context. */
    private final BundleContext context;

    private final Set<Long> usedBundles = Collections.synchronizedSet(new HashSet<Long>());

    /** All created managers. */
    private final List<DynamicClassLoaderManagerImpl> managers = new ArrayList<DynamicClassLoaderManagerImpl>();

    /**
     * Create a new service instance
     * @param ctx The bundle context.
     * @param pckAdmin The package admin.
     */
    public DynamicClassLoaderManagerFactory(final BundleContext ctx,
                                            final PackageAdmin pckAdmin) {
        this.context = ctx;
        this.pckAdmin = pckAdmin;
    }

    /**
     * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
     */
    public Object getService(final Bundle bundle,
                             final ServiceRegistration registration) {
        final DynamicClassLoaderManagerImpl manager =  new DynamicClassLoaderManagerImpl(this.context,
                this.pckAdmin, new BundleProxyClassLoader(bundle), this);
        synchronized ( managers ) {
            managers.add(manager);
        }
        return manager;
    }

    /**
     * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, java.lang.Object)
     */
    public void ungetService(final Bundle bundle,
                             final ServiceRegistration registration,
                             final Object service) {
        if ( service != null ) {
            synchronized ( managers ) {
                managers.remove(service);
            }
            ((DynamicClassLoaderManagerImpl)service).deactivate();
        }
    }

    /**
     * Check if a bundle has been used for class loading.
     * @param bundleId The bundle id.
     * @return <code>true</code> if the bundle has been used.
     */
    public boolean isBundleUsed(final long bundleId) {
        return usedBundles.contains(bundleId);
    }

    /**
     * Notify that a bundle is used as a source for class loading.
     * @param bundle The bundle.
     */
    public void addUsedBundle(final Bundle bundle) {
        final long id = bundle.getBundleId();
        this.usedBundles.add(id);
    }

    /**
     * Clear the negative caches of all registered managers.
     */
    public void clearNegativeCaches() {
        synchronized ( this.managers ) {
            for(final DynamicClassLoaderManagerImpl manager : this.managers) {
                manager.clearNegativeCache();
            }
        }
    }
}
