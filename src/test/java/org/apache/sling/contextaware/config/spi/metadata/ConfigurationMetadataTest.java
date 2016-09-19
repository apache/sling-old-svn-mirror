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

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class ConfigurationMetadataTest {

    @Test
    public void testProps() {
        ConfigurationMetadata underTest = new ConfigurationMetadata("name1");
        assertEquals("name1", underTest.getName());
        assertTrue(underTest.isSingleton());
        assertFalse(underTest.isList());
        
        underTest.setLabel("label1");
        underTest.setDescription("desc1");
        underTest.setList(true);
        Map<String,String> props = ImmutableMap.of("p1", "v1");
        underTest.setProperties(props);
        
        assertEquals("label1", underTest.getLabel());
        assertEquals("desc1", underTest.getDescription());
        assertFalse(underTest.isSingleton());
        assertTrue(underTest.isList());
        assertEquals(props, underTest.getProperties());
    }

}
