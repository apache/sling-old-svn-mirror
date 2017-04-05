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
package org.apache.sling.maven.bundlesupport;

import static org.apache.sling.maven.bundlesupport.JsonSupport.accumulate;
import static org.apache.sling.maven.bundlesupport.JsonSupport.parseArray;
import static org.apache.sling.maven.bundlesupport.JsonSupport.parseObject;
import static org.apache.sling.maven.bundlesupport.JsonSupport.toJson;
import static org.apache.sling.maven.bundlesupport.JsonSupport.validateJsonStructure;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class JsonSupportTest {

    @Test
    public void testParseObject() {
        JsonObject obj = parseObject("{\"prop1\":123}");
        assertEquals(123, obj.getInt("prop1"));
    }

    @Test
    public void testParseArray() {
        JsonArray array = parseArray("[{\"prop1\":123}]");
        assertEquals(1, array.size());
        assertEquals(123, array.getJsonObject(0).getInt("prop1"));
    }

    @Test
    public void testValidateJsonStructure() {
        validateJsonStructure("{\"prop1\":123}");
        validateJsonStructure("[{\"prop1\":123}]");
    }

    @Test(expected=JsonException.class)
    public void testValidateJsonStructure_Invalid() {
        validateJsonStructure("wurstbrot");
    }

    @Test
    public void testAccumulate_NewValue() {
        Map<String,Object> map = new HashMap<>();
        accumulate(map, "prop1", "value1");
        assertEquals(ImmutableMap.of("prop1", "value1"), map);
    }

    @Test
    public void testAccumulate_ExistingValue() {
        Map<String,Object> map = new HashMap<>();
        map.put("prop1", "value1");
        accumulate(map, "prop1", "value2");
        assertEquals(ImmutableMap.of("prop1", ImmutableList.of("value1", "value2")), map);
    }

    @Test
    public void testAccumulate_ExistingArray() {
        Map<String,Object> map = new HashMap<>();
        map.put("prop1", ImmutableList.of("value1","value2"));
        accumulate(map, "prop1", "value3");
        assertEquals(ImmutableMap.of("prop1", ImmutableList.of("value1", "value2","value3")), map);
    }

    @Test
    public void testToJson() {
        Map<String,Object> map = ImmutableMap.<String, Object>builder()
                .put("prop1", "value1")
                .put("prop2", ImmutableList.of("value2","value3"))
                .put("prop3", ImmutableMap.of("prop4", "value4"))
                .build();
        JsonObject obj = toJson(map);
        
        assertEquals("value1", obj.getString("prop1"));
        
        JsonArray array = obj.getJsonArray("prop2");
        assertEquals(2, array.size());
        assertEquals("value2", array.getString(0));
        assertEquals("value3", array.getString(1));
        
        JsonObject prop3 = obj.getJsonObject("prop3");
        assertEquals("value4", prop3.getString("prop4"));
    }

}
