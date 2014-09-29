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
package org.apache.sling.superimposing.impl;

import static org.apache.sling.superimposing.SuperimposingResourceProvider.PROP_SUPERIMPOSE_OVERLAYABLE;
import static org.apache.sling.superimposing.SuperimposingResourceProvider.PROP_SUPERIMPOSE_REGISTER_PARENT;
import static org.apache.sling.superimposing.SuperimposingResourceProvider.PROP_SUPERIMPOSE_SOURCE_PATH;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.superimposing.SuperimposingResourceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

@RunWith(MockitoJUnitRunner.class)
public class SuperimposingManagerImplTest {

    @Mock
    private Dictionary<String, Object> componentContextProperties;
    @Mock
    private ComponentContext componentContext;
    @Mock
    private BundleContext bundleContext;
    @Mock
    private ResourceResolverFactory resourceResolverFactory;
    @Mock
    private ResourceResolver resourceResolver;
    @Mock(answer=Answers.RETURNS_DEEP_STUBS)
    private Session session;
    private List<ServiceRegistration> serviceRegistrations = new ArrayList<ServiceRegistration>();

    private SuperimposingManagerImpl underTest;

    private static final String ORIGINAL_PATH = "/root/path1";
    private static final String SUPERIMPOSED_PATH = "/root/path2";
    private static final String OBSERVATION_PATH = "/root";

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws LoginException {
        when(componentContext.getBundleContext()).thenReturn(bundleContext);
        when(componentContext.getProperties()).thenReturn(componentContextProperties);
        when(componentContextProperties.get(SuperimposingManagerImpl.OBSERVATION_PATHS_PROPERTY)).thenReturn(new String[] { OBSERVATION_PATH });
        when(resourceResolverFactory.getAdministrativeResourceResolver(any(Map.class))).thenReturn(resourceResolver);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);

        // collect a list of all service registrations to validate that they are all unregistered on shutdown
        when(bundleContext.registerService(anyString(), anyObject(), any(Dictionary.class))).thenAnswer(new Answer<ServiceRegistration>() {
            public ServiceRegistration answer(InvocationOnMock invocation) {
                final ServiceRegistration mockRegistration = mock(ServiceRegistration.class);
                serviceRegistrations.add(mockRegistration);
                doAnswer(new Answer() {
                    public Object answer(InvocationOnMock invocation) {
                        return serviceRegistrations.remove(mockRegistration);
                    }
                }).when(mockRegistration).unregister();
                return mockRegistration;
            }
        });

