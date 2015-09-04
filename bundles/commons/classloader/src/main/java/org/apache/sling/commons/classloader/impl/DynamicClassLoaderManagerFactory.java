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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the service factory for the dynamic class loader manager.
 */
public class DynamicClassLoaderManagerFactory
    implements ServiceFactory {

    /** The logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The package admin. */
    private final PackageAdmin pckAdmin;

    /** The bundle context. */
    private final BundleContext context;

    private final Set<Long> usedBundles = Collections.synchronizedSet(new HashSet<Long>());

    private final Set<String> unresolvedPackages = Collections.synchronizedSet(new HashSet<String>());

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
        return manager;
    }

    /**
     * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, java.lang.Object)
     */
    public void ungetService(final Bundle bundle,
                             final ServiceRegistration registration,
                             final Object service) {
        if ( service != null ) {
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
     * Notify that a package is not found during class loading.
     * @param pckName The package name.
     */
    public void addUnresolvedPackage(final String pckName) {
        this.unresolvedPackages.add(pckName);
    }

    /**
     * Check if an exported package from the bundle has not been
     * found during previous class loading attempts.
     * @param bundle The bundle to check
     * @return <code>true</code> if a package has not be found before
     */
    public boolean hasUnresolvedPackages(final Bundle bundle) {
        if ( !this.unresolvedPackages.isEmpty() ) {
            final ExportedPackage[] pcks = this.pckAdmin.getExportedPackages(bundle);
            if ( pcks != null ) {
                for(final ExportedPackage pck : pcks ) {
                    if ( this.unresolvedPackages.contains(pck.getName()) ) {
                        logger.debug("Bundle '{}' provides package '{}' which has been previously unsuccessfully requested!", bundle, pck.getName());
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
