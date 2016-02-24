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
package org.apache.sling.nosql.generic.adapter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.apache.sling.nosql.generic.adapter.MapConverter;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class MapConverterTest {

    @Test
    public void testMapArrayToList() throws Exception {
        Map<String, Object> result = MapConverter.mapArrayToList(Maps.newHashMap(ImmutableMap.<String, Object>builder()
                .put("prop1", "value1")
                .put("prop2", 2)
                .put("stringArray", new String[] { "value1", "value2" })
                .put("integerArray", new Integer[] { 1, 2, 3 })
                .put("integerArray2", new int[] { 1, 2, 3 })
                .put("longArray", new long[] { 1L, 2L })
                .put("doubleArray", new double[] { 1.1d, 1.2d })
                .put("booleanArray", new boolean[] { true, false })
                .build()));

        assertEquals("prop1", "value1", result.get("prop1"));
        assertEquals("prop2", 2, result.get("prop2"));
        assertEquals("stringArray", ImmutableList.of("value1", "value2"), result.get("stringArray"));
        assertEquals("integerArray", ImmutableList.of(1, 2, 3), result.get("integerArray"));
        assertEquals("integerArray2", ImmutableList.of(1, 2, 3), result.get("integerArray2"));
        assertEquals("longArray", ImmutableList.of(1L, 2L), result.get("longArray"));
        assertEquals("doubleArray", ImmutableList.of(1.1d, 1.2d), result.get("doubleArray"));
        assertEquals("booleanArray", ImmutableList.of(true, false), result.get("booleanArray"));
    }

    @Test
    public void testMapListToArray() throws Exception {
        Map<String, Object> result = MapConverter.mapListToArray(Maps.newHashMap(ImmutableMap.<String, Object>builder()
                .put("prop1", "value1")
                .put("prop2", 2)
                .put("stringArray", ImmutableList.of("value1", "value2"))
                .put("integerArray", ImmutableList.of(1, 2, 3))
                .put("longArray", ImmutableList.of(1L, 2L))
                .put("doubleArray", ImmutableList.of(1.1d, 1.2d))
                .put("booleanArray", ImmutableList.of(true, false))
                .build()));

        assertEquals("prop1", "value1", result.get("prop1"));
        assertEquals("prop2", 2, result.get("prop2"));
        assertArrayEquals("stringArray", new String[] { "value1", "value2" }, (String[]) result.get("stringArray"));
        assertArrayEquals("integerArray", new Integer[] { 1, 2, 3 }, (Integer[]) result.get("integerArray"));
        assertArrayEquals("longArray", new Long[] { 1L, 2L }, (Long[]) result.get("longArray"));
        assertArrayEquals("doubleArray", new Double[] { 1.1d, 1.2d }, (Double[]) result.get("doubleArray"));
        assertArrayEquals("booleanArray", new Boolean[] { true, false }, (Boolean[]) result.get("booleanArray"));
    }

}
