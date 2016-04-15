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

import java.util.Hashtable;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.factory.ModelClassException;
import org.apache.sling.models.impl.injectors.RequestAttributeInjector;
import org.apache.sling.models.impl.injectors.SelfInjector;
import org.apache.sling.models.testmodels.classes.InvalidConstructorModel;
import org.apache.sling.models.testmodels.classes.SuperclassConstructorModel;
import org.apache.sling.models.testmodels.classes.WithOneConstructorModel;
import org.apache.sling.models.testmodels.classes.WithThreeConstructorsModel;
import org.apache.sling.models.testmodels.classes.WithTwoConstructorsModel;
import org.apache.sling.models.testmodels.classes.constructorinjection.NoNameModel;
import org.apache.sling.models.testmodels.classes.constructorinjection.WithThreeConstructorsOneInjectModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

@RunWith(MockitoJUnitRunner.class)
public class ConstructorTest {

    @Mock
    private ComponentContext componentCtx;

    @Mock
    private BundleContext bundleContext;

    private ModelAdapterFactory factory;

    @Mock
    private SlingHttpServletRequest request;

    private static final int INT_VALUE = 42;

    private static final String STRING_VALUE = "myValue";

    @Before
    public void setup() {
        when(componentCtx.getBundleContext()).thenReturn(bundleContext);
        when(componentCtx.getProperties()).thenReturn(new Hashtable<String, Object>());

        when(request.getAttribute("attribute")).thenReturn(INT_VALUE);
        when(request.getAttribute("attribute2")).thenReturn(STRING_VALUE);

        factory = new ModelAdapterFactory();
        factory.activate(componentCtx);
        factory.bindInjector(new RequestAttributeInjector(), new ServicePropertiesMap(1, 1));
        factory.bindInjector(new SelfInjector(), new ServicePropertiesMap(2, 2));
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(WithOneConstructorModel.class, WithThreeConstructorsModel.class, WithTwoConstructorsModel.class, SuperclassConstructorModel.class, InvalidConstructorModel.class, WithThreeConstructorsOneInjectModel.class, NoNameModel.class);
    }

    @Test
    public void testConstructorInjection() {
        WithOneConstructorModel model = factory.getAdapter(request, WithOneConstructorModel.class);
        assertNotNull(model);
        assertEquals(request, model.getRequest());
        assertEquals(INT_VALUE, model.getAttribute());
    }

    @Test
    public void testThreeConstructorsInjection() {
        WithThreeConstructorsModel model = factory.getAdapter(request, WithThreeConstructorsModel.class);
        assertNotNull(model);
        assertEquals(request, model.getRequest());
        assertEquals(INT_VALUE, model.getAttribute());
    }

    @Test
    public void testTwoConstructorsInjection() {
        WithTwoConstructorsModel model = factory.getAdapter(request, WithTwoConstructorsModel.class);
        assertNotNull(model);
        assertEquals(request, model.getRequest());
        assertEquals(INT_VALUE, model.getAttribute());
    }

    @Test
    public void testSuperclassConstructorsInjection() {
        SuperclassConstructorModel model = factory.getAdapter(request, SuperclassConstructorModel.class);
        assertNotNull(model);
        assertEquals(request, model.getRequest());
        assertEquals(INT_VALUE, model.getAttribute());
    }

    @Test
    public void testInvalidConstructorInjector() {
        InvalidConstructorModel model = factory.getAdapter(request, InvalidConstructorModel.class);
        assertNull(model);
    }

    @Test(expected = ModelClassException.class)
    public void testInvalidConstructorInjectorException() {
        factory.createModel(request, InvalidConstructorModel.class);
    }

    /**
     * Test model object with three constructors, and make sure that one with @Inject is picked for instantiation.
     * Test mixing of constructor injection and field injection as well.
     */
    @Test
    public void testThreeConstructorsOneInjectInjection() {
        WithThreeConstructorsOneInjectModel model = factory.getAdapter(request,
                WithThreeConstructorsOneInjectModel.class);
        assertNotNull(model);
        assertNull(model.getRequest());
        assertEquals(INT_VALUE, model.getAttribute());
        assertEquals(STRING_VALUE, model.getAttribute2());
    }

    @Test
    public void testNoNameModel() {
        NoNameModel model = factory.getAdapter(request, NoNameModel.class);
        assertNull(model);
    }
}
