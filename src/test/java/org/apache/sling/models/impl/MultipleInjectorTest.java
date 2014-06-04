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

import java.lang.reflect.AnnotatedElement;

import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Source;
import org.apache.sling.models.impl.injectors.BindingsInjector;
import org.apache.sling.models.impl.injectors.RequestAttributeInjector;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
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
        bindings = new SlingBindings();

        factory = new ModelAdapterFactory();
        factory.activate(componentCtx);
        factory.bindInjector(bindingsInjector, new ServicePropertiesMap(2, 2));
        factory.bindInjector(attributesInjector, new ServicePropertiesMap(1, 1));

        when(request.getAttribute(SlingBindings.class.getName())).thenReturn(bindings);
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

        verifyNoMoreInteractions(attributesInjector);
        verify(bindingsInjector).createAnnotationProcessor(any(), any(AnnotatedElement.class));
        verify(bindingsInjector).getValue(eq(request), eq("firstAttribute"), eq(String.class), any(AnnotatedElement.class), any(DisposalCallbackRegistry.class));
        verifyNoMoreInteractions(bindingsInjector);
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

        verify(bindingsInjector).getName();
        verifyNoMoreInteractions(bindingsInjector);
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

}
