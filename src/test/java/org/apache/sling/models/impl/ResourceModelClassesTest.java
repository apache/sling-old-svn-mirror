/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.models.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.impl.injectors.ChildResourceInjector;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.testmodels.classes.ChildModel;
import org.apache.sling.models.testmodels.classes.ChildResourceModel;
import org.apache.sling.models.testmodels.classes.ChildValueMapModel;
import org.apache.sling.models.testmodels.classes.ParentModel;
import org.apache.sling.models.testmodels.classes.ResourceModelWithRequiredField;
import org.apache.sling.models.testmodels.classes.SimplePropertyModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;

@RunWith(MockitoJUnitRunner.class)
public class ResourceModelClassesTest {

    @Mock
    private ComponentContext componentCtx;

    @Mock
    private BundleContext bundleContext;

    private ModelAdapterFactory factory;

    @Before
    public void setup() {
        when(componentCtx.getBundleContext()).thenReturn(bundleContext);

        factory = new ModelAdapterFactory();
        factory.activate(componentCtx);
        factory.bindInjector(new ValueMapInjector(),
                Collections.<String, Object> singletonMap(Constants.SERVICE_ID, 1L));
        factory.bindInjector(new ChildResourceInjector(),
                Collections.<String, Object> singletonMap(Constants.SERVICE_ID, 0L));
    }

    @Test
    public void testSimplePropertyModel() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("first", "first-value");
        map.put("third", "third-value");
        map.put("intProperty", new Integer(3));
        map.put("arrayProperty", new String[] { "three", "four" });
        ValueMap vm = new ValueMapDecorator(map);

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        SimplePropertyModel model = factory.getAdapter(res, SimplePropertyModel.class);
        assertNotNull(model);
        assertEquals("first-value", model.getFirst());
        assertNull(model.getSecond());
        assertEquals("third-value", model.getThirdProperty());
        assertEquals(3, model.getIntProperty());

        String[] array = model.getArrayProperty();
        assertEquals(2, array.length);
        assertEquals("three", array[0]);

        assertTrue(model.isPostConstructCalled());
    }

    @Test
    public void testRequiredPropertyModel() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("first", "first-value");
        map.put("third", "third-value");
        ValueMap vm = spy(new ValueMapDecorator(map));

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        ResourceModelWithRequiredField model = factory.getAdapter(res, ResourceModelWithRequiredField.class);
        assertNull(model);

        verify(vm).get("required", String.class);
    }

    @Test
    public void testChildResource() {
        Resource child = mock(Resource.class);

        Resource res = mock(Resource.class);
        when(res.getChild("firstChild")).thenReturn(child);

        ChildResourceModel model = factory.getAdapter(res, ChildResourceModel.class);
        assertNotNull(model);
        assertEquals(child, model.getFirstChild());
    }

    @Test
    public void testChildValueMap() {
        ValueMap map = ValueMapDecorator.EMPTY;

        Resource child = mock(Resource.class);
        when(child.adaptTo(ValueMap.class)).thenReturn(map);

        Resource res = mock(Resource.class);
        when(res.getChild("firstChild")).thenReturn(child);

        ChildValueMapModel model = factory.getAdapter(res, ChildValueMapModel.class);
        assertNotNull(model);
        assertEquals(map, model.getFirstChild());
    }

    @Test
    public void testChildModel() {
        Object value = RandomStringUtils.randomAlphabetic(10);
        Map<String, Object> props = Collections.singletonMap("property", value);
        ValueMap map = new ValueMapDecorator(props);

        final Resource child = mock(Resource.class);
        when(child.adaptTo(ValueMap.class)).thenReturn(map);
        when(child.adaptTo(ChildModel.class)).thenAnswer(new Answer<ChildModel>() {

            @Override
            public ChildModel answer(InvocationOnMock invocation) throws Throwable {
                return factory.getAdapter(child, ChildModel.class);
            }

        });

        Resource res = mock(Resource.class);
        when(res.getChild("firstChild")).thenReturn(child);

        ParentModel model = factory.getAdapter(res, ParentModel.class);
        assertNotNull(model);

        ChildModel childModel = model.getFirstChild();
        assertNotNull(childModel);
        assertEquals(value, childModel.getProperty());
    }

}
