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

package org.apache.sling.jms;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import  org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ieb on 31/03/2016.
 */
public class JsonTest {


    private static final Logger LOGGER = LoggerFactory.getLogger(JsonTest.class);
    private Map<String, Object> testMap;

    @Before
    public void setup() {
        testMap = JsonTest.createTestMap();
    }

    public static Map<String,Object> createTestMap() {
        Map<String, Object> testMap = new HashMap<String, Object>();
        Map<String, Object> innerTestMap = new HashMap<String, Object>();
        Map<String, Object> inner2TestMap = new HashMap<String, Object>();
        Map<String, Object> listMap = new HashMap<String, Object>();

        listMap.put("listMaplong",100L);
        listMap.put("listMapboolean",true);
        listMap.put("listMapstring","A String");
        listMap.put("listMapdouble",1.001D);

        testMap.put("long",100L);
        testMap.put("boolean",true);
        testMap.put("string","A String");
        testMap.put("double",1.001D);
        testMap.put("map",innerTestMap);
        innerTestMap.put("innerlong",100L);
        innerTestMap.put("innerboolean",true);
        innerTestMap.put("innerstring","A String");
        innerTestMap.put("innerdouble",1.001D);
        innerTestMap.put("innermap",inner2TestMap);
        inner2TestMap.put("inner2long",100L);
        inner2TestMap.put("inner3boolean",true);
        inner2TestMap.put("inner3string","A String");
        inner2TestMap.put("inner3double",1.001D);
        inner2TestMap.put("inner3list", Arrays.asList("string1","string2", "string2"));
        inner2TestMap.put("inner3listofMaps", Arrays.asList(listMap, listMap, listMap));
        return testMap;
    }

    @Test
    public void testJson() throws Exception {
        checkEquals(testMap, Json.toMap(Json.toJson(testMap)));

    }

    public static void checkEquals(Map<String, Object> expected, Map<String, Object> actual) {
        LOGGER.info("Expected {}", expected);
        LOGGER.info("Actual   {}", actual);
        for(Map.Entry<String, Object> e : expected.entrySet()) {
            if ( e.getValue() instanceof Map ) {
                checkEquals((Map<String, Object>) e.getValue(), (Map<String, Object>) actual.get(e.getKey()));
            } else if ( e.getValue() instanceof List ) {
                checkEquals((List<Object>) e.getValue(), (List<Object>) actual.get(e.getKey()));
            } else {
                if ( e.getValue() == null && actual.get(e.getKey()) != null ) {
                    LOGGER.info("Expected value for {}  is null but actual is {} ",  e.getKey(), actual.get(e.getKey()));
                }
                if ( e.getValue() != null && !e.getValue().equals(actual.get(e.getKey()))) {
                    LOGGER.info("Expected value for {}  is {} but actual is {}",  new Object[]{e.getKey(), e.getValue(), actual.get(e.getKey())});
                    LOGGER.info("Expected value for {}  is {} but actual is {}",  new Object[]{e.getKey(), e.getValue().getClass(), actual.get(e.getKey()).getClass()});
                }
                Assert.assertEquals(e.getValue(), actual.get(e.getKey()));
            }
        }
        LOGGER.info("Maps equal ok");
    }

    private static void checkEquals(List<Object> expected, List<Object> actual) {
        Assert.assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            Object e = expected.get(i);
            Object a = actual.get(i);
            if ( e instanceof Map ) {
                checkEquals((Map<String, Object>) e, (Map<String, Object>) a);
            } else if ( e instanceof List ) {
                checkEquals((List<Object>) e, (List<Object>) a);
            } else {
                Assert.assertEquals(e,a);
            }
        }
    }

}