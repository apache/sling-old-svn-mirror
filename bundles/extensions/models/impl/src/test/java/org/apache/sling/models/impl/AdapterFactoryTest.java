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

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.factory.InvalidAdaptableException;
import org.apache.sling.models.factory.ModelClassException;
import org.apache.sling.models.factory.MissingElementsException;
import org.apache.sling.models.impl.injectors.SelfInjector;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.testmodels.classes.BindingsModel;
import org.apache.sling.models.testmodels.classes.ConstructorWithExceptionModel;
import org.apache.sling.models.testmodels.classes.DefaultStringModel;
import org.apache.sling.models.testmodels.classes.InvalidModelWithMissingAnnotation;
import org.apache.sling.models.testmodels.classes.ResourceModelWithRequiredField;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

@RunWith(MockitoJUnitRunner.class)
public class AdapterFactoryTest {
    @Mock
    private ComponentContext componentCtx;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private Resource resource;

    @Mock
    private SlingHttpServletRequest request;

    private ModelAdapterFactory factory;

    @Before
    public void setup() {
        when(componentCtx.getBundleContext()).thenReturn(bundleContext);
        when(componentCtx.getProperties()).thenReturn(new Hashtable<String, Object>());

        factory = new ModelAdapterFactory();
        factory.activate(componentCtx);
        factory.bindInjector(new ValueMapInjector(), new ServicePropertiesMap(0, 0));
        factory.bindInjector(new SelfInjector(), new ServicePropertiesMap(1, 1));
        
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(DefaultStringModel.class, ConstructorWithExceptionModel.class, NestedModel.class, NestedModelWithInvalidAdaptable.class, NestedModelWithInvalidAdaptable2.class, ResourceModelWithRequiredField.class) ;
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testIsModelClass() {
        Assert.assertTrue(factory.isModelClass(resource, DefaultStringModel.class));
        Assert.assertFalse(factory.isModelClass(resource, InvalidModelWithMissingAnnotation.class));
    }

    @Test
    public void testCanCreateFromAdaptable() {
        Assert.assertTrue(factory.canCreateFromAdaptable(resource, DefaultStringModel.class));
        Assert.assertFalse(factory.canCreateFromAdaptable(request, DefaultStringModel.class));
    }

    @Test
    public void testCanCreateFromAdaptableWithInvalidModel() {
        Assert.assertFalse(factory.canCreateFromAdaptable(resource, InvalidModelWithMissingAnnotation.class));
    }

    @Test(expected = ModelClassException.class)
    public void testCreateFromNonModelClass() {
        factory.createModel(resource, InvalidModelWithMissingAnnotation.class);
    }

    @Test(expected = InvalidAdaptableException.class)
    public void testCreateFromInvalidAdaptable() {
        factory.createModel(request, DefaultStringModel.class);
    }

    @Test(expected = RuntimeException.class)
    public void testCreateWithConstructorException() {
        // Internally all exceptions are wrapped within RuntimeExceptions
        factory.createModel(resource, ConstructorWithExceptionModel.class);
    }

    @Model(adaptables = SlingHttpServletRequest.class)
    public static class NestedModelWithInvalidAdaptable {
        @Self
        DefaultStringModel nestedModel;
    }

    @Test(expected = MissingElementsException.class)
    public void testCreatedNestedModelWithInvalidAdaptable() {
        // nested model can only be adapted from another adaptable
        factory.createModel(request, NestedModelWithInvalidAdaptable.class);
    }

    @Model(adaptables = SlingHttpServletRequest.class)
    public static class NestedModelWithInvalidAdaptable2 {
        @Self
        InvalidModelWithMissingAnnotation nestedModel;
    }

    @Test(expected = MissingElementsException.class)
    public void testCreatedNestedModelWithInvalidAdaptable2() {
        // nested model is in fact no valid model
        factory.createModel(request, NestedModelWithInvalidAdaptable2.class);
    }

    @Model(adaptables = Resource.class)
    public static class NestedModel {
        @Self
        ResourceModelWithRequiredField nestedModel;

        public ResourceModelWithRequiredField getNestedModel() {
            return nestedModel;
        }
    }

    @Test
    public void testCreatedNestedModel() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("required", "required");
        ValueMap vm = new ValueMapDecorator(map);
        when(resource.adaptTo(ValueMap.class)).thenReturn(vm);

        NestedModel model = factory.createModel(resource, NestedModel.class);
        Assert.assertNotNull(model);
        Assert.assertEquals("required", model.getNestedModel().getRequired());
    }

    @Test(expected=MissingElementsException.class)
    public void testCreatedNestedModelWithMissingElements() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("invalid", "required");
        ValueMap vm = new ValueMapDecorator(map);
        when(resource.adaptTo(ValueMap.class)).thenReturn(vm);

        factory.createModel(resource, NestedModel.class);
    }
}
