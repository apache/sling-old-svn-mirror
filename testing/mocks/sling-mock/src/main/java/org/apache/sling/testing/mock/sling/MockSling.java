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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.apache.sling.testing.mock.sling.spi.ResourceResolverTypeAdapter;
import org.osgi.framework.BundleContext;

/**
 * Factory for mock Sling objects.
 */
public final class MockSling {

    /**
     * Default resource resolver type is {@link ResourceResolverType#RESOURCERESOLVER_MOCK}.
     */
    public static final ResourceResolverType DEFAULT_RESOURCERESOLVER_TYPE = ResourceResolverType.RESOURCERESOLVER_MOCK;

    private static final ThreadsafeMockAdapterManagerWrapper ADAPTER_MANAGER = new ThreadsafeMockAdapterManagerWrapper();
    static {
        // register mocked adapter manager
        SlingAdaptable.setAdapterManager(ADAPTER_MANAGER);
    }

    private MockSling() {
        // static methods only
    }

    /**
     * Creates new sling resource resolver factory instance.
     * @param type Type of underlying repository.
     * @return Resource resolver factory instance
     * @deprecated Please use {@link #newResourceResolverFactory(ResourceResolverType, BundleContext)}
     *   and shutdown the bundle context after usage.
     */
    @Deprecated
    public static ResourceResolverFactory newResourceResolverFactory(final ResourceResolverType type) {
        return newResourceResolverFactory(type, MockOsgi.newBundleContext());
    }
    
    /**
     * Creates new sling resource resolver factory instance.
     * @param bundleContext Bundle context
     * @return Resource resolver factory instance
     */
    public static ResourceResolverFactory newResourceResolverFactory(final BundleContext bundleContext) {
        return newResourceResolverFactory(DEFAULT_RESOURCERESOLVER_TYPE, bundleContext);
    }

    /**
     * Creates new sling resource resolver factory instance.
     * @param type Type of underlying repository.
     * @param bundleContext Bundle context
     * @return Resource resolver factory instance
     */
    public static ResourceResolverFactory newResourceResolverFactory(final ResourceResolverType type,
            final BundleContext bundleContext) {
        ResourceResolverTypeAdapter adapter = getResourceResolverTypeAdapter(type);
        ResourceResolverFactory factory = adapter.newResourceResolverFactory();
        if (factory == null) {
            SlingRepository repository = adapter.newSlingRepository();
            factory = ResourceResolverFactoryInitializer.setUp(repository, bundleContext, type.getNodeTypeMode());
        }
        else {
            bundleContext.registerService(ResourceResolverFactory.class.getName(), factory, null);
        }
        return factory;
    }

    private static ResourceResolverTypeAdapter getResourceResolverTypeAdapter(final ResourceResolverType type) {
        try {
            Class clazz = Class.forName(type.getResourceResolverTypeAdapterClass());
            return (ResourceResolverTypeAdapter) clazz.newInstance();
        }
        catch (ClassNotFoundException ex) {
            throw new RuntimeException("Unable to instantiate resourcer resolver: "
                    + type.getResourceResolverTypeAdapterClass()
                    + (type.getArtifactCoordinates() != null ? ". Make sure this maven dependency is included: "
                            + type.getArtifactCoordinates() : ""), ex);
        }
        catch (InstantiationException ex) {
            throw new RuntimeException("Unable to instantiate resourcer resolver: "
                    + type.getResourceResolverTypeAdapterClass()
                    + (type.getArtifactCoordinates() != null ? ". Make sure this maven dependency is included: "
                            + type.getArtifactCoordinates() : ""), ex);
        }
        catch (IllegalAccessException ex) {
            throw new RuntimeException("Unable to instantiate resourcer resolver: "
                    + type.getResourceResolverTypeAdapterClass()
                    + (type.getArtifactCoordinates() != null ? ". Make sure this maven dependency is included: "
                            + type.getArtifactCoordinates() : ""), ex);
        }
    }

    /**
     * Creates new sling resource resolver factory instance using
     * {@link #DEFAULT_RESOURCERESOLVER_TYPE}.
     * @return Resource resolver factory instance
     * @deprecated Please use {@link #newResourceResolverFactory(BundleContext)}
     *   and shutdown the bundle context after usage.
     */
    @Deprecated
    public static ResourceResolverFactory newResourceResolverFactory() {
        return newResourceResolverFactory(DEFAULT_RESOURCERESOLVER_TYPE);
    }

