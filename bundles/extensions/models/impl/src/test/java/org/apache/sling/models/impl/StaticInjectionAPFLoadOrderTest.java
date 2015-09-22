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
package org.apache.sling.models.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.impl.injectors.SlingObjectInjector;
import org.apache.sling.models.testutil.ModelAdapterFactoryUtil;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Test load order behavior of StaticInjectionAnnotationProcesssorFactory instances (SLING-5010).
 */
@RunWith(MockitoJUnitRunner.class)
public class StaticInjectionAPFLoadOrderTest {
    
    @Rule
    public OsgiContext context = new OsgiContext();
    
    @Mock
    private SlingHttpServletRequest request;
    @Mock
    private ResourceResolver resourceResolver;
    
    private ModelAdapterFactory factory;
    
    @Before
    public void setUp() {
        registerModelAdapterFactory();
    }
    
    /**
     * Registration order: 1. ModelFactory, 2. custom injector, 3. model
     */
    @Test
    public void testFactory_Injector_Model() {
        when(request.getResourceResolver()).thenReturn(null);

        registerCustomInjector();
        registerModel();
        
        // this should not throw an exception because resourceResovler is marked as optional
        assertFalse(createModel().hasResourceResolver());
    }
    
    /**
     * Registration order: 1. ModelFactory, 2. custom injector, 3. model
     */
    @Test
    public void testFactory_Injector_Model_WithResourceResolver() {
        when(request.getResourceResolver()).thenReturn(resourceResolver);
        
        registerCustomInjector();
        registerModel();
        
        assertTrue(createModel().hasResourceResolver());
    }
    
    /**
     * Registration order: 1. ModelFactory, 2. model, 3. custom injector
     */
    @Test
    public void testFactory_Model_Injector() {
        when(request.getResourceResolver()).thenReturn(null);

        registerModel();
        registerCustomInjector();
        
        // this should not throw an exception because resourceResovler is marked as optional
        assertFalse(createModel().hasResourceResolver());
    }
    
    /**
     * Registration order: 1. ModelFactory, 2. model, 3. custom injector
     */
    @Test
    public void testFactory_Model_Injector_WithResourceResolver() {
        when(request.getResourceResolver()).thenReturn(resourceResolver);
        
        registerModel();
        registerCustomInjector();
        
        assertTrue(createModel().hasResourceResolver());
    }
    
    private void registerModelAdapterFactory() {
        factory = context.registerInjectActivateService(new ModelAdapterFactory());
    }

    private void registerCustomInjector() {
        context.registerInjectActivateService(new SlingObjectInjector());
    }

    private void registerModel() {
        ModelAdapterFactoryUtil.addModelsForPackage(context.bundleContext(), TestModel.class);
    }

    private TestModel createModel() {
        return factory.createModel(request, TestModel.class);
    }
    
    
    @Model(adaptables = SlingHttpServletRequest.class)
    public static class TestModel {
        
        @SlingObject(injectionStrategy = InjectionStrategy.OPTIONAL)
        private ResourceResolver resourceResolver;
        
        public boolean hasResourceResolver() {
            return resourceResolver != null;
        }
        
    }

}
