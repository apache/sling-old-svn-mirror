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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.resourceresolver.impl.helper.ResourceDecoratorTracker;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderStorage;
import org.apache.sling.spi.resource.provider.QueryLanguageProvider;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.junit.Before;
import org.mockito.Mockito;

/** Base class for tests that involve ResourceDecorators */
public abstract class ResourceDecoratorTestBase {

    protected ResourceResolver resolver;
    protected static final String QUERY_LANGUAGE = "some.funnny.language";

    protected abstract Resource wrapResourceForTest(Resource resource);

    @Before
    public void setup() throws LoginException {
        final ResourceDecorator d = new ResourceDecorator() {
            @Override
            public Resource decorate(Resource resource) {
                return ResourceDecoratorTestBase.this.wrapResourceForTest(resource);
            }

            @Override
            public Resource decorate(Resource resource, HttpServletRequest request) {
                throw new UnsupportedOperationException("Not supposed to be used in these tests");
            }

        };

        final ResourceDecoratorTracker t = new ResourceDecoratorTracker();
        t.bindResourceDecorator(d, null);

        final ResourceProvider<?> provider = new ResourceProvider<Object>() {

            @Override
            public QueryLanguageProvider<Object> getQueryLanguageProvider() {
                return new QueryLanguageProvider<Object>() {

                    @Override
                    public String[] getSupportedLanguages(ResolveContext<Object> ctx) {
                        return new String[] { QUERY_LANGUAGE };
                    }

                    @Override
                    public Iterator<Resource> findResources(ResolveContext<Object> ctx, String query, String language) {
                        final List<Resource> found = new ArrayList<Resource>();
                        found.add(mockResource("/tmp/C"));
                        found.add(mockResource("/tmp/D"));
                        found.add(mockResource("/var/one"));
                        found.add(mockResource("/var/two"));
                        return found.iterator();
                    }

                    @Override
                    public Iterator<ValueMap> queryResources(ResolveContext<Object> ctx, String query, String language) {
                        return null;
                    }
                };
            }

            @Override
            public Resource getResource(ResolveContext<Object> ctx, String path, final ResourceContext rCtx, Resource parent) {
                if(path.equals("/") || path.startsWith("/tmp") || path.startsWith("/var")) {
                    return mockResource(path);
                }
                return null;
            }

            @Override
            public Iterator<Resource> listChildren(ResolveContext<Object> ctx, Resource parent) {
                final List<Resource> children = new ArrayList<Resource>();
                if("/".equals(parent.getPath())) {
                    children.add(mockResource("/tmp"));
                    children.add(mockResource("/var"));
                } else if("/var".equals(parent.getPath())) {
                    children.add(mockResource("/var/one"));
                    children.add(mockResource("/var/two"));
                    children.add(mockResource("/var/three"));
                } else if("/tmp".equals(parent.getPath())) {
                    children.add(mockResource("/tmp/A"));
                    children.add(mockResource("/tmp/B"));
                    children.add(mockResource("/tmp/C"));
                    children.add(mockResource("/tmp/D"));
                }
                return children.iterator();
            }

        };

        ResourceResolverFactoryActivator activator = new ResourceResolverFactoryActivator();
        final CommonResourceResolverFactoryImpl crf = new CommonResourceResolverFactoryImpl(activator) {
            @Override
            public ResourceDecoratorTracker getResourceDecoratorTracker() {
                return t;
            }

            @Override
            public ResourceAccessSecurityTracker getResourceAccessSecurityTracker() {
                return new ResourceAccessSecurityTracker();
            }
        };

        List<ResourceProviderHandler> list = Arrays.asList(MockedResourceResolverImplTest.createRPHandler(provider, "A-provider", 0L, "/"));
        resolver = new ResourceResolverImpl(crf, false, null, new ResourceProviderStorage(list));
    }

    protected void assertExistent(Resource r, boolean existent) {
        assertNotNull("Expecting non-null Resource", r);
        assertEquals("Expecting " + (existent ? "existent" : "non-existent") + " resource",
                existent,
                !NonExistingResource.RESOURCE_TYPE_NON_EXISTING.equals(r.getResourceType()));
    }

    protected Resource mockResource(String path) {
        final Resource result = Mockito.mock(Resource.class);
        Mockito.when(result.getPath()).thenReturn(path);
        Mockito.when(result.getName()).thenReturn(ResourceUtil.getName(path));
        final ResourceMetadata m = new ResourceMetadata();
        Mockito.when(result.getResourceMetadata()).thenReturn(m);
        return result;
    }
}