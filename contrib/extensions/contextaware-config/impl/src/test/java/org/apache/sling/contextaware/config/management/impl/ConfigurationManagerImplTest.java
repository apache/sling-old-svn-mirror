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
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.contextaware.config.impl.def.DefaultConfigurationPersistenceStrategy;
import org.apache.sling.contextaware.config.impl.metadata.ConfigurationMetadataProviderMultiplexer;
import org.apache.sling.contextaware.config.management.ConfigurationData;
import org.apache.sling.contextaware.config.management.ConfigurationManager;
import org.apache.sling.contextaware.config.resource.ConfigurationResourceResolver;
import org.apache.sling.contextaware.config.resource.impl.ConfigurationResourceResolvingStrategyMultiplexer;
import org.apache.sling.contextaware.config.resource.spi.ConfigurationResourceResolvingStrategy;
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
    private ConfigurationResourceResolvingStrategy configurationResourceResolvingStrategy;
    @Mock
    private ConfigurationMetadataProvider configurationMetadataProvider;
    
    private ConfigurationManager underTest;
    
    private Resource contextResource;
    protected Resource configResource;
    protected Resource configResourceItem1;
    protected Resource configResourceItem2;
    private ConfigurationMetadata configMetadata;
    
    private static final String CONFIG_NAME = "testConfigName";
    
    @Before
    public void setUp() {
        context.registerService(ConfigurationResourceResolver.class, configurationResourceResolver);
        context.registerService(ConfigurationResourceResolvingStrategy.class, configurationResourceResolvingStrategy);
        context.registerService(ConfigurationMetadataProvider.class, configurationMetadataProvider);
        context.registerInjectActivateService(new ConfigurationResourceResolvingStrategyMultiplexer());
        context.registerInjectActivateService(new ConfigurationMetadataProviderMultiplexer());
        context.registerInjectActivateService(new DefaultConfigurationPersistenceStrategy());
        context.registerInjectActivateService(new ConfigurationPersistenceStrategyMultiplexer());
        underTest = context.registerInjectActivateService(new ConfigurationManagerImpl());
        
        contextResource = context.create().resource("/content/test");
        
        context.create().resource(getConfigPropertiesPath("/conf/test"),
                "prop1", "value1",
                "prop4", true);
        context.create().resource(getConfigPropertiesPath("/conf/item/1"),
                "prop1", "value1");
        context.create().resource(getConfigPropertiesPath("/conf/item/2"),
                "prop4", true);

        configResource = context.resourceResolver().getResource("/conf/test");
        configResourceItem1 = context.resourceResolver().getResource("/conf/item/1");
        configResourceItem2 = context.resourceResolver().getResource("/conf/item/2");
        
        configMetadata = new ConfigurationMetadata(CONFIG_NAME);
        configMetadata.setPropertyMetadata(ImmutableMap.<String,PropertyMetadata<?>>of(
                "prop1", new PropertyMetadata<>("prop1", "defValue"),
                "prop2", new PropertyMetadata<>("prop2", String.class),
                "prop3", new PropertyMetadata<>("prop3", 5)));

        when(configurationResourceResolver.getResourceCollection(any(Resource.class), anyString(), anyString()))
                .thenReturn(ImmutableList.<Resource>of());
    }
    
    protected String getConfigPropertiesPath(String path) {
        return path;
    }
    
    @Test
    public void testGet() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_NAME)).thenReturn(configMetadata);
        when(configurationResourceResolver.getResource(contextResource, CONFIGS_PARENT_NAME, CONFIG_NAME)).thenReturn(configResource);

        ConfigurationData configData = underTest.get(contextResource, CONFIG_NAME);
        assertNotNull(configData);

        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3", "prop4"), configData.getPropertyNames());
        assertEquals("value1", configData.getValues().get("prop1", String.class));
        assertEquals((Integer)5, configData.getEffectiveValues().get("prop3", 0));
    }

    @Test
    public void testGet_NoConfigResource() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_NAME)).thenReturn(configMetadata);

        ConfigurationData configData = underTest.get(contextResource, CONFIG_NAME);
        assertNotNull(configData);

        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3"), configData.getPropertyNames());
        assertNull(configData.getValues().get("prop1", String.class));
        assertEquals((Integer)5, configData.getEffectiveValues().get("prop3", 0));
    }

    @Test
    public void testGet_NoConfigMetadata() {
        when(configurationResourceResolver.getResource(contextResource, CONFIGS_PARENT_NAME, CONFIG_NAME)).thenReturn(configResource);

        ConfigurationData configData = underTest.get(contextResource, CONFIG_NAME);
        assertNotNull(configData);

        assertEquals(ImmutableSet.of("prop1", "prop4"), configData.getPropertyNames());
        assertEquals("value1", configData.getValues().get("prop1", String.class));
        assertEquals((Integer)0, configData.getEffectiveValues().get("prop3", 0));
    }

    @Test
    public void testGet_NoConfigResource_NoConfigMetadata() {
        ConfigurationData configData = underTest.get(contextResource, CONFIG_NAME);
        assertNull(configData);
    }

    @Test
    public void testGetCollection() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_NAME)).thenReturn(configMetadata);
        when(configurationResourceResolver.getResourceCollection(contextResource, CONFIGS_PARENT_NAME, CONFIG_NAME))
            .thenReturn(ImmutableList.of(configResourceItem1, configResourceItem2));
        
        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getCollection(contextResource, CONFIG_NAME));
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
    public void testGetCollection_NoConfigResources() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_NAME)).thenReturn(configMetadata);
        
        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getCollection(contextResource, CONFIG_NAME));
        assertEquals(0, configDatas.size());
    }

    @Test
    public void testGetCollection_NoConfigMetadata() {
        when(configurationResourceResolver.getResourceCollection(contextResource, CONFIGS_PARENT_NAME, CONFIG_NAME))
            .thenReturn(ImmutableList.of(configResourceItem1, configResourceItem2));
        
        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getCollection(contextResource, CONFIG_NAME));
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
    public void testGetCollection_NoConfigResources_NoConfigMetadata() {
        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getCollection(contextResource, CONFIG_NAME));
        assertEquals(0, configDatas.size());
    }

    @Test
    public void testPersist() throws Exception {
        when(configurationResourceResolvingStrategy.getResourcePath(contextResource, CONFIGS_PARENT_NAME, CONFIG_NAME))
            .thenReturn("/conf/new");
        
        underTest.persist(contextResource, CONFIG_NAME,
                ImmutableMap.<String, Object>of("prop1", "value1"));
        context.resourceResolver().commit();

        ValueMap props = context.resourceResolver().getResource(getConfigPropertiesPath("/conf/new")).getValueMap();
        assertEquals("value1", props.get("prop1"));
    }

    @Test
    public void testPersistCollection() throws Exception {
        when(configurationResourceResolvingStrategy.getResourceCollectionParentPath(contextResource, CONFIGS_PARENT_NAME, CONFIG_NAME))
            .thenReturn("/conf/newcol");

        underTest.persistCollection(contextResource, CONFIG_NAME, ImmutableList.<Map<String,Object>>of(
                ImmutableMap.<String, Object>of("prop1", "value1"),
                ImmutableMap.<String, Object>of("prop2", 5)
        ));
        context.resourceResolver().commit();

        ValueMap props0 = context.resourceResolver().getResource(getConfigPropertiesPath("/conf/newcol/0")).getValueMap();
        assertEquals("value1", props0.get("prop1"));
        ValueMap props1 = context.resourceResolver().getResource(getConfigPropertiesPath("/conf/newcol/1")).getValueMap();
        assertEquals((Integer)5, props1.get("prop2"));
    }

    @Test
    public void testNewCollectionItem() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_NAME)).thenReturn(configMetadata);
        
        ConfigurationData newItem = underTest.newCollectionItem(CONFIG_NAME);
        assertNotNull(newItem);
        assertEquals((Integer)5, newItem.getEffectiveValues().get("prop3", 0));
    }

    @Test
    public void testNewCollectionItem_NoConfigMetadata() {
        ConfigurationData newItem = underTest.newCollectionItem(CONFIG_NAME);
        assertNull(newItem);
    }

}
