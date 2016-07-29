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
package org.apache.sling.contextaware.config.spi.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.sling.contextaware.config.spi.metadata.ConfigurationMetadata;
import org.apache.sling.contextaware.config.spi.metadata.PropertyMetadata;
import org.junit.Test;

public class ConfigurationMetadataTest {

    @Test
    public void testSimple() {
        ConfigurationMetadata underTest = new ConfigurationMetadata("hier1/set1");
        assertEquals("hier1/set1", underTest.getName());
        assertTrue(underTest.isSingleton());
        assertFalse(underTest.isList());
        
        underTest.getProperties().put("p1", "v1");
        assertEquals("v1", underTest.getProperties().get("p1", String.class));
        
        underTest.getParts().add(new PropertyMetadata<String>("param1", String.class));
        underTest.getParts().add(new PropertyMetadata<Integer>("param2", Integer.class));
        underTest.getParts().add(new ConfigurationMetadata("set1"));
        
        assertEquals(3, underTest.getParts().size());
    }

    @Test
    public void testList() {
        ConfigurationMetadata underTest = new ConfigurationMetadata("hier1/set1");
        underTest.setList(true);
        assertEquals("hier1/set1", underTest.getName());
        assertFalse(underTest.isSingleton());
        assertTrue(underTest.isList());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidName() {
        new ConfigurationMetadata("$illegal name");
    }

}
