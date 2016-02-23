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

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Wrapper for {@link MockAdapterManager} which makes sure multiple unit tests
 * running in parallel do not get in conflict with each other. Instead, a
 * different {@link MockAdapterManager} is used per thread.
 */
class ThreadsafeMockAdapterManagerWrapper implements AdapterManager {

    private static final ThreadLocal<AdapterManagerBundleContextFactory> THREAD_LOCAL = new ThreadLocal<AdapterManagerBundleContextFactory>() {
        @Override
        protected AdapterManagerBundleContextFactory initialValue() {
            return new AdapterManagerBundleContextFactory();
        }
    };

    @Override
    public <AdapterType> AdapterType getAdapter(final Object adaptable, final Class<AdapterType> type) {
        AdapterManager adapterManager = THREAD_LOCAL.get().getAdapterManager();
        return adapterManager.getAdapter(adaptable, type);
    }

    /**
     * Sets bundle context.
     * @param bundleContext Bundle context
     */
    public void setBundleContext(final BundleContext bundleContext) {
        AdapterManagerBundleContextFactory adapterManager = THREAD_LOCAL.get();
        adapterManager.setBundleContext(bundleContext);
    }

    /**
     * Removes bundle context reference.
     */
    public void clearBundleContext() {
        AdapterManagerBundleContextFactory adapterManager = THREAD_LOCAL.get();
        adapterManager.clearBundleContext();
    }
    
    
    private static class AdapterManagerBundleContextFactory {
        
        private BundleContext bundleContext;
        
        public void setBundleContext(final BundleContext bundleContext) {
            this.bundleContext = bundleContext;

            // register adapter manager
            MockAdapterManagerImpl adapterManagerImpl = new MockAdapterManagerImpl();
            Dictionary<String,Object> properties = new Hashtable<String, Object>();
            MockOsgi.injectServices(adapterManagerImpl, bundleContext);
            MockOsgi.activate(adapterManagerImpl, bundleContext, properties);
            bundleContext.registerService(AdapterManager.class.getName(), adapterManagerImpl, properties);
        }

        public void clearBundleContext() {
            this.bundleContext = null;
        }
        
        public synchronized AdapterManager getAdapterManager() {
            if (bundleContext == null) {
                setBundleContext(MockOsgi.newBundleContext());
            }
            ServiceReference<AdapterManager> serviceReference = bundleContext.getServiceReference(AdapterManager.class);
            if (serviceReference != null) {
                return (AdapterManager)bundleContext.getService(serviceReference);
            }
            else {
                throw new RuntimeException("AdapterManager not registered in bundle context.");
            }
        }
        
    }

}
