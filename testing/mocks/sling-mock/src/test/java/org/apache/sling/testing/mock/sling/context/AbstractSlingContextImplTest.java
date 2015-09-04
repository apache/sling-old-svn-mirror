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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.loader.ContentLoader;
import org.apache.sling.testing.mock.sling.services.MockMimeTypeService;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractSlingContextImplTest {

    private SlingContextImpl context;

    @Before
    public void setUp() throws Exception {
        this.context = new SlingContextImpl();
        this.context.setResourceResolverType(getResourceResolverType());
        this.context.setUp();

        context.addModelsForPackage("org.apache.sling.testing.mock.sling.context");
        
        ContentLoader contentLoader = this.context.load();
        contentLoader.json("/json-import-samples/content.json", "/content/sample/en");
    }

    @After
    public void tearDown() throws Exception {
        this.context.tearDown();
    }
    
    protected abstract ResourceResolverType getResourceResolverType();
    
    @Test
    public void testContextObjects() {
        assertNotNull(context.componentContext());
        assertNotNull(context.bundleContext());
        assertNotNull(context.resourceResolver());
        assertNotNull(context.request());
        assertNotNull(context.requestPathInfo());
        assertNotNull(context.response());
        assertNotNull(context.slingScriptHelper());
    }

    @Test
    public void testSlingBindings() {
        SlingBindings bindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        assertNotNull(bindings);
        assertSame(context.request(), bindings.get(SlingBindings.REQUEST));
        assertSame(context.response(), bindings.get(SlingBindings.RESPONSE));
        assertSame(context.slingScriptHelper(), bindings.get(SlingBindings.SLING));
    }

    @Test
    public void testSetCurrentResource() {
        context.currentResource("/content/sample/en/jcr:content/par/colctrl");
        assertEquals("/content/sample/en/jcr:content/par/colctrl", context.currentResource().getPath());

        context.currentResource(context.resourceResolver().getResource("/content/sample/en/jcr:content/par"));
        assertEquals("/content/sample/en/jcr:content/par", context.currentResource().getPath());

        context.currentResource((Resource) null);
        assertNull(context.request().getResource());

        context.currentResource((String) null);
        assertNull(context.request().getResource());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetCurrentResourceNonExisting() {
        context.currentResource("/non/existing");
    }

    @Test
    public void testSlingModelsRequestAttribute() {
        context.request().setAttribute("prop1", "myValue");
        RequestAttributeModel model = context.request().adaptTo(RequestAttributeModel.class);
        assertEquals("myValue", model.getProp1());
    }

    @Test
    public void testSlingModelsOsgiService() {
        context.registerService(new MockMimeTypeService());

        ResourceResolver resolver = MockSling.newResourceResolver();
        OsgiServiceModel model = resolver.adaptTo(OsgiServiceModel.class);
        assertNotNull(model.getMimeTypeService());
        assertEquals("text/html", model.getMimeTypeService().getMimeType("html"));
    }

    @Test
    public void testSlingModelsInvalidAdapt() {
        OsgiServiceModel model = context.request().adaptTo(OsgiServiceModel.class);
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

    @Test
    public void testRunModes() {
        SlingSettingsService slingSettings = context.getService(SlingSettingsService.class);
        assertEquals(SlingContextImpl.DEFAULT_RUN_MODES, slingSettings.getRunModes());

        context.runMode("mode1", "mode2");
        Set<String> newRunModes = slingSettings.getRunModes();
        assertEquals(2, newRunModes.size());
        assertTrue(newRunModes.contains("mode1"));
        assertTrue(newRunModes.contains("mode2"));
    }
    
    @Test
    public void testResourceResolverFactory() {
        ResourceResolverFactory factory = context.getService(ResourceResolverFactory.class);
        assertNotNull(factory);
    }

    @Test
    public void testWithoutResourceResolverType() {
        SlingContextImpl contextTest = new SlingContextImpl();
        contextTest.setUp();
        ResourceResolverFactory factory = contextTest.getService(ResourceResolverFactory.class);
        assertNotNull(factory);
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
