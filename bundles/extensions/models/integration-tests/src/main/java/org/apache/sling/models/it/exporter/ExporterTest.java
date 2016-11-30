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
package org.apache.sling.models.it.exporter;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.sling.models.factory.MissingExporterException;
import org.apache.sling.models.factory.ModelFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SlingAnnotationsTestRunner.class)
public class ExporterTest {

    @TestReference
    private ResourceResolverFactory rrFactory;

    @TestReference
    private ModelFactory modelFactory;

    @TestReference
    private SlingRequestProcessor slingRequestProcessor;

    private final String baseComponentPath = "/content/exp/baseComponent";
    private final String childComponentPath = "/content/exp/childComponent";
    private final String extendedComponentPath = "/content/exp/extendedComponent";
    private final String interfaceComponentPath = "/content/exp/interfaceComponent";
    private final String baseRequestComponentPath = "/content/exp-request/baseComponent";
    private final String extendedRequestComponentPath = "/content/exp-request/extendedComponent";
    private final String interfaceRequestComponentPath = "/content/exp-request/interfaceComponent";
    private Calendar testDate;

    @Before
    public void setup() throws LoginException, PersistenceException {
        ResourceResolver adminResolver = null;
        try {
            adminResolver = rrFactory.getAdministrativeResourceResolver(null);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("sampleValue", "baseTESTValue");
            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE,
                    "sling/exp/base");
            ResourceUtil.getOrCreateResource(adminResolver, baseComponentPath, properties, null, false);

            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE,
                    "sling/exp-request/base");
            ResourceUtil.getOrCreateResource(adminResolver, baseRequestComponentPath, properties, null, false);
            properties.clear();

