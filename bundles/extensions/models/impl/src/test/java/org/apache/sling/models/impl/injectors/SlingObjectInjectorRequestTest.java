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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SlingObjectInjectorRequestTest {

    private final SlingObjectInjector injector = new SlingObjectInjector();

    @Mock
    private AnnotatedElement annotatedElement;
    @Mock
    private SlingHttpServletRequest request;
    @Mock
    private SlingHttpServletResponse response;
    @Mock
    private SlingScriptHelper scriptHelper;
    @Mock
    private ResourceResolver resourceResolver;
    @Mock
    private Resource resource;
    @Mock
    private DisposalCallbackRegistry registry;

    @Before
    public void setUp() {
        SlingBindings bindings = new SlingBindings();
        bindings.put(SlingBindings.SLING, this.scriptHelper);
        when(this.request.getResourceResolver()).thenReturn(this.resourceResolver);
        when(this.request.getResource()).thenReturn(this.resource);
        when(this.request.getAttribute(SlingBindings.class.getName())).thenReturn(bindings);
        when(this.scriptHelper.getResponse()).thenReturn(this.response);
    }

    @Test
    public void testResourceResolver() {
        Object result = this.injector.getValue(this.request, null, ResourceResolver.class, this.annotatedElement, registry);
        assertSame(this.resourceResolver, result);
    }

    @Test
    public void testResource() {
        Object result = this.injector.getValue(this.request, null, Resource.class, this.annotatedElement, registry);
        assertNull(result);

        when(annotatedElement.isAnnotationPresent(SlingObject.class)).thenReturn(true);
        result = this.injector.getValue(this.request, null, Resource.class, this.annotatedElement, registry);
        assertSame(resource, result);
    }

    @Test
    public void testRequest() {
        Object result = this.injector.getValue(this.request, null, SlingHttpServletRequest.class,
                this.annotatedElement, registry);
        assertSame(this.request, result);

        result = this.injector.getValue(this.request, null, HttpServletRequest.class, this.annotatedElement, registry);
        assertSame(this.request, result);
    }

    @Test
    public void testResponse() {
        Object result = this.injector.getValue(this.request, null, SlingHttpServletResponse.class,
                this.annotatedElement, registry);
        assertSame(this.response, result);

        result = this.injector.getValue(this.request, null, HttpServletResponse.class, this.annotatedElement, registry);
        assertSame(this.response, result);
    }

    @Test
    public void testScriptHelper() {
        Object result = this.injector
                .getValue(this.request, null, SlingScriptHelper.class, this.annotatedElement, registry);
        assertSame(this.scriptHelper, result);
    }

    @Test
    public void testInvalid() {
        Object result = this.injector.getValue(this, null, SlingScriptHelper.class, this.annotatedElement, registry);
        assertNull(result);
    }

}
