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
import org.apache.sling.caconfig.impl.def.ConfigurationDefNameConstants;
import org.apache.sling.caconfig.impl.override.DummyConfigurationOverrideProvider;
import org.apache.sling.caconfig.management.ConfigurationCollectionData;
import org.apache.sling.caconfig.management.ConfigurationData;
import org.apache.sling.caconfig.management.ConfigurationManager;
import org.apache.sling.caconfig.resource.impl.def.ConfigurationResourceNameConstants;
import org.apache.sling.caconfig.spi.ConfigurationCollectionPersistData;
import org.apache.sling.caconfig.spi.ConfigurationMetadataProvider;
import org.apache.sling.caconfig.spi.ConfigurationOverrideProvider;
import org.apache.sling.caconfig.spi.ConfigurationPersistData;
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
        ConfigurationTestUtils.registerConfigurationResolver(context,
                "configBucketNames", getAlternativeBucketNames());
        underTest = context.registerInjectActivateService(new ConfigurationManagerImpl());
        
        contextResource = context.create().resource("/content/test",
                PROPERTY_CONFIG_REF, "/conf/test");
        contextResourceLevel2 = context.create().resource("/content/test/level2",
                PROPERTY_CONFIG_REF, "/conf/test/level2");
        contextResourceLevel3 = context.create().resource("/content/test/level2/level3",
                PROPERTY_CONFIG_REF, "/conf/test/level2/level3");
        contextResourceNoConfig = context.create().resource("/content/testNoConfig",
                PROPERTY_CONFIG_REF, "/conf/testNoConfig");
        
        context.create().resource(getConfigPropsPath("/conf/test/sling:configs/" + CONFIG_NAME),
                "prop1", "value1",
                "prop4", true);
        context.create().resource(getConfigPropsPath("/conf/test/sling:configs/" + CONFIG_COL_NAME + "/1"),
                "prop1", "value1");
        context.create().resource(getConfigPropsPath("/conf/test/sling:configs/" + CONFIG_COL_NAME + "/2"),
                "prop4", true);
        
        // test fixture with resource collection inheritance on level 2
        context.create().resource(getConfigCollectionParentPath("/conf/test/level2/sling:configs/" + CONFIG_COL_NAME),
                PROPERTY_CONFIG_COLLECTION_INHERIT, true);
        context.create().resource(getConfigPropsPath("/conf/test/level2/sling:configs/" + CONFIG_COL_NAME + "/1"),
                "prop1", "value1_level2");
        
        // test fixture with property inheritance and resource collection inheritance on level 3
        context.create().resource(getConfigPropsPath("/conf/test/level2/level3/sling:configs/" + CONFIG_NAME),
                "prop4", false,
                "prop5", "value5_level3",
                PROPERTY_CONFIG_PROPERTY_INHERIT, true);
        context.create().resource(getConfigCollectionParentPath("/conf/test/level2/level3/sling:configs/" + CONFIG_COL_NAME),
                PROPERTY_CONFIG_COLLECTION_INHERIT, true);
        context.create().resource(getConfigPropsPath("/conf/test/level2/level3/sling:configs/" + CONFIG_COL_NAME + "/1"),
                "prop4", false,
                "prop5", "value5_level3",
                PROPERTY_CONFIG_PROPERTY_INHERIT, true);

        // test fixture nested configuration
        context.create().resource(getConfigPropsPath("/conf/test/level2/sling:configs/" + CONFIG_NESTED_NAME),
                "prop1", "value1",
                "prop4", true);
        context.create().resource(getConfigPropsPath(getConfigPropsPath("/conf/test/sling:configs/" + CONFIG_NESTED_NAME) + "/propSub"),
                "prop1", "propSubValue1",
                "prop4", true);
        context.create().resource(getConfigPropsPath(getConfigPropsPath(getConfigPropsPath("/conf/test/sling:configs/" + CONFIG_NESTED_NAME) + "/propSub") + "/propSubLevel2"),
                "prop1", "propSubLevel2Value1",
                "prop4", true);
        context.create().resource(getConfigPropsPath(getConfigPropsPath("/conf/test/level2/sling:configs/" + CONFIG_NESTED_NAME) + "/propSubList/item1"),
                "prop1", "propSubListValue1.1");
        context.create().resource(getConfigPropsPath(getConfigPropsPath("/conf/test/level2/sling:configs/" + CONFIG_NESTED_NAME) + "/propSubList/item2"),
                "prop1", "propSubListValue1.2");
        context.create().resource(getConfigPropsPath(getConfigPropsPath(getConfigPropsPath("/conf/test/sling:configs/" + CONFIG_NESTED_NAME) + "/propSubList/item1") + "/propSub"),
                "prop1", "propSubList1_proSubValue1",
                "prop4", true);
        
        
        // config metadata singleton config
        ConfigurationMetadata configMetadata = new ConfigurationMetadata(CONFIG_NAME, ImmutableList.<PropertyMetadata<?>>of(
                new PropertyMetadata<>("prop1", "defValue"),
                new PropertyMetadata<>("prop2", String.class),
                new PropertyMetadata<>("prop3", 5)),
                false);
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_NAME)).thenReturn(configMetadata);

        // config metadata config collection
        ConfigurationMetadata configColMetadata = new ConfigurationMetadata(CONFIG_COL_NAME, ImmutableList.<PropertyMetadata<?>>of(
                new PropertyMetadata<>("prop1", "defValue"),
                new PropertyMetadata<>("prop2", String.class),
                new PropertyMetadata<>("prop3", 5)),
                true);
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_COL_NAME)).thenReturn(configColMetadata);

        // config metadata nested config
        /*
         * testConfigNested
         *  |
         *  +- propSub
         *  |   |
         *  |   +- propSubLevel2
         *  |
         *  +- propSubList
         *      |
         *      +- <collection>
         *          |
         *          +- propSub
         *              |
         *              +- propSubLevel2
         */
        ConfigurationMetadata propSubLevel2Metadata = new ConfigurationMetadata("propSubLevel2", ImmutableList.<PropertyMetadata<?>>of(
                new PropertyMetadata<>("prop1", "defValueLevel2")),
                false);
        ConfigurationMetadata propSubMetadata = new ConfigurationMetadata("propSub", ImmutableList.<PropertyMetadata<?>>of(
                new PropertyMetadata<>("prop1", "defValue"),
                new PropertyMetadata<>("prop2", String.class),
                new PropertyMetadata<>("prop3", 5),
                new PropertyMetadata<>("propSubLevel2", ConfigurationMetadata.class).configurationMetadata(propSubLevel2Metadata)),
                false);
        ConfigurationMetadata propSubListMetadata = new ConfigurationMetadata("propSubList", ImmutableList.<PropertyMetadata<?>>of(
                new PropertyMetadata<>("prop1", "defValueSubList"),
                new PropertyMetadata<>("propSub", ConfigurationMetadata.class).configurationMetadata(propSubMetadata)),
                true);
        ConfigurationMetadata configNestedMetadata = new ConfigurationMetadata(CONFIG_NESTED_NAME, ImmutableList.<PropertyMetadata<?>>of(
                new PropertyMetadata<>("prop1", "defValue"),
                new PropertyMetadata<>("propSub", ConfigurationMetadata.class).configurationMetadata(propSubMetadata),
                new PropertyMetadata<>("propSubList", ConfigurationMetadata[].class).configurationMetadata(propSubListMetadata)),
                false);
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_NESTED_NAME)).thenReturn(configNestedMetadata);

        when(configurationMetadataProvider.getConfigurationNames()).thenReturn(ImmutableSortedSet.of(CONFIG_NAME, CONFIG_COL_NAME, CONFIG_NESTED_NAME));
    }
    
    protected String getConfigPropsPath(String path) {
        return path;
    }
    
    protected String getConfigPropsPersistPath(String path) {
        return path;
    }
    
    protected String getConfigCollectionParentPath(String path) {
        return path;
    }
    
    protected String[] getAlternativeBucketNames() {
        return new String[0];
    }
    
    @Test
    public void testGetConfiguration() {
        ConfigurationData configData = underTest.getConfiguration(contextResource, CONFIG_NAME);
        assertNotNull(configData);

        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3", "prop4"), configData.getPropertyNames());
        assertEquals("value1", configData.getValues().get("prop1", String.class));
        assertEquals((Integer)5, configData.getEffectiveValues().get("prop3", 0));

        assertFalse(configData.getValueInfo("prop1").isInherited());
        assertFalse(configData.getValueInfo("prop3").isInherited());

        assertFalse(configData.getValues().get(ConfigurationDefNameConstants.PROPERTY_CONFIG_PROPERTY_INHERIT, false));
    }

    @Test
    public void testGetConfiguration_WithResourceInheritance() {
        ConfigurationData configData = underTest.getConfiguration(contextResourceLevel2, CONFIG_NAME);
        assertNotNull(configData);

        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3", "prop4"), configData.getPropertyNames());
        assertNull(configData.getValues().get("prop1", String.class));
        assertEquals("value1", configData.getEffectiveValues().get("prop1", String.class));
        assertEquals((Integer)5, configData.getEffectiveValues().get("prop3", 0));

        String configPath = getConfigPropsPath("/conf/test/sling:configs/" + CONFIG_NAME);
        assertEquals(configPath, configData.getValueInfo("prop1").getConfigSourcePath());
        assertTrue(configData.getValueInfo("prop1").isInherited());
        assertFalse(configData.getValueInfo("prop3").isInherited());
        assertNull(configData.getValueInfo("prop3").getConfigSourcePath());

        assertFalse(configData.getValues().get(ConfigurationDefNameConstants.PROPERTY_CONFIG_PROPERTY_INHERIT, false));
    }
    
    @Test
    public void testGetConfiguration_WithPropertyInheritance() {
        ConfigurationData configData = underTest.getConfiguration(contextResourceLevel3, CONFIG_NAME);
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

        String configPath = getConfigPropsPath("/conf/test/sling:configs/" + CONFIG_NAME);
        String configPathLevel3 = getConfigPropsPath("/conf/test/level2/level3/sling:configs/" + CONFIG_NAME);
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
        
        assertTrue(configData.getValues().get(ConfigurationDefNameConstants.PROPERTY_CONFIG_PROPERTY_INHERIT, false));
    }

    @Test
    public void testGetConfiguration_WithOverride() {
        context.registerService(ConfigurationOverrideProvider.class, new DummyConfigurationOverrideProvider(
                "[/content]" + CONFIG_NAME + "={prop1='override1'}"));
        
        ConfigurationData configData = underTest.getConfiguration(contextResource, CONFIG_NAME);
        assertNotNull(configData);
        assertTrue(configData.isOverridden());

        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3"), configData.getPropertyNames());
        assertEquals("value1", configData.getValues().get("prop1", String.class));
        assertEquals("override1", configData.getEffectiveValues().get("prop1", String.class));
        assertEquals((Integer)5, configData.getEffectiveValues().get("prop3", 0));

        assertFalse(configData.getValueInfo("prop1").isInherited());
        assertTrue(configData.getValueInfo("prop1").isOverridden());
        assertFalse(configData.getValueInfo("prop3").isInherited());
        assertTrue(configData.getValueInfo("prop3").isOverridden());
    }

    @Test
    public void testGetConfiguration_NoConfigResource() {
        ConfigurationData configData = underTest.getConfiguration(contextResourceNoConfig, CONFIG_NAME);
        assertNotNull(configData);

        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3"), configData.getPropertyNames());
        assertNull(configData.getValues().get("prop1", String.class));
        assertEquals((Integer)5, configData.getEffectiveValues().get("prop3", 0));

        assertFalse(configData.getValueInfo("prop1").isInherited());
        assertFalse(configData.getValueInfo("prop3").isInherited());
    }

    @Test
    public void testGetConfiguration_NoConfigMetadata() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_NAME)).thenReturn(null);

        ConfigurationData configData = underTest.getConfiguration(contextResource, CONFIG_NAME);
        assertNotNull(configData);

        assertEquals(ImmutableSet.of("prop1", "prop4"), configData.getPropertyNames());
        assertEquals("value1", configData.getValues().get("prop1", String.class));
        assertEquals((Integer)0, configData.getEffectiveValues().get("prop3", 0));

        assertFalse(configData.getValueInfo("prop1").isInherited());
        assertFalse(configData.getValueInfo("prop3").isInherited());
    }

    @Test
    public void testGetConfiguration_NoConfigResource_NoConfigMetadata() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_NAME)).thenReturn(null);

        ConfigurationData configData = underTest.getConfiguration(contextResourceNoConfig, CONFIG_NAME);
        assertNull(configData);
    }

    @Test
    public void testGetConfigurationCollection() {
        ConfigurationCollectionData configCollectionData = underTest.getConfigurationCollection(contextResource, CONFIG_COL_NAME);
        List<ConfigurationData> configDatas = ImmutableList.copyOf(configCollectionData.getItems());
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

        assertNull(configCollectionData.getProperties().get(ConfigurationResourceNameConstants.PROPERTY_CONFIG_COLLECTION_INHERIT));
    }

    @Test
    public void testGetConfigurationCollection_WithResourceCollectionInheritance() {
        ConfigurationCollectionData configCollectionData = underTest.getConfigurationCollection(contextResourceLevel2, CONFIG_COL_NAME);
        List<ConfigurationData> configDatas = ImmutableList.copyOf(configCollectionData.getItems());
        assertEquals(2, configDatas.size());
        
        ConfigurationData configData1 = configDatas.get(0);
        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3"), configData1.getPropertyNames());
        assertEquals("value1_level2", configData1.getValues().get("prop1", String.class));
        assertEquals("value1_level2", configData1.getEffectiveValues().get("prop1", String.class));
        assertEquals((Integer)5, configData1.getEffectiveValues().get("prop3", 0));

        String configPath1 = getConfigPropsPath("/conf/test/level2/sling:configs/" + CONFIG_COL_NAME + "/1");
        assertFalse(configData1.getValueInfo("prop1").isInherited());
        assertEquals(configPath1, configData1.getValueInfo("prop1").getConfigSourcePath());
        assertFalse(configData1.getValueInfo("prop3").isInherited());
        assertNull(configData1.getValueInfo("prop3").getConfigSourcePath());
        
        ConfigurationData configData2 = configDatas.get(1);
        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3", "prop4"), configData2.getPropertyNames());
        assertNull(configData2.getValues().get("prop1", String.class));
        assertEquals((Integer)5, configData2.getEffectiveValues().get("prop3", 0));

        String configPath2 = getConfigPropsPath("/conf/test/sling:configs/" + CONFIG_COL_NAME + "/2");
        assertTrue(configData2.getValueInfo("prop4").isInherited());
        assertEquals(configPath2, configData2.getValueInfo("prop4").getConfigSourcePath());
        assertFalse(configData2.getValueInfo("prop3").isInherited());
        assertNull(configData2.getValueInfo("prop3").getConfigSourcePath());
        
        assertTrue((Boolean)configCollectionData.getProperties().get(ConfigurationResourceNameConstants.PROPERTY_CONFIG_COLLECTION_INHERIT));
    }

    @Test
    public void testGetConfigurationCollection_WithResourceCollectionAndPropertyInheritance() {
        ConfigurationCollectionData configCollectionData = underTest.getConfigurationCollection(contextResourceLevel3, CONFIG_COL_NAME);
        List<ConfigurationData> configDatas = ImmutableList.copyOf(configCollectionData.getItems());
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

        String configPathLevel2 = getConfigPropsPath("/conf/test/level2/sling:configs/" + CONFIG_COL_NAME + "/1");
        String configPathLevel3 = getConfigPropsPath("/conf/test/level2/level3/sling:configs/" + CONFIG_COL_NAME + "/1");
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

        String configPath2 = getConfigPropsPath("/conf/test/sling:configs/" + CONFIG_COL_NAME + "/2");
        assertTrue(configData2.getValueInfo("prop4").isInherited());
        assertEquals(configPath2, configData2.getValueInfo("prop4").getConfigSourcePath());
        assertFalse(configData2.getValueInfo("prop3").isInherited());
        assertNull(configData2.getValueInfo("prop3").getConfigSourcePath());

        assertTrue((Boolean)configCollectionData.getProperties().get(ConfigurationResourceNameConstants.PROPERTY_CONFIG_COLLECTION_INHERIT));
        assertTrue(configData1.getValues().get(ConfigurationDefNameConstants.PROPERTY_CONFIG_PROPERTY_INHERIT, false));
    }

    @Test
    public void testGetConfigurationCollection_WithOverride() {
        context.registerService(ConfigurationOverrideProvider.class, new DummyConfigurationOverrideProvider(
                "[/content]" + CONFIG_COL_NAME + "/prop1='override1'"));
        
        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getConfigurationCollection(contextResource, CONFIG_COL_NAME).getItems());
        assertEquals(2, configDatas.size());

        ConfigurationData configData1 = configDatas.get(0);
        assertFalse(configData1.isOverridden());
        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3"), configData1.getPropertyNames());
        assertEquals("value1", configData1.getValues().get("prop1", String.class));
        assertEquals("override1", configData1.getEffectiveValues().get("prop1", String.class));
        assertEquals((Integer)5, configData1.getEffectiveValues().get("prop3", 0));

        assertFalse(configData1.getValueInfo("prop1").isInherited());
        assertTrue(configData1.getValueInfo("prop1").isOverridden());
        assertFalse(configData1.getValueInfo("prop3").isInherited());
        assertFalse(configData1.getValueInfo("prop3").isOverridden());
        
        ConfigurationData configData2 = configDatas.get(1);
        assertFalse(configData2.isOverridden());
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
    public void testGetConfigurationCollection_NoConfigResources() {
        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getConfigurationCollection(contextResourceNoConfig, CONFIG_COL_NAME).getItems());
        assertEquals(0, configDatas.size());
    }

    @Test
    public void testGetConfigurationCollection_NoConfigMetadata() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_COL_NAME)).thenReturn(null);
        
        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getConfigurationCollection(contextResource, CONFIG_COL_NAME).getItems());
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
    public void testGetConfigurationCollection_NoConfigResources_NoConfigMetadata() {
        when(configurationMetadataProvider.getConfigurationMetadata(CONFIG_COL_NAME)).thenReturn(null);

        List<ConfigurationData> configDatas = ImmutableList.copyOf(underTest.getConfigurationCollection(contextResourceNoConfig, CONFIG_COL_NAME).getItems());
        assertEquals(0, configDatas.size());
    }

    @Test
    public void testPersistConfiguration() throws Exception {
        underTest.persistConfiguration(contextResourceNoConfig, CONFIG_NAME,
                new ConfigurationPersistData(ImmutableMap.<String, Object>of("prop1", "value1")));
        context.resourceResolver().commit();

        String configPath = getConfigPropsPersistPath("/conf/testNoConfig/sling:configs/" + CONFIG_NAME);
        ValueMap props = context.resourceResolver().getResource(configPath).getValueMap();
        assertEquals("value1", props.get("prop1"));
    }

    @Test
    public void testPersistConfigurationCollection() throws Exception {
        underTest.persistConfigurationCollection(contextResourceNoConfig, CONFIG_COL_NAME,
                new ConfigurationCollectionPersistData(ImmutableList.of(
                        new ConfigurationPersistData(ImmutableMap.<String, Object>of("prop1", "value1")).collectionItemName("0"),
                        new ConfigurationPersistData(ImmutableMap.<String, Object>of("prop2", 5)).collectionItemName("1"))
        ));
        context.resourceResolver().commit();

        String configPath0 = getConfigPropsPersistPath("/conf/testNoConfig/sling:configs/" + CONFIG_COL_NAME + "/0");
        ValueMap props0 = context.resourceResolver().getResource(configPath0).getValueMap();
        assertEquals("value1", props0.get("prop1"));

        String configPath1 = getConfigPropsPersistPath("/conf/testNoConfig/sling:configs/" + CONFIG_COL_NAME + "/1");
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
    public void testGetConfigurationConfigurationNames() {
        assertEquals(ImmutableSortedSet.of(CONFIG_NAME, CONFIG_COL_NAME, CONFIG_NESTED_NAME), underTest.getConfigurationNames());
    }

    @Test
    public void testGetConfigurationConfigurationMetadata() {
        assertEquals(CONFIG_NAME, underTest.getConfigurationMetadata(CONFIG_NAME).getName());
    }

    @Test
    public void testGetConfigurationNested() {
        ConfigurationData configData = underTest.getConfiguration(contextResourceLevel2, CONFIG_NESTED_NAME);
        assertNotNull(configData);

        // root level
        assertEquals(ImmutableSet.of("prop1", "propSub", "propSubList", "prop4"), configData.getPropertyNames());
        assertEquals("value1", configData.getValues().get("prop1", String.class));
        assertEquals("value1", configData.getEffectiveValues().get("prop1", String.class));
        assertTrue(configData.getValues().get("prop4", false));
        assertTrue(configData.getEffectiveValues().get("prop4", false));
        
        assertEquals(ConfigurationMetadata.class, configData.getValueInfo("propSub").getPropertyMetadata().getType());
        assertEquals(ConfigurationMetadata[].class, configData.getValueInfo("propSubList").getPropertyMetadata().getType());
        
        // propSub
        ConfigurationData subData = configData.getValues().get("propSub", ConfigurationData.class);
        ConfigurationData subDataEffective = configData.getEffectiveValues().get("propSub", ConfigurationData.class);
        assertNotNull(subData);
        assertNotNull(subDataEffective);
        
        assertTrue(ConfigurationData.class.isAssignableFrom(configData.getValueInfo("propSub").getValue().getClass()));
        assertTrue(ConfigurationData.class.isAssignableFrom(configData.getValueInfo("propSub").getEffectiveValue().getClass()));
        assertTrue(ConfigurationData[].class.isAssignableFrom(configData.getValueInfo("propSubList").getValue().getClass()));
        assertTrue(ConfigurationData[].class.isAssignableFrom(configData.getValueInfo("propSubList").getEffectiveValue().getClass()));

        assertNull(subData.getValues().get("prop1", String.class));
        assertEquals("propSubValue1", subData.getEffectiveValues().get("prop1", String.class));
        assertFalse(subData.getValues().get("prop4", false));
        assertTrue(subData.getEffectiveValues().get("prop4", false));
        
        // propSub/propSubLevel2
        ConfigurationData subDataLevel2 = subData.getValues().get("propSubLevel2", ConfigurationData.class);
        ConfigurationData subDataLevel2Effective = subData.getEffectiveValues().get("propSubLevel2", ConfigurationData.class);
        assertNotNull(subDataLevel2);
        assertNotNull(subDataLevel2Effective);
        
        assertTrue(ConfigurationData.class.isAssignableFrom(subData.getValueInfo("propSubLevel2").getValue().getClass()));
        assertTrue(ConfigurationData.class.isAssignableFrom(subData.getValueInfo("propSubLevel2").getEffectiveValue().getClass()));

        assertNull(subDataLevel2.getValues().get("prop1", String.class));
        assertEquals("propSubLevel2Value1", subDataLevel2.getEffectiveValues().get("prop1", String.class));
        assertFalse(subDataLevel2.getValues().get("prop4", false));
        assertTrue(subDataLevel2.getEffectiveValues().get("prop4", false));
        
        // propSubList
        ConfigurationData[] subListData = configData.getValues().get("propSubList", ConfigurationData[].class);
        ConfigurationData[] subListDataEffective = configData.getEffectiveValues().get("propSubList", ConfigurationData[].class);
        assertNotNull(subListData);
        assertNotNull(subListDataEffective);
        
        assertTrue(ConfigurationData[].class.isAssignableFrom(configData.getValueInfo("propSubList").getValue().getClass()));
        assertTrue(ConfigurationData[].class.isAssignableFrom(configData.getValueInfo("propSubList").getEffectiveValue().getClass()));
        
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
        assertFalse(subListDataItem1Sub.getValues().get("prop4", false));
        assertTrue(subListDataItem1Sub.getEffectiveValues().get("prop4", false));
    }

    @Test
    public void testGetConfigurationNested_NoConfigResource() {
        ConfigurationData configData = underTest.getConfiguration(contextResourceNoConfig, CONFIG_NESTED_NAME);
        assertNotNull(configData);

        assertEquals(ImmutableSet.of("prop1", "propSub", "propSubList"), configData.getPropertyNames());
        assertNull(configData.getValues().get("prop1", String.class));
        assertEquals("defValue", configData.getEffectiveValues().get("prop1", String.class));
        
        assertEquals(ConfigurationMetadata.class, configData.getValueInfo("propSub").getPropertyMetadata().getType());
        assertEquals(ConfigurationMetadata[].class, configData.getValueInfo("propSubList").getPropertyMetadata().getType());
        
        ConfigurationData subData = configData.getValues().get("propSub", ConfigurationData.class);
        ConfigurationData subDataEffective = configData.getEffectiveValues().get("propSub", ConfigurationData.class);
        assertNotNull(subData);
        assertNotNull(subDataEffective);
        
        assertTrue(ConfigurationData.class.isAssignableFrom(configData.getValueInfo("propSub").getValue().getClass()));
        assertTrue(ConfigurationData.class.isAssignableFrom(configData.getValueInfo("propSub").getEffectiveValue().getClass()));
        assertTrue(ConfigurationData[].class.isAssignableFrom(configData.getValueInfo("propSubList").getValue().getClass()));
        assertTrue(ConfigurationData[].class.isAssignableFrom(configData.getValueInfo("propSubList").getEffectiveValue().getClass()));

        assertNull(subData.getValues().get("prop1", String.class));
        assertEquals("defValue", subData.getEffectiveValues().get("prop1", String.class));
        
        ConfigurationData[] subListData = configData.getValues().get("propSubList", ConfigurationData[].class);
        ConfigurationData[] subListDataEffective = configData.getEffectiveValues().get("propSubList", ConfigurationData[].class);
        assertNotNull(subListData);
        assertNotNull(subListDataEffective);
        
        assertTrue(ConfigurationData[].class.isAssignableFrom(configData.getValueInfo("propSubList").getValue().getClass()));
        assertTrue(ConfigurationData[].class.isAssignableFrom(configData.getValueInfo("propSubList").getEffectiveValue().getClass()));
        
        assertEquals(0, subListData.length);
    }

    @Test
    public void testGetConfigurationNames() {
        assertEquals(ImmutableSortedSet.of(CONFIG_NAME, CONFIG_COL_NAME, CONFIG_NESTED_NAME), underTest.getConfigurationNames());
    }

    @Test
    public void testGetConfigurationMetadata() {
        ConfigurationMetadata configMetadata = underTest.getConfigurationMetadata(CONFIG_NAME);
        assertNotNull(configMetadata);
        assertEquals(CONFIG_NAME, configMetadata.getName());
    }

    @Test
    public void testGetConfigurationMetadata_Nested() {
        ConfigurationMetadata configMetadata = underTest.getConfigurationMetadata(CONFIG_NESTED_NAME);
        assertNotNull(configMetadata);
        assertEquals(CONFIG_NESTED_NAME, configMetadata.getName());
    }
    
    @Test
    public void testGetConfigurationMetadata_Nested_Sub() {
        ConfigurationMetadata configMetadataSub = underTest.getConfigurationMetadata(getConfigPropsPath(CONFIG_NESTED_NAME) + "/propSub");
        assertNotNull(configMetadataSub);
        assertEquals("propSub", configMetadataSub.getName());
    }
    
    @Test
    public void testGetConfigurationMetadata_Nested_SubLevel2() {
        ConfigurationMetadata configMetadataSubLevel2 = underTest.getConfigurationMetadata(getConfigPropsPath(getConfigPropsPath(CONFIG_NESTED_NAME)
                + "/propSub") + "/propSubLevel2");
        assertNotNull(configMetadataSubLevel2);
        assertEquals("propSubLevel2", configMetadataSubLevel2.getName());
    }
    
    @Test
    public void testGetConfigurationMetadata_Nested_SubList() {
        ConfigurationMetadata configMetadataSubList = underTest.getConfigurationMetadata(getConfigPropsPath(CONFIG_NESTED_NAME) + "/propSubList");
        assertNotNull(configMetadataSubList);
        assertEquals("propSubList", configMetadataSubList.getName());
    }
    
    @Test
    public void testGetConfigurationMetadata_Nested_SubList_Sub() throws Exception {
        // delete resource already existing in test fixture to test with non-existing resource but existing collection item as parent
        context.resourceResolver().delete(context.resourceResolver().getResource(
                getConfigPropsPath(getConfigPropsPath(getConfigPropsPath("/conf/test/sling:configs/" + CONFIG_NESTED_NAME)
                        + "/propSubList/item1") + "/propSub")));

        ConfigurationMetadata subListDataItem1Sub = underTest.getConfigurationMetadata(getConfigPropsPath(getConfigPropsPath(CONFIG_NESTED_NAME)
                + "/propSubList/item1") + "/propSub");
        assertNotNull(subListDataItem1Sub);
        assertEquals("propSub", subListDataItem1Sub.getName());
    }
    
    @Test
    public void testGetConfigurationMetadata_Nested_SubList_SubLevel2() throws Exception {
        // delete resource already existing in test fixture to test with non-existing resource but existing collection item as parent
        context.resourceResolver().delete(context.resourceResolver().getResource(
                getConfigPropsPath(getConfigPropsPath(getConfigPropsPath("/conf/test/sling:configs/" + CONFIG_NESTED_NAME)
                        + "/propSubList/item1") + "/propSub")));

        ConfigurationMetadata subListDataItem1SubLevel2 = underTest.getConfigurationMetadata(getConfigPropsPath(getConfigPropsPath(getConfigPropsPath(CONFIG_NESTED_NAME)
                + "/propSubList/item1") + "/propSub") + "/propSubLevel2");
        assertNotNull(subListDataItem1SubLevel2);
        assertEquals("propSubLevel2", subListDataItem1SubLevel2.getName());
    }
    
    @Test
    public void testNewCollectionItem_Nested_SubList() {
        ConfigurationData configData = underTest.newCollectionItem(contextResource, getConfigPropsPath(CONFIG_NESTED_NAME) + "/propSubList");
        assertEquals(getConfigPropsPath(CONFIG_NESTED_NAME) + "/propSubList", configData.getConfigName());
        
        assertNull(configData.getValues().get("prop1", String.class));
        assertEquals("defValueSubList", configData.getEffectiveValues().get("prop1", String.class));
    }

    @Test
    public void testGetPersistenceResourcePath() {
        assertEquals(getConfigPropsPath("/a/b/c"), underTest.getPersistenceResourcePath("/a/b/c"));
        assertEquals(getConfigPropsPath("a/b"), underTest.getPersistenceResourcePath("a/b"));
    }
        
    @Test
    public void testPersistConfiguration_Nested() throws Exception {
        underTest.persistConfiguration(contextResourceLevel2, getConfigPropsPath(getConfigPropsPath(CONFIG_NESTED_NAME)
                + "/propSub") + "/propSubLevel2",
                new ConfigurationPersistData(ImmutableMap.<String, Object>of("prop1", "value1_persist")));
        context.resourceResolver().commit();

        ConfigurationData configData = underTest.getConfiguration(contextResourceLevel2, CONFIG_NESTED_NAME);
        ConfigurationData subData = configData.getValues().get("propSub", ConfigurationData.class);
        ConfigurationData subDataLevel2 = subData.getValues().get("propSubLevel2", ConfigurationData.class);

        assertEquals("value1_persist", subDataLevel2.getValues().get("prop1", String.class));
        assertEquals("value1_persist", subDataLevel2.getEffectiveValues().get("prop1", String.class));
        assertFalse(subDataLevel2.getValues().get("prop4", false));
        assertFalse(subDataLevel2.getEffectiveValues().get("prop4", false));
    }

    @Test
    public void testPersistConfigurationCollection_Nested() throws Exception {
        underTest.persistConfigurationCollection(contextResourceLevel2, getConfigPropsPath(CONFIG_NESTED_NAME) + "/propSubList",
                new ConfigurationCollectionPersistData(ImmutableList.of(
                        new ConfigurationPersistData(ImmutableMap.<String, Object>of("prop1", "value1_persist")).collectionItemName("item1"),
                        new ConfigurationPersistData(ImmutableMap.<String, Object>of("prop1", "value2_persist")).collectionItemName("item2"),
                        new ConfigurationPersistData(ImmutableMap.<String, Object>of("prop1", "value3_persist")).collectionItemName("item3"))
        ));
        context.resourceResolver().commit();

        ConfigurationData configData = underTest.getConfiguration(contextResourceLevel2, CONFIG_NESTED_NAME);
        ConfigurationData[] subListData = configData.getValues().get("propSubList", ConfigurationData[].class);
        
        assertEquals(3, subListData.length);
        assertEquals("value1_persist", subListData[0].getValues().get("prop1", String.class));
        assertEquals("value2_persist", subListData[1].getValues().get("prop1", String.class));
        assertEquals("value3_persist", subListData[2].getValues().get("prop1", String.class));
    }

    @Test
    public void testDeleteConfiguration() throws Exception {
        underTest.deleteConfiguration(contextResource, CONFIG_NAME);
        
        ConfigurationData configData = underTest.getConfiguration(contextResource, CONFIG_NAME);
        assertEquals(ImmutableSet.of("prop1", "prop2", "prop3"), configData.getPropertyNames());

        assertNull(configData.getValues().get("prop1", String.class));
        assertEquals("defValue", configData.getEffectiveValues().get("prop1", String.class));
        assertNull(configData.getValues().get("prop2", String.class));
        assertNull(configData.getEffectiveValues().get("prop2", String.class));
        assertNull(configData.getValues().get("prop3", Integer.class));
        assertEquals((Integer)5, configData.getEffectiveValues().get("prop3", Integer.class));
    }

    @Test
    public void testDeleteConfigurationCollection() throws Exception {
        underTest.deleteConfiguration(contextResource, CONFIG_COL_NAME);
        
        ConfigurationCollectionData configCollectionData = underTest.getConfigurationCollection(contextResource, CONFIG_COL_NAME);
        List<ConfigurationData> configDatas = ImmutableList.copyOf(configCollectionData.getItems());
        assertEquals(0, configDatas.size());
    }

}
