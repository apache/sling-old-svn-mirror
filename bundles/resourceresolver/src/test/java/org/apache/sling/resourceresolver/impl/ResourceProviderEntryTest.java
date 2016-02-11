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
package org.apache.sling.resourceresolver.impl;

import static org.apache.sling.resourceresolver.impl.MockedResourceResolverImplTest.createRPHandler;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderStorage;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderStorageProvider;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Constants;

@SuppressWarnings("unchecked")
public class ResourceProviderEntryTest {

    private List<ResourceProviderHandler> providers = new ArrayList<ResourceProviderHandler>();

    private ResourceResolver providersBasedResolver;

    private ResourceResolver mockedRootResolver;

    @Before public void setUp() throws Exception {
        this.mockedRootResolver = Mockito.mock(ResourceResolver.class);
        this.providersBasedResolver = null;
        this.providers.clear();
        final ResourceProvider<?> rootProvider = Mockito.mock(ResourceProvider.class);
        Mockito.when(rootProvider.getResource(Mockito.any(ResolveContext.class), Mockito.anyString(), Mockito.any(ResourceContext.class), Mockito.any(Resource.class))).thenReturn(new TestResource(this.mockedRootResolver));
        providers.add(createRPHandler(rootProvider, "rp0", 0, "/"));
    }

    @Test public void testRootProvider() throws LoginException {
        assertNull(getResource("relpath"));
        assertEqualsResolver(this.mockedRootResolver, getResource("/"));
        assertEqualsResolver(this.mockedRootResolver, getResource("/rootel"));
        assertEqualsResolver(this.mockedRootResolver, getResource("/rootel/child"));
        assertEqualsResolver(this.mockedRootResolver, getResource("/apps/sling/sample/html.js"));
        assertEqualsResolver(this.mockedRootResolver, getResource("/apps/sling/microsling/html.js"));
    }

    @Test public void testAdd1Provider() throws LoginException {
        String firstPath = "/rootel";
        final ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider<?> first = Mockito.mock(ResourceProvider.class);
        Mockito.when(first.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(firstPath), Mockito.any(ResourceContext.class), Mockito.any(Resource.class))).thenReturn(new TestResource(resolver));

        providers.add(createRPHandler(first, "rp1", 1, "/rootel"));
        this.providersBasedResolver = null;

