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
package org.apache.sling.testing.mock.sling.servlet;

import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.MockSling;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Mock {@link org.apache.sling.api.SlingHttpServletRequest} implementation.
 */
public class MockSlingHttpServletRequest extends org.apache.sling.servlethelpers.MockSlingHttpServletRequest {

    private final BundleContext bundleContext;

    /**
     * Instantiate with default resource resolver
     * @deprecated Please use {@link #MockSlingHttpServletRequest(BundleContext)}
     *   and shutdown the bundle context after usage.
     */
    @Deprecated
    public MockSlingHttpServletRequest() {
        this(MockOsgi.newBundleContext());
    }

    /**
     * Instantiate with default resource resolver
     * @param bundleContext Bundle context
     */
    public MockSlingHttpServletRequest(BundleContext bundleContext) {
        this(MockSling.newResourceResolver(bundleContext));
    }

    /**
     * @param resourceResolver Resource resolver
     * @deprecated Please use {@link #MockSlingHttpServletRequest(ResourceResolver, BundleContext)}
     *   and shutdown the bundle context after usage.
     */
    @Deprecated
    public MockSlingHttpServletRequest(ResourceResolver resourceResolver) {
        this(resourceResolver, MockOsgi.newBundleContext());
    }

    /**
     * @param resourceResolver Resource resolver
     * @param bundleContext Bundle context
     */
    public MockSlingHttpServletRequest(ResourceResolver resourceResolver, BundleContext bundleContext) {
        super(resourceResolver);
        this.bundleContext = bundleContext;
    }

    protected MockRequestPathInfo newMockRequestPathInfo() {
        return new MockRequestPathInfo();
    }

    protected MockHttpSession newMockHttpSession() {
        return new MockHttpSession();
    }
    
    @Override
    public ResourceBundle getResourceBundle(String baseName, Locale locale) {
        // check of ResourceBundleProvider is registered in mock OSGI context
        ResourceBundle resourceBundle = null;
        ServiceReference serviceReference = bundleContext.getServiceReference(ResourceBundleProvider.class.getName());
        if (serviceReference != null) {
            ResourceBundleProvider provider = (ResourceBundleProvider)bundleContext.getService(serviceReference);
            resourceBundle = provider.getResourceBundle(baseName, locale);
        }       
        // if no ResourceBundleProvider exists return empty bundle
        if (resourceBundle == null) {
            resourceBundle = EMPTY_RESOURCE_BUNDLE;
        }
        return resourceBundle;
    }

}
