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
package org.apache.sling.resourceresolver.impl.helper;

import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.resource.runtime.dto.AuthType;
import org.apache.sling.resourceresolver.impl.Fixture;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.SimpleValueMapImpl;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderInfo;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderStorage;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderStorageProvider;
import org.apache.sling.spi.resource.provider.QueryLanguageProvider;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;

@SuppressWarnings("unchecked")
public class ResourceResolverControlTest {

    private static final String TEST_ATTRIBUTE = "some.test.attribute";

    private static final List<String> TEST_FORBIDDEN_ATTRIBUTES = new ArrayList<String>();

    static {
            TEST_FORBIDDEN_ATTRIBUTES.add(ResourceResolverFactory.PASSWORD);
            TEST_FORBIDDEN_ATTRIBUTES.add(ResourceProvider.AUTH_SERVICE_BUNDLE);
            TEST_FORBIDDEN_ATTRIBUTES.add(ResourceResolverFactory.SUBSERVICE);
    }

    // query language names
    private static final String QL_MOCK = "MockQueryLanguage";
    private static final String QL_ANOTHER_MOCK = "AnotherMockQueryLanguage";
    private static final String QL_NOOP = "NoopQueryLanguage";

    // query definitions
    private static final String QUERY_MOCK_FIND_ALL = "FIND ALL";

    private ResourceResolverControl crp;
    private List<ResourceProviderHandler> handlers;
    private ResourceProvider<Object> subProvider;
    private Map<String, Object> authInfo;
    private ResourceProvider<Object> rootProvider;
    private Resource subProviderResource;
    private Resource somethingResource;
    private ResourceResolverContext context;

    @Before
    public void prepare() throws Exception {

        BundleContext bc = MockOsgi.newBundleContext();

        Fixture fixture = new Fixture(bc);

        // sub-provider
        subProvider = Mockito.mock(ResourceProvider.class);
        ResourceProviderInfo info = fixture.registerResourceProvider(subProvider, "/some/path", AuthType.required);
        ResourceProviderHandler handler = new ResourceProviderHandler(bc, info);
        when(subProvider.getQueryLanguageProvider()).thenReturn(new SimpleQueryLanguageProvider(QL_MOCK, QL_ANOTHER_MOCK) {
            @Override
            public Iterator<ValueMap> queryResources(ResolveContext<Object> ctx, String query, String language) {
                if ( query.equals(QUERY_MOCK_FIND_ALL) && language.equals(QL_MOCK)) {
                    SimpleValueMapImpl valueMap = new SimpleValueMapImpl();
                    valueMap.put("key", "value");
                    return Collections.<ValueMap> singletonList(valueMap).iterator();
                }

                throw new UnsupportedOperationException();
            }

            @Override
            public Iterator<Resource> findResources(ResolveContext<Object> ctx, String query, String language) {

                if ( query.equals(QUERY_MOCK_FIND_ALL) && language.equals(QL_MOCK)) {
                    return Collections.<Resource> singletonList(newMockResource("/some/path/object")).iterator();
                }

                throw new UnsupportedOperationException();

            }
        });
        handler.activate();

        rootProvider = mock(ResourceProvider.class);
        ResourceProviderInfo rootInfo = fixture.registerResourceProvider(rootProvider, "/", AuthType.required);
        ResourceProviderHandler rootHandler = new ResourceProviderHandler(bc, rootInfo);
        when(rootProvider.getQueryLanguageProvider()).thenReturn(new SimpleQueryLanguageProvider(QL_NOOP));
        rootHandler.activate();

        // configure mock resources
        Resource root = configureResourceAt(rootProvider, "/");
        somethingResource = configureResourceAt(rootProvider, "/something");
        subProviderResource = configureResourceAt(subProvider, "/some/path/object");

        // configure query at '/'
        when(rootProvider.listChildren((ResolveContext<Object>) Mockito.anyObject(), Mockito.eq(root))).thenReturn(Collections.singleton(somethingResource).iterator());

        ResourceResolver rr = mock(ResourceResolver.class);
        ResourceAccessSecurityTracker securityTracker = Mockito.mock(ResourceAccessSecurityTracker.class);
        authInfo = getAuthInfo();

        handlers = Arrays.asList(rootHandler, handler);
        final ResourceProviderStorage storage = new ResourceProviderStorage(handlers);

        crp = new ResourceResolverControl(false, authInfo, new ResourceProviderStorageProvider() {

            @Override
            public ResourceProviderStorage getResourceProviderStorage() {
                return storage;
            }
        });
        context = new ResourceResolverContext(rr, securityTracker);
    }

    /** Return test auth info */
    private Map<String, Object> getAuthInfo() {
        final Map<String, Object> result = new HashMap<String, Object>();

        // Add all forbidden attributes to be able to verify that
        // they are masked
        for(String str : TEST_FORBIDDEN_ATTRIBUTES) {
            result.put(str, "should be hidden");
        }

        result.put(TEST_ATTRIBUTE, "is " + TEST_ATTRIBUTE);

        return result;
    }

