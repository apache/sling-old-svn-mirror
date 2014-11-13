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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.impl.injectors.ResourcePathInjector;
import org.apache.sling.models.impl.injectors.SelfInjector;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.testmodels.classes.ResourcePathAllOptionalModel;
import org.apache.sling.models.testmodels.classes.ResourcePathModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

@RunWith(MockitoJUnitRunner.class)
public class ResourcePathInjectionTest {

    @Mock
    private ComponentContext componentCtx;

    @Mock
    private BundleContext bundleContext;

    private ModelAdapterFactory factory;

    @Mock
    private Resource adaptable;

    @Mock
    private Resource byPathResource;

    @Mock
    private Resource byPathResource2;

    @Mock
    private Resource byPropertyValueResource;

    @Mock
    private Resource byPropertyValueResource2;

    @Mock
    private ResourceResolver resourceResolver;

    @Before
    public void setup() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("propertyContainingAPath", "/some/other/path");
        map.put("anotherPropertyContainingAPath", "/some/other/path2");

        ValueMap properties = new ValueMapDecorator(map);

        when(componentCtx.getBundleContext()).thenReturn(bundleContext);
        when(componentCtx.getProperties()).thenReturn(new Hashtable<String, Object>());

        when(adaptable.getResourceResolver()).thenReturn(resourceResolver);
        when(adaptable.adaptTo(ValueMap.class)).thenReturn(properties);

        when(resourceResolver.getResource("/some/path")).thenReturn(byPathResource);
        when(resourceResolver.getResource("/some/path2")).thenReturn(byPathResource2);
        when(resourceResolver.getResource("/some/other/path")).thenReturn(byPropertyValueResource);
        when(resourceResolver.getResource("/some/other/path2")).thenReturn(byPropertyValueResource2);

        factory = new ModelAdapterFactory();
        factory.activate(componentCtx);
        factory.bindInjector(new SelfInjector(), new ServicePropertiesMap(1, Integer.MAX_VALUE));
        factory.bindInjector(new ValueMapInjector(), new ServicePropertiesMap(2, 2000));
        factory.bindInjector(new ResourcePathInjector(), new ServicePropertiesMap(3, 2500));
        factory.bindStaticInjectAnnotationProcessorFactory(new ResourcePathInjector(), new ServicePropertiesMap(3, 2500));
    }

    @Test
    public void testPathInjection() {
        ResourcePathModel model = factory.getAdapter(adaptable, ResourcePathModel.class);
        assertNotNull(model);
        assertEquals(byPathResource, model.getFromPath());
        assertEquals(byPropertyValueResource, model.getByDerefProperty());
        assertEquals(byPathResource2, model.getFromPath2());
        assertEquals(byPropertyValueResource2, model.getByDerefProperty2());
    }

    @Test
    public void testPathInjectionWithNonResourceAdaptable() {
        SlingHttpServletRequest nonResourceAdaptable = mock(SlingHttpServletRequest.class);
        ResourcePathModel model = factory.getAdapter(nonResourceAdaptable, ResourcePathModel.class);
        // should be null because mandatory fields could not be injected
        assertNull(model);
    }

    @Test
    public void testOptionalPathInjectionWithNonResourceAdaptable() {
        SlingHttpServletRequest nonResourceAdaptable = mock(SlingHttpServletRequest.class);
        ResourcePathAllOptionalModel model = factory.getAdapter(nonResourceAdaptable, ResourcePathAllOptionalModel.class);
        // should not be null because resource paths fields are optional
        assertNotNull(model);
        // but the field itself are null
        assertNull(model.getFromPath());
        assertNull(model.getByDerefProperty());
        assertNull(model.getFromPath2());
        assertNull(model.getByDerefProperty2());
   }

}