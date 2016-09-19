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
package org.apache.sling.contextaware.config.impl.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.sling.contextaware.config.spi.ConfigurationMetadataProvider;
import org.apache.sling.contextaware.config.spi.metadata.ConfigurationMetadata;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableSortedSet;

public class ConfigurationMetadataProviderMultiplexerTest {

    @Rule
    public SlingContext context = new SlingContext();
    
    private ConfigurationMetadataProviderMultiplexer underTest;

    @Before
    public void setUp() {
        underTest = context.registerInjectActivateService(new ConfigurationMetadataProviderMultiplexer());
    }
    
    @Test
    public void testWithNoProvider() {
        SortedSet<String> configNames = underTest.getConfigurationNames();
        assertTrue(configNames.isEmpty());
        
        ConfigurationMetadata configMetadata = underTest.getConfigurationMetadata("test1");
        assertNull(configMetadata);
    }

    @Test
    public void testWithOneProvider() {
        registerConfigurationMetadataProvider("test1", "test2");

        SortedSet<String> configNames = underTest.getConfigurationNames();
        assertEquals(ImmutableSortedSet.of("test1", "test2"), configNames);
        
        ConfigurationMetadata configMetadata = underTest.getConfigurationMetadata("test1");
        assertEquals("test1", configMetadata.getName());

        configMetadata = underTest.getConfigurationMetadata("test2");
        assertEquals("test2", configMetadata.getName());
    }
    
    @Test
    public void testWithTwoProviders() {
        registerConfigurationMetadataProvider("test1");
        registerConfigurationMetadataProvider("test2");

        SortedSet<String> configNames = underTest.getConfigurationNames();
        assertEquals(ImmutableSortedSet.of("test1", "test2"), configNames);
        
        ConfigurationMetadata configMetadata = underTest.getConfigurationMetadata("test1");
        assertEquals("test1", configMetadata.getName());

        configMetadata = underTest.getConfigurationMetadata("test2");
        assertEquals("test2", configMetadata.getName());
    }
    
    private void registerConfigurationMetadataProvider(String... names) {
        final Map<String,ConfigurationMetadata> metadata = new HashMap<>();
        for (String name : names) {
            metadata.put(name, new ConfigurationMetadata(name));
        }
        context.registerService(ConfigurationMetadataProvider.class, new ConfigurationMetadataProvider() {
            @Override
            public SortedSet<String> getConfigurationNames() {
                return new TreeSet<>(metadata.keySet());
            }
            @Override
            public ConfigurationMetadata getConfigurationMetadata(String configName) {
                return metadata.get(configName);
            }
        });
    }

}