        // simulate absolute path access to properties via session object
        try {
            when(session.itemExists(anyString())).thenAnswer(new Answer<Boolean>() {
                public Boolean answer(InvocationOnMock invocation) throws Throwable {
                    final String absolutePath = (String)invocation.getArguments()[0];
                    final String nodePath = ResourceUtil.getParent(absolutePath);
                    final String propertyName = ResourceUtil.getName(absolutePath);
                    Resource resource = resourceResolver.getResource(nodePath);
                    if (resource!=null) {
                        ValueMap props = resource.adaptTo(ValueMap.class);
                        return props.containsKey(propertyName);
                    }
                    else {
                        return false;
                    }
                }
            });
            when(session.getProperty(anyString())).thenAnswer(new Answer<Property>() {
                public Property answer(InvocationOnMock invocation) throws Throwable {
                    final String absolutePath = (String)invocation.getArguments()[0];
                    final String nodePath = ResourceUtil.getParent(absolutePath);
                    final String propertyName = ResourceUtil.getName(absolutePath);
                    Resource resource = resourceResolver.getResource(nodePath);
                    if (resource!=null) {
                        ValueMap props = resource.adaptTo(ValueMap.class);
                        Object value = props.get(propertyName);
                        if (value==null) {
                            throw new PathNotFoundException();
                        }
                        Property prop = mock(Property.class);
                        when(prop.getName()).thenReturn(propertyName);
                        if (value instanceof String) {
                            when(prop.getString()).thenReturn((String)value);
                        }
                        else if (value instanceof Boolean) {
                            when(prop.getBoolean()).thenReturn((Boolean)value);
                        }
                        return prop;
                    }
                    else {
                        throw new PathNotFoundException();
                    }
                }
            });
        }
        catch (RepositoryException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void initialize(boolean enabled) throws InterruptedException, LoginException, RepositoryException {
        when(componentContextProperties.get(SuperimposingManagerImpl.ENABLED_PROPERTY)).thenReturn(enabled);

        underTest = new SuperimposingManagerImpl().withResourceResolverFactory(resourceResolverFactory);
        underTest.activate(componentContext);

        if (underTest.isEnabled()) {
            // verify observation registration
            verify(session.getWorkspace().getObservationManager()).addEventListener(any(EventListener.class), anyInt(), eq(OBSERVATION_PATH), anyBoolean(), any(String[].class), any(String[].class), anyBoolean());
            // wait until separate initialization thread has finished
            while (!underTest.initialization.isDone()) {
                Thread.sleep(10);
            }
        }
    }

    @After
    public void tearDown() throws RepositoryException {
        underTest.deactivate(componentContext);

        if (underTest.isEnabled()) {
            // verify observation and resource resolver are terminated correctly
            verify(session.getWorkspace().getObservationManager()).removeEventListener(any(EventListener.class));
            verify(resourceResolver).close();
        }

        // make sure all registrations are unregistered on shutdown
        for (ServiceRegistration registration : serviceRegistrations) {
            verify(registration, times(1)).unregister();
        }
    }

    private Resource prepareSuperimposingResource(String superimposedPath, String sourcePath, boolean registerParent, boolean overlayable) {
        Resource resource = mock(Resource.class);
        when(resource.getPath()).thenReturn(superimposedPath);
        ValueMap props = new ValueMapDecorator(new HashMap<String, Object>());
        props.put(PROP_SUPERIMPOSE_SOURCE_PATH, sourcePath);
        props.put(PROP_SUPERIMPOSE_REGISTER_PARENT, registerParent);
        props.put(PROP_SUPERIMPOSE_OVERLAYABLE, overlayable);
        when(resource.adaptTo(ValueMap.class)).thenReturn(props);
        when(resourceResolver.getResource(superimposedPath)).thenReturn(resource);
        return resource;
    }

    private void moveSuperimposedResource(Resource resource, String newPath) {
        String oldPath = resource.getPath();
        when(resource.getPath()).thenReturn(newPath);
        when(resourceResolver.getResource(oldPath)).thenReturn(null);
        when(resourceResolver.getResource(newPath)).thenReturn(resource);
    }

    @Test
    public void testDisabled() throws InterruptedException, LoginException, RepositoryException {
        // make sure that no exception is thrown when service is disabled on activate/deactivate
        initialize(false);

        verifyZeroInteractions(resourceResolverFactory);
        verifyZeroInteractions(bundleContext);
    }

    @Test
    public void testFindAllSuperimposings() throws InterruptedException, LoginException, RepositoryException {
        // prepare a query that returns one existing superimposed resource
        when(componentContextProperties.get(SuperimposingManagerImpl.FINDALLQUERIES_PROPERTY)).thenReturn("syntax|query");
        when(resourceResolver.findResources("query", "syntax")).then(new Answer<Iterator<Resource>>() {
            public Iterator<Resource> answer(InvocationOnMock invocation) {
                return Arrays.asList(new Resource[] {
                        prepareSuperimposingResource(SUPERIMPOSED_PATH, ORIGINAL_PATH, false, false)
                }).iterator();
            }
        });
        initialize(true);

        // ensure the superimposed resource is detected and registered
        Map<String, SuperimposingResourceProvider> providers = underTest.getRegisteredProviders();
        assertEquals(1, providers.size());
        SuperimposingResourceProvider provider = providers.values().iterator().next();
        assertEquals(SUPERIMPOSED_PATH, provider.getRootPath());
        assertEquals(ORIGINAL_PATH, provider.getSourcePath());
        assertFalse(provider.isOverlayable());
        verify(bundleContext).registerService(anyString(), same(provider), any(Dictionary.class));
    }

    private EventIterator prepareNodeCreateEvent(Resource pResource) throws RepositoryException {
        String resourcePath = pResource.getPath();

        Event nodeEvent = mock(Event.class);
        when(nodeEvent.getType()).thenReturn(Event.NODE_ADDED);
        when(nodeEvent.getPath()).thenReturn(resourcePath);

        Event propertyEvent = mock(Event.class);
        when(propertyEvent.getType()).thenReturn(Event.PROPERTY_ADDED);
        when(propertyEvent.getPath()).thenReturn(resourcePath + "/" + SuperimposingResourceProvider.PROP_SUPERIMPOSE_SOURCE_PATH);

        EventIterator eventIterator = mock(EventIterator.class);
        when(eventIterator.hasNext()).thenReturn(true, true, false);
        when(eventIterator.nextEvent()).thenReturn(nodeEvent, propertyEvent);
        return eventIterator;
    }

    private EventIterator prepareNodeChangeEvent(Resource pResource) throws RepositoryException {
        String resourcePath = pResource.getPath();

        Event propertyEvent = mock(Event.class);
        when(propertyEvent.getType()).thenReturn(Event.PROPERTY_CHANGED);
        when(propertyEvent.getPath()).thenReturn(resourcePath + "/" + SuperimposingResourceProvider.PROP_SUPERIMPOSE_SOURCE_PATH);

        EventIterator eventIterator = mock(EventIterator.class);
        when(eventIterator.hasNext()).thenReturn(true, false);
        when(eventIterator.nextEvent()).thenReturn(propertyEvent);
        return eventIterator;
    }

    private EventIterator prepareNodeRemoveEvent(Resource pResource) throws RepositoryException {
        String resourcePath = pResource.getPath();

        Event nodeEvent = mock(Event.class);
        when(nodeEvent.getType()).thenReturn(Event.NODE_REMOVED);
        when(nodeEvent.getPath()).thenReturn(resourcePath);

        EventIterator eventIterator = mock(EventIterator.class);
        when(eventIterator.hasNext()).thenReturn(true, false);
        when(eventIterator.nextEvent()).thenReturn(nodeEvent);
        return eventIterator;
    }

    private EventIterator prepareNodeMoveEvent(Resource pResource, String pOldPath) throws RepositoryException {
        String resourcePath = pResource.getPath();

        Event nodeRemoveEvent = mock(Event.class);
        when(nodeRemoveEvent.getType()).thenReturn(Event.NODE_REMOVED);
        when(nodeRemoveEvent.getPath()).thenReturn(pOldPath);

        Event nodeCreateEvent = mock(Event.class);
        when(nodeCreateEvent.getType()).thenReturn(Event.NODE_ADDED);
        when(nodeCreateEvent.getPath()).thenReturn(resourcePath);

        EventIterator eventIterator = mock(EventIterator.class);
        when(eventIterator.hasNext()).thenReturn(true, true, false);
        when(eventIterator.nextEvent()).thenReturn(nodeRemoveEvent, nodeCreateEvent);
        return eventIterator;
    }

    @Test
    public void testSuperimposedResourceCreateUpdateRemove() throws InterruptedException, LoginException, RepositoryException {
        initialize(true);

        // simulate node create event
        Resource superimposedResource = prepareSuperimposingResource(SUPERIMPOSED_PATH, ORIGINAL_PATH, false, false);
        underTest.onEvent(prepareNodeCreateEvent(superimposedResource));

        // ensure the superimposed resource is detected and registered
        Map<String, SuperimposingResourceProvider> providers = underTest.getRegisteredProviders();
        assertEquals(1, providers.size());
        SuperimposingResourceProvider provider = providers.values().iterator().next();
        assertEquals(SUPERIMPOSED_PATH, provider.getRootPath());
        assertEquals(ORIGINAL_PATH, provider.getSourcePath());
        assertFalse(provider.isOverlayable());
        verify(bundleContext).registerService(anyString(), same(provider), any(Dictionary.class));

        // simulate a change in the original path
        superimposedResource.adaptTo(ValueMap.class).put(PROP_SUPERIMPOSE_SOURCE_PATH, "/other/path");
        underTest.onEvent(prepareNodeChangeEvent(superimposedResource));

        // ensure the superimposed resource update is detected and a new provider instance is registered
        providers = underTest.getRegisteredProviders();
        assertEquals(1, providers.size());
        SuperimposingResourceProvider provider2 = providers.values().iterator().next();
        assertEquals(SUPERIMPOSED_PATH, provider2.getRootPath());
        assertEquals("/other/path", provider2.getSourcePath());
        assertFalse(provider2.isOverlayable());
        verify(bundleContext).registerService(anyString(), same(provider2), any(Dictionary.class));

        // simulate node removal
        underTest.onEvent(prepareNodeRemoveEvent(superimposedResource));

        // ensure provider is removed
        providers = underTest.getRegisteredProviders();
        assertEquals(0, providers.size());
    }

    @Test
    public void testSuperimposedResourceCreateMove() throws InterruptedException, LoginException, RepositoryException {
        when(componentContextProperties.get(SuperimposingManagerImpl.FINDALLQUERIES_PROPERTY)).thenReturn("syntax|query");
        initialize(true);

        // simulate node create event
        final Resource superimposedResource = prepareSuperimposingResource(SUPERIMPOSED_PATH, ORIGINAL_PATH, false, false);
        underTest.onEvent(prepareNodeCreateEvent(superimposedResource));

        // simulate a node move event
        String oldPath = superimposedResource.getPath();
        moveSuperimposedResource(superimposedResource, "/new/path");

        // prepare a query that returns the moved superimposed resource
        when(resourceResolver.findResources("query", "syntax")).then(new Answer<Iterator<Resource>>() {
            public Iterator<Resource> answer(InvocationOnMock invocation) {
                return Arrays.asList(new Resource[] {
                        superimposedResource
                }).iterator();
            }
        });

        underTest.onEvent(prepareNodeMoveEvent(superimposedResource, oldPath));

        // ensure the superimposed resource update is detected and a new provider instance is registered
        Map<String, SuperimposingResourceProvider> providers = underTest.getRegisteredProviders();
        assertEquals(1, providers.size());
        SuperimposingResourceProvider provider = providers.values().iterator().next();
        assertEquals("/new/path", provider.getRootPath());
        assertEquals(ORIGINAL_PATH, provider.getSourcePath());
        assertFalse(provider.isOverlayable());
        verify(bundleContext).registerService(anyString(), same(provider), any(Dictionary.class));
    }

}
