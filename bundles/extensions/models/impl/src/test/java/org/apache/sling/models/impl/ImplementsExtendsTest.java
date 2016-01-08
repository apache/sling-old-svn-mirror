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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.jws.WebParam.Mode;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.factory.ModelClassException;
import org.apache.sling.models.impl.injectors.ValueMapInjector;
import org.apache.sling.models.spi.ImplementationPicker;
import org.apache.sling.models.testmodels.classes.implextend.EvenSimplerPropertyModel;
import org.apache.sling.models.testmodels.classes.implextend.ExtendsClassPropertyModel;
import org.apache.sling.models.testmodels.classes.implextend.ImplementsInterfacePropertyModel;
import org.apache.sling.models.testmodels.classes.implextend.ImplementsInterfacePropertyModel2;
import org.apache.sling.models.testmodels.classes.implextend.InvalidImplementsInterfacePropertyModel;
import org.apache.sling.models.testmodels.classes.implextend.InvalidSampleServiceInterface;
import org.apache.sling.models.testmodels.classes.implextend.SampleServiceInterface;
import org.apache.sling.models.testmodels.classes.implextend.SimplePropertyModel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

@RunWith(MockitoJUnitRunner.class)
public class ImplementsExtendsTest {

    @Mock
    private ComponentContext componentCtx;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private Bundle bundle;

    @Mock
    private BundleEvent bundleEvent;

    private ModelAdapterFactory factory;

    private ServiceRegistration[] registeredAdapterFactories;

    private ImplementationPicker firstImplementationPicker = new FirstImplementationPicker();

