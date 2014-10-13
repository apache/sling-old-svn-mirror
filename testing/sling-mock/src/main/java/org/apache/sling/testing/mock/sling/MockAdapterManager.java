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
package org.apache.sling.testing.mock.sling;

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.adapter.AdapterManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Mock {@link AdapterManager} implementation.
 */
class MockAdapterManager implements AdapterManager {

    /**
     * OSGi bundle context to detect all services that implement
     * {@link AdapterFactory}.
     */
    private BundleContext bundleContext;

    /**
     * Returns the adapted <code>adaptable</code> or <code>null</code> if the
     * object cannot be adapted.
     */
    @Override
    public <AdapterType> AdapterType getAdapter(final Object adaptable, final Class<AdapterType> type) {

        // iterate over all adapter factories and try to adapt the object
        if (this.bundleContext != null) {
            try {
                ServiceReference[] references = bundleContext
                        .getServiceReferences(AdapterFactory.class.getName(), null);
                for (ServiceReference serviceReference : references) {
                    AdapterFactory adapterFactory = (AdapterFactory) bundleContext.getService(serviceReference);
                    AdapterType instance = adapterFactory.getAdapter(adaptable, type);
                    if (instance != null) {
                        return instance;
                    }
                }
            } catch (InvalidSyntaxException ex) {
                throw new RuntimeException("Unable to get adapter factories.", ex);
            }
        }

        // no matching adapter factory found
        return null;
    }

    /**
     * Sets bundle context.
     * @param bundleContext Bundle context
     */
    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * Removes bundle context reference.
     */
    public void clearBundleContext() {
        this.bundleContext = null;
    }

}
