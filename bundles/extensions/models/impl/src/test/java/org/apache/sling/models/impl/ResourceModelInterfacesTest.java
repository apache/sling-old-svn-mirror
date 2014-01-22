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

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.testmodels.classes.ResourceModelWithRequiredField;
import org.apache.sling.models.testmodels.interfaces.SimplePropertyModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;

@RunWith(MockitoJUnitRunner.class)
public class ResourceModelInterfacesTest {

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
                Collections.<String, Object> singletonMap(Constants.SERVICE_ID, 0L));
    }

    @Test
    public void testSimplePropertyModel() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("first", "first-value");
        map.put("third", "third-value");
        map.put("fourth", true);
        ValueMap vm = new ValueMapDecorator(map);

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        SimplePropertyModel model = factory.getAdapter(res, SimplePropertyModel.class);
        assertNotNull(model);
        assertEquals("first-value", model.getFirst());
        assertNull(model.getSecond());
        assertEquals("third-value", model.getThirdProperty());
        assertTrue(model.isFourth());
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

}