        assertEqualsResolver(this.mockedRootResolver, getResource("/"));
        assertEqualsResolver(resolver, getResource("/rootel"));
        assertEqualsResolver(resolver, getResource("/rootel/html.js"));
        assertEqualsResolver(resolver, getResource("/rootel/child"));
        assertEqualsResolver(resolver, getResource("/rootel/child/html.js"));
        assertEqualsResolver(this.mockedRootResolver, getResource("/apps/sling/sample/html.js"));
        assertEqualsResolver(this.mockedRootResolver, getResource("/apps/sling/microsling/html.js"));
    }

    @Test public void testAdd3Providers() throws LoginException {
        String firstPath = "/rootel";
        String secondPath = firstPath + "/child";
        String thirdPath = "/apps/sling/sample";

        final ResourceResolver firstResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider<?> first = Mockito.mock(ResourceProvider.class);
        Mockito.when(first.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(firstPath), Mockito.any(ResourceContext.class), Mockito.any(Resource.class))).thenReturn(new TestResource(firstResolver));
        final ResourceResolver secondResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider<?> second = Mockito.mock(ResourceProvider.class);
        Mockito.when(second.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(secondPath), Mockito.any(ResourceContext.class), Mockito.any(Resource.class))).thenReturn(new TestResource(secondResolver));
        final ResourceResolver thirdResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider<?> third = Mockito.mock(ResourceProvider.class);
        Mockito.when(third.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(thirdPath), Mockito.any(ResourceContext.class), Mockito.any(Resource.class))).thenReturn(new TestResource(thirdResolver));

        providers.add(createRPHandler(first, "rp1", 1, firstPath));
        providers.add(createRPHandler(second, "rp2", 2, secondPath));
        providers.add(createRPHandler(third, "rp3", 3, thirdPath));
        this.providersBasedResolver = null;

        assertEqualsResolver(this.mockedRootResolver, getResource("/"));
        assertEqualsResolver(firstResolver, getResource("/rootel"));
        assertEqualsResolver(firstResolver, getResource("/rootel/html.js"));
        assertEqualsResolver(secondResolver, getResource("/rootel/child"));
        assertEqualsResolver(secondResolver, getResource("/rootel/child/html.js"));
        assertEqualsResolver(thirdResolver, getResource("/apps/sling/sample/html.js"));
        assertEqualsResolver(this.mockedRootResolver, getResource("/apps/sling/microsling/html.js"));
    }

    @Test public void testAdd3ProvidersReverse() throws LoginException {
        String firstPath = "/rootel";
        String secondPath = firstPath + "/child";
        String thirdPath = "/apps/sling/sample";

        final ResourceResolver firstResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider<?> first = Mockito.mock(ResourceProvider.class);
        Mockito.when(first.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(firstPath), Mockito.any(ResourceContext.class), Mockito.any(Resource.class))).thenReturn(new TestResource(firstResolver));
        final ResourceResolver secondResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider<?> second = Mockito.mock(ResourceProvider.class);
        Mockito.when(second.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(secondPath), Mockito.any(ResourceContext.class), Mockito.any(Resource.class))).thenReturn(new TestResource(secondResolver));
        final ResourceResolver thirdResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider<?> third = Mockito.mock(ResourceProvider.class);
        Mockito.when(third.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(thirdPath), Mockito.any(ResourceContext.class), Mockito.any(Resource.class))).thenReturn(new TestResource(thirdResolver));

        providers.add(createRPHandler(first, "rp1", 1, firstPath));
        providers.add(createRPHandler(second, "rp2", 2, secondPath));
        providers.add(createRPHandler(third, "rp3", 3, thirdPath));
        this.providersBasedResolver = null;

        assertEqualsResolver(this.mockedRootResolver, getResource("/"));
        assertEqualsResolver(firstResolver, getResource("/rootel"));
        assertEqualsResolver(firstResolver, getResource("/rootel/html.js"));
        assertEqualsResolver(secondResolver, getResource("/rootel/child"));
        assertEqualsResolver(secondResolver, getResource("/rootel/child/html.js"));
        assertEqualsResolver(thirdResolver, getResource("/apps/sling/sample/html.js"));
        assertEqualsResolver(this.mockedRootResolver, getResource("/apps/sling/microsling/html.js"));
    }

    @Test public void testRemoveProviders() throws LoginException {
        String firstPath = "/rootel";
        String thirdPath = "/apps/sling/sample";
        String secondPath = firstPath + "/child";

        final ResourceResolver firstResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider<?> first = Mockito.mock(ResourceProvider.class);
        Mockito.when(first.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(firstPath), Mockito.any(ResourceContext.class), Mockito.any(Resource.class))).thenReturn(new TestResource(firstResolver));
        final ResourceResolver secondResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider<?> second = Mockito.mock(ResourceProvider.class);
        Mockito.when(second.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(secondPath), Mockito.any(ResourceContext.class), Mockito.any(Resource.class))).thenReturn(new TestResource(secondResolver));
        final ResourceResolver thirdResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider<?> third = Mockito.mock(ResourceProvider.class);
        Mockito.when(third.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(thirdPath), Mockito.any(ResourceContext.class), Mockito.any(Resource.class))).thenReturn(new TestResource(thirdResolver));

        final Map<String, Object> firstProps = new HashMap<String, Object>();
        firstProps.put(Constants.SERVICE_ID, (long)1);
        final Map<String, Object> secondProps = new HashMap<String, Object>();
        secondProps.put(Constants.SERVICE_ID, (long)2);
        final Map<String, Object> thirdProps = new HashMap<String, Object>();
        thirdProps.put(Constants.SERVICE_ID, (long)3);

        ResourceProviderHandler firstH = createRPHandler(first, "rp1", 1, firstPath);
        providers.add(firstH);
        providers.add(createRPHandler(second, "rp2", 2, secondPath));
        providers.add(createRPHandler(third, "rp3", 3, thirdPath));
        this.providersBasedResolver = null;

        assertEqualsResolver(this.mockedRootResolver, getResource("/"));
        assertEqualsResolver(firstResolver, getResource("/rootel/html.js"));
        assertEqualsResolver(secondResolver, getResource("/rootel/child/html.js"));

        providers.remove(firstH);
        this.providersBasedResolver = null;

        assertEqualsResolver(this.mockedRootResolver, getResource("/"));
        assertEqualsResolver(this.mockedRootResolver, getResource("/rootel/sddsf/sdfsdf/html.js"));
        assertEqualsResolver(this.mockedRootResolver, getResource("/rootel/html.js"));
        assertEqualsResolver(secondResolver, getResource("/rootel/child/html.js"));

        providers.add(firstH);
        this.providersBasedResolver = null;

        assertEqualsResolver(this.mockedRootResolver, getResource("/"));
        assertEqualsResolver(firstResolver, getResource("/rootel/html.js"));
        assertEqualsResolver(secondResolver, getResource("/rootel/child/html.js"));
    }

    @Test public void testRemoveTheOnlyProvider() throws LoginException {
        long counter = 1;

        providers.clear();
        for(String path : new String[] { "/foo", "/", "/foo/bar" }) {
            final ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
            final ResourceProvider<?> p = Mockito.mock(ResourceProvider.class);
            Mockito.when(p.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(path), Mockito.any(ResourceContext.class), Mockito.any(Resource.class))).thenReturn(new TestResource(resolver));

            ++counter;

            ResourceProviderHandler h = createRPHandler(p, "rp"+counter, counter, path);
            providers.add(h);
            this.providersBasedResolver = null;
            {
                final Resource r = getResource(path);
                assertEqualsResolver(resolver, r);
                assertFalse(r instanceof SyntheticResource);
            }

            providers.remove(h);
            this.providersBasedResolver = null;
            {
                final Resource r = getResource(path);
                // If our provider is indeed gone, we should get one of the following conditions
                if(r == null) {
                    //fine
                } else if(!p.equals(r.getResourceResolver())) {
                    //fine
                } else {
                    fail("Expecting inactive provider after removing it for " + path);
                }
            }
        }
    }

    private void assertEqualsResolver(final ResourceResolver resolver, final Resource res) {
        assertEquals(resolver, res.getResourceResolver());
    }

    private Resource getResource(String path) throws LoginException {
        return getResolver().getResource(path);
    }

    private ResourceResolver getResolver() throws LoginException {
        if (providersBasedResolver == null) {
            final ResourceResolverFactoryActivator activator = new ResourceResolverFactoryActivator();
            activator.resourceAccessSecurityTracker = new ResourceAccessSecurityTracker();
            providersBasedResolver = new ResourceResolverImpl(new CommonResourceResolverFactoryImpl(activator), false, null,
                    new ResourceProviderStorageProvider() {

                        @Override
                        public ResourceProviderStorage getResourceProviderStorage() {
                            return new ResourceProviderStorage(providers);
                        }
                    });
        }
        return providersBasedResolver;
    }

    private static class TestResource extends AbstractResource {

        private final ResourceResolver resourceResolver;

        public TestResource(ResourceResolver resourceResolver) {
            this.resourceResolver = resourceResolver;
        }

        @Override
        public String getPath() {
            return null;
        }

        @Override
        public ResourceMetadata getResourceMetadata() {
            return new ResourceMetadata();
        }

        @Override
        public ResourceResolver getResourceResolver() {
            return resourceResolver;
        }

        @Override
        public String getResourceType() {
            return null;
        }

        @Override
        public String getResourceSuperType() {
            return null;
        }

        @Override
        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            return null;
        }

		@Override
        public boolean hasChildren() {
			return false;
		}
    }
}
