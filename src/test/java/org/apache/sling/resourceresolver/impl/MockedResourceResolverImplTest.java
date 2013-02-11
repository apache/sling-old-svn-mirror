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

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This tests the ResourceResolver using mocks. The Unit test is in addition to
 * ResourceResolverImplTest which covers API conformance more than it covers all
 * code paths.
 */
public class MockedResourceResolverImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockedResourceResolverImplTest.class);

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
        Resource etcMapResource = buildResource("/etc/map", buildMapIterable());
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
        ArgumentCaptor<Dictionary> propertiesCaptor = ArgumentCaptor.forClass(Dictionary.class);
        Mockito.verify(bundleContext, Mockito.atLeastOnce()).registerService(
            classesCaptor.capture(), serviceCaptor.capture(),
            propertiesCaptor.capture());

        int si = 0;
        List<Object> serviceList = serviceCaptor.getAllValues();
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

    private Iterable<Resource> buildMapIterable() {
        List<Resource> mappingResources = new ArrayList<Resource>();
        mappingResources.add(buildResource("/etc/map/m1", EMPTY_RESOURCE_LIST));
        mappingResources.add(buildResource("/etc/map/m2", EMPTY_RESOURCE_LIST));
        mappingResources.add(buildResource("/etc/map/m3", EMPTY_RESOURCE_LIST));
        return mappingResources;
    }

    private Resource buildResource(String fullpath, Iterable<Resource> children) {
        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(resource.getName()).thenReturn(getName(fullpath));
        Mockito.when(resource.getPath()).thenReturn(fullpath);
        ResourceMetadata resourceMetadata = new ResourceMetadata();
        Mockito.when(resource.getResourceMetadata()).thenReturn(resourceMetadata);
        Mockito.when(resource.listChildren()).thenReturn(children.iterator());
        return resource;
    }
        

    private String getName(String fullpath) {
        int n = fullpath.lastIndexOf("/");
        return fullpath.substring(n);
    }

    private Map<String, Object> buildResourceProviderProperties(String servicePID, long serviceID, String[] roots) {
        Map<String, Object> resourceProviderProperties = new HashMap<String, Object>();
        resourceProviderProperties.put(Constants.SERVICE_PID, servicePID);
        resourceProviderProperties.put(Constants.SERVICE_ID, serviceID);
        resourceProviderProperties.put(Constants.SERVICE_VENDOR, "Apache");
        resourceProviderProperties.put(Constants.SERVICE_DESCRIPTION,
            "Dummy Provider");
        resourceProviderProperties.put(ResourceProvider.ROOTS, roots);
        return resourceProviderProperties;
    }

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

    @Test
    public void testGetResolver() throws LoginException {
        ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(null);
        Assert.assertNotNull(resourceResolver);
        Map<String, Object> authenticationInfo = new HashMap<String, Object>();
        resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(authenticationInfo);
        Assert.assertNotNull(resourceResolver);
    }

    @Test
    public void testGetAuthenticatedResolve() throws LoginException {
        ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
        Assert.assertNotNull(resourceResolver);
        Map<String, Object> authenticationInfo = new HashMap<String, Object>();

        resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(authenticationInfo);
        Assert.assertNotNull(resourceResolver);
    }

    @Test
    public void testGetResource() throws LoginException {
        ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(null);
        Assert.assertNotNull(resourceResolver);
        Resource singleResource = buildResource("/sling/test", EMPTY_RESOURCE_LIST);
        Mockito.when(
            resourceProvider.getResource(Mockito.any(ResourceResolver.class),
                Mockito.eq("/single/test"))).thenReturn(singleResource);
        Resource resource = resourceResolver.getResource("/single/test");
        Assert.assertEquals(singleResource, resource);
    }
    @Test
    public void testGetFactoryResource() throws LoginException {
        ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(null);
        Assert.assertNotNull(resourceResolver);
        Resource factoryResource = buildResource("/factory/test", EMPTY_RESOURCE_LIST);
        Mockito.when(
            factoryResourceProvider.getResource(
                Mockito.any(ResourceResolver.class),
                Mockito.eq("/factory/test"))).thenReturn(factoryResource);
        Resource resource = resourceResolver.getResource("/factory/test");
        Assert.assertEquals(factoryResource,resource);
    }
    
}
