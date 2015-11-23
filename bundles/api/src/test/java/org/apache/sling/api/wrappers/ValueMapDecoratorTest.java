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
package org.apache.sling.api.wrappers;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ValueMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ValueMapDecoratorTest {

    private Map<String, Object> map;
    private ValueMap valueMap;

    @Before
    public void setUp() {
        map = new HashMap<String, Object>();
        valueMap = new ValueMapDecorator(map);
    }

    // SLING-4178
    @Test
    public void testIncompatibleTypeInArray() {
        map.put("prop1", new String[] { "test", "test2" });
        map.put("prop2", "test");
        Assert.assertNull("Not convertible type should return null", valueMap.get("prop1", Integer[].class));
        Assert.assertNull("Not convertible type should return null", valueMap.get("prop2", Integer[].class));
    }

    // SLING-662
    @Test
    public void testGettingArraysFromSingleValueEntries() {
        map.put("prop1", "test");
        map.put("prop2", "1");
        Assert.assertArrayEquals(
                "Even though the underlying entry is single-value if should be enclosed in a single element array",
                new String[] { "test" }, valueMap.get("prop1", String[].class));
        Assert.assertArrayEquals(
                "Even though the underlying entry is single-value if should be enclosed in a single element array",
                new Integer[] { 1 }, valueMap.get("prop2", Integer[].class));
    }

    @Test
    public void testGettingArraysFromMultiValueEntries() {
        map.put("prop1", new String[] { "test", "test2" });
        map.put("prop2", new String[] { "1", "2" });
        Assert.assertArrayEquals("Could not get values from underlying array", new String[] { "test", "test2" },
                valueMap.get("prop1", String[].class));
        Assert.assertArrayEquals("Conversion to Integer was not possible", new Integer[] { 1, 2 },
                valueMap.get("prop2", Integer[].class));
    }

    @Test
    public void testGettingSingleValuesFromMultiValueEntries() {
        map.put("prop1", new String[] { "test", "test2" });
        map.put("prop2", new String[] { "1", "2" });
        Assert.assertEquals("First element from underlying array should be returned", "test",
                valueMap.get("prop1", String.class));
        Assert.assertEquals("First element from underlying array should be returned", Integer.valueOf(1),
                valueMap.get("prop2", Integer.class));
    }

    @Test
    public void testGettingInvalidEntryWithDefaultValue() {
        Assert.assertEquals(Integer.valueOf(1), valueMap.get("prop1", 1));
        Assert.assertEquals("test", valueMap.get("prop1", "test"));
    }

    @Test
    public void testPrimitiveTypes() {
        map.put("prop1", new String[] { "1", "2" });
        Assert.assertNull("ValueMap should not support conversion to primitive type", valueMap.get("prop1", int.class));
        Assert.assertNull("ValueMap should not support conversion to array of primitive type",
                valueMap.get("prop1", int[].class));
    }

    @Test
    public void testEqualsAndHashCodeOfEqualValueMapsWithNonArrayTypes() {
        map.put("prop1", "some string");
        ValueMapDecorator valueMap2 = new ValueMapDecorator(map);
        Assert.assertTrue("Two ValueMapDecorators based on the same map should be equal", valueMap.equals(valueMap2));
        Assert.assertEquals("Two equal ValueMapDecorators should have the same hash code", valueMap.hashCode(),
                valueMap2.hashCode());

        ValueMapDecorator valueMap3 = new ValueMapDecorator(new HashMap<String, Object>());
        valueMap3.put("prop1", "some string");
        Assert.assertEquals(valueMap, valueMap3);
        Assert.assertEquals("Two equal ValueMapDecorators should have the same hash code", valueMap.hashCode(),
                valueMap3.hashCode());

        Assert.assertEquals(map, valueMap);
        Assert.assertEquals(valueMap, map);
    }

    @Ignore("SLING-4784")
    @Test
    public void testEqualsAndHashCodeOfEqualValueMapsWithArrayTypes() {
        map.put("prop1", new String[] { "1", "2" });
        ValueMapDecorator valueMap2 = new ValueMapDecorator(map);
        Assert.assertTrue("Two ValueMapDecorators based on the same map should be equal", valueMap.equals(valueMap2));
        Assert.assertEquals("Two equal ValueMapDecorators should have the same hash code", valueMap.hashCode(),
                valueMap2.hashCode());

        ValueMapDecorator valueMap3 = new ValueMapDecorator(new HashMap<String, Object>());
        valueMap3.put("prop1", new String[] { "1", "2" });
        Assert.assertEquals(valueMap, valueMap3);
        Assert.assertEquals("Two equal ValueMapDecorators should have the same hash code", valueMap.hashCode(),
                valueMap3.hashCode());
    }

    @Test
    public void testEqualsOfInequalValueMapsWithNonArrayTypes() {
        valueMap.put("prop", "value");
        ValueMapDecorator valueMap2 = new ValueMapDecorator(new HashMap<String, Object>());
        valueMap2.put("prop", "value2");
        Assert.assertFalse("Two ValueMapDecorators based on maps with different entries should not be equal",
                valueMap.equals(valueMap2));

    }

    @Test
    public void testEqualsOfInequalValueMapsWithArrayTypes() {
        valueMap.put("prop", new String[] { "1", "2" });
        ValueMapDecorator valueMap2 = new ValueMapDecorator(new HashMap<String, Object>());
        valueMap2.put("prop", new String[] { "3", "4" });
        Assert.assertFalse("Two ValueMapDecorators based on maps with different entries should not be equal",
                valueMap.equals(valueMap2));
    }
}
