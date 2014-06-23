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

import javax.inject.Inject;

import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.impl.injector.CustomAnnotation;
import org.apache.sling.models.impl.injector.CustomAnnotationInjector;
import org.apache.sling.models.impl.injector.SimpleInjector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

@RunWith(MockitoJUnitRunner.class)
public class CustomInjectorTest {

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
    }

    @Test
    public void testInjectorWhichDoesNotImplementAnnotationProcessor() {
        factory.bindInjector(new SimpleInjector(), new ServicePropertiesMap(1, 1));

        TestModel model = factory.getAdapter(new Object(), TestModel.class);
        assertNotNull(model);
        assertEquals("test string", model.getTestString());
    }

    @Test
    public void testInjectorWithCustomAnnotation() {
        CustomAnnotationInjector injector = new CustomAnnotationInjector();

        factory.bindInjector(new SimpleInjector(), new ServicePropertiesMap(1, 1));
        factory.bindInjector(injector, new ServicePropertiesMap(1, 1));
        factory.bindInjectAnnotationProcessorFactory(injector, new ServicePropertiesMap(1, 1));

        CustomAnnotationModel model = factory.getAdapter(new Object(), CustomAnnotationModel.class);
        assertNotNull(model);
        assertEquals("default value", model.getDefaultString());
        assertEquals("custom value", model.getCustomString());
    }

    @Model(adaptables = Object.class)
    public interface TestModel {
        @Inject
        String getTestString();
    }

    @Model(adaptables = Object.class)
    public interface CustomAnnotationModel {
        @CustomAnnotation
        String getDefaultString();

        @Inject
        String getCustomString();
    }

}
