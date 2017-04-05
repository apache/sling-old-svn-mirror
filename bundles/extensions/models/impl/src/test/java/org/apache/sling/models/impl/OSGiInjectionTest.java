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
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.models.impl.injectors.OSGiServiceInjector;
import org.apache.sling.models.testmodels.classes.ArrayOSGiModel;
import org.apache.sling.models.testmodels.classes.CollectionOSGiModel;
import org.apache.sling.models.testmodels.classes.ListOSGiModel;
import org.apache.sling.models.testmodels.classes.OptionalArrayOSGiModel;
import org.apache.sling.models.testmodels.classes.OptionalListOSGiModel;
import org.apache.sling.models.testmodels.classes.RequestOSGiModel;
import org.apache.sling.models.testmodels.classes.SetOSGiModel;
import org.apache.sling.models.testmodels.classes.SimpleOSGiModel;
import org.apache.sling.models.testmodels.interfaces.ServiceInterface;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

@RunWith(MockitoJUnitRunner.class)
public class OSGiInjectionTest {
    private ModelAdapterFactory factory;

    @Mock
    private ComponentContext componentCtx;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private SlingScriptHelper helper;

    private SlingBindings bindings = new SlingBindings();

    @Before
    public void setup() {
        when(componentCtx.getBundleContext()).thenReturn(bundleContext);
        when(componentCtx.getProperties()).thenReturn(new Hashtable<String, Object>());

        factory = new ModelAdapterFactory();
        factory.activate(componentCtx);

        OSGiServiceInjector injectorFactory = new OSGiServiceInjector();
        injectorFactory.activate(componentCtx);
        factory.bindInjector(injectorFactory, new ServicePropertiesMap(1, 1));

        bindings.setSling(helper);
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(SimpleOSGiModel.class, ListOSGiModel.class, RequestOSGiModel.class, ArrayOSGiModel.class, SetOSGiModel.class, OptionalListOSGiModel.class, org.apache.sling.models.testmodels.classes.constructorinjection.ListOSGiModel.class, org.apache.sling.models.testmodels.classes.constructorinjection.SimpleOSGiModel.class, CollectionOSGiModel.class, OptionalArrayOSGiModel.class);
    }

    @Test
    public void testSimpleOSGiModelField() throws Exception {
        ServiceReference ref = mock(ServiceReference.class);
        ServiceInterface service = mock(ServiceInterface.class);
        when(bundleContext.getServiceReferences(ServiceInterface.class.getName(), null)).thenReturn(
                new ServiceReference[] { ref });
        when(bundleContext.getService(ref)).thenReturn(service);

        Resource res = mock(Resource.class);

        SimpleOSGiModel model = factory.getAdapter(res, SimpleOSGiModel.class);
        assertNotNull(model);
        assertNotNull(model.getService());
        assertEquals(service, model.getService());

        verifyNoMoreInteractions(res);
    }

    @Test
    public void testListOSGiModelField() throws Exception {
        ServiceReference ref1 = mock(ServiceReference.class);
        ServiceInterface service1 = mock(ServiceInterface.class);
        when(bundleContext.getService(ref1)).thenReturn(service1);
        ServiceReference ref2 = mock(ServiceReference.class);
        ServiceInterface service2 = mock(ServiceInterface.class);
        when(bundleContext.getService(ref2)).thenReturn(service2);

        when(bundleContext.getServiceReferences(ServiceInterface.class.getName(), null)).thenReturn(
                new ServiceReference[] { ref1, ref2 });

        Resource res = mock(Resource.class);

        ListOSGiModel model = factory.getAdapter(res, ListOSGiModel.class);
        assertNotNull(model);
        assertNotNull(model.getServices());
        // the order on those is non deterministic as the ServiceReference.compareTo() is always returning 0
        // the real order is tested in the IT
        assertThat(model.getServices(), Matchers.containsInAnyOrder(service1, service2));

        verifyNoMoreInteractions(res);
    }

    @Test
    public void testArrayOSGiModelField() throws Exception {
        ServiceReference ref1 = mock(ServiceReference.class);
        ServiceInterface service1 = mock(ServiceInterface.class);
        when(bundleContext.getService(ref1)).thenReturn(service1);
        ServiceReference ref2 = mock(ServiceReference.class);
        ServiceInterface service2 = mock(ServiceInterface.class);
        when(bundleContext.getService(ref2)).thenReturn(service2);

        when(bundleContext.getServiceReferences(ServiceInterface.class.getName(), null)).thenReturn(
                new ServiceReference[] { ref1, ref2 });

        Resource res = mock(Resource.class);

        ArrayOSGiModel model = factory.getAdapter(res, ArrayOSGiModel.class);
        assertNotNull(model);
        assertNotNull(model.getServices());
        // the order on those is non deterministic as the ServiceReference.compareTo() is always returning 0
        // the real order is tested in the IT
        assertThat(Arrays.asList(model.getServices()), Matchers.containsInAnyOrder(service1, service2));

        verifyNoMoreInteractions(res);
    }

