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

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.text.Format;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.sling.api.SlingConstants;
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
    private final String doubledComponentPath = "/content/exp/doubledComponent";
    private final String childComponentPath = "/content/exp/childComponent";
    private final String extendedComponentPath = "/content/exp/extendedComponent";
    private final String interfaceComponentPath = "/content/exp/interfaceComponent";
    private final String baseRequestComponentPath = "/content/exp-request/baseComponent";
    private final String extendedRequestComponentPath = "/content/exp-request/extendedComponent";
    private final String interfaceRequestComponentPath = "/content/exp-request/interfaceComponent";
    private Calendar testDate;

    private Format dateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Before
    public void setup() throws Exception {
        ResourceResolver adminResolver = null;
        try {
            adminResolver = rrFactory.getAdministrativeResourceResolver(null);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("sampleValue", "baseTESTValue");
            properties.put("sampleBooleanValue", true);
            properties.put("sampleLongValue", 1l);
            properties.put("sampleDoubleValue", 1d);
            properties.put("sampleArray", new String[] { "a", "b", "c" });
            properties.put("sampleEmptyArray", new String[0]);
            properties.put("sampleBinary", new ByteArrayInputStream("abc".getBytes("UTF-8")));
            properties.put("sampleBinaryArray", new InputStream[] {
                    new ByteArrayInputStream("abc".getBytes("UTF-8")),
                    new ByteArrayInputStream("def".getBytes("UTF-8"))
            });
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

            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE,
                    "sling/exp/doubled");
            ResourceUtil.getOrCreateResource(adminResolver, doubledComponentPath, properties, null, false);


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

            JSONObject parsed = new JSONObject(jsonData);
            JSONObject resource = parsed.getJSONObject("resource");
            Assert.assertEquals(3, resource.getJSONArray("sampleArray").length());
            Assert.assertEquals(1.0d, resource.getDouble("sampleDoubleValue"), .1);
            Assert.assertEquals(2, resource.getJSONArray(":sampleBinaryArray").length());
            Assert.assertTrue(resource.getBoolean("sampleBooleanValue"));
            Assert.assertEquals(1, resource.getLong("sampleLongValue"));
            Assert.assertEquals(3, resource.getLong(":sampleBinary"));
            Assert.assertEquals(0, resource.getJSONArray("sampleEmptyArray").length());

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
    public void testExportToTidyJSON() throws Exception {
        ResourceResolver resolver = null;
        try {
            resolver = rrFactory.getAdministrativeResourceResolver(null);
            final Resource baseComponentResource = resolver.getResource(baseComponentPath);
            Assert.assertNotNull(baseComponentResource);
            String jsonData = modelFactory.exportModelForResource(baseComponentResource, "jackson", String.class,
                    Collections.<String, String>emptyMap());
            Assert.assertFalse(jsonData.contains(System.lineSeparator()));

            jsonData = modelFactory.exportModelForResource(baseComponentResource, "jackson", String.class,
                    Collections.<String, String>singletonMap("tidy", "true"));
            Assert.assertTrue(jsonData.contains(System.lineSeparator()));
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
            String stringOutput = response.getStringWriter().toString();

            Assert.assertTrue(stringOutput.startsWith("{\"UPPER\":"));

            JSONObject obj = new JSONObject(stringOutput);
            Assert.assertEquals("application/json", response.getContentType());
            Assert.assertEquals("BASETESTVALUE", obj.getString("UPPER"));
            Assert.assertTrue(obj.has("testBindingsObject"));
            JSONObject testBindingsObject = obj.getJSONObject("testBindingsObject");
            Assert.assertEquals("value", testBindingsObject.getString("name"));
            Assert.assertTrue(obj.has("testBindingsObject2"));
            JSONObject testBindingsObject2 = obj.getJSONObject("testBindingsObject2");
            Assert.assertEquals("value2", testBindingsObject2.getString("name2"));
            Assert.assertEquals(baseRequestComponentPath, obj.getString("id"));

            response = new FakeResponse();
            slingRequestProcessor.processRequest(new FakeRequest(extendedRequestComponentPath + ".model.json"), response, resolver);
            obj = new JSONObject(response.getStringWriter().toString());
            Assert.assertEquals("application/json", response.getContentType());
            Assert.assertEquals(extendedRequestComponentPath, obj.getString("id"));
            Assert.assertEquals(dateFormat.format(testDate), obj.getString("date"));

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
    public void testDoubledServlets() throws Exception {
        ResourceResolver resolver = null;
        try {
            resolver = rrFactory.getAdministrativeResourceResolver(null);
            FakeResponse response = new FakeResponse();
            slingRequestProcessor.processRequest(new FakeRequest(doubledComponentPath + ".firstmodel.json"), response, resolver);

            JSONObject obj = new JSONObject(response.getStringWriter().toString());
            Assert.assertEquals("application/json", response.getContentType());
            Assert.assertEquals("first", obj.getString("value"));

            response = new FakeResponse();
            slingRequestProcessor.processRequest(new FakeRequest(doubledComponentPath + ".secondmodel.json"), response, resolver);
            obj = new JSONObject(response.getStringWriter().toString());
            Assert.assertEquals("application/json", response.getContentType());
            Assert.assertEquals("second", obj.getString("value"));
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
