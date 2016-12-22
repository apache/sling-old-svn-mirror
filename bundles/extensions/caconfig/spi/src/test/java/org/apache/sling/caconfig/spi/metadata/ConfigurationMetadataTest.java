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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class ConfigurationMetadataTest {

    @Test
    public void testProps() {
        ConfigurationMetadata underTest = new ConfigurationMetadata("name1", ImmutableList.<PropertyMetadata<?>>of(), false);
        assertEquals("name1", underTest.getName());
        assertTrue(underTest.isSingleton());
        assertFalse(underTest.isCollection());
        
        Map<String,String> props = ImmutableMap.of("p1", "v1");
        underTest.label("label1")
            .description("desc1")
            .properties(props);
        
        assertEquals("label1", underTest.getLabel());
        assertEquals("desc1", underTest.getDescription());
        assertEquals(props, underTest.getProperties());
    }

    @Test
    public void testCollectionProps() {
        ConfigurationMetadata underTest = new ConfigurationMetadata("name1", ImmutableList.<PropertyMetadata<?>>of(), true);
        assertEquals("name1", underTest.getName());
        assertFalse(underTest.isSingleton());
        assertTrue(underTest.isCollection());
    }

    @Test
    public void testPropertyMap() {
        ConfigurationMetadata underTest = new ConfigurationMetadata("name1", ImmutableList.<PropertyMetadata<?>>of(
                new PropertyMetadata<>("prop1", "devValue"),
                new PropertyMetadata<>("prop2", 5)), false);
        assertEquals(2, underTest.getPropertyMetadata().size());
        assertTrue(underTest.getPropertyMetadata().containsKey("prop1"));
        assertTrue(underTest.getPropertyMetadata().containsKey("prop2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateKey() {
        new ConfigurationMetadata("name1", ImmutableList.<PropertyMetadata<?>>of(
                new PropertyMetadata<>("prop1", "devValue"),
                new PropertyMetadata<>("prop1", 5)), false);
    }

}
