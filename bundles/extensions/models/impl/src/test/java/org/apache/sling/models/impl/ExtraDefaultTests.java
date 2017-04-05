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

import java.util.Hashtable;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

@RunWith(MockitoJUnitRunner.class)
public class ExtraDefaultTests {

    @Mock
    private ComponentContext componentCtx;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private Resource resource;

    private ModelAdapterFactory factory;

    @Before
    public void setup() {
        when(componentCtx.getBundleContext()).thenReturn(bundleContext);
        when(componentCtx.getProperties()).thenReturn(new Hashtable<String, Object>());

        factory = new ModelAdapterFactory();
        factory.activate(componentCtx);
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(EmptyDefaultsModel.class, WrongTypeDefaultsModel.class);
    }

    @Test
    public void testEmptyDefaults() {
        EmptyDefaultsModel model = factory.getAdapter(resource, EmptyDefaultsModel.class);
        assertNotNull(model);
        assertNotNull(model.booleanArray);
        assertEquals(0, model.booleanArray.length);
        assertNotNull(model.doubleArray);
        assertEquals(0, model.doubleArray.length);
        assertNotNull(model.floatArray);
        assertEquals(0, model.floatArray.length);
        assertNotNull(model.intArray);
        assertEquals(0, model.intArray.length);
        assertNotNull(model.longArray);
        assertEquals(0, model.longArray.length);
        assertNotNull(model.shortArray);
        assertEquals(0, model.shortArray.length);
        assertNotNull(model.booleanWrapperArray);
        assertEquals(0, model.booleanWrapperArray.length);
        assertNotNull(model.doubleWrapperArray);
        assertEquals(0, model.doubleWrapperArray.length);
        assertNotNull(model.floatWrapperArray);
        assertEquals(0, model.floatWrapperArray.length);
        assertNotNull(model.intWrapperArray);
        assertEquals(0, model.intWrapperArray.length);
        assertNotNull(model.longWrapperArray);
        assertEquals(0, model.longWrapperArray.length);
        assertNotNull(model.shortWrapperArray);
        assertEquals(0, model.shortWrapperArray.length);
        assertNotNull(model.stringArray);
        assertEquals(0, model.stringArray.length);
        assertEquals(false, model.singleBoolean);
        assertEquals(0d, model.singleDouble, 0.0001);
        assertEquals(0f, model.singleFloat, 0.0001);
        assertEquals(0, model.singleInt);
        assertEquals(0l, model.singleLong);
        assertEquals((short) 0, model.singleShort);
        assertEquals(false, model.singleBooleanWrapper);
        assertEquals(0d, model.singleDoubleWrapper, 0.0001);
        assertEquals(0f, model.singleFloatWrapper, 0.0001);
        assertEquals(0, model.singleIntWrapper.intValue());
        assertEquals((short) 0, model.singleShortWrapper.shortValue());
        assertEquals(0l, model.singleLongWrapper.longValue());
        assertEquals("", model.singleString);
    }

    @Test
    public void testWrongDefaultValues() {
        WrongTypeDefaultsModel model = factory.getAdapter(resource, WrongTypeDefaultsModel.class);
        assertNotNull(model);
        assertNotNull(model.booleanArray);
        assertEquals(0, model.booleanArray.length);
        assertNotNull(model.doubleArray);
        assertEquals(0, model.doubleArray.length);
        assertNotNull(model.floatArray);
        assertEquals(0, model.floatArray.length);
        assertNotNull(model.intArray);
        assertEquals(0, model.intArray.length);
        assertNotNull(model.longArray);
        assertEquals(0, model.longArray.length);
        assertNotNull(model.shortArray);
        assertEquals(0, model.shortArray.length);
        assertNotNull(model.stringArray);
        assertEquals(0, model.stringArray.length);
    }

    @Model(adaptables = Resource.class)
    public static class EmptyDefaultsModel {

        @Inject
        @Default
        private boolean[] booleanArray;

        @Inject
        @Default
        private double[] doubleArray;

        @Inject
        @Default
        private float[] floatArray;

        @Inject
        @Default
        private int[] intArray;

        @Inject
        @Default
        private long[] longArray;

        @Inject
        @Default
        private short[] shortArray;
        
        @Inject
        @Default
        private Boolean[] booleanWrapperArray;

        @Inject
        @Default
        private Double[] doubleWrapperArray;

        @Inject
        @Default
        private Float[] floatWrapperArray;

        @Inject
        @Default
        private Integer[] intWrapperArray;

        @Inject
        @Default
        private Long[] longWrapperArray;

        @Inject
        @Default
        private Short[] shortWrapperArray;

        @Inject
        @Default
        private String[] stringArray;

        @Inject
        @Default
        private boolean singleBoolean;

        @Inject
        @Default
        private double singleDouble;

        @Inject
        @Default
        private float singleFloat;

        @Inject
        @Default
        private int singleInt;

        @Inject
        @Default
        private long singleLong;

        @Inject
        @Default
        private short singleShort;

        @Inject
        @Default
        private Boolean singleBooleanWrapper;

        @Inject
        @Default
        private Double singleDoubleWrapper;

        @Inject
        @Default
        private Float singleFloatWrapper;

        @Inject
        @Default
        private Integer singleIntWrapper;

        @Inject
        @Default
        private Long singleLongWrapper;

        @Inject
        @Default
        private Short singleShortWrapper;

        @Inject
        @Default
        private String singleString;

    }

    @Model(adaptables = Resource.class)
    public static class WrongTypeDefaultsModel {

        @Inject
        @Default(intValues = { 1, 1 })
        private boolean[] booleanArray;

        @Inject
        @Default(intValues = { 1, 1 })
        private double[] doubleArray;

        @Inject
        @Default(intValues = { 1, 1 })
        private float[] floatArray;

        @Inject
        @Default(longValues = { 1, 1 })
        private int[] intArray;

        @Inject
        @Default(intValues = { 1, 1 })
        private long[] longArray;

        @Inject
        @Default(intValues = { 1, 1 })
        private short[] shortArray;

        @Inject
        @Default(intValues = { 1, 1 })
        private String[] stringArray;

    }
}