    /**
     * Configures the provider to return a mock resource for the specified path
     * @return
     */
    private <T> Resource configureResourceAt(ResourceProvider<T> provider, String path) {

        Resource mockResource = newMockResource(path);

        when(provider.getResource((ResolveContext<T>) Mockito.any(), Mockito.eq(path), (ResourceContext) Mockito.any(), (Resource) Mockito.any()))
            .thenReturn(mockResource);

        return mockResource;
    }

    private Resource newMockResource(String path) {

        Resource mockResource = mock(Resource.class);
        when(mockResource.getPath()).thenReturn(path);
        when(mockResource.getName()).thenReturn(ResourceUtil.getName(path));
        when(mockResource.getResourceMetadata()).thenReturn(mock(ResourceMetadata.class));
        when(mockResource.getChildren()).thenReturn(Collections.<Resource> emptyList());

        return mockResource;
    }

    /**
     * Verifies that login and logout calls are invoked as expected on
     * ResourceProviders with authType = {@link AuthType#required}
     */
    @Test
    public void loginLogout() throws LoginException {

        context.getProviderManager().authenticateAll(handlers, crp);

        verify(subProvider).authenticate(authInfo);

        crp.close();

        verify(subProvider).logout(mockContext());
    }

    private ResolveContext<Object> mockContext() {
        return (ResolveContext<Object>) Mockito.any();
    }

    /**
     * Verifies that a synthetic resource is returned for a path which holds no
     * actual resource but is an ancestor of another resource provider
     */
    @Test
    public void getResource_synthetic() {

        Resource resource = crp.getResource(context, "/some", null, null, false);

        assertTrue("Not a syntethic resource : " + resource, ResourceUtil.isSyntheticResource(resource));
    }

    /**
     * Verifies that a getResource call for a missing resource returns null
     */
    @Test
    public void getResource_missing() {
        assertThat(crp.getResource(context, "/nothing", null, null, false), nullValue());
    }

    /**
     * Verifies that a resource is returned when it should be
     */
    @Test
    public void getResource_found() {
        assertThat(crp.getResource(context, "/something", null, null, false), not(nullValue()));
        assertThat(crp.getResource(context, "/some/path/object", null, null, false), not(nullValue()));
    }


    /**
     * Verifies that the existing parent of a resource is found
     */
    @Test
    public void getParent_found() {
        Resource parent = crp.getParent(context, ResourceUtil.getParent(somethingResource.getPath()), somethingResource);
        assertThat(parent, notNullValue());
        assertThat("parent.path", parent.getPath(), equalTo("/"));
    }



    /**
     * Verifies that a synthetic parent is returned for a resource without an actual parent
     */
    @Test
    public void getParent_synthetic() {
        Resource parent = crp.getParent(context, ResourceUtil.getParent(subProviderResource.getPath()), subProviderResource);
        assertThat(parent, notNullValue());
        assertTrue("parent is a synthetic resource", ResourceUtil.isSyntheticResource(parent));
    }

    /**
     * Test parent from a different provider
     */
    @Test
    public void getParent_differentProviders() {
        final Resource childResource = mock(Resource.class);
        when(childResource.getPath()).thenReturn("/some/path");
        when(subProvider.getResource((ResolveContext<Object>) Mockito.anyObject(), Mockito.eq("/some/path"), (ResourceContext) Mockito.anyObject(), (Resource)Mockito.eq(null))).thenReturn(childResource);

        final Resource parentResource = mock(Resource.class);
        when(parentResource.getPath()).thenReturn("/some");
        when(rootProvider.getResource((ResolveContext<Object>) Mockito.anyObject(), Mockito.eq("/some"), (ResourceContext) Mockito.anyObject(), (Resource)Mockito.eq(null))).thenReturn(parentResource);

        Resource child = crp.getResource(context, "/some/path", null, null, false);
        assertNotNull(child);
        assertTrue(childResource == child);

        Resource parent = crp.getParent(context, ResourceUtil.getParent(child.getPath()), child);
        assertNotNull(parent);
        assertTrue(parentResource == parent);
    }

    /**
     * Verifies that listing the children at root lists both the synthetic and the 'real' children
     */
    @Test
    public void listChildren_root() {
        Resource root = crp.getResource(context, "/", null, null, false);
        Iterator<Resource> children = crp.listChildren(context, root);

        Map<String, Resource> all = new HashMap<String, Resource>();
        while ( children.hasNext() ) {
            Resource child = children.next();
            all.put(child.getPath(), child);
        }

        assertThat(all.entrySet(), Matchers.hasSize(2));
        assertThat("Resource at /something", all.get("/something"), not(nullValue()));
        assertThat("Resource at /some", all.get("/some"), not(nullValue()));
    }

    /**
     * Verifies listing the children at a level below the root
     */
    @Test
    public void listChildren_lowerLevel() {

        Resource root = crp.getResource(context, "/some", null, null, false);
        Iterator<Resource> children = crp.listChildren(context, root);
        Map<String, Resource> all = new HashMap<String, Resource>();

        while ( children.hasNext() ) {
            Resource child = children.next();
            all.put(child.getPath(), child);
        }

        assertThat(all.entrySet(), Matchers.hasSize(1));
        assertThat("Resource at /some/path", all.get("/some/path"), not(nullValue()));

    }

