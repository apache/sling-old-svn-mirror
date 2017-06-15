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

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
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

        JsonObject json = JsonObjectCreator.create(resource).build();

        assertEquals(10, json.getInt("byte"));
        assertEquals(10, json.getInt("int"));
        assertEquals("10.0", json.getString("float"));
        assertEquals("10.0", json.getString("double"));
        assertEquals("10", json.getString("string"));
        assertEquals(false, json.getBoolean("boolean"));
        assertEquals("object", json.getString("object"));
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

        JsonObject json = JsonObjectCreator.create(resource).build();
        assertEquals(0, json.getJsonArray("emptyArray").size());
        JsonArray array;
        array = json.getJsonArray("stringArray");
        assertEquals("10", array.getString(0));
        array = json.getJsonArray("intArray");
        assertEquals(10, array.getInt(0));
        array = json.getJsonArray("doubleArray");
        assertEquals("10.0", array.getString(0));
        array = json.getJsonArray("byteArray");
        assertEquals("10", array.getString(0));
        array = json.getJsonArray("floatArray");
        assertEquals("10.0", array.getString(0));
        array = json.getJsonArray("shortArray");
        assertEquals("10", array.getString(0));
        array = json.getJsonArray("longArray");
        assertEquals(10L, array.getJsonNumber(0).longValue());
        array = json.getJsonArray("booleanArray");
        assertEquals(true, array.getBoolean(0));
        array = json.getJsonArray("charArray");
        assertEquals("a", array.getString(0));

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

        JsonObject json = JsonObjectCreator.create(resource).build();
        assertEquals("v1", json.getString("p1"));
        JsonObject path = json.getJsonObject("path");
        assertEquals("v2", path.getString("p2"));

    }
}