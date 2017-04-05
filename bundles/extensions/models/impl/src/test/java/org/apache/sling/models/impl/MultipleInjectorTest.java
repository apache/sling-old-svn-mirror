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
import static org.mockito.Mockito.when;

import java.util.Hashtable;

import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Source;
import org.apache.sling.models.factory.ModelClassException;
import org.apache.sling.models.impl.injectors.BindingsInjector;
import org.apache.sling.models.impl.injectors.RequestAttributeInjector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

@RunWith(MockitoJUnitRunner.class)
public class MultipleInjectorTest {

    @Spy
    private BindingsInjector bindingsInjector;

    @Spy
    private RequestAttributeInjector attributesInjector;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private ComponentContext componentCtx;

    @Mock
    private BundleContext bundleContext;

    private ModelAdapterFactory factory;

    private SlingBindings bindings;

    @Before
    public void setup() {
        when(componentCtx.getBundleContext()).thenReturn(bundleContext);
        when(componentCtx.getProperties()).thenReturn(new Hashtable<String, Object>());

        bindings = new SlingBindings();

        factory = new ModelAdapterFactory();
        factory.activate(componentCtx);
        // binding injector should be asked first as it has a lower service ranking!
        factory.bindInjector(bindingsInjector, new ServicePropertiesMap(1, 1));
        factory.bindInjector(attributesInjector, new ServicePropertiesMap(2, 2));
        factory.bindStaticInjectAnnotationProcessorFactory(bindingsInjector, new ServicePropertiesMap(1, 1));

        when(request.getAttribute(SlingBindings.class.getName())).thenReturn(bindings);
        factory.adapterImplementations.addClassesAsAdapterAndImplementation(
                ForTwoInjectorsWithSource.class, ForTwoInjectors.class, ForTwoInjectorsWithInvalidSource.class);
    }

    @Test
    public void testInjectorOrder() {
        String bindingsValue = "bindings value";
        bindings.put("firstAttribute", bindingsValue);

        String attributeValue = "attribute value";
        when(request.getAttribute("firstAttribute")).thenReturn(attributeValue);

        ForTwoInjectors obj = factory.getAdapter(request, ForTwoInjectors.class);

        assertNotNull(obj);
        assertEquals(obj.firstAttribute, bindingsValue);
    }

    @Test
    public void testInjectorOrderWithSource() {
        String bindingsValue = "bindings value";
        bindings.put("firstAttribute", bindingsValue);

        String attributeValue = "attribute value";
        when(request.getAttribute("firstAttribute")).thenReturn(attributeValue);

        ForTwoInjectorsWithSource obj = factory.getAdapter(request, ForTwoInjectorsWithSource.class);

        assertNotNull(obj);
        assertEquals(obj.firstAttribute, attributeValue);
    }

    @Test
    public void testInjectorWithInvalidSource() {
        ForTwoInjectorsWithInvalidSource obj = factory.getAdapter(request, ForTwoInjectorsWithInvalidSource.class);
        assertNull(obj);
    }

    @Test(expected=ModelClassException.class)
    public void testInjectorWithInvalidSourceWithException() {
        factory.createModel(request, ForTwoInjectorsWithInvalidSource.class);
    }

    @Model(adaptables = SlingHttpServletRequest.class)
    public static class ForTwoInjectors {

        @Inject
        private String firstAttribute;

    }

    @Model(adaptables = SlingHttpServletRequest.class)
    public static class ForTwoInjectorsWithSource {

        @Inject
        @Source("request-attributes")
        private String firstAttribute;

    }

    @Model(adaptables = SlingHttpServletRequest.class)
    public static class ForTwoInjectorsWithInvalidSource {

        @Inject
        @Source("this-is-an-invalid-source")
        private String firstAttribute;

    }

}