            properties.put("sampleValue", "childTESTValue");
            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE,
                    "sling/exp/child");
            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_SUPER_TYPE,
                    "sling/exp/base");
            ResourceUtil.getOrCreateResource(adminResolver, childComponentPath, properties, null, false);
            properties.clear();

            properties.put("sampleValue", "extendedTESTValue");
            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE,
                    "sling/exp/extended");
            testDate = Calendar.getInstance();
            testDate.setTimeZone(TimeZone.getTimeZone("UTC"));
            testDate.setTimeInMillis(0);
            testDate.set(2015, 6, 29);
            properties.put("date", testDate);
            ResourceUtil.getOrCreateResource(adminResolver, extendedComponentPath, properties, null, false);

            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE,
                    "sling/exp-request/extended");
            ResourceUtil.getOrCreateResource(adminResolver, extendedRequestComponentPath, properties, null, false);
            properties.clear();

            properties.put("sampleValue", "interfaceTESTValue");
            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE,
                    "sling/exp/interface");
            ResourceUtil.getOrCreateResource(adminResolver, interfaceComponentPath, properties, null, false);

            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE,
                    "sling/exp-request/interface");
            ResourceUtil.getOrCreateResource(adminResolver, interfaceRequestComponentPath, properties, null, false);
            properties.clear();

            adminResolver.commit();
        } finally {
            if (adminResolver != null && adminResolver.isLive()) {
                adminResolver.close();
            }
        }
    }

    @Test
    public void testExportToJSON() throws Exception {
        ResourceResolver resolver = null;
        try {
            resolver = rrFactory.getAdministrativeResourceResolver(null);
            final Resource baseComponentResource = resolver.getResource(baseComponentPath);
            Assert.assertNotNull(baseComponentResource);
            String jsonData = modelFactory.exportModelForResource(baseComponentResource, "jackson", String.class,
                    Collections.<String, String> emptyMap());
            Assert.assertTrue("JSON Data should contain the property value",
                    StringUtils.contains(jsonData, "baseTESTValue"));

            final Resource extendedComponentResource = resolver.getResource(extendedComponentPath);
            Assert.assertNotNull(extendedComponentResource);
            jsonData = modelFactory.exportModelForResource(extendedComponentResource, "jackson", String.class,
                    Collections.<String, String> emptyMap());
            Assert.assertTrue("JSON Data should contain the property value",
                    StringUtils.contains(jsonData, "extendedTESTValue"));

            final Resource interfaceComponentResource = resolver.getResource(interfaceComponentPath);
            Assert.assertNotNull(baseComponentResource);
            jsonData = modelFactory.exportModelForResource(interfaceComponentResource, "jackson", String.class,
                    Collections.<String, String> emptyMap());
            Assert.assertTrue("JSON Data should contain the property value",
                    StringUtils.contains(jsonData, "interfaceTESTValue"));
        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
    }

    @Test
    public void testExportToMap() throws Exception {
        ResourceResolver resolver = null;
        try {
            resolver = rrFactory.getAdministrativeResourceResolver(null);
            final Resource baseComponentResource = resolver.getResource(baseComponentPath);
            Assert.assertNotNull(baseComponentResource);
            Map<String, Object> data = modelFactory.exportModelForResource(baseComponentResource, "jackson", Map.class,
                    Collections.<String, String> emptyMap());
            Assert.assertEquals("Should have resource value", "baseTESTValue", data.get("sampleValue"));
            Assert.assertEquals("Should have resource value", "BASETESTVALUE", data.get("UPPER"));
        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
    }

    @Test
    public void testResourceServlets() throws Exception {
        ResourceResolver resolver = null;
        try {
            resolver = rrFactory.getAdministrativeResourceResolver(null);
            FakeResponse response = new FakeResponse();
            slingRequestProcessor.processRequest(new FakeRequest(baseComponentPath + ".model.json"), response, resolver);
            JSONObject obj = new JSONObject(response.getStringWriter().toString());
            Assert.assertEquals("application/json", response.getContentType());
            Assert.assertEquals("BASETESTVALUE", obj.getString("UPPER"));
            Assert.assertEquals(baseComponentPath, obj.getString("id"));

            response = new FakeResponse();
            slingRequestProcessor.processRequest(new FakeRequest(extendedComponentPath + ".model.json"), response, resolver);
            obj = new JSONObject(response.getStringWriter().toString());
            Assert.assertEquals("application/json", response.getContentType());
            Assert.assertEquals(extendedComponentPath, obj.getString("id"));
            Assert.assertEquals(testDate.getTimeInMillis(), obj.getLong("date"));

            response = new FakeResponse();
            slingRequestProcessor.processRequest(new FakeRequest(interfaceComponentPath + ".model.json"), response, resolver);
            obj = new JSONObject(response.getStringWriter().toString());
            Assert.assertEquals("application/json", response.getContentType());
            Assert.assertEquals(interfaceComponentPath, obj.getString("id"));
            Assert.assertEquals("interfaceTESTValue", obj.getString("sampleValue"));
        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
    }

    @Test
    public void testRequestServlets() throws Exception {
        ResourceResolver resolver = null;
        try {
            resolver = rrFactory.getAdministrativeResourceResolver(null);
            FakeResponse response = new FakeResponse();
            slingRequestProcessor.processRequest(new FakeRequest(baseRequestComponentPath + ".model.json"), response, resolver);
            JSONObject obj = new JSONObject(response.getStringWriter().toString());
            Assert.assertEquals("application/json", response.getContentType());
            Assert.assertEquals("BASETESTVALUE", obj.getString("UPPER"));
            Assert.assertEquals(baseRequestComponentPath, obj.getString("id"));

            response = new FakeResponse();
            slingRequestProcessor.processRequest(new FakeRequest(extendedRequestComponentPath + ".model.json"), response, resolver);
            obj = new JSONObject(response.getStringWriter().toString());
            Assert.assertEquals("application/json", response.getContentType());
            Assert.assertEquals(extendedRequestComponentPath, obj.getString("id"));
            Assert.assertEquals(testDate.getTimeInMillis(), obj.getLong("date"));

            response = new FakeResponse();
            slingRequestProcessor.processRequest(new FakeRequest(interfaceRequestComponentPath + ".model.json"), response, resolver);
            obj = new JSONObject(response.getStringWriter().toString());
            Assert.assertEquals("application/json", response.getContentType());
            Assert.assertEquals(interfaceRequestComponentPath, obj.getString("id"));
            Assert.assertEquals("interfaceTESTValue", obj.getString("sampleValue"));
        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
    }

    @Test
    public void testFailedExport() throws Exception {
        boolean thrown = false;
        try {
            ResourceResolver resolver = null;
            try {
                resolver = rrFactory.getAdministrativeResourceResolver(null);
                final Resource baseComponentResource = resolver.getResource(baseComponentPath);
                Assert.assertNotNull(baseComponentResource);
                String data = modelFactory.exportModelForResource(baseComponentResource, "jaxb", String.class,
                        Collections.<String, String>emptyMap());
                Assert.fail("Should have thrown missing serializer error.");
            } finally {
                if (resolver != null && resolver.isLive()) {
                    resolver.close();
                }
            }
        } catch (MissingExporterException e) {
            thrown = true;
            Assert.assertEquals("No exporter named jaxb supports java.lang.String.", e.getMessage());
        }
        Assert.assertTrue(thrown);

    }

}
