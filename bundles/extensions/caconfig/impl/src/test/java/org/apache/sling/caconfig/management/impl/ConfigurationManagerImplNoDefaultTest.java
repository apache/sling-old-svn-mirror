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

import static org.apache.sling.caconfig.resource.impl.def.ConfigurationResourceNameConstants.PROPERTY_CONFIG_REF;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.impl.ConfigurationTestUtils;
import org.apache.sling.caconfig.management.ConfigurationData;
import org.apache.sling.caconfig.management.ConfigurationManager;
import org.apache.sling.caconfig.spi.ConfigurationMetadataProvider;
import org.apache.sling.caconfig.spi.metadata.ConfigurationMetadata;
import org.apache.sling.caconfig.spi.metadata.PropertyMetadata;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Test {@link ConfigurationManagerImpl} with no default implementation of the multiplexed services.
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigurationManagerImplNoDefaultTest {
    
    @Rule
    public SlingContext context = new SlingContext();

    @Mock
    private ConfigurationMetadataProvider configurationMetadataProvider;
    
    private ConfigurationManager underTest;
    
    private Resource contextResourceNoConfig;
    private ConfigurationMetadata configMetadata;
    
    private static final String CONFIG_NAME = "testConfig";
    private static final String CONFIG_COL_NAME = "testConfigCol";
   
    @Before
    public void setUp() {
        context.registerService(ConfigurationMetadataProvider.class, configurationMetadataProvider);
        ConfigurationTestUtils.registerConfigurationResolverWithoutDefaultImpl(context);
        underTest = context.registerInjectActivateService(new ConfigurationManagerImpl());
        
        contextResourceNoConfig = context.create().resource("/content/testNoConfig",
                PROPERTY_CONFIG_REF, "/conf/testNoConfig");
        
        configMetadata = new ConfigurationMetadata(CONFIG_NAME, ImmutableList.<PropertyMetadata<?>>of(
                new PropertyMetadata<>("prop1", "defValue"),
                new PropertyMetadata<>("prop2", String.class),
                new PropertyMetadata<>("prop3", 5)),
                false);
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_NAME)).thenReturn(configMetadata);

        configMetadata = new ConfigurationMetadata(CONFIG_COL_NAME, ImmutableList.<PropertyMetadata<?>>of(
                new PropertyMetadata<>("prop1", "defValue"),
                new PropertyMetadata<>("prop2", String.class),
                new PropertyMetadata<>("prop3", 5)),
                true);
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_COL_NAME)).thenReturn(configMetadata);
    }
    
    protected String getConfigPropertiesPath(String path) {
        return path;
    }
    
    @Test
    public void testGet_NoConfigResource() {
        ConfigurationData configData = underTest.getConfiguration(contextResourceNoConfig, CONFIG_NAME);
        assertNotNull(configData);

        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3"), configData.getPropertyNames());
        assertNull(configData.getValues().get("prop1", String.class));
        assertEquals((Integer)5, configData.getEffectiveValues().get("prop3", 0));

        assertFalse(configData.getValueInfo("prop1").isInherited());
        assertFalse(configData.getValueInfo("prop3").isInherited());
    }

    @Test
    public void testGet_NoConfigResource_NoConfigMetadata() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_NAME)).thenReturn(null);

        ConfigurationData configData = underTest.getConfiguration(contextResourceNoConfig, CONFIG_NAME);
        assertNull(configData);
    }

    @Test
    public void testGetCollection_NoConfigResources() {
        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getConfigurationCollection(contextResourceNoConfig, CONFIG_COL_NAME).getItems());
        assertEquals(0, configDatas.size());
    }

    @Test
    public void testGetCollection_NoConfigResources_NoConfigMetadata() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_COL_NAME)).thenReturn(null);

        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getConfigurationCollection(contextResourceNoConfig, CONFIG_COL_NAME).getItems());
        assertEquals(0, configDatas.size());
    }

    @Test
    public void testNewCollectionItem() {
        ConfigurationData newItem = underTest.newCollectionItem(contextResourceNoConfig, CONFIG_COL_NAME);
        assertNotNull(newItem);
        assertEquals((Integer)5, newItem.getEffectiveValues().get("prop3", 0));
    }

    @Test
    public void testNewCollectionItem_NoConfigMetadata() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_COL_NAME)).thenReturn(null);

        ConfigurationData newItem = underTest.newCollectionItem(contextResourceNoConfig, CONFIG_COL_NAME);
        assertNull(newItem);
    }

}
