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
        return new DynamicClassLoaderManagerImpl(this.context, this.pckAdmin, new BundleProxyClassLoader(bundle));
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
}
