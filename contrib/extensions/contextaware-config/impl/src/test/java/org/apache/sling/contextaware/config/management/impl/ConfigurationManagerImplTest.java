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
package org.apache.sling.contextaware.config.management.impl;

import static org.apache.sling.contextaware.config.impl.ConfigurationNameConstants.CONFIGS_PARENT_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.contextaware.config.impl.metadata.ConfigurationMetadataProviderMultiplexer;
import org.apache.sling.contextaware.config.management.ConfigurationData;
import org.apache.sling.contextaware.config.management.ConfigurationManager;
import org.apache.sling.contextaware.config.resource.ConfigurationResourceResolver;
import org.apache.sling.contextaware.config.spi.ConfigurationMetadataProvider;
import org.apache.sling.contextaware.config.spi.metadata.ConfigurationMetadata;
import org.apache.sling.contextaware.config.spi.metadata.PropertyMetadata;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationManagerImplTest {
    
    @Rule
    public SlingContext context = new SlingContext();

    @Mock
    private ConfigurationResourceResolver configurationResourceResolver;
    @Mock
    private ConfigurationMetadataProvider configurationMetadataProvider;
    
    private ConfigurationManager underTest;
    
    private Resource contextResource;
    private Resource configResource;
    private Resource configResourceItem1;
    private Resource configResourceItem2;
    private ConfigurationMetadata configMetadata;
    
    private static final String CONFIG_NAME = "testConfigName";
    
    @Before
    public void setUp() {
        context.registerService(ConfigurationResourceResolver.class, configurationResourceResolver);
        context.registerService(ConfigurationMetadataProvider.class, configurationMetadataProvider);
        context.registerInjectActivateService(new ConfigurationMetadataProviderMultiplexer());
        underTest = context.registerInjectActivateService(new ConfigurationManagerImpl());
        
        configResource = context.create().resource("/conf/test",
                "prop1", "value1",
                "prop4", true);
        contextResource = context.create().resource("/content/test");
        configResourceItem1 = context.create().resource("/conf/item/1",
                "prop1", "value1");
        configResourceItem2 = context.create().resource("/conf/item/2",
                "prop4", true);
        
        configMetadata = new ConfigurationMetadata(CONFIG_NAME);
        configMetadata.setPropertyMetadata(ImmutableMap.<String,PropertyMetadata<?>>of(
                "prop1", new PropertyMetadata<>("prop1", "defValue"),
                "prop2", new PropertyMetadata<>("prop2", String.class),
                "prop3", new PropertyMetadata<>("prop3", 5)));

        when(configurationResourceResolver.getResourceCollection(any(Resource.class), anyString(), anyString()))
            .thenReturn(ImmutableList.<Resource>of());
    }
    
    @Test
    public void testGetConfigurationData() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_NAME)).thenReturn(configMetadata);
        when(configurationResourceResolver.getResource(contextResource, CONFIGS_PARENT_NAME, CONFIG_NAME)).thenReturn(configResource);

        ConfigurationData configData = underTest.getConfigurationData(contextResource, CONFIG_NAME);
        assertNotNull(configData);

        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3", "prop4"), configData.getPropertyNames());
        assertEquals("value1", configData.getValues().get("prop1", String.class));
        assertEquals((Integer)5, configData.getEffectiveValues().get("prop3", 0));
    }

    @Test
    public void testGetConfigurationData_NoConfigResource() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_NAME)).thenReturn(configMetadata);

        ConfigurationData configData = underTest.getConfigurationData(contextResource, CONFIG_NAME);
        assertNotNull(configData);

        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3"), configData.getPropertyNames());
        assertNull(configData.getValues().get("prop1", String.class));
        assertEquals((Integer)5, configData.getEffectiveValues().get("prop3", 0));
    }

    @Test
    public void testGetConfigurationData_NoConfigMetadata() {
        when(configurationResourceResolver.getResource(contextResource, CONFIGS_PARENT_NAME, CONFIG_NAME)).thenReturn(configResource);

        ConfigurationData configData = underTest.getConfigurationData(contextResource, CONFIG_NAME);
        assertNotNull(configData);

        assertEquals(ImmutableSet.of("prop1", "prop4"), configData.getPropertyNames());
        assertEquals("value1", configData.getValues().get("prop1", String.class));
        assertEquals((Integer)0, configData.getEffectiveValues().get("prop3", 0));
    }

    @Test
    public void testGetConfigurationData_NoConfigResource_NoConfigMetadata() {
        ConfigurationData configData = underTest.getConfigurationData(contextResource, CONFIG_NAME);
        assertNull(configData);
    }

    @Test
    public void testGetConfigurationDataCollection() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_NAME)).thenReturn(configMetadata);
        when(configurationResourceResolver.getResourceCollection(contextResource, CONFIGS_PARENT_NAME, CONFIG_NAME))
            .thenReturn(ImmutableList.of(configResourceItem1, configResourceItem2));
        
        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getConfigurationDataCollection(contextResource, CONFIG_NAME));
        assertEquals(2, configDatas.size());
        
        ConfigurationData configData1 = configDatas.get(0);
        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3"), configData1.getPropertyNames());
        assertEquals("value1", configData1.getValues().get("prop1", String.class));
        assertEquals((Integer)5, configData1.getEffectiveValues().get("prop3", 0));

        ConfigurationData configData2 = configDatas.get(1);
        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3", "prop4"), configData2.getPropertyNames());
        assertNull(configData2.getValues().get("prop1", String.class));
        assertEquals((Integer)5, configData2.getEffectiveValues().get("prop3", 0));
    }

    @Test
    public void testGetConfigurationDataCollection_NoConfigResources() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_NAME)).thenReturn(configMetadata);
        
        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getConfigurationDataCollection(contextResource, CONFIG_NAME));
        assertEquals(0, configDatas.size());
    }

    @Test
    public void testGetConfigurationDataCollection_NoConfigMetadata() {
        when(configurationResourceResolver.getResourceCollection(contextResource, CONFIGS_PARENT_NAME, CONFIG_NAME))
            .thenReturn(ImmutableList.of(configResourceItem1, configResourceItem2));
        
        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getConfigurationDataCollection(contextResource, CONFIG_NAME));
        assertEquals(2, configDatas.size());
        
        ConfigurationData configData1 = configDatas.get(0);
        assertEquals(ImmutableSet.of("prop1"), configData1.getPropertyNames());
        assertEquals("value1", configData1.getValues().get("prop1", String.class));
        assertEquals((Integer)0, configData1.getEffectiveValues().get("prop3", 0));

        ConfigurationData configData2 = configDatas.get(1);
        assertEquals(ImmutableSet.of("prop4"), configData2.getPropertyNames());
        assertNull(configData2.getValues().get("prop1", String.class));
        assertEquals((Integer)0, configData2.getEffectiveValues().get("prop3", 0));
    }

    @Test
    public void testGetConfigurationDataCollection_NoConfigResources_NoConfigMetadata() {
        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getConfigurationDataCollection(contextResource, CONFIG_NAME));
        assertEquals(0, configDatas.size());
    }

}
