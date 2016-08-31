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
package org.apache.sling.models.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.spi.ImplementationPicker;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;

@RunWith(MockitoJUnitRunner.class)
public class AdapterImplementationsTest {

    private static final Class<?> SAMPLE_ADAPTER = Comparable.class;
    private static final Object SAMPLE_ADAPTABLE = new Object();    

    private AdapterImplementations underTest;

    @Mock
    private Resource resource;

    @Mock
    private Resource childResource;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private ResourceResolver resourceResolver;
    
    @Before
    public void setUp() {
        underTest = new AdapterImplementations();
        underTest.setImplementationPickers(Arrays.asList(new ImplementationPicker[] {
            new FirstImplementationPicker()
        }));
    }
    
    @Test
    public void testNoMapping() {
        assertNull(underTest.lookup(SAMPLE_ADAPTER, SAMPLE_ADAPTABLE));
        
        // make sure this raises no exception
        underTest.remove(SAMPLE_ADAPTER.getName(), String.class.getName());
    }
    
    @Test
    public void testSingleMapping() {
        underTest.add(SAMPLE_ADAPTER, String.class);
        
        assertEquals(String.class, underTest.lookup(SAMPLE_ADAPTER, SAMPLE_ADAPTABLE).getType());
        
        underTest.remove(SAMPLE_ADAPTER.getName(), String.class.getName());

        assertNull(underTest.lookup(SAMPLE_ADAPTER, SAMPLE_ADAPTABLE));
    }

    @Test
    public void testMultipleMappings() {
        underTest.add(SAMPLE_ADAPTER, String.class);
        underTest.add(SAMPLE_ADAPTER, Integer.class);
        underTest.add(SAMPLE_ADAPTER, Long.class);
        
        assertEquals(Integer.class, underTest.lookup(SAMPLE_ADAPTER, SAMPLE_ADAPTABLE).getType());
        
        underTest.remove(SAMPLE_ADAPTER.getName(), Integer.class.getName());

        assertEquals(Long.class, underTest.lookup(SAMPLE_ADAPTER, SAMPLE_ADAPTABLE).getType());

        underTest.remove(SAMPLE_ADAPTER.getName(), Long.class.getName());
        underTest.remove(SAMPLE_ADAPTER.getName(), String.class.getName());
        
        assertNull(underTest.lookup(SAMPLE_ADAPTER, SAMPLE_ADAPTABLE));
    }
    
    @Test
    public void testRemoveAll() {
        underTest.add(SAMPLE_ADAPTER, String.class);
        underTest.add(SAMPLE_ADAPTER, Integer.class);
        underTest.add(SAMPLE_ADAPTER, Long.class);
        
        underTest.removeAll();
        
        assertNull(underTest.lookup(SAMPLE_ADAPTER, SAMPLE_ADAPTABLE));
    }
    
    @Test
    public void testMultipleImplementationPickers() {
        underTest.setImplementationPickers(Arrays.asList(
            new NoneImplementationPicker(),
            new LastImplementationPicker(),
            new FirstImplementationPicker()
        ));

        underTest.add(SAMPLE_ADAPTER, String.class);
        underTest.add(SAMPLE_ADAPTER, Integer.class);
        underTest.add(SAMPLE_ADAPTER, Long.class);
        
        assertEquals(String.class, underTest.lookup(SAMPLE_ADAPTER, SAMPLE_ADAPTABLE).getType());
    }
    
    @Test
    public void testSimpleModel() {
        underTest.add(SAMPLE_ADAPTER, SAMPLE_ADAPTER);
        
        assertEquals(SAMPLE_ADAPTER, underTest.lookup(SAMPLE_ADAPTER, SAMPLE_ADAPTABLE).getType());
    }

    @Test
    public void testResourceTypeRegistrationForResource() {
        when(resource.getResourceType()).thenReturn("sling/rt/one");
        when(resource.getResourceResolver()).thenReturn(resourceResolver);
        when(childResource.getResourceType()).thenReturn("sling/rt/child");
        when(childResource.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getParentResourceType(resource)).thenReturn(null);
        when(resourceResolver.getParentResourceType(childResource)).thenReturn("sling/rt/one");
        when(resourceResolver.getSearchPath()).thenReturn(new String[] { "/apps/", "/libs/" });

        // ensure we don't have any registrations for 'sling/rt/one'
        assertNull(underTest.getModelClassForResource(resource));
        assertNull(underTest.getModelClassForResource(childResource));

        // now add a mapping for Resource -> String
        BundleContext bundleContext = MockOsgi.newBundleContext();
        underTest.registerModelToResourceType(bundleContext.getBundle(), "sling/rt/one", Resource.class, String.class);
        assertEquals(String.class, underTest.getModelClassForResource(resource));
        assertEquals(String.class, underTest.getModelClassForResource(childResource));

        // ensure that trying to reregister the resource type is a no-op
        BundleContext secondBundleContext = MockOsgi.newBundleContext();
        underTest.registerModelToResourceType(secondBundleContext.getBundle(), "sling/rt/one", Resource.class, Integer.class);
        assertEquals(String.class, underTest.getModelClassForResource(resource));
        assertEquals(String.class, underTest.getModelClassForResource(childResource));

        underTest.removeResourceTypeBindings(bundleContext.getBundle());
        assertNull(underTest.getModelClassForResource(resource));
        assertNull(underTest.getModelClassForResource(childResource));
    }

