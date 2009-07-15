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

import java.util.Arrays;
import java.util.Comparator;

import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.classloader.DynamicClassLoaderProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * This is the default implementation of the dynamic class loader
 * manager.
 */
public class DynamicClassLoaderManagerImpl
    extends ServiceTracker
    implements DynamicClassLoaderManager, ServiceTrackerCustomizer {

    /** The package admin class loader. */
    private final PackageAdminClassLoader pckAdminCL;

    /** The dynamic class loader. */
    private final ClassLoaderFacade facade;

    /** The bundle context. */
    private final BundleContext context;

    private ClassLoader[] cache;

    private boolean updateCache = false;

    /**
     * Create a new service instance
     * @param ctx The bundle context.
     * @param pckAdmin The package admin.
     */
    public DynamicClassLoaderManagerImpl(final BundleContext ctx,
            final PackageAdmin pckAdmin) {
        super(ctx, DynamicClassLoaderProvider.class.getName(), null);
        this.context = ctx;
        this.pckAdminCL = new PackageAdminClassLoader(pckAdmin, this.getClass().getClassLoader());
        this.cache = new ClassLoader[] {this.pckAdminCL};
        this.open();
        this.facade = new ClassLoaderFacade(this);
    }

    public Object addingService(ServiceReference reference) {
        this.updateCache = true;
        return this.context.getService(reference);
    }

    public void modifiedService(ServiceReference reference, Object service) {
        // as the ranking property has changed we have to update the cache
        this.updateCache = true;
    }

    public void removedService(ServiceReference reference, Object service) {
        this.context.ungetService(reference);
        this.updateCache = true;
    }

    private synchronized void updateCache() {
        if ( this.updateCache ) {
            final ServiceReference[] refs = this.getServiceReferences();
            final ClassLoader[] loaders = new ClassLoader[1 + refs.length];
            Arrays.sort(refs, ServiceReferenceComparator.INSTANCE);
            loaders[0] = this.pckAdminCL;
            int index = 1;
            for(final ServiceReference ref : refs) {
                final DynamicClassLoaderProvider provider = (DynamicClassLoaderProvider)this.getService(ref);
                loaders[index] = provider.getClassLoader();
                index++;
            }
            // and now use new array
            this.cache = loaders;
            this.updateCache = false;
        }
    }

    /**
     * Deactivate this service.
     */
    public void deactivate() {
        this.close();
    }


    /**
     * @see org.apache.sling.commons.classloader.DynamicClassLoaderManager#getDynamicClassLoader()
     */
    public ClassLoader getDynamicClassLoader() {
        return this.facade;
    }

    /**
     * This list contains the current list of class loaders. The first class loader
     * is always the package admin class loader, therefore this list is never null
     * and has always a size greater than zero.
     * @return The list of class loaders.
     */
    public ClassLoader[] getDynamicClassLoaders() {
        if ( this.updateCache ) {
            updateCache();
        }
        return this.cache;
    }

    /**
     * Comparator for service references.
     */
    protected static final class ServiceReferenceComparator implements Comparator<ServiceReference> {
        public static ServiceReferenceComparator INSTANCE = new ServiceReferenceComparator();

        public int compare(ServiceReference o1, ServiceReference o2) {
            return o1.compareTo(o2);
        }
    }
}