    /**
     * Creates new sling resource resolver instance.
     * @param type Type of underlying repository.
     * @return Resource resolver instance
     * @deprecated Please use {@link #newResourceResolver(ResourceResolverType, BundleContext)}
     *   and shutdown the bundle context after usage.
     */
    @Deprecated
    public static ResourceResolver newResourceResolver(final ResourceResolverType type) {
        ResourceResolverFactory factory = newResourceResolverFactory(type);
        try {
            return factory.getAdministrativeResourceResolver(null);
        } catch (LoginException ex) {
            throw new RuntimeException("Mock resource resolver factory implementation seems to require login.", ex);
        }
    }

    /**
     * Creates new sling resource resolver instance using
     * {@link #DEFAULT_RESOURCERESOLVER_TYPE}.
     * @return Resource resolver instance
     * @deprecated Please use {@link #newResourceResolver(BundleContext)}
     *   and shutdown the bundle context after usage.
     */
    @Deprecated
    public static ResourceResolver newResourceResolver() {
        return newResourceResolver(DEFAULT_RESOURCERESOLVER_TYPE);
    }

    /**
     * Creates new sling resource resolver instance.
     * @param type Type of underlying repository.
     * @param bundleContext Bundle context
     * @return Resource resolver instance
     */
    public static ResourceResolver newResourceResolver(final ResourceResolverType type, BundleContext bundleContext) {
        ResourceResolverFactory factory = newResourceResolverFactory(type, bundleContext);
        try {
            return factory.getAdministrativeResourceResolver(null);
        } catch (LoginException ex) {
            throw new RuntimeException("Mock resource resolver factory implementation seems to require login.", ex);
        }
    }

    /**
     * Creates new sling resource resolver instance using
     * {@link #DEFAULT_RESOURCERESOLVER_TYPE}.
     * @param bundleContext Bundle context
     * @return Resource resolver instance
     */
    public static ResourceResolver newResourceResolver(BundleContext bundleContext) {
        return newResourceResolver(DEFAULT_RESOURCERESOLVER_TYPE, bundleContext);
    }

    /**
     * Creates a new sling script helper instance.
     * @param request Request
     * @param response Response
     * @param bundleContext Bundle context
     * @return Sling script helper instance
     */
    public static SlingScriptHelper newSlingScriptHelper(final SlingHttpServletRequest request,
            final SlingHttpServletResponse response, final BundleContext bundleContext) {
        return new MockSlingScriptHelper(request, response, bundleContext);
    }

    /**
     * Creates a new sling script helper instance using
     * {@link #DEFAULT_RESOURCERESOLVER_TYPE} for the resource resolver.
     * @return Sling script helper instance
     * @deprecated Please use {@link #newSlingScriptHelper(BundleContext)}
     *   and shutdown the bundle context after usage.
     */
    @Deprecated
    public static SlingScriptHelper newSlingScriptHelper() {
        BundleContext bundleContext = MockOsgi.newBundleContext();
        return newSlingScriptHelper(bundleContext);
    }

    /**
     * Creates a new sling script helper instance using
     * {@link #DEFAULT_RESOURCERESOLVER_TYPE} for the resource resolver.
     * @param bundleContext Bundle context
     * @return Sling script helper instance
     */
    public static SlingScriptHelper newSlingScriptHelper(BundleContext bundleContext) {
        SlingHttpServletRequest request = new MockSlingHttpServletRequest(newResourceResolver(bundleContext));
        SlingHttpServletResponse response = new MockSlingHttpServletResponse();
        return newSlingScriptHelper(request, response, bundleContext);
    }

    /**
     * Set bundle context for adapter manager. From this bundle context the
     * adapter factories are detected.
     * @param bundleContext OSGi bundle context
     */
    public static void setAdapterManagerBundleContext(final BundleContext bundleContext) {
        ADAPTER_MANAGER.setBundleContext(bundleContext);
    }

    /**
     * Clear adapter registrations..
     */
    public static void clearAdapterManagerBundleContext() {
        ADAPTER_MANAGER.clearBundleContext();
    }

}
