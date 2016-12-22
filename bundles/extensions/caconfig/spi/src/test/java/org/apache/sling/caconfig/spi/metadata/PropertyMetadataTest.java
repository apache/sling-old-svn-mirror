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
package org.apache.sling.caconfig.spi.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class PropertyMetadataTest {

    @Test
    public void testProps() {
        PropertyMetadata<String> underTest = new PropertyMetadata<>("name1", String.class);
        assertEquals("name1", underTest.getName());
        assertEquals(String.class, underTest.getType());
        
        ConfigurationMetadata configMetadata = new ConfigurationMetadata("test", ImmutableList.<PropertyMetadata<?>>of(), false);
        Map<String,String> props = ImmutableMap.of("p1", "v1");
        underTest.label("label1")
            .description("desc1")
            .defaultValue("value1")
            .properties(props)
            .configurationMetadata(configMetadata);
        
        assertEquals("label1", underTest.getLabel());
        assertEquals("desc1", underTest.getDescription());
        assertEquals("value1", underTest.getDefaultValue());
        assertEquals(props, underTest.getProperties());
        assertSame(configMetadata, underTest.getConfigurationMetadata());
    }

    @Test
    public void testAllowedTypes() {
        new PropertyMetadata<>("name1", String.class);
        new PropertyMetadata<>("name1", String[].class);
        new PropertyMetadata<>("name1", int.class);
        new PropertyMetadata<>("name1", int[].class);
        new PropertyMetadata<>("name1", long.class);
        new PropertyMetadata<>("name1", long[].class);
        new PropertyMetadata<>("name1", double.class);
        new PropertyMetadata<>("name1", double[].class);
        new PropertyMetadata<>("name1", boolean.class);
        new PropertyMetadata<>("name1", boolean[].class);
    }

    @Test
    public void testAllowedTypesConverted() {
        assertEquals(int.class, new PropertyMetadata<>("name1", Integer.class).getType());
        assertEquals(int[].class, new PropertyMetadata<>("name1", Integer[].class).getType());
        assertEquals(long.class, new PropertyMetadata<>("name1", Long.class).getType());
        assertEquals(long[].class, new PropertyMetadata<>("name1", Long[].class).getType());
        assertEquals(double.class, new PropertyMetadata<>("name1", Double.class).getType());
        assertEquals(double[].class, new PropertyMetadata<>("name1", Double[].class).getType());
        assertEquals(boolean.class, new PropertyMetadata<>("name1", Boolean.class).getType());
        assertEquals(boolean[].class, new PropertyMetadata<>("name1", Boolean[].class).getType());
    }

    @Test
    public void testNestedConfiguration() {
        new PropertyMetadata<>("name1", ConfigurationMetadata.class);
        new PropertyMetadata<>("name1", ConfigurationMetadata[].class);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testDisallowedType() {
        new PropertyMetadata<>("name1", Object.class);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNullNale() {
        new PropertyMetadata<>(null, Object.class);
    }

    @Test
    public void testDefaultValueConstructor() {
        PropertyMetadata<String> stringProp = new PropertyMetadata<>("name1", "defValue");
        assertEquals("defValue", stringProp.getDefaultValue());
        assertEquals(String.class, stringProp.getType());
        
        PropertyMetadata<Integer> intProp = new PropertyMetadata<>("name1", 5);
        assertEquals((Integer)5, intProp.getDefaultValue());
        assertEquals(int.class, intProp.getType());
    }
    
}
