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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.testmodels.classes.DefaultPrimitivesModel;
import org.apache.sling.models.testmodels.classes.DefaultStringModel;
import org.apache.sling.models.testmodels.classes.DefaultWrappersModel;
import org.apache.sling.models.testmodels.interfaces.PropertyModelWithDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
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
        when(componentCtx.getProperties()).thenReturn(new Hashtable<String, Object>());

        factory = new ModelAdapterFactory();
        factory.activate(componentCtx);
        factory.bindInjector(new ValueMapInjector(), new ServicePropertiesMap(0, 0));
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(DefaultStringModel.class, PropertyModelWithDefaults.class, DefaultPrimitivesModel.class, DefaultWrappersModel.class,  org.apache.sling.models.testmodels.classes.constructorinjection.DefaultPrimitivesModel.class, org.apache.sling.models.testmodels.classes.constructorinjection.DefaultStringModel.class, org.apache.sling.models.testmodels.classes.constructorinjection.DefaultWrappersModel.class);
    }

    @Test
    public void testDefaultStringValueField() {
        ValueMap vm = new ValueMapDecorator(Collections.<String, Object>emptyMap());

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        DefaultStringModel model = factory.getAdapter(res, DefaultStringModel.class);
        assertNotNull(model);
        assertEquals("firstDefault", model.getFirstProperty());
        assertEquals(2, model.getSecondProperty().length);
    }

    @Test
    public void testDefaultStringValueOnInterfaceField() {
        ValueMap vm = new ValueMapDecorator(Collections.<String, Object>singletonMap("first", "first value"));

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        PropertyModelWithDefaults model = factory.getAdapter(res, PropertyModelWithDefaults.class);
        assertNotNull(model);
        assertEquals("first value", model.getFirst());
        assertEquals("second default", model.getSecond());
    }


    @Test
    public void testDefaultPrimitivesField() {
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
    public void testDefaultWrappersField() {
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

    @Test
    public void testDefaultStringValueConstructor() {
        ValueMap vm = new ValueMapDecorator(Collections.<String, Object>emptyMap());

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        org.apache.sling.models.testmodels.classes.constructorinjection.DefaultStringModel model
                = factory.getAdapter(res, org.apache.sling.models.testmodels.classes.constructorinjection.DefaultStringModel.class);
        assertNotNull(model);
        assertEquals("firstDefault", model.getFirstProperty());
        assertEquals(2, model.getSecondProperty().length);
    }

    @Test
    public void testDefaultPrimitivesConstructor() {
        ValueMap vm = new ValueMapDecorator(Collections.<String, Object>emptyMap());

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        org.apache.sling.models.testmodels.classes.constructorinjection.DefaultPrimitivesModel model
                = factory.getAdapter(res, org.apache.sling.models.testmodels.classes.constructorinjection.DefaultPrimitivesModel.class);
        assertNotNull(model);

        assertEquals(true, model.getBooleanProperty());
        // we need to wait for JUnit 4.12 for this assertArrayEquals to be working on primitive boolean arrays, https://github.com/junit-team/junit/issues/86!
        assertTrue(Arrays.equals(new boolean[] { true, true }, model.getBooleanArrayProperty()));

        assertEquals(1L, model.getLongProperty());
        assertArrayEquals(new long[] { 1L, 1L }, model.getLongArrayProperty());
    }

    @Test
    public void testDefaultWrappersConstructor() {
        ValueMap vm = new ValueMapDecorator(Collections.<String, Object>emptyMap());

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        org.apache.sling.models.testmodels.classes.constructorinjection.DefaultWrappersModel model
                = factory.getAdapter(res, org.apache.sling.models.testmodels.classes.constructorinjection.DefaultWrappersModel.class);
        assertNotNull(model);

        assertEquals(Boolean.valueOf(true), model.getBooleanWrapperProperty());
        // we need to wait for JUnit 4.12 for this assertArrayEquals to be working on primitive boolean arrays, https://github.com/junit-team/junit/issues/86!
        assertTrue(Arrays.equals(new Boolean[] { Boolean.TRUE, Boolean.TRUE }, model.getBooleanWrapperArrayProperty()));

        assertEquals(Long.valueOf(1L), model.getLongWrapperProperty());
        assertArrayEquals(new Long[] { Long.valueOf(1L), Long.valueOf(1L) }, model.getLongWrapperArrayProperty());
    }

}
