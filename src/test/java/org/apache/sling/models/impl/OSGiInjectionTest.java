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

import java.util.Collections;
import java.util.Dictionary;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.models.impl.injectors.OSGiServiceInjector;
import org.apache.sling.models.testmodels.classes.ArrayOSGiModel;
import org.apache.sling.models.testmodels.classes.CollectionOSGiModel;
import org.apache.sling.models.testmodels.classes.ListOSGiModel;
import org.apache.sling.models.testmodels.classes.RequestOSGiModel;
import org.apache.sling.models.testmodels.classes.SetOSGiModel;
import org.apache.sling.models.testmodels.classes.SimpleOSGiModel;
import org.apache.sling.models.testmodels.interfaces.ServiceInterface;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
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
        factory = new ModelAdapterFactory();
        factory.activate(componentCtx);

        OSGiServiceInjector injectorFactory = new OSGiServiceInjector();
        injectorFactory.activate(componentCtx);
        factory.bindInjector(injectorFactory, Collections.<String, Object> singletonMap(Constants.SERVICE_ID, 0L));

        bindings.setSling(helper);
    }

    @Test
    public void testSimpleOSGiModel() throws Exception {
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
    public void testRequestOSGiModel() throws Exception {
        ServiceInterface service = mock(ServiceInterface.class);

        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        when(request.getAttribute(SlingBindings.class.getName())).thenReturn(bindings);

        when(helper.getServices(ServiceInterface.class, null)).thenReturn(new ServiceInterface[] { service });

        RequestOSGiModel model = factory.getAdapter(request, RequestOSGiModel.class);
        assertNotNull(model);
        assertNotNull(model.getService());
        assertEquals(service, model.getService());

        verify(bundleContext).registerService(eq(Runnable.class.getName()), eq(factory), any(Dictionary.class));
        verify(bundleContext).addBundleListener(any(BundleListener.class));
        verify(bundleContext).getBundles();
        verifyNoMoreInteractions(bundleContext);
    }

    @Test
    public void testListOSGiModel() throws Exception {
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
        assertEquals(2, model.getServices().size());
        assertEquals(service1, model.getServices().get(0));
        assertEquals(service2, model.getServices().get(1));

        verifyNoMoreInteractions(res);
    }

    @Test
    public void testArrayOSGiModel() throws Exception {
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
        assertEquals(2, model.getServices().length);
        assertEquals(service1, model.getServices()[0]);
        assertEquals(service2, model.getServices()[1]);

        verifyNoMoreInteractions(res);
    }

    @Test
    public void testCollectionOSGiModel() throws Exception {
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
        assertEquals(2, model.getServices().size());

        assertTrue(model.getServices().contains(service1));
        assertTrue(model.getServices().contains(service2));

        verifyNoMoreInteractions(res);
    }

    @Test
    public void testSetOSGiModel() throws Exception {
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
        verify(bundleContext).getBundles();
        verifyNoMoreInteractions(res, bundleContext);
    }
}
