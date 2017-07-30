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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.apache.sling.scripting.api.BindingsValuesProvidersByContext;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import javax.script.Bindings;
import javax.script.ScriptEngineFactory;
import java.util.Collections;
import java.util.Hashtable;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class RequestWrapperTest {
    @Mock
    private ComponentContext componentCtx;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private AdapterManager adapterManager;

    @Mock
    private BindingsValuesProvidersByContext bindingsValuesProvidersByContext;

    @Mock
    private BindingsValuesProvider bindingsValuesProvider;

    @Mock
    private Resource resource;

    @Mock
    private SlingHttpServletRequest request;

    @InjectMocks
    private ModelAdapterFactory factory;

    @Before
    public void setup() {
        when(componentCtx.getBundleContext()).thenReturn(bundleContext);
        when(componentCtx.getProperties()).thenReturn(new Hashtable<String, Object>());
        factory.activate(componentCtx);
        when(bindingsValuesProvidersByContext.getBindingsValuesProviders(any(ScriptEngineFactory.class), eq(BindingsValuesProvider.DEFAULT_CONTEXT))).
                thenReturn(Collections.singleton(bindingsValuesProvider));
    }

    @Test
    public void testWrapper() {
        Target expected = new Target();
        when(adapterManager.getAdapter(any(SlingHttpServletRequest.class), eq(Target.class))).thenReturn(expected);

        Target actual = factory.getModelFromWrappedRequest(request, resource, Target.class);
        assertEquals(expected, actual);

        verify(adapterManager, times(1)).getAdapter(argThat(requestHasResource(resource)), eq(Target.class));
        verify(bindingsValuesProvider, times(1)).addBindings(argThat(bindingsHasResource(resource)));
    }

    private Matcher<Bindings> bindingsHasResource(final Resource resource) {
        return new TypeSafeMatcher<Bindings>() {
            @Override
            protected boolean matchesSafely(Bindings bindings) {
                return bindings.get(SlingBindings.RESOURCE) == resource;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a bindings object with the resource " + resource);
            }
        };
    }

    private Matcher<SlingHttpServletRequest> requestHasResource(final Resource resource) {
        return new TypeSafeMatcher<SlingHttpServletRequest>() {
            @Override
            protected boolean matchesSafely(SlingHttpServletRequest slingHttpServletRequest) {
                return slingHttpServletRequest.getResource().equals(resource);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a request with the resource " + resource);
            }
        };
    }

    class Target {

    }

}
