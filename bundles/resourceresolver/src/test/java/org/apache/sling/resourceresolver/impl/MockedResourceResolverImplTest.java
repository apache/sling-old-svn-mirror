/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.resourceresolver.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.resourceresolver.impl.mapping.MapEntries;
import org.apache.sling.resourceresolver.impl.observation.ResourceChangeListenerWhiteboard;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderInfo;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderStorage;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderTracker;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.apache.sling.spi.resource.provider.QueryLanguageProvider;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;

/**
 * This tests the ResourceResolver using mocks. The Unit test is in addition to
 * ResourceResolverImplTest which covers API conformance more than it covers all
 * code paths.
 */
// TODO: Configure mapping to react correctly.
// TODO: test external redirect.
// TODO: Map to URI
// TODO: Statresource
public class MockedResourceResolverImplTest {

    private static final List<Resource> EMPTY_RESOURCE_LIST = new ArrayList<Resource>();
    private static final String FAKE_QUERY_LANGUAGE = "fake";
    private static final String PATH = "path";

    private ResourceResolverFactoryActivator activator;

    private List<ResourceProviderHandler> handlers = new ArrayList<ResourceProviderHandler>();

    @Mock
    private BundleContext bundleContext;

    @Mock
    private Bundle usingBundle;

    @Mock
    private BundleContext usingBundleContext;

    @Mock
    private ResourceProviderTracker resourceProviderTracker;

    @Mock
    private ResourceChangeListenerWhiteboard resourceChangeListenerWhiteboard;

    @SuppressWarnings("rawtypes")
    @Mock
    private QueryLanguageProvider queryProvider;

    private Map<String, Object> services = new HashMap<String, Object>();

    private Map<String, Object> serviceProperties = new HashMap<String, Object>();

    private ResourceResolverFactoryImpl resourceResolverFactory;

    @Mock
    private ResourceProvider<?> resourceProvider;

    /**
     * deals with /etc resolution.
     */
    @Mock
    private ResourceProvider<?> mappingResourceProvider;

    /**
     * deals with /apps and /libs resolution.
     */
    @Mock
    private ResourceProvider<?> appsResourceProvider;

    /**
     * QueriableResourceProviders
     */
    @Mock
    private ResourceProvider<?> queriableResourceProviderA;


    public MockedResourceResolverImplTest() {
        MockitoAnnotations.initMocks(this);
    }

    @SuppressWarnings("unchecked")
    @Before
    public void before() throws LoginException {
        activator = new ResourceResolverFactoryActivator();

        // system bundle access
        final Bundle systemBundle = Mockito.mock(Bundle.class);
        Mockito.when(systemBundle.getState()).thenReturn(Bundle.ACTIVE);
        Mockito.when(bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION)).thenReturn(systemBundle);
        activator.resourceAccessSecurityTracker = new ResourceAccessSecurityTracker();
        activator.resourceProviderTracker = resourceProviderTracker;
        activator.changeListenerWhiteboard = resourceChangeListenerWhiteboard;
        activator.serviceUserMapper = Mockito.mock(ServiceUserMapper.class);
        handlers.add(createRPHandler(resourceProvider, "org.apache.sling.resourceresolver.impl.DummyTestProvider", 10L, "/single"));

        // setup mapping resources at /etc/map to exercise vanity etc.
        // hmm, can't provide the resolver since its not up and ready.
        // mapping almost certainly work properly until this can be setup correctly.
        buildMappingResource("/etc/map", mappingResourceProvider, null);

        handlers.add(createRPHandler(mappingResourceProvider, "org.apache.sling.resourceresolver.impl.MapProvider", 11L, "/etc"));
        handlers.add(createRPHandler(appsResourceProvider, "org.apache.sling.resourceresolver.impl.AppsProvider", 12L, "/libs"));
        handlers.add(createRPHandler(appsResourceProvider, "org.apache.sling.resourceresolver.impl.AppsProvider", 13L, "/apps"));
        handlers.add(createRPHandler(queriableResourceProviderA, "org.apache.sling.resourceresolver.impl.QueriableResourceProviderA", 14L, "/searchA"));
        Mockito.when(queriableResourceProviderA.getQueryLanguageProvider()).thenReturn(queryProvider);

