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
package org.apache.sling.testing.mock.sling.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.impl.FirstImplementationPicker;
import org.apache.sling.models.impl.injectors.OSGiServiceInjector;
import org.apache.sling.models.impl.injectors.RequestAttributeInjector;
import org.apache.sling.models.spi.ImplementationPicker;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

public class MockModelAdapterFactoryTest {

    private ComponentContext componentContext;
    private BundleContext bundleContext;

    @Before
    public void setUp() throws Exception {
        componentContext = MockOsgi.newComponentContext();
        bundleContext = componentContext.getBundleContext();
        MockSling.setAdapterManagerBundleContext(bundleContext);

        // register sling models adapter factory
        MockModelAdapterFactory mockModelAdapterFactory = new MockModelAdapterFactory(componentContext);
        bundleContext.registerService(AdapterFactory.class.getName(), mockModelAdapterFactory, null);

        // register some injectors
        bundleContext.registerService(Injector.class.getName(), new RequestAttributeInjector(), null);
        OSGiServiceInjector osgiServiceInjector = new OSGiServiceInjector();
        osgiServiceInjector.activate(componentContext);
        bundleContext.registerService(Injector.class.getName(), osgiServiceInjector, null);

        // register implementation pickers
        bundleContext.registerService(ImplementationPicker.class.getName(), new FirstImplementationPicker(), null);

        // scan for @Model classes
        mockModelAdapterFactory.addModelsForPackage("org.apache.sling.testing.mock.sling.services");
    }

    @After
    public void tearDown() throws Exception {
        MockSling.clearAdapterManagerBundleContext();
    }

    @Test
    public void testRequestAttribute() {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest();
        request.setAttribute("prop1", "myValue");
        RequestAttributeModel model = request.adaptTo(RequestAttributeModel.class);
        assertNotNull(model);
        assertEquals("myValue", model.getProp1());
    }

    @Test
    public void testOsgiService() {
        bundleContext.registerService(MimeTypeService.class.getName(), new MockMimeTypeService(), null);

        ResourceResolver resolver = MockSling.newResourceResolver();
        OsgiServiceModel model = resolver.adaptTo(OsgiServiceModel.class);
        assertNotNull(model);
        assertNotNull(model.getMimeTypeService());
        assertEquals("text/html", model.getMimeTypeService().getMimeType("html"));
    }

    @Test
    public void testInvalidAdapt() {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest();
        OsgiServiceModel model = request.adaptTo(OsgiServiceModel.class);
        assertNull(model);
    }

    @Test
    public void testAdaptToInterface() {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest();
        request.setAttribute("prop1", "myValue");
        ServiceInterface model = request.adaptTo(ServiceInterface.class);
        assertNotNull(model);
        assertEquals("myValue", model.getPropValue());
    }

    @Model(adaptables = SlingHttpServletRequest.class)
    public interface RequestAttributeModel {
        @Inject
        String getProp1();
    }

    @Model(adaptables = ResourceResolver.class)
    public interface OsgiServiceModel {
        @Inject
        MimeTypeService getMimeTypeService();
    }

    public interface ServiceInterface {
        String getPropValue();
    }

    @Model(adaptables = SlingHttpServletRequest.class, adapters = ServiceInterface.class)
    public static class ServiceInterfaceImpl implements ServiceInterface {
        @Inject
        private String prop1;

        @Override
        public String getPropValue() {
            return this.prop1;
        }
    }

}
