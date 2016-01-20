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
package org.apache.sling.resourceresolver.impl.stateful;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.runtime.dto.AuthType;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderInfo;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderStorage;
import org.apache.sling.resourceresolver.impl.providers.stateful.CombinedResourceProvider;
import org.apache.sling.resourceresolver.impl.providers.stateful.ResourceProviderAuthenticator;
import org.apache.sling.spi.resource.provider.ResolverContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@SuppressWarnings("unchecked")
public class CombinedResourceProviderTest {

    private ResourceProviderAuthenticator authenticator;
    private CombinedResourceProvider crp;
    private List<ResourceProviderHandler> handlers;
    private ResourceProvider<Object> subProvider;
    private Map<String, Object> authInfo;

    @Before
    public void prepare() throws Exception {

        BundleContext bc = MockOsgi.newBundleContext();
        
        // sub-provider
        subProvider = Mockito.mock(ResourceProvider.class);
        ResourceProviderInfo info = registerResourceProvider(bc, subProvider, "/some/path", AuthType.required);
        ResourceProviderHandler handler = new ResourceProviderHandler(bc, info);
        handler.activate();

        // root provider
        ResourceProvider<Object> rootProvider = mock(ResourceProvider.class);
        ResourceProviderInfo rootInfo = registerResourceProvider(bc, rootProvider, "/", AuthType.required);
        ResourceProviderHandler rootHandler = new ResourceProviderHandler(bc, rootInfo);
        rootHandler.activate();

        // configure mock resources
        Resource root = configureResourceAt(rootProvider, "/");
        Resource something = configureResourceAt(rootProvider, "/something");
        configureResourceAt(subProvider, "/some/path/object");
        
        // configure query at '/'
        when(rootProvider.listChildren((ResolverContext<Object>) Mockito.anyObject(), Mockito.eq(root))).thenReturn(Collections.singleton(something).iterator());
        
        ResourceResolver rr = mock(ResourceResolver.class);
        ResourceAccessSecurityTracker securityTracker = Mockito.mock(ResourceAccessSecurityTracker.class);
        authInfo = Collections.emptyMap();

        handlers = Arrays.asList(rootHandler, handler);
        ResourceProviderStorage storage = new ResourceProviderStorage(handlers);
        authenticator = new ResourceProviderAuthenticator(rr, authInfo, securityTracker);

        crp = new CombinedResourceProvider(storage, rr, authenticator);
    }

    private ResourceProviderInfo registerResourceProvider(BundleContext bc, ResourceProvider<?> rp, String root, AuthType authType) throws InvalidSyntaxException {
        
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(ResourceProvider.PROPERTY_ROOT, root);
        props.put(ResourceProvider.PROPERTY_AUTHENTICATE, authType.name());
        
        bc.registerService(ResourceProvider.class.getName(), rp, props);
        
        ServiceReference sr = bc.getServiceReferences(ResourceProvider.class.getName(),
                "(" + ResourceProvider.PROPERTY_ROOT + "=" + root + ")")[0];
        
        return new ResourceProviderInfo(sr);
    }

    /**
     * Configures the provider to return a mock resource for the specified path
     * @return 
     */
    private <T> Resource configureResourceAt(ResourceProvider<T> provider, String path) {
        
        Resource mockResource = mock(Resource.class);
        when(mockResource.getPath()).thenReturn(path);
        when(mockResource.getResourceMetadata()).thenReturn(mock(ResourceMetadata.class));
        
        when(provider.getResource((ResolverContext<T>) Mockito.any(), Mockito.eq(path), (ResourceContext) Mockito.any(), (Resource) Mockito.any()))
            .thenReturn(mockResource);
        
        return mockResource;
    }

    /**
     * Verifies that login and logout calls are invoked as expected on
     * ResourceProviders with authType = {@link AuthType#required}
     */
    @Test
    public void loginLogout() throws LoginException {

        authenticator.authenticateAll(handlers, crp);

        verify(subProvider).authenticate(authInfo);

        crp.logout();

        verify(subProvider).logout((ResolverContext<Object>) Mockito.any());
    }

    /**
     * Verifies that a synthetic resource is returned for a path which holds no
     * actual resource but is an ancestor of another resource provider
     */
    @Test
    public void getResource_synthetic() {

        Resource resource = crp.getResource("/some", null, null, false);

        assertTrue("Not a syntethic resource : " + resource, ResourceUtil.isSyntheticResource(resource));
    }
    
    /**
     * Verifies that a getResource call for a missing resource returns null
     */
    @Test
    public void getResource_missing() {
        assertThat(crp.getResource("/nothing", null, null, false), nullValue());
    }

    /**
     * Verifies that a resource is returned when it should be
     */
    @Test
    public void getResource_found() {
        assertThat(crp.getResource("/something", null, null, false), not(nullValue()));
        assertThat(crp.getResource("/some/path/object", null, null, false), not(nullValue()));
    }

    /**
     * Verifies that listing the children at root lists both the synthetic and the 'real' children
     */
    @Test
    public void listChildren_root() {
        Resource root = crp.getResource("/", null, null, false);
        Iterator<Resource> children = crp.listChildren(root);
        
        Map<String, Resource> all = new HashMap<String, Resource>();
        while ( children.hasNext() ) {
            Resource child = children.next();
            all.put(child.getPath(), child);
        }
        
        assertThat(all.entrySet(), Matchers.hasSize(2));
        assertThat("Resource at /something", all.get("/something"), not(nullValue()));
        assertThat("Resource at /some", all.get("/some"), not(nullValue()));
    }
    
    @Test
    public void listChildren_lowerLevel() {
        
        Resource root = crp.getResource("/some", null, null, false);
        Iterator<Resource> children = crp.listChildren(root);
        Map<String, Resource> all = new HashMap<String, Resource>();

        while ( children.hasNext() ) {
            Resource child = children.next();
            all.put(child.getPath(), child);
        }
        
        assertThat(all.entrySet(), Matchers.hasSize(1));
        assertThat("Resource at /some/path", all.get("/some/path"), not(nullValue()));
        
    }
}
