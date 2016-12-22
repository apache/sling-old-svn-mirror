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
package org.apache.sling.caconfig.impl;

import static org.apache.sling.caconfig.resource.impl.def.ConfigurationResourceNameConstants.PROPERTY_CONFIG_REF;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.ConfigurationResolveException;
import org.apache.sling.caconfig.ConfigurationResolver;
import org.apache.sling.caconfig.impl.override.DummyConfigurationOverrideProvider;
import org.apache.sling.caconfig.spi.ConfigurationMetadataProvider;
import org.apache.sling.caconfig.spi.ConfigurationOverrideProvider;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Test {@link ConfigurationResolver} with ValueMap for reading the config.
 */
public class ConfigurationResolverValueMapTest {

    @Rule
    public SlingContext context = new SlingContext();

    private ConfigurationResolver underTest;

    private Resource site1Page1;

    @Before
    public void setUp() {
        underTest = ConfigurationTestUtils.registerConfigurationResolver(context);

        // content resources
        context.build().resource("/content/site1", PROPERTY_CONFIG_REF, "/conf/content/site1");
        site1Page1 = context.create().resource("/content/site1/page1");
    }

    @Test
    public void testNonExistingConfigMap() {
        ValueMap props = underTest.get(site1Page1).name("sampleName").asValueMap();

        assertNull(props.get("stringParam", String.class));
        assertEquals(0, (int)props.get("intParam", 0));
        assertEquals(false, props.get("boolParam", false));
    }

    @Test
    public void testNonExistingConfigCollection() {
        Collection<ValueMap> propsList = underTest.get(site1Page1).name("sampleList").asValueMapCollection();
        assertTrue(propsList.isEmpty());
    }

    @Test
    public void testConfig() {
        context.build().resource("/conf/content/site1/sling:configs/sampleName", 
                "stringParam", "configValue1",
                "intParam", 111,
                "boolParam", true);

        ValueMap props = underTest.get(site1Page1).name("sampleName").asValueMap();

        assertEquals("configValue1", props.get("stringParam", String.class));
        assertEquals(111, (int)props.get("intParam", 0));
        assertEquals(true, props.get("boolParam", false));
    }

    @Test
    public void testConfigCollection() {
        context.build().resource("/conf/content/site1/sling:configs/sampleList")
            .siblingsMode()
            .resource("1", "stringParam", "configValue1.1")
            .resource("2", "stringParam", "configValue1.2")
            .resource("3", "stringParam", "configValue1.3");

        Collection<ValueMap> propsList = underTest.get(site1Page1).name("sampleList").asValueMapCollection();

        Iterator<ValueMap> propsIterator = propsList.iterator();
        assertEquals("configValue1.1", propsIterator.next().get("stringParam", String.class));
        assertEquals("configValue1.2", propsIterator.next().get("stringParam", String.class));
        assertEquals("configValue1.3", propsIterator.next().get("stringParam", String.class));
    }

    @Test
    public void testConfigWithDefaultValues() {
        context.registerService(ConfigurationMetadataProvider.class, new DummyConfigurationMetadataProvider("sampleName", 
                ImmutableMap.<String, Object>of("stringParam", "defValue1", "intParam", 999), false));
        
        context.build().resource("/conf/content/site1/sling:configs/sampleName", 
                "boolParam", true);

        ValueMap props = underTest.get(site1Page1).name("sampleName").asValueMap();

        assertEquals("defValue1", props.get("stringParam", String.class));
        assertEquals(999, (int)props.get("intParam", 0));
        assertEquals(true, props.get("boolParam", false));
    }

    @Test
    public void testConfigCollectionWithDefaultValues() {
        context.registerService(ConfigurationMetadataProvider.class, new DummyConfigurationMetadataProvider("sampleList", 
                ImmutableMap.<String, Object>of("intParam", 999), true));

        context.build().resource("/conf/content/site1/sling:configs/sampleList")
            .siblingsMode()
            .resource("1", "stringParam", "configValue1.1")
            .resource("2", "stringParam", "configValue1.2")
            .resource("3", "stringParam", "configValue1.3");

        List<ValueMap> propsList = ImmutableList.copyOf(underTest.get(site1Page1).name("sampleList").asValueMapCollection());

        assertEquals("configValue1.1", propsList.get(0).get("stringParam", String.class));
        assertEquals(999, (int)propsList.get(0).get("intParam", 0));
        assertEquals("configValue1.2", propsList.get(1).get("stringParam", String.class));
        assertEquals(999, (int)propsList.get(1).get("intParam", 0));
        assertEquals("configValue1.3", propsList.get(2).get("stringParam", String.class));
        assertEquals(999, (int)propsList.get(2).get("intParam", 0));
    }

    @Test
    public void testConfigWithOverride() {
        context.registerService(ConfigurationOverrideProvider.class, new DummyConfigurationOverrideProvider(
                "[/content]sampleName={stringParam='override1',intParam=222}"));

        context.build().resource("/conf/content/site1/sling:configs/sampleName", 
                "stringParam", "configValue1",
                "intParam", 111,
                "boolParam", true);

        ValueMap props = underTest.get(site1Page1).name("sampleName").asValueMap();

        assertEquals("override1", props.get("stringParam", String.class));
        assertEquals(222, (int)props.get("intParam", 0));
        assertEquals(false, props.get("boolParam", false));
    }

    @Test
    public void testConfigCollectionWithOverride() {
        context.registerService(ConfigurationOverrideProvider.class, new DummyConfigurationOverrideProvider(
                "[/content]sampleList/stringParam='override1'"));

        context.build().resource("/conf/content/site1/sling:configs/sampleList")
            .siblingsMode()
            .resource("1", "stringParam", "configValue1.1")
            .resource("2", "stringParam", "configValue1.2")
            .resource("3", "stringParam", "configValue1.3");

        Collection<ValueMap> propsList = underTest.get(site1Page1).name("sampleList").asValueMapCollection();

        Iterator<ValueMap> propsIterator = propsList.iterator();
        assertEquals("override1", propsIterator.next().get("stringParam", String.class));
        assertEquals("override1", propsIterator.next().get("stringParam", String.class));
        assertEquals("override1", propsIterator.next().get("stringParam", String.class));
    }

    @Test
    public void testNonExistingContentResource() {
        ValueMap props = underTest.get(null).name("sampleName").asValueMap();

        assertNull(props.get("stringParam", String.class));
        assertEquals(0, (int)props.get("intParam", 0));
        assertEquals(false, props.get("boolParam", false));
    }

    @Test
    public void testNonExistingContentResourceCollection() {
        Collection<ValueMap> propsList = underTest.get(null).name("sampleList").asValueMapCollection();
        assertTrue(propsList.isEmpty());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNullConfigName() {
        underTest.get(site1Page1).name(null).asValueMap();
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidConfigName() {
        underTest.get(site1Page1).name("/a/b/c").asValueMap();
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidConfigName2() {
        underTest.get(site1Page1).name("../a/b/c").asValueMap();
    }

    @Test(expected=ConfigurationResolveException.class)
    public void testWithoutConfigName() {
        underTest.get(site1Page1).asValueMap();
    }

}
