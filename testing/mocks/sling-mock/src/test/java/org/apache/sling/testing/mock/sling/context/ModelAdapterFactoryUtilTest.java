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
package org.apache.sling.testing.mock.sling.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.services.MockMimeTypeService;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ModelAdapterFactoryUtilTest {

    @Rule
    public SlingContext context = new SlingContext();
    
    @Before
    public void setUp() throws Exception {
        // scan for @Model classes
        context.addModelsForPackage("org.apache.sling.testing.mock.sling.context");
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
        context.registerService(MimeTypeService.class, new MockMimeTypeService(), null);

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
