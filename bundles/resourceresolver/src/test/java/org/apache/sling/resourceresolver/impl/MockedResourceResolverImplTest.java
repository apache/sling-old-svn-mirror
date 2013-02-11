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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.QueriableResourceProvider;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;

/**
 * This tests the ResourceResolver using mocks. The Unit test is in addition to
 * ResourceResolverImplTest which covers API conformance more than it covers all
 * code paths.
 */
// TODO: Configure mapping to react correctly.
// TODO: test external redirect.
// TODO: Map to URI
// TODO: Statresource
// TODO: relative resource.
// TODO: SLING-864, path with . in it.
// TODO: search path eg text/html which will search on /apps/text/html and then /libs/text/html
public class MockedResourceResolverImplTest {

    private static final List<Resource> EMPTY_RESOURCE_LIST = new ArrayList<Resource>();

    private ResourceResolverFactoryActivator activator;

    @Mock
    private ComponentContext componentContext;

    @Mock
    private EventAdmin eventAdmin;

    @Mock
    private BundleContext bundleContext;

    private Map<String, Object> services = new HashMap<String, Object>();

    private Map<String, Object> serviceProperties = new HashMap<String, Object>();

    private ResourceResolverFactoryImpl resourceResolverFactory;

    @Mock
    private ResourceProvider resourceProvider;

    @Mock
    private ResourceProviderFactory resourceProviderFactory;

    /**
     * the factory creates these.
     */
    @Mock
    private ResourceProvider factoryResourceProvider;

    @Mock
    private ResourceProvider factoryAdministrativeResourceProvider;

    @Mock
    private ResourceProvider mappingResourceProvider;


    public MockedResourceResolverImplTest() {
        MockitoAnnotations.initMocks(this);
    }

