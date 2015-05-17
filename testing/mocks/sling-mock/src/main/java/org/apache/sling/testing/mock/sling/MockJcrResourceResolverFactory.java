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
import java.util.Map;

import javax.jcr.query.Query;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.QueriableResourceProvider;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProviderFactory;
import org.apache.sling.resourceresolver.impl.CommonResourceResolverFactoryImpl;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.ResourceResolverFactoryActivator;
import org.apache.sling.resourceresolver.impl.ResourceResolverImpl;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl;
import org.apache.sling.testing.mock.osgi.MockEventAdmin;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.event.EventAdmin;

import com.google.common.collect.ImmutableMap;

/**
 * Mock {@link ResourceResolver} implementation. Simulates OSGi environment and
 * initiates real Sling ResourceResolver and JCR implementation, but with a
 * mocked JCR repository implementation underneath.
 */
class MockJcrResourceResolverFactory implements ResourceResolverFactory {

    private final SlingRepository slingRepository;
    private final BundleContext bundleContext;

    public MockJcrResourceResolverFactory(final SlingRepository repository, BundleContext bundleContext) {
        this.slingRepository = repository;
        this.bundleContext = bundleContext;
    }

    @SuppressWarnings("deprecation")
    private ResourceResolver getResourceResolverInternal(Map<String, Object> authenticationInfo, boolean isAdmin) throws LoginException {
        Dictionary<String, Object> resourceProviderFactoryFactoryProps = new Hashtable<String, Object>();
        resourceProviderFactoryFactoryProps.put(Constants.SERVICE_VENDOR, "sling-mock");
        resourceProviderFactoryFactoryProps.put(Constants.SERVICE_DESCRIPTION, "sling-mock");
        resourceProviderFactoryFactoryProps.put("resource.resolver.manglenamespaces", true);
        resourceProviderFactoryFactoryProps.put("resource.resolver.searchpath", new String[] { "/apps", "/libs" });

        // setup mocked JCR environment
        if (bundleContext.getServiceReference(SlingRepository.class.getName()) == null) {
            bundleContext.registerService(SlingRepository.class.getName(), this.slingRepository, null);
        }

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
        resourceProviderProps.put(QueriableResourceProvider.LANGUAGES, new String[] { Query.XPATH, Query.SQL, Query.JCR_SQL2 });
        bundleContext.registerService(ResourceProvider.class.getName(), resourceProvider, resourceProviderProps);

        // setup real sling resource resolver implementation for use in mocked context
        ensureResourceResolverFactoryActivatorDependencies();
        ResourceResolverFactoryActivator activator = new ResourceResolverFactoryActivator();
        MockOsgi.injectServices(activator, bundleContext);
        MockOsgi.activate(activator, resourceProviderFactoryFactoryProps);
        
        CommonResourceResolverFactoryImpl commonFactoryImpl = new CommonResourceResolverFactoryImpl(activator);
        ResourceResolverContext context = new ResourceResolverContext(true, authenticationInfo, new ResourceAccessSecurityTracker());
        ResourceResolverImpl resourceResolver = new ResourceResolverImpl(commonFactoryImpl, context);
        return resourceResolver;
    }
    
    /**
     * Make sure all dependencies required by {@link ResourceResolverFactoryActivator} exist - if not register them.
     */
    private void ensureResourceResolverFactoryActivatorDependencies() {
        if (bundleContext.getServiceReference(ServiceUserMapper.class.getName()) == null) {
            ServiceUserMapper serviceUserMapper = new ServiceUserMapperImpl();
            MockOsgi.injectServices(serviceUserMapper, bundleContext);
            MockOsgi.activate(serviceUserMapper);
            bundleContext.registerService(ServiceUserMapper.class.getName(), serviceUserMapper, null);
        }

        if (bundleContext.getServiceReference(ResourceAccessSecurityTracker.class.getName()) == null) {
            ResourceAccessSecurityTracker resourceAccessSecurityTracker = new ResourceAccessSecurityTracker();
            MockOsgi.injectServices(resourceAccessSecurityTracker, bundleContext);
            MockOsgi.activate(resourceAccessSecurityTracker);
            bundleContext.registerService(ResourceAccessSecurityTracker.class.getName(), resourceAccessSecurityTracker, null);
        }

        if (bundleContext.getServiceReference(EventAdmin.class.getName()) == null) {
            EventAdmin eventAdmin = new MockEventAdmin();
            MockOsgi.injectServices(eventAdmin, bundleContext);
            MockOsgi.activate(eventAdmin);
            bundleContext.registerService(EventAdmin.class.getName(), eventAdmin, null);
        }
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
