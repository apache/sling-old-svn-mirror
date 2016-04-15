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
package org.apache.sling.testing.mock.osgi;

import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;

/**
 * Mock {@link ComponentContext} implementation.
 */
class MockComponentContext implements ComponentContext {

    private final MockBundleContext bundleContext;
    private final Dictionary<String, Object> properties;
    private final Bundle usingBundle;

    public MockComponentContext(final MockBundleContext mockBundleContext, 
            final Dictionary<String, Object> properties, final Bundle usingBundle) {
        this.bundleContext = mockBundleContext;
        this.properties = properties;
        this.usingBundle = usingBundle;
    }

    @Override
    public Dictionary<String, Object> getProperties() {
        return this.properties;
    }

    @Override
    public <S> S locateService(final String name, final ServiceReference<S> reference) {
        return this.bundleContext.locateService(name, reference);
    }

    @Override
    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    @Override
    public void disableComponent(final String name) {
        // allow calling, but ignore
    }

    @Override
    public void enableComponent(final String name) {
        // allow calling, but ignore
    }

    @Override
    public Bundle getUsingBundle() {
        return usingBundle;
    }

    // --- unsupported operations ---
    @Override
    public ComponentInstance getComponentInstance() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceReference<?> getServiceReference() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object locateService(final String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] locateServices(final String name) {
        throw new UnsupportedOperationException();
    }

}
