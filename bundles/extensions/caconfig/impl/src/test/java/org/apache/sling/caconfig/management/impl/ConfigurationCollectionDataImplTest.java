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
package org.apache.sling.caconfig.management.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Map;

import org.apache.sling.caconfig.management.ConfigurationCollectionData;
import org.apache.sling.caconfig.management.ConfigurationData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationCollectionDataImplTest {
    
    @Mock
    private Collection<ConfigurationData> items;

    @Test
    public void testProperties() {
        Map<String,Object> props = ImmutableMap.<String,Object>of("jcr:primaryType", "test", "prop1", "value1"); 
        ConfigurationCollectionData underTest = new ConfigurationCollectionDataImpl("name1", items, "/path1", props);
        
        assertEquals("name1", underTest.getConfigName());;
        assertSame(items, underTest.getItems());
        assertEquals("/path1", underTest.getResourcePath());
        assertEquals(ImmutableMap.<String,Object>of("prop1", "value1"), underTest.getProperties());
    }

    @Test
    public void testEmpty() {
        ConfigurationCollectionData underTest = new ConfigurationCollectionDataImpl("name1", ImmutableList.<ConfigurationData>of(), "/path1", null);
        
        assertEquals("name1", underTest.getConfigName());;
        assertTrue(underTest.getItems().isEmpty());
        assertEquals("/path1", underTest.getResourcePath());
        assertTrue(underTest.getProperties().isEmpty());
    }

}
