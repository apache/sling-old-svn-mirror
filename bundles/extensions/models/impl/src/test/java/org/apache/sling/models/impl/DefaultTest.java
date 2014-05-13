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

import java.util.Arrays;
import java.util.Collections;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.testmodels.classes.DefaultPrimitivesModel;
import org.apache.sling.models.testmodels.classes.DefaultStringModel;
import org.apache.sling.models.testmodels.classes.DefaultWrappersModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;

@RunWith(MockitoJUnitRunner.class)
public class DefaultTest {

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
    public void testDefaultStringValue() {
        ValueMap vm = new ValueMapDecorator(Collections.<String, Object>emptyMap());

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        DefaultStringModel model = factory.getAdapter(res, DefaultStringModel.class);
        assertNotNull(model);
        assertEquals("firstDefault", model.getFirstProperty());
        assertEquals(2, model.getSecondProperty().length);
    }

    @Test
    public void testDefaultPrimitives() {
        ValueMap vm = new ValueMapDecorator(Collections.<String, Object>emptyMap());

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        DefaultPrimitivesModel model = factory.getAdapter(res, DefaultPrimitivesModel.class);
        assertNotNull(model);

        assertEquals(true, model.getBooleanProperty());
        // we need to wait for JUnit 4.12 for this assertArrayEquals to be working on primitive boolean arrays, https://github.com/junit-team/junit/issues/86!
        assertTrue(Arrays.equals(new boolean[] { true, true }, model.getBooleanArrayProperty()));

        assertEquals(1L, model.getLongProperty());
        assertArrayEquals(new long[] { 1L, 1L }, model.getLongArrayProperty());
    }

    @Test
    public void testDefaultWrappers() {
        ValueMap vm = new ValueMapDecorator(Collections.<String, Object>emptyMap());

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        DefaultWrappersModel model = factory.getAdapter(res, DefaultWrappersModel.class);
        assertNotNull(model);

        assertEquals(Boolean.valueOf(true), model.getBooleanWrapperProperty());
        // we need to wait for JUnit 4.12 for this assertArrayEquals to be working on primitive boolean arrays, https://github.com/junit-team/junit/issues/86!
        assertTrue(Arrays.equals(new Boolean[] { Boolean.TRUE, Boolean.TRUE }, model.getBooleanWrapperArrayProperty()));

        assertEquals(Long.valueOf(1L), model.getLongWrapperProperty());
        assertArrayEquals(new Long[] { Long.valueOf(1L), Long.valueOf(1L) }, model.getLongWrapperArrayProperty());
    }
}