        ResourceProviderStorage storage = new ResourceProviderStorage(handlers);

        Mockito.when(resourceProviderTracker.getResourceProviderStorage()).thenReturn(storage);

        // activate the components.
        activator.activate(bundleContext, new ResourceResolverFactoryConfig() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String[] resource_resolver_virtual() {
                return new String[] {"/:/"};
            }

            @Override
            public String[] resource_resolver_vanitypath_whitelist() {
                return null;
            }

            @Override
            public boolean resource_resolver_vanitypath_maxEntries_startup() {
                return true;
            }

            @Override
            public int resource_resolver_vanitypath_maxEntries() {
                return -1;
            }

            @Override
            public int resource_resolver_vanitypath_bloomfilter_maxBytes() {
                return 1024000;
            }

            @Override
            public String[] resource_resolver_vanitypath_blacklist() {
                return null;
            }

            @Override
            public boolean resource_resolver_vanity_precedence() {
                return false;
            }

            @Override
            public String[] resource_resolver_searchpath() {
                return new String[] {"/apps", "/libs"};
            }

            @Override
            public String[] resource_resolver_required_providers() {
                return  new String[] { "org.apache.sling.resourceresolver.impl.DummyTestProvider" };
            }

            @Override
            public String[] resource_resolver_required_providernames() {
                return null;
            }

            @Override
            public boolean resource_resolver_providerhandling_paranoid() {
                return false;
            }

            @Override
            public boolean resource_resolver_optimize_alias_resolution() {
                return true;
            }

            @Override
            public String[] resource_resolver_mapping() {
                return new String[] { "/:/",
                        "/content/:/", "/system/docroot/:/", "/content.html-/$" };
            }

            @Override
            public String[] resource_resolver_map_observation() {
                return new String[] {"/"};
            }

            @Override
            public String resource_resolver_map_location() {
                return MapEntries.DEFAULT_MAP_ROOT;
            }

            @Override
            public boolean resource_resolver_manglenamespaces() {
                return true;
            }

            @Override
            public boolean resource_resolver_log_closing() {
                return false;
            }

            @Override
            public boolean resource_resolver_enable_vanitypath() {
                return true;
            }

            @Override
            public int resource_resolver_default_vanity_redirect_status() {
                return 302;
            }

            @Override
            public boolean resource_resolver_allowDirect() {
                return true;
            }

            @Override
            public boolean resource_resolver_log_unclosed() {
                return true;
            }
        });

        // configure using Bundle
        Mockito.when(usingBundle.getBundleContext()).thenReturn(usingBundleContext);
        Mockito.when(usingBundleContext.getBundle()).thenReturn(usingBundle);

        // extract any services that were registered into a map.
        ArgumentCaptor<Class> classesCaptor = ArgumentCaptor.forClass(Class.class);
        ArgumentCaptor<ServiceFactory> serviceCaptor = ArgumentCaptor.forClass(ServiceFactory.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Dictionary> propertiesCaptor = ArgumentCaptor.forClass(Dictionary.class);
        Mockito.verify(bundleContext, Mockito.atLeastOnce()).registerService(
            (Class<ResourceResolverFactory>)classesCaptor.capture(), (ServiceFactory<ResourceResolverFactory>)serviceCaptor.capture(),
            propertiesCaptor.capture());

        int si = 0;
        List<ServiceFactory> serviceList = serviceCaptor.getAllValues();
        @SuppressWarnings({ "unused", "rawtypes" })
        List<Dictionary> servicePropertiesList = propertiesCaptor.getAllValues();
        for (Class serviceClass : classesCaptor.getAllValues()) {
            services.put(serviceClass.getName(), serviceList.get(si));
            serviceProperties.put(serviceClass.getName(), serviceProperties.get(si));
            si++;
        }
        // verify that a ResourceResolverFactoryImpl was created and registered.
        Assert.assertNotNull("" + services, services.get(ResourceResolverFactory.class.getName()));
        Object rrf = services.get(ResourceResolverFactory.class.getName());
        if (rrf instanceof ServiceFactory) {
            rrf = ((ServiceFactory) rrf).getService(usingBundle, null);
        }
        Assert.assertTrue(rrf instanceof ResourceResolverFactoryImpl);
        resourceResolverFactory = (ResourceResolverFactoryImpl) rrf;
    }

    public static ResourceProviderHandler createRPHandler(ResourceProvider<?> rp, String pid, long ranking,
            String path) {
        ServiceReference ref = Mockito.mock(ServiceReference.class);
        BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.getService(Mockito.eq(ref))).thenReturn(rp);
        Mockito.when(ref.getProperty(Mockito.eq(Constants.SERVICE_ID))).thenReturn(new Random().nextLong());
        Mockito.when(ref.getProperty(Mockito.eq(Constants.SERVICE_PID))).thenReturn(pid);
        Mockito.when(ref.getProperty(Mockito.eq(Constants.SERVICE_RANKING))).thenReturn(ranking);
        Mockito.when(ref.getProperty(Mockito.eq(ResourceProvider.PROPERTY_ROOT))).thenReturn(path);
        Mockito.when(ref.getProperty(Mockito.eq(ResourceProvider.PROPERTY_MODIFIABLE))).thenReturn(true);
        Mockito.when(ref.getProperty(Mockito.eq(ResourceProvider.PROPERTY_ATTRIBUTABLE))).thenReturn(true);
        Mockito.when(ref.getProperty(Mockito.eq(ResourceProvider.PROPERTY_ADAPTABLE))).thenReturn(true);

        ResourceProviderInfo info = new ResourceProviderInfo(ref);
        final ResourceProviderHandler handler = new ResourceProviderHandler(bc, info);
        handler.activate();
        return handler;
    }

    @SuppressWarnings("unchecked")
    private Resource buildMappingResource(String path,
            ResourceProvider<?> provider, ResourceResolver resourceResolver) {
        List<Resource> localHostAnyList = new ArrayList<Resource>();
        localHostAnyList.add(buildResource(path+"/http/example.com.80/cgi-bin", EMPTY_RESOURCE_LIST, resourceResolver, provider, "sling:internalRedirect", "/scripts" ));
        localHostAnyList.add(buildResource(path+"/http/example.com.80/gateway", EMPTY_RESOURCE_LIST, resourceResolver, provider,"sling:internalRedirect", "http://gbiv.com"));
        localHostAnyList.add(buildResource(path+"/http/example.com.80/stories", EMPTY_RESOURCE_LIST, resourceResolver, provider,"sling:internalRedirect", "/anecdotes/$1"));

        List<Resource> mappingChildren = new ArrayList<Resource>();
        mappingChildren.add(buildResource(path+"/http/example.com.80", EMPTY_RESOURCE_LIST, resourceResolver, provider,"sling:redirect", "http://www.example.com/"));
        mappingChildren.add(buildResource(path+"/http/www.example.com.80", EMPTY_RESOURCE_LIST, resourceResolver, provider,"sling:internalRedirect", "/example"));
        mappingChildren.add(buildResource(path+"/http/any_example.com.80", EMPTY_RESOURCE_LIST, resourceResolver, provider,"sling:match", ".+\\.example\\.com\\.80", "sling:redirect", "http://www.example.com/"));
        mappingChildren.add(buildResource(path+"/http/localhost_any", localHostAnyList, resourceResolver, provider,"sling:match", "localhost\\.\\d*", "sling:internalRedirect", "/content"));

        Resource etcMapResource = buildResource(path+"/http", mappingChildren);
        Mockito.when(provider.getResource(Mockito.any(ResolveContext.class), Mockito.eq(path), Mockito.any(ResourceContext.class), Mockito.any(Resource.class))).thenReturn(etcMapResource);
        return etcMapResource;
    }

    @After
    public void after() {
        handlers.clear();
    }

    /**
     * build child resources as an iterable of resources.
     * @param parent
     * @return
     */
    private Iterable<Resource> buildChildResources(String parent) {
        List<Resource> mappingResources = new ArrayList<Resource>();
        for ( int i = 0; i < 5; i++ ) {
            mappingResources.add(buildResource(parent+"/m"+i, EMPTY_RESOURCE_LIST));
        }
        return mappingResources;
    }
    /**
     * Build a resource based on path and children.
     * @param fullpath
     * @param children
     * @return
     */
    private Resource buildResource(String fullpath, Iterable<Resource> children) {
        return buildResource(fullpath, children, null, null, new String[0]);
    }

    /** Build a List of ValueMap */
    private List<ValueMap> buildValueMapCollection(int howMany, String pathPrefix) {
        final List<ValueMap> result = new ArrayList<ValueMap>();
        for(int i=0;  i < howMany; i++) {
            final Map<String, Object> m = new HashMap<String, Object>();
            m.put(PATH, pathPrefix + i);
            result.add(new ValueMapDecorator(m));
        }
        return result;
    }

    /**
     * Build a resource with path, children and resource resolver.
     * @param fullpath
     * @param children
     * @param resourceResolver
     * @return
     */
    @SuppressWarnings("unchecked")
    private Resource buildResource(String fullpath, Iterable<Resource> children, ResourceResolver resourceResolver, ResourceProvider<?> provider, String ... properties) {
        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(resource.getName()).thenReturn(getName(fullpath));
        Mockito.when(resource.getPath()).thenReturn(fullpath);
        ResourceMetadata resourceMetadata = new ResourceMetadata();
        Mockito.when(resource.getResourceMetadata()).thenReturn(resourceMetadata);
        Mockito.when(resource.listChildren()).thenReturn(children.iterator());
        Mockito.when(resource.getResourceResolver()).thenReturn(resourceResolver);

        // register the resource with the provider
        if ( provider != null ) {
            Mockito.when(provider.listChildren(Mockito.any(ResolveContext.class), Mockito.eq(resource))).thenReturn(children.iterator());
            Mockito.when(provider.getResource(Mockito.any(ResolveContext.class), Mockito.eq(fullpath), Mockito.any(ResourceContext.class), Mockito.any(Resource.class))).thenReturn(resource);
        }
        if ( properties != null ) {
            ValueMap vm = new SimpleValueMapImpl();
            for ( int i=0; i < properties.length; i+=2) {
                resourceMetadata.put(properties[i], properties[i+1]);
                vm.put(properties[i], properties[i+1]);
            }
            Mockito.when(resource.getValueMap()).thenReturn(vm);
            Mockito.when(resource.adaptTo(Mockito.eq(ValueMap.class))).thenReturn(vm);
        } else {
            Mockito.when(resource.getValueMap()).thenReturn(ValueMapDecorator.EMPTY);
            Mockito.when(resource.adaptTo(Mockito.eq(ValueMap.class))).thenReturn(ValueMapDecorator.EMPTY);
        }

        return resource;
    }


    /**
     * extract the name from a path.
     * @param fullpath
     * @return
     */
    private String getName(String fullpath) {
        int n = fullpath.lastIndexOf("/");
        return fullpath.substring(n+1);
    }

    /**
     * Test getting a resolver.
     * @throws LoginException
     */
    @Test
    public void testGetResolver() throws LoginException {
        ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(null);
        Assert.assertNotNull(resourceResolver);
        Map<String, Object> authenticationInfo = new HashMap<String, Object>();
        resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(authenticationInfo);
        Assert.assertNotNull(resourceResolver);
    }

    /**
     * Misceleneous coverage.
     * @throws LoginException
     */
    @Test
    public void testResolverMisc() throws LoginException {
        ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(null);
        try {
            resourceResolver.getAttribute(null);
            Assert.fail("Should have thrown a NPE");
        } catch ( NullPointerException e) {
            // this is expected.
        }
        Assert.assertArrayEquals(new String[]{"/apps/","/libs/"}, resourceResolver.getSearchPath());
    }

    /**
     * Test various administrative resource resolvers.
     * @throws LoginException
     */
    @Test
    public void testGetAuthenticatedResolve() throws LoginException {
        ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
        Assert.assertNotNull(resourceResolver);
        Map<String, Object> authenticationInfo = new HashMap<String, Object>();

        resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(authenticationInfo);
        Assert.assertNotNull(resourceResolver);
    }

    /**
     * Test getResource for a resource provided by a resource provider.
     * @throws LoginException
     */
    @Test
    public void testGetResource() throws LoginException {
        ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(null);
        Assert.assertNotNull(resourceResolver);
        Resource singleResource = buildResource("/single/test", EMPTY_RESOURCE_LIST, resourceResolver, resourceProvider);
        Resource resource = resourceResolver.getResource("/single/test");
        Assert.assertEquals(singleResource, resource);
    }

    /**
     * Test getResource where path contains intermediate . verifying fix for SLING-864
     * @throws LoginException
     */
    @Test
    public void testGetResourceSLING864() throws LoginException {
        ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(null);
        Assert.assertNotNull(resourceResolver);
        Resource singleResource = buildResource("/single/test.with/extra.dots/inthepath", EMPTY_RESOURCE_LIST,resourceResolver, resourceProvider);
        Resource resource = resourceResolver.getResource("/single/test.with/extra.dots/inthepath");
        Assert.assertEquals(singleResource, resource);
    }


    /**
     * Test search paths
     * @throws LoginException
     */
    @Test
    public void testRelativeResource() throws LoginException {
        ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(null);
        Assert.assertNotNull(resourceResolver);
        Resource appResource = buildResource("/apps/store/inventory", EMPTY_RESOURCE_LIST, resourceResolver, appsResourceProvider);
        Resource libResource = buildResource("/libs/store/catalog", EMPTY_RESOURCE_LIST, resourceResolver, appsResourceProvider);
        Resource testResource = resourceResolver.getResource("store/inventory");
        Assert.assertEquals(appResource, testResource);
        testResource = resourceResolver.getResource("store/catalog");
        Assert.assertEquals(libResource, testResource);
    }

    /**
     * Basic test of mapping functionality, at the moment needs more
     * configuration in the virtual /etc/map.
     *
     * @throws LoginException
     */
    @Test
    public void testMapping() throws LoginException {
        ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(null);
        buildResource("/single/test", EMPTY_RESOURCE_LIST, resourceResolver, resourceProvider);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getScheme()).thenReturn("http");
        Mockito.when(request.getServerPort()).thenReturn(80);
        Mockito.when(request.getServerName()).thenReturn("localhost");
        String path = resourceResolver.map(request,"/single/test?q=123123");
        Assert.assertEquals("/single/test?q=123123", path);
        path = resourceResolver.map(request,"/single/test");
        Assert.assertEquals("/single/test", path);

        // test path mapping without a request.
        path = resourceResolver.map("/single/test");
        Assert.assertEquals("/single/test", path);

        buildResource("/content", EMPTY_RESOURCE_LIST, resourceResolver, resourceProvider);
        path = resourceResolver.map("/content.html");
        Assert.assertEquals("/content.html", path);

    }


    /**
     * Tests list children via the resource (NB, this doesn't really test the
     * resource resolver at all, but validates this unit test.)
     *
     * @throws LoginException
     */
    @Test
    public void testListChildren() throws LoginException {
        ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(null);
        buildResource("/single/test/withchildren", buildChildResources("/single/test/withchildren"), resourceResolver, resourceProvider );


        Resource resource = resourceResolver.getResource("/single/test/withchildren");
        Assert.assertNotNull(resource);

        // test via the resource list children itself, this really just tests this test case.
        Iterator<Resource> resourceIterator = resource.listChildren();
        Assert.assertNotNull(resourceResolver);
        int i = 0;
        while(resourceIterator.hasNext()) {
            Assert.assertEquals("m"+i, resourceIterator.next().getName());
            i++;
        }
        Assert.assertEquals(5, i);
    }

    /**
     * Test listing children via the resource resolver listChildren call.
     * @throws LoginException
     */
    @Test
    public void testResourceResolverListChildren() throws LoginException {
        ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(null);
        buildResource("/single/test/withchildren", buildChildResources("/single/test/withchildren"), resourceResolver, resourceProvider);


        Resource resource = resourceResolver.getResource("/single/test/withchildren");
        Assert.assertNotNull(resource);

        // test via the resource list children itself, this really just tests this test case.
        Iterator<Resource> resourceIterator = resourceResolver.listChildren(resource);
        Assert.assertNotNull(resourceResolver);
        int i = 0;
        while(resourceIterator.hasNext()) {
            Assert.assertEquals("m"+i, resourceIterator.next().getName());
            i++;
        }
        Assert.assertEquals(5,i);
    }

    /**
     * Tests listing children via the resource resolver getChildren call.
     * @throws LoginException
     */
    @Test
    public void testResourceResolverGetChildren() throws LoginException {
        ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(null);
        buildResource("/single/test/withchildren", buildChildResources("/single/test/withchildren"), resourceResolver, resourceProvider);


        Resource resource = resourceResolver.getResource("/single/test/withchildren");
        Assert.assertNotNull(resource);

        // test via the resource list children itself, this really just tests this test case.
        Iterable<Resource> resourceIterator = resourceResolver.getChildren(resource);
        Assert.assertNotNull(resourceResolver);
        int i = 0;
        for(Resource r : resourceIterator) {
            Assert.assertEquals("m"+i, r.getName());
            i++;
        }
        Assert.assertEquals(5,i);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testQueryResources() throws LoginException {
        final int n = 3;
        String[] languages = new String[] {FAKE_QUERY_LANGUAGE};
        Mockito.when(queryProvider.getSupportedLanguages(Mockito.any(ResolveContext.class))).thenReturn(languages);
        Mockito.when(queryProvider.queryResources(Mockito.any(ResolveContext.class), Mockito.any(String.class), Mockito.any(String.class)))
        .thenReturn(buildValueMapCollection(n, "A_").iterator());

        final ResourceResolver rr = resourceResolverFactory.getResourceResolver(null);
        buildResource("/search/test/withchildren", buildChildResources("/search/test/withchildren"), rr, resourceProvider);
        final Iterator<Map<String, Object>> it = rr.queryResources("/search", FAKE_QUERY_LANGUAGE);
        final Set<String> toFind = new HashSet<String>();
        for(int i=0; i < n; i++) {
            toFind.add("A_" + i);
        }

        assertTrue("Expecting non-empty result (" + n + ")", it.hasNext());
        while(it.hasNext()) {
            final Map<String, Object> m = it.next();
            toFind.remove(m.get(PATH));
        }
        assertTrue("Expecting no leftovers (" + n + ") in" + toFind, toFind.isEmpty());
    }

    @Test public void test_versions() throws LoginException {
        ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(null);

        Resource resource = resourceResolver.resolve("/content/test.html;v=1.0");
        Map<String, String> parameters = resource.getResourceMetadata().getParameterMap();
        assertEquals("/content/test.html", resource.getPath());
        assertEquals("test.html", resource.getName());
        assertEquals(Collections.singletonMap("v", "1.0"), parameters);

        resource = resourceResolver.resolve("/content/test;v='1.0'.html");
        parameters = resource.getResourceMetadata().getParameterMap();
        assertEquals("/content/test.html", resource.getPath());
        assertEquals("test.html", resource.getName());
        assertEquals(Collections.singletonMap("v", "1.0"), parameters);

        buildResource("/single/test/withchildren", buildChildResources("/single/test/withchildren"), resourceResolver, resourceProvider);
        resource = resourceResolver.getResource("/single/test/withchildren;v='1.0'");
        assertNotNull(resource);
        assertEquals("/single/test/withchildren", resource.getPath());
        assertEquals("withchildren", resource.getName());
    }
}
