/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.models.impl.injectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.AnnotatedElement;

import javax.servlet.ServletRequest;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BindingsInjectorTest {

    private BindingsInjector injector = new BindingsInjector();

    @Mock
    private ServletRequest request;
    @Mock
    private SlingBindings bindings;
    @Mock
    private AnnotatedElement element;
    @Mock
    private DisposalCallbackRegistry registry;

    private static final String STRING_PARAM = "param1";
    private static final String INTEGER_PARAM = "param2";
    private static final String CLASS_PARAM = "param3";
    private static final String STRING_VALUE = "myValue";
    private static final int INTEGER_VALUE = 42;
    private static final ResourceResolver CLASS_INSTANCE = mock(ResourceResolver.class);

    @Before
    public void setUp() {
        when(request.getAttribute(SlingBindings.class.getName())).thenReturn(bindings);
        when(bindings.get(STRING_PARAM)).thenReturn(STRING_VALUE);
        when(bindings.get(INTEGER_PARAM)).thenReturn(INTEGER_VALUE);
        when(bindings.get(CLASS_PARAM)).thenReturn(CLASS_INSTANCE);
    }

    @Test
    public void testStringParam() {
        Object result = injector.getValue(request, STRING_PARAM, String.class, element, registry);
        assertEquals(STRING_VALUE, result);
    }

    @Test
    public void testIntegerParam() {
        Object result = injector.getValue(request, INTEGER_PARAM, Integer.class, element, registry);
        assertEquals(INTEGER_VALUE, result);
    }

    @Test
    public void testClassInstance() {
        Object result = injector.getValue(request, CLASS_PARAM, ResourceResolver.class, element, registry);
        assertSame(CLASS_INSTANCE, result);
    }

    @Test
    public void testNonRequestAdaptable() {
        Object result = injector.getValue(mock(ResourceResolver.class), STRING_PARAM, String.class, element, registry);
        assertNull(result);
    }

    @Test
    public void testRequestThatDoesNotContainBindings() {
        when(request.getAttribute(SlingBindings.class.getName())).thenReturn(null);
        Object result = injector.getValue(request, STRING_PARAM, String.class, element, registry);
        assertNull(result);
    }

}