    /**
     * Verifies copying resources between the same ResourceProvider
     *
     * @throws PersistenceException persistence exception
     */
    @Test
    public void copy_sameProvider() throws PersistenceException {

        when(subProvider.copy(mockContext(), Mockito.eq("/some/path/object"), Mockito.eq("/some/path/new")))
            .thenReturn(true);
        configureResourceAt(subProvider, "/some/path/new/object");
        configureResourceAt(subProvider, "/some/path/new");

        Resource resource = crp.copy(context, "/some/path/object", "/some/path/new");


        assertThat(resource, not(nullValue()));
    }

    /**
     * Verifies copying resources between different ResourceProviders
     *
     * @throws PersistenceException persistence exception
     */
    @Test
    public void copy_differentProvider() throws PersistenceException {

        Resource newRes = newMockResource("/object");
        when(rootProvider.create(mockContext(), Mockito.eq("/object"), Mockito.anyMap()))
            .thenReturn(newRes);

        Resource resource = crp.copy(context, "/some/path/object", "/");

        assertThat(resource, not(nullValue()));
    }

    /**
     * Verifies moving resources between the same ResourceProvider
     *
     * @throws PersistenceException persistence exception
     */
    @Test
    public void move_sameProvider() throws PersistenceException {

        when(subProvider.move(mockContext(), Mockito.eq("/some/path/object"), Mockito.eq("/some/path/new")))
                .thenReturn(true);
        configureResourceAt(subProvider, "/some/path/new/object");
        configureResourceAt(subProvider, "/some/path/new");

        Resource resource = crp.move(context, "/some/path/object", "/some/path/new");

        assertThat(resource, not(nullValue()));
    }

    /**
     * Verifies moving resources between different ResourceProviders
     *
     * @throws PersistenceException persistence exception
     */
    @Test
    public void move_differentProvider() throws PersistenceException {

        Resource newRes = newMockResource("/object");
        when(rootProvider.create(mockContext(), Mockito.eq("/object"), Mockito.anyMap())).thenReturn(newRes);

        Resource resource = crp.move(context, "/some/path/object", "/");

        assertThat(resource, not(nullValue()));

        verify(subProvider).delete(mockContext(), Mockito.eq(subProviderResource));
    }

    /**
     * Verifies listing the query languages
     */
    @Test
    public void queryLanguages() throws PersistenceException {

        assertThat(crp.getSupportedLanguages(context), arrayContainingInAnyOrder(QL_NOOP, QL_MOCK, QL_ANOTHER_MOCK));
    }

    /**
     * Verifies running a query
     */
    @Test
    public void queryResources() throws PersistenceException {

        Iterator<Map<String, Object>> queryResources = crp.queryResources(context, QUERY_MOCK_FIND_ALL, QL_MOCK);

        int count = 0;

        while ( queryResources.hasNext() ) {
            assertThat("ValueMap returned from query", queryResources.next(), hasEntry("key", (Object) "value"));
            count++;
        }

        assertThat("query result count", count, Matchers.equalTo(1));
    }

    /**
     * Verifies finding resources
     */
    @Test
    public void findResource() throws PersistenceException {

        Iterator<Resource> resources = crp.findResources(context, QUERY_MOCK_FIND_ALL, QL_MOCK);

        int count = 0;

        while ( resources.hasNext() ) {
            assertThat("resources[0].path", resources.next().getPath(), equalTo("/some/path/object"));
            count++;
        }

        assertThat("query result count", count, Matchers.equalTo(1));
    }

    @Test
    public void forbiddenAttributeNames() {
        for(String name : crp.getAttributeNames(context)) {
            if(TEST_FORBIDDEN_ATTRIBUTES.contains(name)) {
                fail("Attribute " + name + " should not be accessible");
            }
        }
        assertTrue("Expecting non-forbidden attribute", crp.getAttributeNames(context).contains(TEST_ATTRIBUTE));
    }

    @Test
    public void forbiddenAttributeValues() {
        for(String name : TEST_FORBIDDEN_ATTRIBUTES) {
            assertNull("Expecting " + name + " to be hidden", crp.getAttribute(context, name));
        }
        assertEquals("is " + TEST_ATTRIBUTE, crp.getAttribute(context, TEST_ATTRIBUTE));
    }

    /**
     * Simple test-only QueryLanguageProvider
     *
     */
    private static class SimpleQueryLanguageProvider implements QueryLanguageProvider<Object> {

        private final String[] queryLanguages;

        public SimpleQueryLanguageProvider(String... queryLanguages) {
            this.queryLanguages = queryLanguages;
        }

        @Override
        public String[] getSupportedLanguages(ResolveContext<Object> ctx) {
            return queryLanguages;
        }

        @Override
        public Iterator<ValueMap> queryResources(ResolveContext<Object> ctx, String query, String language) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Resource> findResources(ResolveContext<Object> ctx, String query, String language) {
            throw new UnsupportedOperationException();
        }
    }
}