    private ServicePropertiesMap firstImplementationPickerProps = new ServicePropertiesMap(3, Integer.MAX_VALUE);

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws ClassNotFoundException, MalformedURLException {
        when(componentCtx.getBundleContext()).thenReturn(bundleContext);
        when(componentCtx.getProperties()).thenReturn(new Hashtable<String, Object>());
        when(bundleContext.registerService(anyString(), anyObject(), any(Dictionary.class))).then(new Answer<ServiceRegistration>() {
            @Override
            public ServiceRegistration answer(InvocationOnMock invocation) throws Throwable {
                final Dictionary<String, Object> props = (Dictionary<String, Object>)invocation.getArguments()[2];
                ServiceRegistration reg = mock(ServiceRegistration.class);
                ServiceReference ref = mock(ServiceReference.class);
                when(reg.getReference()).thenReturn(ref);
                when(ref.getProperty(anyString())).thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        String key = (String)invocation.getArguments()[0];
                        return props.get(key);
                    }
                });
                return reg;
            }
        });

        factory = new ModelAdapterFactory();
        factory.activate(componentCtx);
        factory.bindInjector(new ValueMapInjector(), new ServicePropertiesMap(2, 2));
        factory.bindImplementationPicker(firstImplementationPicker, firstImplementationPickerProps);

        // simulate bundle add for ModelPackageBundleListener
        Dictionary<String, String> headers = new Hashtable<String,String>();
        headers.put(ModelPackageBundleListener.HEADER, "org.apache.sling.models.testmodels.classes.implextend");
        when(bundle.getHeaders()).thenReturn(headers);

        Vector<URL> classUrls = new Vector<URL>();
        classUrls.add(getClassUrl(ExtendsClassPropertyModel.class));
        classUrls.add(getClassUrl(ImplementsInterfacePropertyModel.class));
        classUrls.add(getClassUrl(ImplementsInterfacePropertyModel2.class));
        classUrls.add(getClassUrl(InvalidImplementsInterfacePropertyModel.class));
        classUrls.add(getClassUrl(InvalidSampleServiceInterface.class));
        classUrls.add(getClassUrl(SampleServiceInterface.class));
        classUrls.add(getClassUrl(SimplePropertyModel.class));
        when(bundle.findEntries(anyString(), anyString(), anyBoolean())).thenReturn(classUrls.elements());

        when(bundle.loadClass(anyString())).then(new Answer<Class<?>>() {
            @Override
            public Class<?> answer(InvocationOnMock invocation) throws ClassNotFoundException {
                String className = (String)invocation.getArguments()[0];
                return ImplementsExtendsTest.this.getClass().getClassLoader().loadClass(className);
            }
        });

        registeredAdapterFactories = (ServiceRegistration[])factory.listener.addingBundle(bundle, bundleEvent);
    }

    private URL getClassUrl(Class<?> clazz) throws MalformedURLException {
        String path = "file:/" + clazz.getName().replace('.', '/') + ".class";
        return new URL(path);
    }

    @After
    public void tearDown() {
        // simulate bundle remove for ModelPackageBundleListener
        factory.listener.removedBundle(bundle, bundleEvent, registeredAdapterFactories);
        
        // make sure adaption is not longer possible: implementation class mapping is removed
        Resource res = getMockResourceWithProps();
        try {
            SampleServiceInterface model = factory.getAdapter(res, SampleServiceInterface.class);
            Assert.fail("Getting the model for interface 'SampleServiceInterface' should fail after the accroding adapter factory has been unregistered");
        } catch (ModelClassException e) {
            
        }
    }

    /**
     * Try to adapt to interface, with an different implementation class that has the @Model annotation
     */
    @Test
    public void testImplementsInterfaceModel() {
        Resource res = getMockResourceWithProps();
        SampleServiceInterface model = factory.getAdapter(res, SampleServiceInterface.class);
        assertNotNull(model);
        assertEquals(ImplementsInterfacePropertyModel.class, model.getClass());
        assertEquals("first-value|null|third-value", model.getAllProperties());
        assertTrue(factory.canCreateFromAdaptable(res, SampleServiceInterface.class));
    }

    /**
     * Try to adapt in a case where there is no picker available.
     * This causes the extend adaptation to fail.
     */
    @Test
    public void testImplementsNoPickerWithAdapterEqualsImplementation() {
        factory.unbindImplementationPicker(firstImplementationPicker, firstImplementationPickerProps);

        Resource res = getMockResourceWithProps();
        
        SampleServiceInterface model = factory.getAdapter(res, ImplementsInterfacePropertyModel.class);
        assertNotNull(model);
        assertEquals("first-value|null|third-value", model.getAllProperties());
        assertTrue(factory.canCreateFromAdaptable(res, ImplementsInterfacePropertyModel.class));
    }
    
    /**
     * Try to adapt in a case where there is no picker available.
     * The case where the class is the adapter still works.
     */
    @Test(expected=ModelClassException.class)
    public void testImplementsNoPickerWithDifferentImplementations() {
        factory.unbindImplementationPicker(firstImplementationPicker, firstImplementationPickerProps);

        Resource res = getMockResourceWithProps();
        factory.getAdapter(res, SampleServiceInterface.class);
    }

    /**
     * Ensure that the implementation class itself cannot be adapted to if it is not part of the "adapter" property in the annotation.
     */
    /*
    -- disabled because this cannot work in unit test where the adapterFactory is called directly
    -- it is enabled in integration tests
    @Test
    public void testImplementsInterfaceModel_ImplClassNotMapped() {
        Resource res = getMockResourceWithProps();
        ImplementsInterfacePropertyModel model = factory.getAdapter(res, ImplementsInterfacePropertyModel.class);
        assertNull(model);
    }
    */

    /**
     * Test implementation class with a mapping that is not valid (an interface that is not implemented).
     */
    @Test(expected=ModelClassException.class)
    public void testInvalidImplementsInterfaceModel() {
        Resource res = getMockResourceWithProps();
        factory.getAdapter(res, InvalidSampleServiceInterface.class);
    }

    /**
     * Test to adapt to a superclass of the implementation class with the appropriate mapping in the @Model annotation.
     */
    @Test
    public void testExtendsClassModel() {
        Resource res = getMockResourceWithProps();

        // this is not having a model annotation nor does implement an interface/extend a class with a model annotation
        SimplePropertyModel model = factory.getAdapter(res, SimplePropertyModel.class);
        assertNotNull(model);
        assertEquals("!first-value|null|third-value!", model.getAllProperties());
        assertTrue(factory.canCreateFromAdaptable(res, SimplePropertyModel.class));

        EvenSimplerPropertyModel simplerModel = factory.getAdapter(res, EvenSimplerPropertyModel.class);
        assertNotNull(simplerModel);
        assertEquals("first-value", model.getFirst());
        assertTrue(factory.canCreateFromAdaptable(res, EvenSimplerPropertyModel.class));
    }

    /**
     * Try to adapt to interface, with an different implementation class that has the @Model annotation
     */
    @Test
    public void testImplementsInterfaceModelWithPickLastImplementationPicker() {
        factory.bindImplementationPicker(new AdapterImplementationsTest.LastImplementationPicker(), new ServicePropertiesMap(3, 1));

        Resource res = getMockResourceWithProps();
        SampleServiceInterface model = factory.getAdapter(res, SampleServiceInterface.class);
        assertNotNull(model);
        assertEquals(ImplementsInterfacePropertyModel2.class, model.getClass());
        assertEquals("first-value|null|third-value", model.getAllProperties());
        assertTrue(factory.canCreateFromAdaptable(res, SampleServiceInterface.class));
    }

    private Resource getMockResourceWithProps() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("first", "first-value");
        map.put("third", "third-value");
        ValueMap vm = new ValueMapDecorator(map);

        Resource res = mock(Resource.class);
        when(res.adaptTo(ValueMap.class)).thenReturn(vm);
        return res;
    }

}