    @Test
    public void testOptionalArrayOSGiModelField() throws Exception {

        Resource res = mock(Resource.class);

        OptionalArrayOSGiModel model = factory.getAdapter(res, OptionalArrayOSGiModel.class);
        assertNotNull(model);
        assertNull(model.getServices());

        verifyNoMoreInteractions(res);
    }

    @Test
    public void testOptionalListOSGiModelField() throws Exception {
        Resource res = mock(Resource.class);

        OptionalListOSGiModel model = factory.getAdapter(res, OptionalListOSGiModel.class);
        assertNotNull(model);
        assertNull(model.getServices());

        verifyNoMoreInteractions(res);
    }

    @Test
    public void testCollectionOSGiModelField() throws Exception {
        ServiceReference ref1 = mock(ServiceReference.class);
        ServiceInterface service1 = mock(ServiceInterface.class);
        when(bundleContext.getService(ref1)).thenReturn(service1);
        ServiceReference ref2 = mock(ServiceReference.class);
        ServiceInterface service2 = mock(ServiceInterface.class);
        when(bundleContext.getService(ref2)).thenReturn(service2);

        when(bundleContext.getServiceReferences(ServiceInterface.class.getName(), null)).thenReturn(
                new ServiceReference[] { ref1, ref2 });

        Resource res = mock(Resource.class);

        CollectionOSGiModel model = factory.getAdapter(res, CollectionOSGiModel.class);
        assertNotNull(model);
        assertNotNull(model.getServices());
        // the order on those is non deterministic as the ServiceReference.compareTo() is always returning 0
        // the real order is tested in the IT
        assertThat(model.getServices(), Matchers.containsInAnyOrder(service1, service2));

        verifyNoMoreInteractions(res);
    }

    @Test
    public void testSetOSGiModelField() throws Exception {
        ServiceReference ref1 = mock(ServiceReference.class);
        ServiceInterface service1 = mock(ServiceInterface.class);
        when(bundleContext.getService(ref1)).thenReturn(service1);
        ServiceReference ref2 = mock(ServiceReference.class);
        ServiceInterface service2 = mock(ServiceInterface.class);
        when(bundleContext.getService(ref2)).thenReturn(service2);

        when(bundleContext.getServiceReferences(ServiceInterface.class.getName(), null)).thenReturn(
                new ServiceReference[] { ref1, ref2 });

        Resource res = mock(Resource.class);

        SetOSGiModel model = factory.getAdapter(res, SetOSGiModel.class);
        assertNull(model);

        verify(bundleContext).registerService(eq(Runnable.class.getName()), eq(factory), any(Dictionary.class));
        verify(bundleContext).addBundleListener(any(BundleListener.class));
        verify(bundleContext).registerService(eq(Object.class.getName()), any(Object.class), any(Dictionary.class));
        verify(bundleContext).getBundles();
        verify(bundleContext).getBundle();
        verifyNoMoreInteractions(res, bundleContext);
    }

    @Test
    public void testSimpleOSGiModelConstructor() throws Exception {
        ServiceReference ref = mock(ServiceReference.class);
        ServiceInterface service = mock(ServiceInterface.class);
        when(bundleContext.getServiceReferences(ServiceInterface.class.getName(), null)).thenReturn(
                new ServiceReference[] { ref });
        when(bundleContext.getService(ref)).thenReturn(service);

        Resource res = mock(Resource.class);

        org.apache.sling.models.testmodels.classes.constructorinjection.SimpleOSGiModel model
                = factory.getAdapter(res, org.apache.sling.models.testmodels.classes.constructorinjection.SimpleOSGiModel.class);
        assertNotNull(model);
        assertNotNull(model.getService());
        assertEquals(service, model.getService());

        verifyNoMoreInteractions(res);
    }

    @Test
    public void testListOSGiModelConstructor() throws Exception {
        ServiceReference ref1 = mock(ServiceReference.class);
        ServiceInterface service1 = mock(ServiceInterface.class);
        when(bundleContext.getService(ref1)).thenReturn(service1);
        ServiceReference ref2 = mock(ServiceReference.class);
        ServiceInterface service2 = mock(ServiceInterface.class);
        when(bundleContext.getService(ref2)).thenReturn(service2);

        when(bundleContext.getServiceReferences(ServiceInterface.class.getName(), null)).thenReturn(
                new ServiceReference[] { ref1, ref2 });

        Resource res = mock(Resource.class);

        org.apache.sling.models.testmodels.classes.constructorinjection.ListOSGiModel model
                = factory.getAdapter(res, org.apache.sling.models.testmodels.classes.constructorinjection.ListOSGiModel.class);
        assertNotNull(model);
        assertNotNull(model.getServices());
        // the order on those is non deterministic as the ServiceReference.compareTo() is always returning 0
        // the real order is tested in the IT
        assertThat(model.getServices(), Matchers.containsInAnyOrder(service1, service2));

        verifyNoMoreInteractions(res);
    }

}
