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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Constants;

public class ResourceProviderEntryTest {

    private ResourceResolver rootResolver;

    private ResourceProviderEntry root;

    @Before public void setUp() throws Exception {
        this.rootResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider rootProvider = Mockito.mock(ResourceProvider.class);
        Mockito.when(rootProvider.getResource(Mockito.any(ResourceResolver.class), Mockito.anyString())).thenReturn(new TestResource(this.rootResolver));
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.SERVICE_ID, (long)0);
        this.root = new ResourceProviderEntry("/", new ResourceProviderHandler[]{ new ResourceProviderHandler(rootProvider, props)});
    }

    @Test public void testRootProvider() {
        assertNull(root.getResource(null, null, "relpath"));
        final ResourceResolverContext ctx = getResourceResolverContext();
        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/"));
        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/rootel"));
        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/rootel/child"));
        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/apps/sling/sample/html.js"));
        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null,
            "/apps/sling/microsling/html.js"));
    }

    @Test public void testAdd1Provider() {
        String firstPath = "/rootel";
        final ResourceResolverContext ctx = getResourceResolverContext();
        final ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider first = Mockito.mock(ResourceProvider.class);
        Mockito.when(first.getResource(Mockito.any(ResourceResolver.class), Mockito.startsWith(firstPath))).thenReturn(new TestResource(resolver));

        final Map<String, Object> firstProps = new HashMap<String, Object>();
        firstProps.put(Constants.SERVICE_ID, (long)1);
        root.addResourceProvider(firstPath, new ResourceProviderHandler(first, firstProps));


        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/"));
        assertEqualsResolver(resolver, root.getResource(ctx, null, "/rootel"));
        assertEqualsResolver(resolver, root.getResource(ctx, null, "/rootel/html.js"));
        assertEqualsResolver(resolver, root.getResource(ctx, null, "/rootel/child"));
        assertEqualsResolver(resolver, root.getResource(ctx, null, "/rootel/child/html.js"));
        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null,
            "/apps/sling/sample/html.js"));
        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null,
            "/apps/sling/microsling/html.js"));
    }

    @Test public void testAdd3Providers() {
        String firstPath = "/rootel";
        String thirdPath = "/apps/sling/sample";
        String secondPath = firstPath + "/child";

        final ResourceResolverContext ctx = getResourceResolverContext();
        final ResourceResolver firstResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider first = Mockito.mock(ResourceProvider.class);
        Mockito.when(first.getResource(Mockito.any(ResourceResolver.class), Mockito.startsWith(firstPath))).thenReturn(new TestResource(firstResolver));
        final ResourceResolver secondResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider second = Mockito.mock(ResourceProvider.class);
        Mockito.when(second.getResource(Mockito.any(ResourceResolver.class), Mockito.startsWith(secondPath))).thenReturn(new TestResource(secondResolver));
        final ResourceResolver thirdResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider third = Mockito.mock(ResourceProvider.class);
        Mockito.when(third.getResource(Mockito.any(ResourceResolver.class), Mockito.startsWith(thirdPath))).thenReturn(new TestResource(thirdResolver));

        final Map<String, Object> firstProps = new HashMap<String, Object>();
        firstProps.put(Constants.SERVICE_ID, (long)1);
        final Map<String, Object> secondProps = new HashMap<String, Object>();
        secondProps.put(Constants.SERVICE_ID, (long)2);
        final Map<String, Object> thirdProps = new HashMap<String, Object>();
        thirdProps.put(Constants.SERVICE_ID, (long)3);

        root.addResourceProvider(firstPath, new ResourceProviderHandler(first, firstProps));
        root.addResourceProvider(secondPath, new ResourceProviderHandler(second, secondProps));
        root.addResourceProvider(thirdPath, new ResourceProviderHandler(third, thirdProps));

        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/"));
        assertEqualsResolver(firstResolver, root.getResource(ctx, null, "/rootel"));
        assertEqualsResolver(firstResolver, root.getResource(ctx, null, "/rootel/html.js"));
        assertEqualsResolver(secondResolver, root.getResource(ctx, null, "/rootel/child"));
        assertEqualsResolver(secondResolver, root.getResource(ctx, null, "/rootel/child/html.js"));
        assertEqualsResolver(thirdResolver,
            root.getResource(ctx, null, "/apps/sling/sample/html.js"));
        final Resource resource = root.getResource(ctx, null,
            "/apps/sling/microsling/html.js");
        assertEqualsResolver(this.rootResolver, resource);
    }

    @Test public void testAdd3ProvidersReverse() {
        String firstPath = "/rootel";
        String thirdPath = "/apps/sling/sample";
        String secondPath = firstPath + "/child";

        final ResourceResolverContext ctx = getResourceResolverContext();
        final ResourceResolver firstResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider first = Mockito.mock(ResourceProvider.class);
        Mockito.when(first.getResource(Mockito.any(ResourceResolver.class), Mockito.startsWith(firstPath))).thenReturn(new TestResource(firstResolver));
        final ResourceResolver secondResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider second = Mockito.mock(ResourceProvider.class);
        Mockito.when(second.getResource(Mockito.any(ResourceResolver.class), Mockito.startsWith(secondPath))).thenReturn(new TestResource(secondResolver));
        final ResourceResolver thirdResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider third = Mockito.mock(ResourceProvider.class);
        Mockito.when(third.getResource(Mockito.any(ResourceResolver.class), Mockito.startsWith(thirdPath))).thenReturn(new TestResource(thirdResolver));

        final Map<String, Object> firstProps = new HashMap<String, Object>();
        firstProps.put(Constants.SERVICE_ID, (long)1);
        final Map<String, Object> secondProps = new HashMap<String, Object>();
        secondProps.put(Constants.SERVICE_ID, (long)2);
        final Map<String, Object> thirdProps = new HashMap<String, Object>();
        thirdProps.put(Constants.SERVICE_ID, (long)3);

        root.addResourceProvider(firstPath, new ResourceProviderHandler(first, firstProps));
        root.addResourceProvider(secondPath, new ResourceProviderHandler(second, secondProps));
        root.addResourceProvider(thirdPath, new ResourceProviderHandler(third, thirdProps));

        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/"));
        assertEqualsResolver(firstResolver, root.getResource(ctx, null, "/rootel"));
        assertEqualsResolver(firstResolver, root.getResource(ctx, null, "/rootel/html.js"));
        assertEqualsResolver(secondResolver, root.getResource(ctx, null, "/rootel/child"));
        assertEqualsResolver(secondResolver, root.getResource(ctx, null, "/rootel/child/html.js"));
        assertEqualsResolver(thirdResolver,
           root.getResource(ctx, null, "/apps/sling/sample/html.js"));
        Resource resource = root.getResource(ctx, null,
              "/apps/sling/microsling/html.js");
        assertEqualsResolver(this.rootResolver, resource);
    }

    @Test public void testRemoveProviders() {
        String firstPath = "/rootel";
        String thirdPath = "/apps/sling/sample";
        String secondPath = firstPath + "/child";

        final ResourceResolverContext ctx = getResourceResolverContext();
        final ResourceResolver firstResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider first = Mockito.mock(ResourceProvider.class);
        Mockito.when(first.getResource(Mockito.any(ResourceResolver.class), Mockito.startsWith(firstPath))).thenReturn(new TestResource(firstResolver));
        final ResourceResolver secondResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider second = Mockito.mock(ResourceProvider.class);
        Mockito.when(second.getResource(Mockito.any(ResourceResolver.class), Mockito.startsWith(secondPath))).thenReturn(new TestResource(secondResolver));
        final ResourceResolver thirdResolver = Mockito.mock(ResourceResolver.class);
        final ResourceProvider third = Mockito.mock(ResourceProvider.class);
        Mockito.when(third.getResource(Mockito.any(ResourceResolver.class), Mockito.startsWith(thirdPath))).thenReturn(new TestResource(thirdResolver));

        final Map<String, Object> firstProps = new HashMap<String, Object>();
        firstProps.put(Constants.SERVICE_ID, (long)1);
        final Map<String, Object> secondProps = new HashMap<String, Object>();
        secondProps.put(Constants.SERVICE_ID, (long)2);
        final Map<String, Object> thirdProps = new HashMap<String, Object>();
        thirdProps.put(Constants.SERVICE_ID, (long)3);

        root.addResourceProvider(firstPath, new ResourceProviderHandler(first, firstProps));
        root.addResourceProvider(secondPath, new ResourceProviderHandler(second, secondProps));
        root.addResourceProvider(thirdPath, new ResourceProviderHandler(third, thirdProps));

        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/"));
        assertEqualsResolver(firstResolver, root.getResource(ctx, null, "/rootel/html.js"));
        assertEqualsResolver(secondResolver, root.getResource(ctx, null, "/rootel/child/html.js"));

        root.removeResourceProvider(firstPath, new ResourceProviderHandler(first, firstProps));

        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/"));
        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/rootel/sddsf/sdfsdf/html.js"));
        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/rootel/html.js"));
        assertEqualsResolver(secondResolver, root.getResource(ctx, null, "/rootel/child/html.js"));

        root.addResourceProvider(firstPath, new ResourceProviderHandler(first, firstProps));

        assertEqualsResolver(this.rootResolver, root.getResource(ctx, null, "/"));
        assertEqualsResolver(firstResolver, root.getResource(ctx, null, "/rootel/html.js"));
        assertEqualsResolver(secondResolver, root.getResource(ctx, null, "/rootel/child/html.js"));
    }

    @Test public void testRemoveTheOnlyProvider() {
        final ResourceProviderEntry e = new ResourceProviderEntry("/", null);
        long counter = 1;

        for(String path : new String[] { "/foo", "/", "/foo/bar" }) {
            final ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
            final ResourceProvider p = Mockito.mock(ResourceProvider.class);
            Mockito.when(p.getResource(Mockito.any(ResourceResolver.class), Mockito.startsWith(path))).thenReturn(new TestResource(resolver));
            final ResourceResolverContext ctx = getResourceResolverContext();

            final Map<String, Object> props = new HashMap<String, Object>();
            props.put(Constants.SERVICE_ID, ++counter);

            e.addResourceProvider(path, new ResourceProviderHandler(p, props));
            {
                final Resource r = e.getResource(ctx, null, path);
                assertEqualsResolver(resolver, r);
                assertFalse(r instanceof SyntheticResource);
            }

            e.removeResourceProvider(path, new ResourceProviderHandler(p, props));
            {
                final Resource r = e.getResource(ctx, null, path);
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
        Mockito.when(ctx.applyFeatures(Mockito.any(Resource.class))).then(new Answer<Resource>() {
            public Resource answer(InvocationOnMock invocation) {
                return (Resource) invocation.getArguments()[0];
            }
        });
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
}
