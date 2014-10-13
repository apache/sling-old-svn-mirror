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

import org.apache.sling.api.adapter.AdapterManager;
import org.osgi.framework.BundleContext;

/**
 * Wrapper for {@link MockAdapterManager} which makes sure multiple unit tests
 * running in parallel do not get in conflict with each other. Instead, a
 * different {@link MockAdapterManager} is used per thread.
 */
class ThreadsafeMockAdapterManagerWrapper implements AdapterManager {

    private static final ThreadLocal<MockAdapterManager> THREAD_LOCAL = new ThreadLocal<MockAdapterManager>() {
        @Override
        protected MockAdapterManager initialValue() {
            return new MockAdapterManager();
        }
    };

    @Override
    public <AdapterType> AdapterType getAdapter(final Object adaptable, final Class<AdapterType> type) {
        MockAdapterManager adapterManager = THREAD_LOCAL.get();
        return adapterManager.getAdapter(adaptable, type);
    }

    /**
     * Sets bundle context.
     * @param bundleContext Bundle context
     */
    public void setBundleContext(final BundleContext bundleContext) {
        MockAdapterManager adapterManager = THREAD_LOCAL.get();
        adapterManager.setBundleContext(bundleContext);
    }

    /**
     * Removes bundle context reference.
     */
    public void clearBundleContext() {
        MockAdapterManager adapterManager = THREAD_LOCAL.get();
        adapterManager.clearBundleContext();
    }

}
