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
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.impl.injectors.ChildResourceInjector;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

@RunWith(MockitoJUnitRunner.class)
public class InvalidAdaptationsTest {

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
        factory.bindInjector(new ValueMapInjector(), new ServicePropertiesMap(1, 1));
        factory.bindInjector(new ChildResourceInjector(), new ServicePropertiesMap(2, 0));
    }

    @Test
    public void testNonModelClass() {
        Map<String, Object> emptyMap = Collections.<String, Object> emptyMap();

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(emptyMap));

        assertNull(factory.getAdapter(res, NonModel.class));
    }

    @Test
    public void testWrongAdaptableClass() {
        Map<String, Object> emptyMap = Collections.<String, Object> emptyMap();

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(emptyMap));

        assertNull(factory.getAdapter(res, RequestModel.class));
    }

    private class NonModel {
    }

    @Model(adaptables = SlingHttpServletRequest.class)
    private class RequestModel {
    }

}
