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
package org.apache.sling.fsprovider.internal.mapper;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ValueMap;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class ValueMapUtilTest {

    @Test
    public void testToValueMap() {
        Map<String,Object> content = new HashMap<>();
        content.put("stringProp", "abc");
        content.put("intProp", 123);
        content.put("childNode", ImmutableMap.<String,Object>of());
        content.put("stringArray", new String[] { "a", "b", "c" });
        content.put("stringList", ImmutableList.of("ab", "cd"));
        content.put("intList", ImmutableList.of(12, 34));
        
        ValueMap props = ValueMapUtil.toValueMap(content);
        assertEquals("abc", props.get("stringProp", String.class));
        assertEquals((Integer)123, props.get("intProp", 0));
        assertNull(props.get("childNode"));
        assertArrayEquals(new String[] { "a", "b", "c" }, props.get("stringArray", String[].class));
        assertArrayEquals(new String[] { "ab", "cd" }, props.get("stringList", String[].class));
        assertArrayEquals(new Integer[] { 12, 34 }, props.get("intList", Integer[].class));
    }

}
