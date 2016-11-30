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

import static org.apache.sling.caconfig.impl.ConfigurationNameConstants.CONFIGS_PARENT_NAME;
import static org.apache.sling.caconfig.impl.def.ConfigurationDefNameConstants.PROPERTY_CONFIG_PROPERTY_INHERIT;
import static org.apache.sling.caconfig.resource.impl.def.ConfigurationResourceNameConstants.PROPERTY_CONFIG_COLLECTION_INHERIT;
import static org.apache.sling.caconfig.resource.impl.def.ConfigurationResourceNameConstants.PROPERTY_CONFIG_REF;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.impl.ConfigurationTestUtils;
import org.apache.sling.caconfig.impl.metadata.ConfigurationMetadataProviderMultiplexer;
import org.apache.sling.caconfig.impl.override.DummyConfigurationOverrideProvider;
import org.apache.sling.caconfig.management.ConfigurationData;
import org.apache.sling.caconfig.management.ConfigurationManager;
import org.apache.sling.caconfig.spi.ConfigurationMetadataProvider;
import org.apache.sling.caconfig.spi.ConfigurationOverrideProvider;
import org.apache.sling.caconfig.spi.ResourceCollectionItem;
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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationManagerImplTest {
    
    @Rule
    public SlingContext context = new SlingContext();

    @Mock
    private ConfigurationMetadataProvider configurationMetadataProvider;
    
    private ConfigurationManager underTest;
    
    private Resource contextResource;
    private Resource contextResourceLevel2;
    private Resource contextResourceLevel3;
    private Resource contextResourceNoConfig;
    
    private static final String CONFIG_NAME = "testConfig";
    private static final String CONFIG_COL_NAME = "testConfigCol";
    private static final String CONFIG_NESTED_NAME = "testConfigNested";
   
    @Before
    public void setUp() {
        context.registerService(ConfigurationMetadataProvider.class, configurationMetadataProvider);
        context.registerInjectActivateService(new ConfigurationMetadataProviderMultiplexer());
        ConfigurationTestUtils.registerConfigurationResolver(context);
        underTest = context.registerInjectActivateService(new ConfigurationManagerImpl());
        
        contextResource = context.create().resource("/content/test",
                PROPERTY_CONFIG_REF, "/conf/test");
        contextResourceLevel2 = context.create().resource("/content/test/level2",
                PROPERTY_CONFIG_REF, "/conf/test/level2");
        contextResourceLevel3 = context.create().resource("/content/test/level2/level3",
                PROPERTY_CONFIG_REF, "/conf/test/level2/level3");
        contextResourceNoConfig = context.create().resource("/content/testNoConfig",
                PROPERTY_CONFIG_REF, "/conf/testNoConfig");
        
        context.create().resource(getConfigPropsPath("/conf/test/" + CONFIGS_PARENT_NAME + "/" + CONFIG_NAME),
                "prop1", "value1",
                "prop4", true);
        context.create().resource(getConfigPropsPath("/conf/test/" + CONFIGS_PARENT_NAME + "/" + CONFIG_COL_NAME + "/1"),
                "prop1", "value1");
        context.create().resource(getConfigPropsPath("/conf/test/" + CONFIGS_PARENT_NAME + "/" + CONFIG_COL_NAME + "/2"),
                "prop4", true);
        
        // test fixture with resource collection inheritance on level 2
        context.create().resource("/conf/test/level2/" + CONFIGS_PARENT_NAME + "/" + CONFIG_COL_NAME,
                PROPERTY_CONFIG_COLLECTION_INHERIT, true);
        context.create().resource(getConfigPropsPath("/conf/test/level2/" + CONFIGS_PARENT_NAME + "/" + CONFIG_COL_NAME + "/1"),
                "prop1", "value1_level2");
        
        // test fixture with property inheritance and resource collection inheritance on level 3
        context.create().resource(getConfigPropsPath("/conf/test/level2/level3/" + CONFIGS_PARENT_NAME + "/" + CONFIG_NAME),
                "prop4", false,
                "prop5", "value5_level3",
                PROPERTY_CONFIG_PROPERTY_INHERIT, true);
        context.create().resource("/conf/test/level2/level3/" + CONFIGS_PARENT_NAME + "/" + CONFIG_COL_NAME,
                PROPERTY_CONFIG_COLLECTION_INHERIT, true);
        context.create().resource(getConfigPropsPath("/conf/test/level2/level3/" + CONFIGS_PARENT_NAME + "/" + CONFIG_COL_NAME + "/1"),
                "prop4", false,
                "prop5", "value5_level3",
                PROPERTY_CONFIG_PROPERTY_INHERIT, true);

        // test fixture nested configuration
        context.create().resource(getConfigPropsPath("/conf/test/" + CONFIGS_PARENT_NAME + "/" + CONFIG_NESTED_NAME),
                "prop1", "value1",
                "prop4", true);
        context.create().resource(getConfigPropsPath(getConfigPropsPath("/conf/global/" + CONFIGS_PARENT_NAME + "/" + CONFIG_NESTED_NAME) + "/propSub"),
                "prop1", "propSubValue1",
                "prop4", true);
        context.create().resource(getConfigPropsPath(getConfigPropsPath("/conf/test/" + CONFIGS_PARENT_NAME + "/" + CONFIG_NESTED_NAME) + "/propSubList/item1"),
                "prop1", "propSubListValue1.1");
        context.create().resource(getConfigPropsPath(getConfigPropsPath("/conf/test/" + CONFIGS_PARENT_NAME + "/" + CONFIG_NESTED_NAME) + "/propSubList/item2"),
                "prop1", "propSubListValue1.2");
        context.create().resource(getConfigPropsPath(getConfigPropsPath(getConfigPropsPath("/conf/global/" + CONFIGS_PARENT_NAME + "/" + CONFIG_NESTED_NAME) + "/propSubList/item1") + "/propSub"),
                "prop1", "propSubList1_proSubValue1",
                "prop4", true);
        
        
        // config metadata singleton config
        ConfigurationMetadata configMetadata = new ConfigurationMetadata(CONFIG_NAME);
        configMetadata.setPropertyMetadata(ImmutableMap.<String,PropertyMetadata<?>>of(
                "prop1", new PropertyMetadata<>("prop1", "defValue"),
                "prop2", new PropertyMetadata<>("prop2", String.class),
                "prop3", new PropertyMetadata<>("prop3", 5)));
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_NAME)).thenReturn(configMetadata);

        // config metadata config collection
        configMetadata = new ConfigurationMetadata(CONFIG_COL_NAME);
        configMetadata.setPropertyMetadata(ImmutableMap.<String,PropertyMetadata<?>>of(
                "prop1", new PropertyMetadata<>("prop1", "defValue"),
                "prop2", new PropertyMetadata<>("prop2", String.class),
                "prop3", new PropertyMetadata<>("prop3", 5)));
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_COL_NAME)).thenReturn(configMetadata);

        // config metadata nested config
        configMetadata = new ConfigurationMetadata(CONFIG_NESTED_NAME);
        configMetadata.setPropertyMetadata(ImmutableMap.<String,PropertyMetadata<?>>of(
                "prop1", new PropertyMetadata<>("prop1", "defValue"),
                "propSub", new PropertyMetadata<>("propSub", ConfigurationMetadata.class),
                "propSubList", new PropertyMetadata<>("propSubList", ConfigurationMetadata[].class)));
        ConfigurationMetadata propSubMetadata = new ConfigurationMetadata("propSub");
        propSubMetadata.setPropertyMetadata(ImmutableMap.<String,PropertyMetadata<?>>of(
                "prop1", new PropertyMetadata<>("prop1", "defValue"),
                "prop2", new PropertyMetadata<>("prop2", String.class),
                "prop3", new PropertyMetadata<>("prop3", 5)));
        ConfigurationMetadata propSubListMetadata = new ConfigurationMetadata("propSubList");
        propSubListMetadata.setPropertyMetadata(ImmutableMap.<String,PropertyMetadata<?>>of(
                "prop1", new PropertyMetadata<>("prop1", String.class),
                "propSub", new PropertyMetadata<>("propSub", ConfigurationMetadata.class)));
        configMetadata.getPropertyMetadata().get("propSub").setConfigurationMetadata(propSubMetadata);
        configMetadata.getPropertyMetadata().get("propSubList").setConfigurationMetadata(propSubListMetadata);
        propSubListMetadata.getPropertyMetadata().get("propSub").setConfigurationMetadata(propSubMetadata);

        when(configurationMetadataProvider.getConfigurationNames()).thenReturn(ImmutableSortedSet.of(CONFIG_NAME, CONFIG_COL_NAME, CONFIG_NESTED_NAME));
        
        
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_NESTED_NAME)).thenReturn(configMetadata);

    }
    
    protected String getConfigPropsPath(String path) {
        return path;
    }
    
    @Test
    public void testGet() {
        ConfigurationData configData = underTest.get(contextResource, CONFIG_NAME);
        assertNotNull(configData);

        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3", "prop4"), configData.getPropertyNames());
        assertEquals("value1", configData.getValues().get("prop1", String.class));
        assertEquals((Integer)5, configData.getEffectiveValues().get("prop3", 0));

        assertFalse(configData.getValueInfo("prop1").isInherited());
        assertFalse(configData.getValueInfo("prop3").isInherited());
    }

    @Test
    public void testGet_WithResourceInheritance() {
        ConfigurationData configData = underTest.get(contextResourceLevel2, CONFIG_NAME);
        assertNotNull(configData);

        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3", "prop4"), configData.getPropertyNames());
        assertNull(configData.getValues().get("prop1", String.class));
        assertEquals("value1", configData.getEffectiveValues().get("prop1", String.class));
        assertEquals((Integer)5, configData.getEffectiveValues().get("prop3", 0));

        String configPath = getConfigPropsPath("/conf/test/" + CONFIGS_PARENT_NAME + "/" + CONFIG_NAME);
        assertEquals(configPath, configData.getValueInfo("prop1").getConfigSourcePath());
        assertTrue(configData.getValueInfo("prop1").isInherited());
        assertFalse(configData.getValueInfo("prop3").isInherited());
        assertNull(configData.getValueInfo("prop3").getConfigSourcePath());
    }

    @Test
    public void testGet_WithPropertyInheritance() {
        ConfigurationData configData = underTest.get(contextResourceLevel3, CONFIG_NAME);
        assertNotNull(configData);

        assertTrue(configData.getPropertyNames().containsAll(ImmutableSet.of("prop1", "prop2", "prop3", "prop4", "prop5")));
        assertNull(configData.getValues().get("prop1", String.class));
        assertNull(configData.getValues().get("prop2", String.class));
        assertNull(configData.getValues().get("prop3", Integer.class));
        assertFalse(configData.getValues().get("prop4", Boolean.class));
        assertEquals("value5_level3", configData.getValues().get("prop5", String.class));
        
        assertEquals("value1", configData.getEffectiveValues().get("prop1", String.class));
        assertNull(configData.getEffectiveValues().get("prop2", String.class));
        assertEquals((Integer)5, configData.getEffectiveValues().get("prop3", 0));
        assertFalse(configData.getEffectiveValues().get("prop4", Boolean.class));
        assertEquals("value5_level3", configData.getEffectiveValues().get("prop5", String.class));

        String configPath = getConfigPropsPath("/conf/test/" + CONFIGS_PARENT_NAME + "/" + CONFIG_NAME);
        String configPathLevel3 = getConfigPropsPath("/conf/test/level2/level3/" + CONFIGS_PARENT_NAME + "/" + CONFIG_NAME);
        assertTrue(configData.getValueInfo("prop1").isInherited());
        assertEquals(configPath, configData.getValueInfo("prop1").getConfigSourcePath());
        assertFalse(configData.getValueInfo("prop2").isInherited());
        assertNull(configData.getValueInfo("prop2").getConfigSourcePath());
        assertFalse(configData.getValueInfo("prop3").isInherited());
        assertNull(configData.getValueInfo("prop3").getConfigSourcePath());
        assertFalse(configData.getValueInfo("prop4").isInherited());
        assertEquals(configPathLevel3, configData.getValueInfo("prop4").getConfigSourcePath());
        assertFalse(configData.getValueInfo("prop5").isInherited());
        assertEquals(configPathLevel3, configData.getValueInfo("prop5").getConfigSourcePath());
    }

    @Test
    public void testGet_WithOverride() {
        context.registerService(ConfigurationOverrideProvider.class, new DummyConfigurationOverrideProvider(
                "[/content]" + CONFIG_NAME + "={prop1='override1'}"));
        
        ConfigurationData configData = underTest.get(contextResource, CONFIG_NAME);
        assertNotNull(configData);

        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3"), configData.getPropertyNames());
        assertEquals("value1", configData.getValues().get("prop1", String.class));
        assertEquals("override1", configData.getEffectiveValues().get("prop1", String.class));
        assertEquals((Integer)5, configData.getEffectiveValues().get("prop3", 0));

        assertFalse(configData.getValueInfo("prop1").isInherited());
        assertTrue(configData.getValueInfo("prop1").isOverridden());
        assertFalse(configData.getValueInfo("prop3").isInherited());
        assertFalse(configData.getValueInfo("prop3").isOverridden());
    }

    @Test
    public void testGet_NoConfigResource() {
        ConfigurationData configData = underTest.get(contextResourceNoConfig, CONFIG_NAME);
        assertNotNull(configData);

        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3"), configData.getPropertyNames());
        assertNull(configData.getValues().get("prop1", String.class));
        assertEquals((Integer)5, configData.getEffectiveValues().get("prop3", 0));

        assertFalse(configData.getValueInfo("prop1").isInherited());
        assertFalse(configData.getValueInfo("prop3").isInherited());
    }

    @Test
    public void testGet_NoConfigMetadata() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_NAME)).thenReturn(null);

        ConfigurationData configData = underTest.get(contextResource, CONFIG_NAME);
        assertNotNull(configData);

        assertEquals(ImmutableSet.of("prop1", "prop4"), configData.getPropertyNames());
        assertEquals("value1", configData.getValues().get("prop1", String.class));
        assertEquals((Integer)0, configData.getEffectiveValues().get("prop3", 0));

        assertFalse(configData.getValueInfo("prop1").isInherited());
        assertFalse(configData.getValueInfo("prop3").isInherited());
    }

    @Test
    public void testGet_NoConfigResource_NoConfigMetadata() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_NAME)).thenReturn(null);

        ConfigurationData configData = underTest.get(contextResourceNoConfig, CONFIG_NAME);
        assertNull(configData);
    }

    @Test
    public void testGetCollection() {
        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getCollection(contextResource, CONFIG_COL_NAME));
        assertEquals(2, configDatas.size());

        ConfigurationData configData1 = configDatas.get(0);
        assertEquals("1", configData1.getCollectionItemName());
        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3"), configData1.getPropertyNames());
        assertEquals("value1", configData1.getValues().get("prop1", String.class));
        assertEquals((Integer)5, configData1.getEffectiveValues().get("prop3", 0));

        assertFalse(configData1.getValueInfo("prop1").isInherited());
        assertFalse(configData1.getValueInfo("prop3").isInherited());
        
        ConfigurationData configData2 = configDatas.get(1);
        assertEquals("2", configData2.getCollectionItemName());
        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3", "prop4"), configData2.getPropertyNames());
        assertNull(configData2.getValues().get("prop1", String.class));
        assertEquals((Integer)5, configData2.getEffectiveValues().get("prop3", 0));

        assertFalse(configData2.getValueInfo("prop1").isInherited());
        assertFalse(configData2.getValueInfo("prop3").isInherited());
    }

    @Test
    public void testGetCollection_WithResourceCollectionInheritance() {
        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getCollection(contextResourceLevel2, CONFIG_COL_NAME));
        assertEquals(2, configDatas.size());
        
        ConfigurationData configData1 = configDatas.get(0);
        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3"), configData1.getPropertyNames());
        assertEquals("value1_level2", configData1.getValues().get("prop1", String.class));
        assertEquals("value1_level2", configData1.getEffectiveValues().get("prop1", String.class));
        assertEquals((Integer)5, configData1.getEffectiveValues().get("prop3", 0));

        String configPath1 = getConfigPropsPath("/conf/test/level2/" + CONFIGS_PARENT_NAME + "/" + CONFIG_COL_NAME + "/1");
        assertFalse(configData1.getValueInfo("prop1").isInherited());
        assertEquals(configPath1, configData1.getValueInfo("prop1").getConfigSourcePath());
        assertFalse(configData1.getValueInfo("prop3").isInherited());
        assertNull(configData1.getValueInfo("prop3").getConfigSourcePath());
        
        ConfigurationData configData2 = configDatas.get(1);
        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3", "prop4"), configData2.getPropertyNames());
        assertNull(configData2.getValues().get("prop1", String.class));
        assertEquals((Integer)5, configData2.getEffectiveValues().get("prop3", 0));

        String configPath2 = getConfigPropsPath("/conf/test/" + CONFIGS_PARENT_NAME + "/" + CONFIG_COL_NAME + "/2");
        assertTrue(configData2.getValueInfo("prop4").isInherited());
        assertEquals(configPath2, configData2.getValueInfo("prop4").getConfigSourcePath());
        assertFalse(configData2.getValueInfo("prop3").isInherited());
        assertNull(configData2.getValueInfo("prop3").getConfigSourcePath());
    }

    @Test
    public void testGetCollection_WithResourceCollectionAndPropertyInheritance() {
        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getCollection(contextResourceLevel3, CONFIG_COL_NAME));
        assertEquals(2, configDatas.size());
        
        ConfigurationData configData1 = configDatas.get(0);
        assertTrue(configData1.getPropertyNames().containsAll(ImmutableSet.of("prop1", "prop2", "prop3", "prop4", "prop5")));

        assertTrue(configData1.getPropertyNames().containsAll(ImmutableSet.of("prop1", "prop2", "prop3", "prop4", "prop5")));
        assertNull(configData1.getValues().get("prop1", String.class));
        assertNull(configData1.getValues().get("prop2", String.class));
        assertNull(configData1.getValues().get("prop3", Integer.class));
        assertFalse(configData1.getValues().get("prop4", Boolean.class));
        assertEquals("value5_level3", configData1.getValues().get("prop5", String.class));
        
        assertEquals("value1_level2", configData1.getEffectiveValues().get("prop1", String.class));
        assertNull(configData1.getEffectiveValues().get("prop2", String.class));
        assertEquals((Integer)5, configData1.getEffectiveValues().get("prop3", 0));
        assertFalse(configData1.getEffectiveValues().get("prop4", Boolean.class));
        assertEquals("value5_level3", configData1.getEffectiveValues().get("prop5", String.class));

        String configPathLevel2 = getConfigPropsPath("/conf/test/level2/" + CONFIGS_PARENT_NAME + "/" + CONFIG_COL_NAME + "/1");
        String configPathLevel3 = getConfigPropsPath("/conf/test/level2/level3/" + CONFIGS_PARENT_NAME + "/" + CONFIG_COL_NAME + "/1");
        assertTrue(configData1.getValueInfo("prop1").isInherited());
        assertEquals(configPathLevel2, configData1.getValueInfo("prop1").getConfigSourcePath());
        assertFalse(configData1.getValueInfo("prop2").isInherited());
        assertNull(configData1.getValueInfo("prop2").getConfigSourcePath());
        assertFalse(configData1.getValueInfo("prop3").isInherited());
        assertNull(configData1.getValueInfo("prop3").getConfigSourcePath());
        assertFalse(configData1.getValueInfo("prop4").isInherited());
        assertEquals(configPathLevel3, configData1.getValueInfo("prop4").getConfigSourcePath());
        assertFalse(configData1.getValueInfo("prop5").isInherited());
        assertEquals(configPathLevel3, configData1.getValueInfo("prop5").getConfigSourcePath());
                
        ConfigurationData configData2 = configDatas.get(1);
        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3", "prop4"), configData2.getPropertyNames());
        assertNull(configData2.getValues().get("prop1", String.class));
        assertEquals((Integer)5, configData2.getEffectiveValues().get("prop3", 0));

        String configPath2 = getConfigPropsPath("/conf/test/" + CONFIGS_PARENT_NAME + "/" + CONFIG_COL_NAME + "/2");
        assertTrue(configData2.getValueInfo("prop4").isInherited());
        assertEquals(configPath2, configData2.getValueInfo("prop4").getConfigSourcePath());
        assertFalse(configData2.getValueInfo("prop3").isInherited());
        assertNull(configData2.getValueInfo("prop3").getConfigSourcePath());
    }

    @Test
    public void testGetCollection_WithOverride() {
        context.registerService(ConfigurationOverrideProvider.class, new DummyConfigurationOverrideProvider(
                "[/content]" + CONFIG_COL_NAME + "/prop1='override1'"));
        
        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getCollection(contextResource, CONFIG_COL_NAME));
        assertEquals(2, configDatas.size());

        ConfigurationData configData1 = configDatas.get(0);
        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3"), configData1.getPropertyNames());
        assertEquals("value1", configData1.getValues().get("prop1", String.class));
        assertEquals("override1", configData1.getEffectiveValues().get("prop1", String.class));
        assertEquals((Integer)5, configData1.getEffectiveValues().get("prop3", 0));

        assertFalse(configData1.getValueInfo("prop1").isInherited());
        assertTrue(configData1.getValueInfo("prop1").isOverridden());
        assertFalse(configData1.getValueInfo("prop3").isInherited());
        assertFalse(configData1.getValueInfo("prop3").isOverridden());
        
        ConfigurationData configData2 = configDatas.get(1);
        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3", "prop4"), configData2.getPropertyNames());
        assertNull(configData2.getValues().get("prop1", String.class));
        assertEquals("override1", configData2.getEffectiveValues().get("prop1", String.class));
        assertEquals((Integer)5, configData2.getEffectiveValues().get("prop3", 0));

        assertFalse(configData2.getValueInfo("prop1").isInherited());
        assertTrue(configData2.getValueInfo("prop1").isOverridden());
        assertFalse(configData2.getValueInfo("prop3").isInherited());
        assertFalse(configData2.getValueInfo("prop3").isOverridden());
    }

    @Test
    public void testGetCollection_NoConfigResources() {
        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getCollection(contextResourceNoConfig, CONFIG_COL_NAME));
        assertEquals(0, configDatas.size());
    }

    @Test
    public void testGetCollection_NoConfigMetadata() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_COL_NAME)).thenReturn(null);
        
        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getCollection(contextResource, CONFIG_COL_NAME));
        assertEquals(2, configDatas.size());
        
        ConfigurationData configData1 = configDatas.get(0);
        assertEquals(ImmutableSet.of("prop1"), configData1.getPropertyNames());
        assertEquals("value1", configData1.getValues().get("prop1", String.class));
        assertEquals((Integer)0, configData1.getEffectiveValues().get("prop3", 0));

        assertFalse(configData1.getValueInfo("prop1").isInherited());
        assertFalse(configData1.getValueInfo("prop3").isInherited());

        ConfigurationData configData2 = configDatas.get(1);
        assertEquals(ImmutableSet.of("prop4"), configData2.getPropertyNames());
        assertNull(configData2.getValues().get("prop1", String.class));
        assertEquals((Integer)0, configData2.getEffectiveValues().get("prop3", 0));

        assertFalse(configData2.getValueInfo("prop1").isInherited());
        assertFalse(configData2.getValueInfo("prop3").isInherited());
    }

    @Test
    public void testGetCollection_NoConfigResources_NoConfigMetadata() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_COL_NAME)).thenReturn(null);

        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getCollection(contextResourceNoConfig, CONFIG_COL_NAME));
        assertEquals(0, configDatas.size());
    }

    @Test
    public void testPersist() throws Exception {
        underTest.persist(contextResourceNoConfig, CONFIG_NAME,
                ImmutableMap.<String, Object>of("prop1", "value1"));
        context.resourceResolver().commit();

        String configPath = getConfigPropsPath("/conf/testNoConfig/" + CONFIGS_PARENT_NAME + "/" + CONFIG_NAME);
        ValueMap props = context.resourceResolver().getResource(configPath).getValueMap();
        assertEquals("value1", props.get("prop1"));
    }

    @Test
    public void testPersistCollection() throws Exception {
        underTest.persistCollection(contextResourceNoConfig, CONFIG_COL_NAME, ImmutableList.of(
                new ResourceCollectionItem("0",  ImmutableMap.<String, Object>of("prop1", "value1")),
                new ResourceCollectionItem("1", ImmutableMap.<String, Object>of("prop2", 5))
        ));
        context.resourceResolver().commit();

        String configPath0 = getConfigPropsPath("/conf/testNoConfig/" + CONFIGS_PARENT_NAME + "/" + CONFIG_COL_NAME + "/0");
        ValueMap props0 = context.resourceResolver().getResource(configPath0).getValueMap();
        assertEquals("value1", props0.get("prop1"));

        String configPath1 = getConfigPropsPath("/conf/testNoConfig/" + CONFIGS_PARENT_NAME + "/" + CONFIG_COL_NAME + "/1");
        ValueMap props1 = context.resourceResolver().getResource(configPath1).getValueMap();
        assertEquals((Integer)5, props1.get("prop2"));
    }

    @Test
    public void testNewCollectionItem() {
        ConfigurationData newItem = underTest.newCollectionItem(contextResource, CONFIG_COL_NAME);
        assertNotNull(newItem);
        assertEquals((Integer)5, newItem.getEffectiveValues().get("prop3", 0));
    }

    @Test
    public void testNewCollectionItem_NoConfigMetadata() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_COL_NAME)).thenReturn(null);

        ConfigurationData newItem = underTest.newCollectionItem(contextResource, CONFIG_COL_NAME);
        assertNull(newItem);
    }

    @Test
    public void testGetConfigurationNames() {
        assertEquals(ImmutableSortedSet.of(CONFIG_NAME, CONFIG_COL_NAME, CONFIG_NESTED_NAME), underTest.getConfigurationNames());
    }

    @Test
    public void testGetConfigurationMetadata() {
        assertEquals(CONFIG_NAME, underTest.getConfigurationMetadata(CONFIG_NAME).getName());
    }

    @Test
    public void testGetNested() {
        ConfigurationData configData = underTest.get(contextResource, CONFIG_NESTED_NAME);
        assertNotNull(configData);

        assertEquals(ImmutableSet.of("prop1", "propSub", "propSubList", "prop4"), configData.getPropertyNames());
        assertEquals("value1", configData.getValues().get("prop1", String.class));
        assertEquals("value1", configData.getEffectiveValues().get("prop1", String.class));
        assertEquals(true, configData.getValues().get("prop4", false));
        assertEquals(true, configData.getEffectiveValues().get("prop4", false));
        
        assertEquals(ConfigurationMetadata.class, configData.getValueInfo("propSub").getPropertyMetadata().getType());
        assertEquals(ConfigurationMetadata[].class, configData.getValueInfo("propSubList").getPropertyMetadata().getType());
        
        ConfigurationData subData = configData.getValues().get("propSub", ConfigurationData.class);
        ConfigurationData subDataEffective = configData.getEffectiveValues().get("propSub", ConfigurationData.class);
        assertNotNull(subData);
        assertNotNull(subDataEffective);
        
        assertNull(subData.getValues().get("prop1", String.class));
        assertEquals("propSubValue1", subData.getEffectiveValues().get("prop1", String.class));
        assertNull(subData.getValues().get("prop4", String.class));
        assertEquals(true, subData.getEffectiveValues().get("prop4", false));
        
        ConfigurationData[] subListData = configData.getValues().get("propSubList", ConfigurationData[].class);
        ConfigurationData[] subListDataEffective = configData.getEffectiveValues().get("propSubList", ConfigurationData[].class);
        assertNotNull(subListData);
        assertNotNull(subListDataEffective);
        
        assertEquals(2, subListData.length);
        assertEquals("propSubListValue1.1", subListData[0].getValues().get("prop1", String.class));
        assertEquals("propSubListValue1.1", subListData[0].getEffectiveValues().get("prop1", String.class));
        assertEquals("propSubListValue1.2", subListData[1].getValues().get("prop1", String.class));
        assertEquals("propSubListValue1.2", subListData[1].getEffectiveValues().get("prop1", String.class));
        
        ConfigurationData subListDataItem1Sub = subListData[0].getValues().get("propSub", ConfigurationData.class);
        ConfigurationData subListDataItem1SubEffecive = subListData[0].getEffectiveValues().get("propSub", ConfigurationData.class);
        assertNotNull(subListDataItem1Sub);
        assertNotNull(subListDataItem1SubEffecive);

        assertNull(subListDataItem1Sub.getValues().get("prop1", String.class));
        assertEquals("propSubList1_proSubValue1", subListDataItem1Sub.getEffectiveValues().get("prop1", String.class));
        assertNull(subListDataItem1Sub.getValues().get("prop4", String.class));
        assertEquals(true, subListDataItem1Sub.getEffectiveValues().get("prop4", false));
    }

}
