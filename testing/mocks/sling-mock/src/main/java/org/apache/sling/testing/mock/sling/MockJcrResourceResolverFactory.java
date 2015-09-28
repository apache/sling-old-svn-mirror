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
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProviderFactory;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.osgi.framework.BundleContext;

/**
 * Mock {@link ResourceResolverFactory} implementation.
 * Uses real Sling ResourceResolverFactory in simulated OSGi environment
 * with a mocked JCR repository implementation underneath.
 */
class MockJcrResourceResolverFactory extends AbstractMockResourceResolverFactory {

    private final SlingRepository slingRepository;

    public MockJcrResourceResolverFactory(final SlingRepository repository, BundleContext bundleContext) {
        super(bundleContext);
        this.slingRepository = repository;
    }

    @SuppressWarnings("deprecation")
    protected ResourceResolver getResourceResolverInternal(Map<String, Object> authenticationInfo, boolean isAdmin) throws LoginException {
        // setup mocked JCR environment
        if (bundleContext.getServiceReference(SlingRepository.class.getName()) == null) {
            bundleContext.registerService(SlingRepository.class.getName(), this.slingRepository, null);
        }
        
        // setup PathMapper which is a mandatory service for JcrProviderFactory (since org.apache.sling.jcr.resource 2.5.4)
        // use reflection to not depend on it if running with older version of org.apache.sling.jcr.resource
        registerServiceIfFoundInClasspath("org.apache.sling.jcr.resource.internal.helper.jcr.PathMapper");

        // setup real sling JCR resource provider implementation for use in mocked context
        JcrResourceProviderFactory jcrResourceProviderFactory = new JcrResourceProviderFactory();
        Dictionary<String, Object> resourceProviderProps = new Hashtable<String, Object>();
        resourceProviderProps.put(ResourceProvider.ROOTS, new String[] { "/" });
        resourceProviderProps.put(QueriableResourceProvider.LANGUAGES, new String[] { Query.XPATH, Query.SQL, Query.JCR_SQL2 });
        MockOsgi.injectServices(jcrResourceProviderFactory, bundleContext);
        MockOsgi.activate(jcrResourceProviderFactory, bundleContext, resourceProviderProps);
        bundleContext.registerService(ResourceProviderFactory.class.getName(), jcrResourceProviderFactory, resourceProviderProps);

        return super.getResourceResolverInternal(authenticationInfo, isAdmin);
    }
    
    private void registerServiceIfFoundInClasspath(String className) {
        try {
            Class pathMapperClass = Class.forName(className);
            if (bundleContext.getServiceReference(className) == null) {
                Object instance = pathMapperClass.newInstance();
                MockOsgi.injectServices(instance, bundleContext);
                MockOsgi.activate(instance);
                bundleContext.registerService(className, instance, null);
            }
        }
        catch (ClassNotFoundException ex) {
            // skip service registration
        }
        catch (InstantiationException e) {
            // skip service registration
        }
        catch (IllegalAccessException e) {
            // skip service registration
        }
    }
    
}