    @SuppressWarnings("unchecked")
    @Before
    public void before() throws LoginException {
        activator = new ResourceResolverFactoryActivator();

        Mockito.when(componentContext.getProperties()).thenReturn(buildBundleProperties());
        Mockito.when(componentContext.getBundleContext()).thenReturn(
            bundleContext);
        activator.eventAdmin = eventAdmin;

        activator.bindResourceProvider(resourceProvider,
            buildResourceProviderProperties("org.apache.sling.resourceresolver.impl.DummyTestProvider", 
                10L, 
                new String[] { "/single" }));
        
        // setup mapping resources at /etc/map to exercise vanity etc.
        Resource etcMapResource = buildResource("/etc/map", buildChildResources("/etc/map"));
        Mockito.when(mappingResourceProvider.getResource(Mockito.any(ResourceResolver.class), Mockito.eq("/etc/map"))).thenReturn(etcMapResource);
        
        activator.bindResourceProvider(mappingResourceProvider,
            buildResourceProviderProperties("org.apache.sling.resourceresolver.impl.MapProvider",
                11L,
                new String[] { "/etc" }));

        // bind a ResourceProviderFactory to satidy the pre-requirements
        Mockito.when(resourceProviderFactory.getResourceProvider(null)).thenReturn(
            factoryResourceProvider);
        Mockito.when(
            resourceProviderFactory.getResourceProvider(Mockito.anyMap())).thenReturn(
            factoryResourceProvider);
        Mockito.when(
            resourceProviderFactory.getAdministrativeResourceProvider(null)).thenReturn(
            factoryAdministrativeResourceProvider);
        Mockito.when(
            resourceProviderFactory.getAdministrativeResourceProvider(Mockito.anyMap())).thenReturn(
            factoryAdministrativeResourceProvider);
        
        activator.bindResourceProviderFactory(resourceProviderFactory,
            buildResourceProviderProperties("org.apache.sling.resourceresolver.impl.DummyTestProviderFactory",
                12L,
                new String[] { "/factory" } ));

        // activate the components.
        activator.activate(componentContext);

        // extract any services that were registered into a map.
        ArgumentCaptor<String> classesCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> serviceCaptor = ArgumentCaptor.forClass(Object.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Dictionary> propertiesCaptor = ArgumentCaptor.forClass(Dictionary.class);
        Mockito.verify(bundleContext, Mockito.atLeastOnce()).registerService(
            classesCaptor.capture(), serviceCaptor.capture(),
            propertiesCaptor.capture());

        int si = 0;
        List<Object> serviceList = serviceCaptor.getAllValues();
        @SuppressWarnings({ "unused", "rawtypes" })
        List<Dictionary> servicePropertiesList = propertiesCaptor.getAllValues();
        for (String serviceName : classesCaptor.getAllValues()) {
            services.put(serviceName, serviceList.get(si));
            serviceProperties.put(serviceName, serviceProperties.get(si));
            si++;
        }
        // verify that a ResourceResolverFactoryImpl was created and registered.
        Assert.assertNotNull(services.get(ResourceResolverFactory.class.getName()));
        ResourceResolverFactory rrf = (ResourceResolverFactory) services.get(ResourceResolverFactory.class.getName());
        Assert.assertTrue(rrf instanceof ResourceResolverFactoryImpl);
        resourceResolverFactory = (ResourceResolverFactoryImpl) rrf;
    }

    @After
    public void after() {
        activator.unbindResourceProvider(resourceProvider,
            buildResourceProviderProperties("org.apache.sling.resourceresolver.impl.DummyTestProvider",
                                            10L,
                                            new String[] { "/single" }));
        activator.unbindResourceProvider(mappingResourceProvider,
            buildResourceProviderProperties("org.apache.sling.resourceresolver.impl.MapProvider",
                                            11L,
                                            new String[] { "/etc" }));
        activator.bindResourceProviderFactory(resourceProviderFactory,
            buildResourceProviderProperties("org.apache.sling.resourceresolver.impl.DummyTestProviderFactory",
                                            12L,
                                            new String[] { "/factory" } ));

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
        return buildResource(fullpath, children, null);
    }

    /**
     * Build a resource with path, children and resource resolver.
     * @param fullpath
     * @param children
     * @param resourceResolver
     * @return
     */
    private Resource buildResource(String fullpath, Iterable<Resource> children, ResourceResolver resourceResolver) {
        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(resource.getName()).thenReturn(getName(fullpath));
        Mockito.when(resource.getPath()).thenReturn(fullpath);
        ResourceMetadata resourceMetadata = new ResourceMetadata();
        Mockito.when(resource.getResourceMetadata()).thenReturn(resourceMetadata);
        Mockito.when(resource.listChildren()).thenReturn(children.iterator());
        Mockito.when(resource.getResourceResolver()).thenReturn(resourceResolver);
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
     * Build properties for a resource provider.
     * @param servicePID
     * @param serviceID
     * @param roots
     * @return
     */
    private Map<String, Object> buildResourceProviderProperties(String servicePID, long serviceID, String[] roots) {
        Map<String, Object> resourceProviderProperties = new HashMap<String, Object>();
        resourceProviderProperties.put(Constants.SERVICE_PID, servicePID);
        resourceProviderProperties.put(Constants.SERVICE_ID, serviceID);
        resourceProviderProperties.put(Constants.SERVICE_VENDOR, "Apache");
        resourceProviderProperties.put(Constants.SERVICE_DESCRIPTION,
            "Dummy Provider");
        resourceProviderProperties.put(QueriableResourceProvider.LANGUAGES,
                new String[] { "lang1"} );


        resourceProviderProperties.put(ResourceProvider.ROOTS, roots);
        return resourceProviderProperties;
    }

    /**
     * build a properties for a resource resolver bundle.
     * @return
     */
    private Dictionary<String, Object> buildBundleProperties() {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("resource.resolver.virtual", new String[] { "/:/" });
        properties.put("resource.resolver.mapping", new String[] { "/:/",
            "/content/:/", "/system/docroot/:/" });
        properties.put("resource.resolver.allowDirect", true);
        properties.put("resource.resolver.searchpath", new String[] { "/apps",
            "/libs" });
        properties.put("resource.resolver.manglenamespaces", true);
        properties.put("resource.resolver.map.location", "/etc/map");
        properties.put("resource.resolver.default.vanity.redirect.status", 302);
        properties.put(
            "resource.resolver.required.providers",
            new String[] { "org.apache.sling.resourceresolver.impl.DummyTestProvider" });
        properties.put(Constants.SERVICE_VENDOR, "Apache");
        properties.put(Constants.SERVICE_DESCRIPTION, "Testing");
        return properties;
    }

    /**
     * Register a resource for testing purposes at a path, with a provider, with children.
     * @param resourceResolver
     * @param targetResourceProvider
     * @param path
     * @param children
     * @return
     */
    private Resource registerTestResource(ResourceResolver resourceResolver, ResourceProvider targetResourceProvider, String path,  Iterable<Resource> children) {
        Resource resource = buildResource(path, children, resourceResolver);
        Mockito.when(
            targetResourceProvider.getResource(
                Mockito.any(ResourceResolver.class),
                Mockito.eq(path))).thenReturn(resource);
        Mockito.when(targetResourceProvider.listChildren(resource)).thenReturn(children.iterator());
        return resource;
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
        Resource singleResource = registerTestResource(resourceResolver, resourceProvider, "/single/test", EMPTY_RESOURCE_LIST);
        Resource resource = resourceResolver.getResource("/single/test");
        Assert.assertEquals(singleResource, resource);
    }
    /**
     * Test getResource for a resource provided by a factory provider.
     * @throws LoginException
     */
    @Test
    public void testGetFactoryResource() throws LoginException {
        ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(null);
        Assert.assertNotNull(resourceResolver);

        Resource factoryResource = registerTestResource(resourceResolver, factoryResourceProvider, "/factory/test", EMPTY_RESOURCE_LIST);
        Resource resource = resourceResolver.getResource("/factory/test");
        Assert.assertEquals(factoryResource, resource);
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
        registerTestResource(resourceResolver, factoryResourceProvider, "/factory/test", EMPTY_RESOURCE_LIST);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getScheme()).thenReturn("http");
        Mockito.when(request.getServerPort()).thenReturn(8080);
        Mockito.when(request.getServerName()).thenReturn("localhost");
        String path = resourceResolver.map(request,"/factory/test?q=123123");
        Assert.assertEquals("/factory/test?q=123123", path);
        path = resourceResolver.map(request,"/factory/test");
        Assert.assertEquals("/factory/test", path);

        // test path mapping without a request.
        path = resourceResolver.map("/factory/test");
        Assert.assertEquals("/factory/test", path);


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
        registerTestResource(resourceResolver, resourceProvider, "/single/test/withchildren", buildChildResources("/single/test/withchildren"));


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
        registerTestResource(resourceResolver, resourceProvider, "/single/test/withchildren", buildChildResources("/single/test/withchildren"));


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
        registerTestResource(resourceResolver, resourceProvider, "/single/test/withchildren", buildChildResources("/single/test/withchildren"));


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

}
