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
package org.apache.sling.testing.mock.osgi;

import static org.junit.Assert.assertEquals;

import java.util.Dictionary;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class MapUtilTest {

    @Test
    public void testMapDictionary() {
        Map<String,Object> map = ImmutableMap.<String, Object>of("param1", "var1", "param2", 123, "param3", true);
        
        Dictionary<String, Object> dict = MapUtil.toDictionary(map);
        Map<String,Object> convertedMap = MapUtil.toMap(dict);
        
        assertEquals(map, convertedMap);
    }
    
    @Test
    public void testMapObjectVarargs() {
        Map<String, Object> convertedMap = MapUtil.toMap("param1", "var1", "param2", 123, "param3", true);
        
        assertEquals(ImmutableMap.<String, Object>of("param1", "var1", "param2", 123, "param3", true), convertedMap);
    }
    
    @Test
    public void testDictionaryObjectVarargs() {
        Dictionary<String, Object> dict = MapUtil.toDictionary("param1", "var1", "param2", 123, "param3", true);
        Map<String,Object> convertedMap = MapUtil.toMap(dict);
        
        assertEquals(ImmutableMap.<String, Object>of("param1", "var1", "param2", 123, "param3", true), convertedMap);
    }
    
}
