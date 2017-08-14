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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.export.spi.ModelExporter;
import org.apache.sling.models.factory.ExportException;
import org.apache.sling.models.factory.InvalidAdaptableException;
import org.apache.sling.models.factory.MissingElementsException;
import org.apache.sling.models.factory.MissingExporterException;
import org.apache.sling.models.factory.ModelClassException;
import org.apache.sling.models.impl.injectors.SelfInjector;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

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
        factory.bindModelExporter(new FirstStringExporter(), new ServicePropertiesMap(2, 0));
        factory.bindModelExporter(new SecondStringExporter(), new ServicePropertiesMap(3, 1));
        factory.bindModelExporter(new FirstIntegerExporter(), new ServicePropertiesMap(4, 2));
        
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

    @Test
    public void testSelectExporterByName() throws Exception {
        Result<Object> result = mock(Result.class);
        when(result.wasSuccessful()).thenReturn(true);
        when(result.getValue()).thenReturn(new Object());

        String exported = factory.handleAndExportResult(result, "second", String.class, Collections.<String, String>emptyMap());
        Assert.assertEquals("Export from second", exported);
    }

    @Test
    public void testSelectExporterByType() throws Exception {
        Result<Object> result = mock(Result.class);
        when(result.wasSuccessful()).thenReturn(true);
        when(result.getValue()).thenReturn(new Object());

        Integer exported = factory.handleAndExportResult(result, "first", Integer.class, Collections.<String, String>emptyMap());
        Assert.assertEquals(Integer.valueOf(42), exported);
    }

    @Test(expected = MissingExporterException.class)
    public void testSelectExporterByNameAndWrongType() throws Exception {
        Result<Object> result = mock(Result.class);
        when(result.wasSuccessful()).thenReturn(true);
        when(result.getValue()).thenReturn(new Object());

        factory.handleAndExportResult(result, "second", Integer.class, Collections.<String, String>emptyMap());
    }

    private static class FirstStringExporter implements ModelExporter {
        @Override
        public boolean isSupported(@Nonnull Class<?> aClass) {
            return aClass == String.class;
        }

        @CheckForNull
        @Override
        public <T> T export(@Nonnull Object o, @Nonnull Class<T> aClass, @Nonnull Map<String, String> map) throws ExportException {
            if (aClass == String.class) {
                return (T) "Export from first";
            } else {
                throw new ExportException(String.format("%s is not supported.", aClass));
            }
        }

        @Nonnull
        @Override
        public String getName() {
            return "first";
        }
    }

    private static class SecondStringExporter implements ModelExporter {
        @Override
        public boolean isSupported(@Nonnull Class<?> aClass) {
            return aClass == String.class;
        }

        @CheckForNull
        @Override
        public <T> T export(@Nonnull Object o, @Nonnull Class<T> aClass, @Nonnull Map<String, String> map) throws ExportException {
            if (aClass == String.class) {
                return (T) "Export from second";
            } else {
                throw new ExportException(String.format("%s is not supported.", aClass));
            }
        }

        @Nonnull
        @Override
        public String getName() {
            return "second";
        }
    }

    private static class FirstIntegerExporter implements ModelExporter {
        @Override
        public boolean isSupported(@Nonnull Class<?> aClass) {
            return aClass == Integer.class;
        }

        @CheckForNull
        @Override
        public <T> T export(@Nonnull Object o, @Nonnull Class<T> aClass, @Nonnull Map<String, String> map) throws ExportException {
            if (aClass == Integer.class) {
                return (T) Integer.valueOf(42);
            } else {
                throw new ExportException(String.format("%s is not supported.", aClass));
            }
        }

        @Nonnull
        @Override
        public String getName() {
            return "first";
        }
    }
}
