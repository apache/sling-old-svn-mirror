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
package org.apache.sling.caconfig.impl.override;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class OverrideStringParserTest {

    private static final Map<String,Object> BASICTYPES_MAP = ImmutableMap.<String,Object>builder()
            .put("param1", "value1")
            .put("param2", "value2")
            .put("param3", 555)
            .put("param4", 1.23d)
            .put("param5", true)
            .build();
    
    private static final Map<String,Object> BASICTYPES_ARRAY_MAP = ImmutableMap.<String,Object>builder()
            .put("param1", new String[] { "v1a", "v1b" })
            .put("param2", new String[] { "v2a", "v2b" })
            .put("param3", new Integer[] { 555, 666 })
            .put("param4", new Double[] { 1.23d, 2.34d })
            .put("param5", new Boolean[] { true, false })
            .put("param6", new String[0])
            .build();
    
    @Test
    public void testEmptyList() {
        List<OverrideItem> result = parse();
        assertTrue(result.isEmpty());
    }
    
    @Test
    public void testBasicTypes() {
        List<OverrideItem> result = parse(
                "configName/param1=\"value1\"",
                "configName/param2='value2'",
                "configName/param3=555",
                "configName/param4=1.23",
                "configName/param5=true");
        
        assertEquals(1, result.size());
        OverrideItem item = result.get(0);
        assertNull(item.getPath());
        assertEquals("configName", item.getConfigName());
        assertEquals(BASICTYPES_MAP, item.getProperties());
        assertFalse(item.isAllProperties());
    }

    @Test
    public void testBasicTypesArray() {
        List<OverrideItem> result = parse(
                "config.name/param1=[\"v1a\",\"v1b\"]",
                "config.name/param2=['v2a','v2b']",
                "config.name/param3=[555,666]",
                "config.name/param4=[1.23,2.34]",
                "config.name/param5=[true,false]",
                "config.name/param6=[]");
        
        assertEquals(1, result.size());
        OverrideItem item = result.get(0);
        assertNull(item.getPath());
        assertEquals("config.name", item.getConfigName());
        for (Map.Entry<String,Object> entry : item.getProperties().entrySet()) {
            assertArrayEquals("array " + entry.getKey(), (Object[])BASICTYPES_ARRAY_MAP.get(entry.getKey()), (Object[])item.getProperties().get(entry.getKey()));
        }
        assertFalse(item.isAllProperties());
    }

    @Test
    public void testBasicTypesJson() {
        List<OverrideItem> result = parse(
                "configName={\"param1\":\"value1\","
                + "'param2':'value2',"
                + "param3:555,"
                + "param4:1.23,"
                + "param5:true}");

        assertEquals(1, result.size());
        OverrideItem item = result.get(0);
        assertNull(item.getPath());
        assertEquals("configName", item.getConfigName());
        assertEquals(BASICTYPES_MAP, item.getProperties());
        assertTrue(item.isAllProperties());
    }

    @Test
    public void testBasicTypesJsonArray() {
        List<OverrideItem> result = parse(
                "configName={\"param1\":[\"v1a\",\"v1b\"],"
                + "'param2':['v2a','v2b'],"
                + "param3:[555,666],"
                + "param4:[1.23,2.34],"
                + "param5:[true,false],"
                + "param6:[]}");

        assertEquals(1, result.size());
        OverrideItem item = result.get(0);
        assertNull(item.getPath());
        assertEquals("configName", item.getConfigName());
        for (Map.Entry<String,Object> entry : item.getProperties().entrySet()) {
            assertArrayEquals("array " + entry.getKey(), (Object[])BASICTYPES_ARRAY_MAP.get(entry.getKey()), (Object[])item.getProperties().get(entry.getKey()));
        }
        assertTrue(item.isAllProperties());
    }

    @Test
    public void testWithPath() {
        List<OverrideItem> result = parse(
                "[/a/b]configName/sub1/param1=\"value1\"",
                "configName/sub2/param2=\"value2\"");
        
        assertEquals(2, result.size());
        
        OverrideItem item1 = result.get(0);
        assertEquals("/a/b", item1.getPath());
        assertEquals("configName/sub1", item1.getConfigName());
        assertEquals("value1", item1.getProperties().get("param1"));
        assertFalse(item1.isAllProperties());

        OverrideItem item2 = result.get(1);
        assertNull(item2.getPath());
        assertEquals("configName/sub2", item2.getConfigName());
        assertEquals("value2", item2.getProperties().get("param2"));
        assertFalse(item2.isAllProperties());
    }

    @Test
    public void testCombined() {
        List<OverrideItem> result = parse(
                "[/a/b]configName/param1=\"value1\"",
                "configName/param2=\"value2\"",
                "[/a/b]configName={\"param1\":\"value1\","
                        + "'param2':'value2',"
                        + "param3:555,"
                        + "param4:1.23,"
                        + "param5:true}");
        
        assertEquals(3, result.size());
        
        OverrideItem item1 = result.get(0);
        assertEquals("/a/b", item1.getPath());
        assertEquals("configName", item1.getConfigName());
        assertEquals("value1", item1.getProperties().get("param1"));
        assertFalse(item1.isAllProperties());

        OverrideItem item2 = result.get(1);
        assertNull(item2.getPath());
        assertEquals("configName", item2.getConfigName());
        assertEquals("value2", item2.getProperties().get("param2"));
        assertFalse(item2.isAllProperties());

        OverrideItem item3 = result.get(2);
        assertEquals("/a/b", item3.getPath());
        assertEquals("configName", item3.getConfigName());
        assertEquals(BASICTYPES_MAP, item3.getProperties());
        assertTrue(item3.isAllProperties());
    }

    @Test
    public void testInvalidSyntax() {
        List<OverrideItem> result = parse(
                "/configName/param1=\"value1\"",
                "configName/../param1=\"value1\"",
                "[/a/b]=\"value1\"",
                "[/a/b]configName=\"value1\"",
                "[/a/../b]configName/param1=\"value1\"",
                "[]configName=\"value1\"",
                "configName/param2:'value2'",
                "configName/param3",
                "configName/param3=",
                "[[/a/b]]configName/param4=1.23",
                "[a/b]configName/param5=true",
                "configName/param1=null");
        
        // all ignored
        assertEquals(0, result.size());
    }

    @Test
    public void testInvalidJson() {
        List<OverrideItem> result = parse(
                "configName1={param1:\"value1\"",
                "configName1={\"param1/xyz\":\"value1\"}",
                "configName1={param1:[\"value1\",123]}",
                "configName2={param1:{\"subparam1\":\"value1\"}}",
                "configName1={param1:null}",
                "configName1={'param1:'value1'}");

        // all ignored
        assertEquals(0, result.size());
    }

    private List<OverrideItem> parse(String... values) {
        return ImmutableList.copyOf(OverrideStringParser.parse(ImmutableList.copyOf(values)));
    }

}
