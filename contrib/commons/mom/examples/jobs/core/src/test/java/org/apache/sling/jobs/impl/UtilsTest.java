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
package org.apache.sling.jobs.impl;

import org.apache.sling.jobs.impl.spi.MapValueAdapter;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by ieb on 05/04/2016.
 */
public class UtilsTest {

    @Test
    public void testGenerateId() throws Exception {
        // check the IDs don't clash. Cant check between separate JVMs without separate JVMs.
        Set<String> ids = new HashSet<String>();
        for ( int i = 0; i < 100; i++ ) {
            ids.add(Utils.generateId());
        }
        assertEquals(100, ids.size());

    }

    @Test
    public void testToMapValue() throws Exception {
        final Map<String, Object> testMap = new HashMap<String, Object>();
        testMap.put("v", "a");
        Object mv = Utils.toMapValue(testMap);
        assertEquals(testMap.size(), ((Map)mv).size());
        assertEquals("a", ((Map)mv).get("v"));

        mv = Utils.toMapValue(new MapValueAdapter() {
            @Override
            public void fromMapValue(Object mapValue) {

            }

            @Override
            public Object toMapValue() {
                return testMap;
            }
        });
        assertEquals(testMap.size(), ((Map)mv).size());
        assertEquals("a", ((Map)mv).get("v"));

        try {
            mv = Utils.toMapValue(new HashSet<String>());
            fail("Should have rejected a hash set");
        } catch ( IllegalArgumentException e) {
            // ok
        }

        try {
            mv = Utils.toMapValue(new MapValueAdapter() {
                @Override
                public void fromMapValue(Object mapValue) {

                }

                @Override
                public Object toMapValue() {
                    return "should not be allowed as a MapValue is supposed to be a Map";
                }
            });
            fail("Should have rejected a string from map Utils.toMapValue even if internal");
        } catch ( IllegalArgumentException e) {
            // ok
        }

    }

    @Test
    public void testGetRequired() throws Exception {
        final Map<String, Object> testMap = new HashMap<String, Object>();
        testMap.put("v", "a");
        assertEquals("a",Utils.getRequired(testMap, "v"));
        try {
            Utils.getRequired(testMap, "z");
            fail("Expected z to be missing and fail required test");
        } catch ( IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void testGetOptional() throws Exception {
        final Map<String, Object> testMap = new HashMap<String, Object>();
        testMap.put("v", "a");
        assertEquals("a", Utils.getOptional(testMap, "v", "xx"));
        assertEquals("xx",Utils.getOptional(testMap, "z", "xx"));
    }
}