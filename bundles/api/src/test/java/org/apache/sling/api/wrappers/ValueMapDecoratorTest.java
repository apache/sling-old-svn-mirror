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
import org.junit.Test;

public class ValueMapDecoratorTest {
	
	private Map<String, Object> map;
	private ValueMap valueMap;
	
	@Before
	public void setUp() {
		map = new HashMap<String,Object>();
		valueMap = new ValueMapDecorator(map);
	}

	// SLING-4178
	@Test
	public void testIncompatibleTypeInArray() {
		map.put("prop1", new String[] {"test", "test2"});
		map.put("prop2", "test");
		Assert.assertNull("Not convertible type should return null", valueMap.get("prop1", Integer[].class));
		Assert.assertNull("Not convertible type should return null", valueMap.get("prop2", Integer[].class));
	}
	
	// SLING-662
	@Test
	public void testGettingArraysFromSingleValueEntries() {
		map.put("prop1", "test");
		map.put("prop2", "1");
		Assert.assertArrayEquals("Even though the underlying entry is single-value if should be enclosed in a single element array", new String[] {"test"}, valueMap.get("prop1", String[].class));
		Assert.assertArrayEquals("Even though the underlying entry is single-value if should be enclosed in a single element array", new Integer[] {1}, valueMap.get("prop2", Integer[].class));
	}
	
	@Test
	public void testGettingArraysFromMultiValueEntries() {
		map.put("prop1", new String[] {"test", "test2"});
		map.put("prop2", new String[] {"1", "2"});
		Assert.assertArrayEquals("Could not get values from underlying array", new String[] {"test", "test2"}, valueMap.get("prop1", String[].class));
		Assert.assertArrayEquals("Conversion to Integer was not possible", new Integer[] {1, 2}, valueMap.get("prop2", Integer[].class));
	}
	
	@Test
	public void testGettingInvalidEntryWithDefaultValue() {
		Assert.assertEquals(Integer.valueOf(1), valueMap.get("prop1", 1));
		Assert.assertEquals("test", valueMap.get("prop1", "test"));
	}
}
