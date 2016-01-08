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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Hashtable;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.impl.injectors.ChildResourceInjector;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.testmodels.classes.constructorinjection.OptionalPrimitivesModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

/**
 * Validates that @Optional annotations works with primitive values which do not support null
 */
@RunWith(MockitoJUnitRunner.class)
public class OptionalPrimitivesTest {

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
        factory.bindInjector(new ValueMapInjector(), new ServicePropertiesMap(2, 2));
        factory.bindInjector(new ChildResourceInjector(), new ServicePropertiesMap(1, 1));
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(org.apache.sling.models.testmodels.classes.OptionalPrimitivesModel.class, org.apache.sling.models.testmodels.interfaces.OptionalPrimitivesModel.class, org.apache.sling.models.testmodels.classes.constructorinjection.OptionalPrimitivesModel.class);
    }

    @Test
    public void testFieldInjectionClass() {
        ValueMap vm = ValueMap.EMPTY;

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        org.apache.sling.models.testmodels.classes.OptionalPrimitivesModel model
                = factory.getAdapter(res, org.apache.sling.models.testmodels.classes.OptionalPrimitivesModel.class);
        assertNotNull(model);

        // make sure primitives are initialized with initial value
        assertEquals(0, model.getByteValue());
        assertEquals(0, model.getShortValue());
        assertEquals(0, model.getIntValue());
        assertEquals(0L, model.getLongValue());
        assertEquals(0.0f, model.getFloatValue(), 0.00001d);
        assertEquals(0.0d, model.getDoubleValue(), 0.00001d);
        assertEquals('\u0000', model.getCharValue());
        assertEquals(false, model.getBooleanValue());

        // make sure object wrapper of primitives are null
        assertNull(model.getByteObjectValue());
        assertNull(model.getShortObjectValue());
        assertNull(model.getIntObjectValue());
        assertNull(model.getLongObjectValue());
        assertNull(model.getFloatObjectValue());
        assertNull(model.getDoubleObjectValue());
        assertNull(model.getCharObjectValue());
        assertNull(model.getBooleanObjectValue());
}

    @Test
    public void testConstructorInjection() {
        ValueMap vm = ValueMap.EMPTY;

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        org.apache.sling.models.testmodels.classes.constructorinjection.OptionalPrimitivesModel model
                = factory.getAdapter(res, org.apache.sling.models.testmodels.classes.constructorinjection.OptionalPrimitivesModel.class);
        assertNotNull(model);

        // make sure primitives are initialized with initial value
        assertEquals(0, model.getByteValue());
        assertEquals(0, model.getShortValue());
        assertEquals(0, model.getIntValue());
        assertEquals(0L, model.getLongValue());
        assertEquals(0.0f, model.getFloatValue(), 0.00001d);
        assertEquals(0.0d, model.getDoubleValue(), 0.00001d);
        assertEquals('\u0000', model.getCharValue());
        assertEquals(false, model.getBooleanValue());

        // make sure object wrapper of primitives are null
        assertNull(model.getByteObjectValue());
        assertNull(model.getShortObjectValue());
        assertNull(model.getIntObjectValue());
        assertNull(model.getLongObjectValue());
        assertNull(model.getFloatObjectValue());
        assertNull(model.getDoubleObjectValue());
        assertNull(model.getCharObjectValue());
        assertNull(model.getBooleanObjectValue());
}

    @Test
    public void testFieldInjectionInterface() {
        ValueMap vm = ValueMap.EMPTY;

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);

        org.apache.sling.models.testmodels.interfaces.OptionalPrimitivesModel model
                = factory.getAdapter(res, org.apache.sling.models.testmodels.interfaces.OptionalPrimitivesModel.class);
        assertNotNull(model);

        // make sure primitives are initialized with initial value
        assertEquals(0, model.getByteValue());
        assertEquals(0, model.getShortValue());
        assertEquals(0, model.getIntValue());
        assertEquals(0L, model.getLongValue());
        assertEquals(0.0f, model.getFloatValue(), 0.00001d);
        assertEquals(0.0d, model.getDoubleValue(), 0.00001d);
        assertEquals('\u0000', model.getCharValue());
        assertEquals(false, model.getBooleanValue());

        // make sure object wrapper of primitives are null
        assertNull(model.getByteObjectValue());
        assertNull(model.getShortObjectValue());
        assertNull(model.getIntObjectValue());
        assertNull(model.getLongObjectValue());
        assertNull(model.getFloatObjectValue());
        assertNull(model.getDoubleObjectValue());
        assertNull(model.getCharObjectValue());
        assertNull(model.getBooleanObjectValue());
    }

}