    @Test
    public void testResourceTypeRegistrationForAbsolutePath() {
        when(resource.getResourceType()).thenReturn("sling/rt/one");
        when(resource.getResourceResolver()).thenReturn(resourceResolver);
        when(childResource.getResourceType()).thenReturn("sling/rt/child");
        when(childResource.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getParentResourceType(resource)).thenReturn(null);
        when(resourceResolver.getParentResourceType(childResource)).thenReturn("sling/rt/one");
        when(resourceResolver.getSearchPath()).thenReturn(new String[] { "/apps/", "/libs/" });

        // ensure we don't have any registrations for 'sling/rt/one'
        assertNull(underTest.getModelClassForResource(resource));
        assertNull(underTest.getModelClassForResource(childResource));

        // now add a mapping for Resource -> String
        BundleContext bundleContext = MockOsgi.newBundleContext();
        underTest.registerModelToResourceType(bundleContext.getBundle(), "/apps/sling/rt/one", Resource.class, String.class);
        assertEquals(String.class, underTest.getModelClassForResource(resource));
        assertEquals(String.class, underTest.getModelClassForResource(childResource));

        underTest.removeResourceTypeBindings(bundleContext.getBundle());
        assertNull(underTest.getModelClassForResource(resource));
        assertNull(underTest.getModelClassForResource(childResource));
    }

    @Test
    public void testResourceTypeRegistrationForResourceHavingAbsolutePath() {
        when(resource.getResourceType()).thenReturn("/apps/sling/rt/one");
        when(resource.getResourceResolver()).thenReturn(resourceResolver);
        when(childResource.getResourceType()).thenReturn("/apps/sling/rt/child");
        when(childResource.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getParentResourceType(resource)).thenReturn(null);
        when(resourceResolver.getParentResourceType(childResource)).thenReturn("/apps/sling/rt/one");
        when(resourceResolver.getSearchPath()).thenReturn(new String[] { "/apps/", "/libs/" });

        // ensure we don't have any registrations for 'sling/rt/one'
        assertNull(underTest.getModelClassForResource(resource));
        assertNull(underTest.getModelClassForResource(childResource));

        // now add a mapping for Resource -> String
        BundleContext bundleContext = MockOsgi.newBundleContext();
        underTest.registerModelToResourceType(bundleContext.getBundle(), "sling/rt/one", Resource.class, String.class);
        assertEquals(String.class, underTest.getModelClassForResource(resource));
        assertEquals(String.class, underTest.getModelClassForResource(childResource));

        underTest.removeResourceTypeBindings(bundleContext.getBundle());
        assertNull(underTest.getModelClassForResource(resource));
        assertNull(underTest.getModelClassForResource(childResource));
    }

    @Test
    public void testResourceTypeRegistrationForRequest() {
        when(resource.getResourceType()).thenReturn("sling/rt/one");
        when(resource.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getParentResourceType(resource)).thenReturn(null);
        when(resourceResolver.getSearchPath()).thenReturn(new String[] { "/apps/", "/libs/" });
        when(request.getResource()).thenReturn(resource);

        // ensure we don't have any registrations for 'sling/rt/one'
        assertNull(underTest.getModelClassForRequest(request));
        assertNull(underTest.getModelClassForResource(resource));

        // now add a mapping for SlingHttpServletRequest -> String
        BundleContext bundleContext = MockOsgi.newBundleContext();
        underTest.registerModelToResourceType(bundleContext.getBundle(), "sling/rt/one", SlingHttpServletRequest.class, String.class);
        underTest.registerModelToResourceType(bundleContext.getBundle(), "sling/rt/one", Resource.class, Integer.class);
        assertEquals(String.class, underTest.getModelClassForRequest(request));
        assertEquals(Integer.class, underTest.getModelClassForResource(resource));

        // ensure that trying to reregister the resource type is a no-op
        BundleContext secondBundleContext = MockOsgi.newBundleContext();
        underTest.registerModelToResourceType(secondBundleContext.getBundle(), "sling/rt/one", SlingHttpServletRequest.class, Integer.class);
        assertEquals(String.class, underTest.getModelClassForRequest(request));

        underTest.removeResourceTypeBindings(bundleContext.getBundle());
        assertNull(underTest.getModelClassForRequest(request));
        assertNull(underTest.getModelClassForResource(resource));
    }
    
    static final class NoneImplementationPicker implements ImplementationPicker {
        @Override
        public Class<?> pick(Class<?> adapterType, Class<?>[] implementationsTypes, Object adaptable) {
            return null;
        }        
    }
    
    static final class LastImplementationPicker implements ImplementationPicker {
        @Override
        public Class<?> pick(Class<?> adapterType, Class<?>[] implementationsTypes, Object adaptable) {
            return implementationsTypes[implementationsTypes.length - 1];
        }        
    }
    
}
