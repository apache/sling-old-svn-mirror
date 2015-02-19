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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProviderFactory;
import org.apache.sling.jcr.resource.internal.helper.jcr.PathMapper;
import org.apache.sling.resourceresolver.impl.CommonResourceResolverFactoryImpl;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.ResourceResolverImpl;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.google.common.collect.ImmutableMap;

/**
 * Mock {@link ResourceResolver} implementation. Simulates OSGi environment and
 * initiates real Sling ResourceResolver and JCR implementation, but with a
 * mocked JCR repository implementation underneath.
 */
public class MockJcrResourceResolverFactory implements ResourceResolverFactory {

    private final SlingRepository slingRepository;

    public MockJcrResourceResolverFactory(final SlingRepository repository) {
        this.slingRepository = repository;
    }

    private ResourceResolver getResourceResolverInternal(Map<String, Object> authenticationInfo, boolean isAdmin) throws LoginException {
        // setup mock OSGi environment
        BundleContext bundleContext = MockOsgi.newBundleContext();

        Dictionary<String, Object> resourceProviderFactoryFactoryProps = new Hashtable<String, Object>();
        resourceProviderFactoryFactoryProps.put(Constants.SERVICE_VENDOR, "sling-mock");
        resourceProviderFactoryFactoryProps.put(Constants.SERVICE_DESCRIPTION, "sling-mock");
        resourceProviderFactoryFactoryProps.put("resource.resolver.manglenamespaces", true);
        resourceProviderFactoryFactoryProps.put("resource.resolver.searchpath", new String[] { "/apps", "/libs" });
        ComponentContext resourceProviderComponentContext = MockOsgi.newComponentContext(bundleContext, resourceProviderFactoryFactoryProps);

        // setup mocked JCR environment
        bundleContext.registerService(SlingRepository.class.getName(), this.slingRepository, null);
        bundleContext.registerService(PathMapper.class.getName(), new PathMapper(), null);

        // setup real sling JCR resource provider implementation for use in
        // mocked context
        JcrResourceProviderFactory jcrResourceProviderFactory = new JcrResourceProviderFactory();
        MockOsgi.injectServices(jcrResourceProviderFactory, bundleContext);
        MockOsgi.activate(jcrResourceProviderFactory, bundleContext, ImmutableMap.<String, Object> of());

        ResourceProvider resourceProvider;
        if (isAdmin) {
            resourceProvider = jcrResourceProviderFactory.getAdministrativeResourceProvider(authenticationInfo);
        }
        else {
            resourceProvider = jcrResourceProviderFactory.getResourceProvider(authenticationInfo);
        }

        Dictionary<Object, Object> resourceProviderProps = new Hashtable<Object, Object>();
        resourceProviderProps.put(ResourceProvider.ROOTS, new String[] { "/" });
        bundleContext.registerService(ResourceProvider.class.getName(), resourceProvider, resourceProviderProps);
        ServiceReference resourceProviderServiceReference = bundleContext.getServiceReference(ResourceProvider.class.getName());

        // setup real sling resource resolver implementation for use in mocked
        // context
        MockResourceResolverFactoryActivator activator = new MockResourceResolverFactoryActivator();
        activator.bindResourceProvider(resourceProvider,
                getServiceReferenceProperties(resourceProviderServiceReference));
        activator.activate(resourceProviderComponentContext);
        CommonResourceResolverFactoryImpl commonFactoryImpl = new CommonResourceResolverFactoryImpl(activator);
        ResourceResolverContext context = new ResourceResolverContext(true, authenticationInfo, new ResourceAccessSecurityTracker());
        ResourceResolverImpl resourceResolver = new ResourceResolverImpl(commonFactoryImpl, context);
        return resourceResolver;
    }

    private Map<String, Object> getServiceReferenceProperties(final ServiceReference serviceReference) {
        Map<String, Object> props = new HashMap<String, Object>();
        String[] keys = serviceReference.getPropertyKeys();
        for (String key : keys) {
            props.put(key, serviceReference.getProperty(key));
        }
        return props;
    }

    @Override
    public ResourceResolver getResourceResolver(final Map<String, Object> authenticationInfo) throws LoginException {
        return getResourceResolverInternal(authenticationInfo, false);
    }

    @Override
    public ResourceResolver getAdministrativeResourceResolver(final Map<String, Object> authenticationInfo)
            throws LoginException {
        return getResourceResolverInternal(authenticationInfo, true);
    }

    // part of Sling API 2.7
    public ResourceResolver getServiceResourceResolver(final Map<String, Object> authenticationInfo)
            throws LoginException {
        return getResourceResolverInternal(authenticationInfo, true);
    }

    // part of Sling API 2.8
    public ResourceResolver getThreadResourceResolver() {
        throw new UnsupportedOperationException();
    }

}
