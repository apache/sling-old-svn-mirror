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

import java.lang.reflect.Array;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.InvalidServiceFilterSyntaxException;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Mock {@link SlingScriptHelper} implementation.
 */
class MockSlingScriptHelper implements SlingScriptHelper {

    private final SlingHttpServletRequest request;
    private final SlingHttpServletResponse response;
    private final BundleContext bundleContext;

    public MockSlingScriptHelper(final SlingHttpServletRequest request, final SlingHttpServletResponse response,
            final BundleContext bundleContext) {
        this.request = request;
        this.response = response;
        this.bundleContext = bundleContext;
    }

    @Override
    public SlingHttpServletRequest getRequest() {
        return this.request;
    }

    @Override
    public SlingHttpServletResponse getResponse() {
        return this.response;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ServiceType> ServiceType getService(final Class<ServiceType> serviceType) {
        ServiceReference serviceReference = this.bundleContext.getServiceReference(serviceType.getName());
        if (serviceReference != null) {
            return (ServiceType) this.bundleContext.getService(serviceReference);
        } else {
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ServiceType> ServiceType[] getServices(final Class<ServiceType> serviceType, final String filter) {
        try {
            ServiceReference[] serviceReferences = this.bundleContext.getServiceReferences(serviceType.getName(),
                    filter);
            if (serviceReferences != null) {
                ServiceType[] services = (ServiceType[]) Array.newInstance(serviceType, serviceReferences.length);
                for (int i = 0; i < serviceReferences.length; i++) {
                    services[i] = (ServiceType) this.bundleContext.getService(serviceReferences[i]);
                }
                return services;
            } else {
                return (ServiceType[]) ArrayUtils.EMPTY_OBJECT_ARRAY;
            }
        } catch (InvalidSyntaxException ex) {
            throw new InvalidServiceFilterSyntaxException(filter, ex.getMessage(), ex);
        }
    }

    // --- unsupported operations ---
    @Override
    public void dispose() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forward(final String path, final RequestDispatcherOptions requestDispatcherOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forward(final String path, final String requestDispatcherOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forward(final String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forward(final Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forward(final Resource resource, final String requestDispatcherOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forward(final Resource resource, final RequestDispatcherOptions requestDispatcherOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SlingScript getScript() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void include(final String path, final RequestDispatcherOptions requestDispatcherOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void include(final String path, final String requestDispatcherOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void include(final String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void include(final Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void include(final Resource resource, final String requestDispatcherOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void include(final Resource resource, final RequestDispatcherOptions requestDispatcherOptions) {
        throw new UnsupportedOperationException();
    }

}
