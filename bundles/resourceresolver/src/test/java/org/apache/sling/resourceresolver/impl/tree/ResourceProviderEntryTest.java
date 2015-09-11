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
package org.apache.sling.resourceresolver.impl.tree;

import static org.apache.sling.resourceresolver.impl.MockedResourceResolverImplTest.createRPHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Constants;

public class ResourceProviderEntryTest {

    private static final Map<String, String> EMPTY_PARAMS = Collections.emptyMap();

    private ResourceResolver rootResolver;

    private ResourceProviderEntry root;

    private List<ResourceProviderHandler> providers = new ArrayList<ResourceProviderHandler>();

    @Before public void setUp() throws Exception {
        this.providers.clear();
        this.rootResolver = Mockito.mock(ResourceResolver.class);

        final ResourceProvider rootProvider = Mockito.mock(ResourceProvider.class);
        Mockito.when(rootProvider.getResource(Mockito.any(ResolveContext.class), Mockito.anyString(), Mockito.any(Resource.class))).thenReturn(new TestResource(this.rootResolver));
        providers.add(createRPHandler(rootProvider, "rp0", 0, "/"));

        this.root = new RootResourceProviderEntry();
    }

    @Test public void testRootProvider() {
        assertNull(root.getResource(null, null, "relpath", EMPTY_PARAMS, false));
        final ResourceResolverContext ctx = getResourceResolverContext();
        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/", EMPTY_PARAMS, false));
        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/rootel", EMPTY_PARAMS, false));
        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/rootel/child", EMPTY_PARAMS, false));
        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/apps/sling/sample/html.js", EMPTY_PARAMS, false));
        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null,
            "/apps/sling/microsling/html.js", EMPTY_PARAMS, false));
    }

    @Test public void testAdd1Provider() {
        String firstPath = "/rootel";
        final ResourceResolverContext ctx = getResourceResolverContext();
        final ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider first = Mockito.mock(ResourceProvider.class);
        Mockito.when(first.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(firstPath), Mockito.any(Resource.class))).thenReturn(new TestResource(resolver));

        providers.add(createRPHandler(first, "rp1", 1, "/rootel"));
        Collections.sort(providers);

        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/", EMPTY_PARAMS, false));
        assertEqualsResolver(resolver, root.getResource(ctx, null, "/rootel", EMPTY_PARAMS, false));
        assertEqualsResolver(resolver, root.getResource(ctx, null, "/rootel/html.js", EMPTY_PARAMS, false));
        assertEqualsResolver(resolver, root.getResource(ctx, null, "/rootel/child", EMPTY_PARAMS, false));
        assertEqualsResolver(resolver, root.getResource(ctx, null, "/rootel/child/html.js", EMPTY_PARAMS, false));
        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null,
            "/apps/sling/sample/html.js", EMPTY_PARAMS, false));
        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null,
            "/apps/sling/microsling/html.js", EMPTY_PARAMS, false));
    }

    @Test public void testAdd3Providers() {
        String firstPath = "/rootel";
        String secondPath = firstPath + "/child";
        String thirdPath = "/apps/sling/sample";

        final ResourceResolverContext ctx = getResourceResolverContext();
        final ResourceResolver firstResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider first = Mockito.mock(ResourceProvider.class);
        Mockito.when(first.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(firstPath), Mockito.any(Resource.class))).thenReturn(new TestResource(firstResolver));
        final ResourceResolver secondResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider second = Mockito.mock(ResourceProvider.class);
        Mockito.when(second.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(secondPath), Mockito.any(Resource.class))).thenReturn(new TestResource(secondResolver));
        final ResourceResolver thirdResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider third = Mockito.mock(ResourceProvider.class);
        Mockito.when(third.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(thirdPath), Mockito.any(Resource.class))).thenReturn(new TestResource(thirdResolver));

        providers.add(createRPHandler(first, "rp1", 1, firstPath));
        providers.add(createRPHandler(second, "rp2", 2, secondPath));
        providers.add(createRPHandler(third, "rp3", 3, thirdPath));
        Collections.sort(providers);

        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/", EMPTY_PARAMS, false));
        assertEqualsResolver(firstResolver, root.getResource(ctx, null, "/rootel", EMPTY_PARAMS, false));
        assertEqualsResolver(firstResolver, root.getResource(ctx, null, "/rootel/html.js", EMPTY_PARAMS, false));
        assertEqualsResolver(secondResolver, root.getResource(ctx, null, "/rootel/child", EMPTY_PARAMS, false));
        assertEqualsResolver(secondResolver, root.getResource(ctx, null, "/rootel/child/html.js", EMPTY_PARAMS, false));
        assertEqualsResolver(thirdResolver,
            root.getResource(ctx, null, "/apps/sling/sample/html.js", EMPTY_PARAMS, false));
        final Resource resource = root.getResource(ctx, null,
            "/apps/sling/microsling/html.js", EMPTY_PARAMS, false);
        assertEqualsResolver(this.rootResolver, resource);
    }

    @Test public void testAdd3ProvidersReverse() {
        String firstPath = "/rootel";
        String secondPath = firstPath + "/child";
        String thirdPath = "/apps/sling/sample";

        final ResourceResolverContext ctx = getResourceResolverContext();
        final ResourceResolver firstResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider first = Mockito.mock(ResourceProvider.class);
        Mockito.when(first.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(firstPath), Mockito.any(Resource.class))).thenReturn(new TestResource(firstResolver));
        final ResourceResolver secondResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider second = Mockito.mock(ResourceProvider.class);
        Mockito.when(second.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(secondPath), Mockito.any(Resource.class))).thenReturn(new TestResource(secondResolver));
        final ResourceResolver thirdResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider third = Mockito.mock(ResourceProvider.class);
        Mockito.when(third.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(thirdPath), Mockito.any(Resource.class))).thenReturn(new TestResource(thirdResolver));

        providers.add(createRPHandler(first, "rp1", 1, firstPath));
        providers.add(createRPHandler(second, "rp2", 2, secondPath));
        providers.add(createRPHandler(third, "rp3", 3, thirdPath));
        Collections.sort(providers);

        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/", EMPTY_PARAMS, false));
        assertEqualsResolver(firstResolver, root.getResource(ctx, null, "/rootel", EMPTY_PARAMS, false));
        assertEqualsResolver(firstResolver, root.getResource(ctx, null, "/rootel/html.js", EMPTY_PARAMS, false));
        assertEqualsResolver(secondResolver, root.getResource(ctx, null, "/rootel/child", EMPTY_PARAMS, false));
        assertEqualsResolver(secondResolver, root.getResource(ctx, null, "/rootel/child/html.js", EMPTY_PARAMS, false));
        assertEqualsResolver(thirdResolver,
           root.getResource(ctx, null, "/apps/sling/sample/html.js", EMPTY_PARAMS, false));
        Resource resource = root.getResource(ctx, null,
              "/apps/sling/microsling/html.js", EMPTY_PARAMS, false);
        assertEqualsResolver(this.rootResolver, resource);
    }

    @Test public void testRemoveProviders() {
        String firstPath = "/rootel";
        String thirdPath = "/apps/sling/sample";
        String secondPath = firstPath + "/child";

        final ResourceResolverContext ctx = getResourceResolverContext();
        final ResourceResolver firstResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider first = Mockito.mock(ResourceProvider.class);
        Mockito.when(first.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(firstPath), Mockito.any(Resource.class))).thenReturn(new TestResource(firstResolver));
        final ResourceResolver secondResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider second = Mockito.mock(ResourceProvider.class);
        Mockito.when(second.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(secondPath), Mockito.any(Resource.class))).thenReturn(new TestResource(secondResolver));
        final ResourceResolver thirdResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider third = Mockito.mock(ResourceProvider.class);
        Mockito.when(third.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(thirdPath), Mockito.any(Resource.class))).thenReturn(new TestResource(thirdResolver));

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
        Collections.sort(providers);

        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/", EMPTY_PARAMS, false));
        assertEqualsResolver(firstResolver, root.getResource(ctx, null, "/rootel/html.js", EMPTY_PARAMS, false));
        assertEqualsResolver(secondResolver, root.getResource(ctx, null, "/rootel/child/html.js", EMPTY_PARAMS, false));

        providers.remove(firstH);

        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/", EMPTY_PARAMS, false));
        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/rootel/sddsf/sdfsdf/html.js", EMPTY_PARAMS, false));
        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/rootel/html.js", EMPTY_PARAMS, false));
        assertEqualsResolver(secondResolver, root.getResource(ctx, null, "/rootel/child/html.js", EMPTY_PARAMS, false));

        providers.add(firstH);
        Collections.sort(providers);

        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/", EMPTY_PARAMS, false));
        assertEqualsResolver(firstResolver, root.getResource(ctx, null, "/rootel/html.js", EMPTY_PARAMS, false));
        assertEqualsResolver(secondResolver, root.getResource(ctx, null, "/rootel/child/html.js", EMPTY_PARAMS, false));
    }

    @Test public void testRemoveTheOnlyProvider() {
        final ResourceProviderEntry e = new RootResourceProviderEntry();
        long counter = 1;

        for(String path : new String[] { "/foo", "/", "/foo/bar" }) {
            final ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
            final ResourceProvider p = Mockito.mock(ResourceProvider.class);
            Mockito.when(p.getResource(Mockito.any(ResolveContext.class), Mockito.startsWith(path), Mockito.any(Resource.class))).thenReturn(new TestResource(resolver));
            final ResourceResolverContext ctx = getResourceResolverContext();

            final Map<String, Object> props = new HashMap<String, Object>();
            ++counter;

            ResourceProviderHandler h = createRPHandler(p, "rp"+counter, counter, path);
            providers.add(h);
            {
                final Resource r = e.getResource(ctx, null, path, EMPTY_PARAMS, false);
                assertEqualsResolver(resolver, r);
                assertFalse(r instanceof SyntheticResource);
            }

            providers.remove(h);
            {
                final Resource r = e.getResource(ctx, null, path, EMPTY_PARAMS, false);
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

    private ResourceResolverContext getResourceResolverContext() {
        final ResourceResolverContext ctx = Mockito.mock(ResourceResolverContext.class);
        Mockito.when(ctx.getResourceAccessSecurityTracker()).thenReturn(new ResourceAccessSecurityTracker());
        Mockito.when(ctx.getProviders()).thenReturn(providers);
        return ctx;
    }

    private static class TestResource extends AbstractResource {

        private final ResourceResolver resourceResolver;

        public TestResource(ResourceResolver resourceResolver) {
            this.resourceResolver = resourceResolver;
        }

        public String getPath() {
            return null;
        }

        public ResourceMetadata getResourceMetadata() {
            return null;
        }

        public ResourceResolver getResourceResolver() {
            return resourceResolver;
        }

        public String getResourceType() {
            return null;
        }

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

    private static class MyProviderProvider extends ResourceProvider {

        @Override
        public Resource getResource(ResolveContext ctx, String path, Resource parent) {
            return null;
        }

        @Override
        public Iterator listChildren(ResolveContext ctx, Resource parent) {
            return null;
        }

    }
}
