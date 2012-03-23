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

import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.classloader.DynamicClassLoaderProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the default implementation of the dynamic class loader
 * manager.
 */
public class DynamicClassLoaderManagerImpl
    implements DynamicClassLoaderManager {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The class loaders */
    private final ClassLoader[] loaders;

    /** The dynamic class loader. */
    private final ClassLoaderFacade facade;

    /** Is this still active? */
    private volatile boolean active = true;

    private final ServiceTracker deprecatedProviderTracker;

    /**
     * Create a new service instance
     * @param ctx The bundle context of the class loader bundle
     * @param pckAdmin The package admin.
     * @param parent The parent class loader.
     */
    public DynamicClassLoaderManagerImpl(final BundleContext ctx,
            final PackageAdmin pckAdmin,
            final ClassLoader parent,
            final DynamicClassLoaderManagerFactory factory) {
        this.deprecatedProviderTracker = new ServiceTracker(ctx, DynamicClassLoaderProvider.class.getName(),
                new ServiceTrackerCustomizer() {

                    public void removedService(final ServiceReference serviceRef,
                            final Object paramObject) {
                        ctx.ungetService(serviceRef);
                    }

                    public void modifiedService(final ServiceReference serviceRef,
                            final Object paramObject) {
                        // nothing to do
                    }

                    public Object addingService(final ServiceReference serviceRef) {
                        final Object obj = ctx.getService(serviceRef);
                        if ( obj != null ) {
                            logger.warn("Dynamic class loader does not support deprecated dynamic class loader providers: {} : {}",
                                serviceRef, obj);
                        }
                        return obj;
                    }
                });
        this.deprecatedProviderTracker.open();
        this.loaders = new ClassLoader[] {new PackageAdminClassLoader(pckAdmin, parent, factory)};
        this.facade = new ClassLoaderFacade(this);
    }

    /**
     * Deactivate this service.
     */
    public void deactivate() {
        this.deprecatedProviderTracker.close();
        this.active = false;
    }

    /**
     * Check if this service is still active.
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * @see org.apache.sling.commons.classloader.DynamicClassLoaderManager#getDynamicClassLoader()
     */
    public ClassLoader getDynamicClassLoader() {
        return this.facade;
    }

    /**
     * Return the dynamic class loaders to use
     * Currently this is just the package admin class loader.
     */
    public ClassLoader[] getDynamicClassLoaders() {
        return this.loaders;
    }
}
