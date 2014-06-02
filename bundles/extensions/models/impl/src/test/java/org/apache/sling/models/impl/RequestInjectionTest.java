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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.models.impl.injectors.BindingsInjector;
import org.apache.sling.models.testmodels.classes.BindingsModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

@RunWith(MockitoJUnitRunner.class)
public class RequestInjectionTest {

    @Mock
    private ComponentContext componentCtx;

    @Mock
    private BundleContext bundleContext;

    private ModelAdapterFactory factory;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingScriptHelper sling;

    @Before
    public void setup() {
        when(componentCtx.getBundleContext()).thenReturn(bundleContext);

        SlingBindings bindings = new SlingBindings();
        bindings.setSling(sling);
        when(request.getAttribute(SlingBindings.class.getName())).thenReturn(bindings);

        factory = new ModelAdapterFactory();
        factory.activate(componentCtx);
        factory.bindInjector(new BindingsInjector(), new ServicePropertiesMap(1, 1));
    }

    @Test
    public void testNamedInjection() {
        BindingsModel model = factory.getAdapter(request, BindingsModel.class);
        assertNotNull(model.getSling());
        assertEquals(sling, model.getSling());
    }

}
