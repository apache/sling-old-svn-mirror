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
package org.apache.sling.resource.inventory.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.testing.resourceresolver.MockHelper;
import org.apache.sling.testing.resourceresolver.MockResource;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonObjectCreatorTest {

    private ResourceResolver resolver;

    @Before
    public void setup() throws LoginException {
        resolver = new MockResourceResolverFactory()
                .getAdministrativeResourceResolver(null);
    }

    @Test
    public void testCreate() throws Exception {

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("byte", 0x0A);
        properties.put("int", 10);
        properties.put("long", 10L);
        properties.put("float", 10.0f);
        properties.put("double", 10.0d);
        properties.put("string", "10");
        properties.put("boolean", false);
        properties.put("object", new Object(){
            public String toString() {
                return "object";
            }
        });
        Resource resource = new MockResource("/some/path", properties, resolver);

        JSONObject json = JsonObjectCreator.create(resource);

        assertEquals(10, json.get("byte"));
        assertEquals(10, json.get("int"));
        assertEquals("10.0", json.get("float"));
        assertEquals("10.0", json.get("double"));
        assertEquals("10", json.get("string"));
        assertEquals(false, json.get("boolean"));
        assertEquals("object", json.get("object"));
    }

    @Test
    public void testCreateArray() throws Exception {

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("emptyArray", new Object[]{});
        properties.put("stringArray", new String[]{"10","100"});
        properties.put("intArray", new int[]{10, 100});
        properties.put("doubleArray", new double[]{10d, 100d});
        properties.put("byteArray", new byte[]{0x0A, 0x64});
        properties.put("floatArray", new float[]{10.0f, 100.0f});
        properties.put("shortArray", new short[]{10, 100});
        properties.put("longArray", new long[]{10, 100});
        properties.put("booleanArray", new boolean[]{true, false});
        properties.put("charArray", new char[]{'a', 'b'});
        Resource resource = new MockResource("/some/path", properties, resolver);

        JSONObject json = JsonObjectCreator.create(resource);
        assertEquals(0, json.getJSONArray("emptyArray").length());
        JSONArray array;
        array = json.getJSONArray("stringArray");
        assertEquals("10", array.get(0));
        array = json.getJSONArray("intArray");
        assertEquals(10, array.get(0));
        array = json.getJSONArray("doubleArray");
        assertEquals("10.0", array.get(0));
        array = json.getJSONArray("byteArray");
        assertEquals("10", array.get(0));
        array = json.getJSONArray("floatArray");
        assertEquals("10.0", array.get(0));
        array = json.getJSONArray("shortArray");
        assertEquals("10", array.get(0));
        array = json.getJSONArray("longArray");
        assertEquals(10L, array.get(0));
        array = json.getJSONArray("booleanArray");
        assertEquals(true, array.get(0));
        array = json.getJSONArray("charArray");
        assertEquals("a", array.get(0));

    }

    @Test
    public void testCreateDeep() throws Exception {

        MockHelper.create(resolver)
                .resource("/some")
                .p("p1", "v1")
                .resource("/some/path")
                .p("p2", "v2")
                .commit();
        Resource resource = resolver.getResource("/some");

        JSONObject json = JsonObjectCreator.create(resource);
        assertEquals("v1", json.get("p1"));
        JSONObject path = json.getJSONObject("path");
        assertEquals("v2", path.get("p2"));

    }
}