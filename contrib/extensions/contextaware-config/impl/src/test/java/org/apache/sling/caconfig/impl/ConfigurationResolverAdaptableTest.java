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
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.caconfig.ConfigurationResolveException;
import org.apache.sling.caconfig.ConfigurationResolver;
import org.apache.sling.caconfig.example.SimpleSlingModel;
import org.apache.sling.caconfig.spi.ConfigurationMetadataProvider;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Test {@link ConfigurationResolver} with custom adaptions (in this case: Sling Models) for reading the config.
 */
public class ConfigurationResolverAdaptableTest {

    @Rule
    public SlingContext context = new SlingContext();

    private ConfigurationResolver underTest;

    private Resource site1Page1;

    @Before
    public void setUp() {
        underTest = ConfigurationTestUtils.registerConfigurationResolver(context);

        context.addModelsForPackage("org.apache.sling.caconfig.example");
        
        // content resources
        context.build().resource("/content/site1", PROPERTY_CONFIG_REF, "/conf/content/site1");
        site1Page1 = context.create().resource("/content/site1/page1");
    }

    @Test
    public void testNonExistingConfig() {
        SimpleSlingModel model = underTest.get(site1Page1).name("sampleName").asAdaptable(SimpleSlingModel.class);
        assertNull(model);
    }

    @Test
    public void testNonExistingConfigCollection() {
        Collection<SimpleSlingModel> propsList = underTest.get(site1Page1).name("sampleList").asAdaptableCollection(SimpleSlingModel.class);
        assertTrue(propsList.isEmpty());
    }

    @Test
    public void testConfig() {
        context.build().resource("/conf/content/site1/sling:configs/sampleName",
                "stringParam", "configValue1",
                "intParam", 111,
                "boolParam", true);

        SimpleSlingModel model = underTest.get(site1Page1).name("sampleName").asAdaptable(SimpleSlingModel.class);
        assertEquals("configValue1", model.getStringParam());
        assertEquals(111, model.getIntParam());
        assertEquals(true, model.getBoolParam());
    }

    @Test
    public void testConfigCollection() {
        context.build().resource("/conf/content/site1/sling:configs/sampleList")
            .siblingsMode()
            .resource("1", "stringParam", "configValue1.1")
            .resource("2", "stringParam", "configValue1.2")
            .resource("3", "stringParam", "configValue1.3");

        Collection<SimpleSlingModel> propsList = underTest.get(site1Page1).name("sampleList").asAdaptableCollection(SimpleSlingModel.class);

        Iterator<SimpleSlingModel> propsIterator = propsList.iterator();
        assertEquals("configValue1.1", propsIterator.next().getStringParam());
        assertEquals("configValue1.2", propsIterator.next().getStringParam());
        assertEquals("configValue1.3", propsIterator.next().getStringParam());
    }

    @Test
    public void testConfigWithDefaultValues() {
        context.registerService(ConfigurationMetadataProvider.class, new DummyConfigurationMetadataProvider("sampleName", 
                ImmutableMap.<String, Object>of("stringParam", "defValue1", "intParam", 999), false));

        context.build().resource("/conf/content/site1/sling:configs/sampleName",
                "boolParam", true);

        SimpleSlingModel model = underTest.get(site1Page1).name("sampleName").asAdaptable(SimpleSlingModel.class);
        assertEquals("defValue1", model.getStringParam());
        assertEquals(999, model.getIntParam());
        assertEquals(true, model.getBoolParam());
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

        List<SimpleSlingModel> propsList = ImmutableList.copyOf(underTest.get(site1Page1).name("sampleList").asAdaptableCollection(SimpleSlingModel.class));

        assertEquals("configValue1.1", propsList.get(0).getStringParam());
        assertEquals(999, propsList.get(0).getIntParam());
        assertEquals("configValue1.2", propsList.get(1).getStringParam());
        assertEquals(999, propsList.get(1).getIntParam());
        assertEquals("configValue1.3", propsList.get(2).getStringParam());
        assertEquals(999, propsList.get(2).getIntParam());
    }

    @Test
    public void testNonExistingContentResource() {
        SimpleSlingModel model = underTest.get(null).name("sampleName").asAdaptable(SimpleSlingModel.class);
        assertNull(model);
    }

    @Test
    public void testNonExistingContentResourceCollection() {
        Collection<SimpleSlingModel> propsList = underTest.get(null).name("sampleList").asAdaptableCollection(SimpleSlingModel.class);
        assertTrue(propsList.isEmpty());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNullConfigName() {
        underTest.get(site1Page1).name(null).asAdaptable(SimpleSlingModel.class);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidConfigName() {
        underTest.get(site1Page1).name("/a/b/c").asAdaptable(SimpleSlingModel.class);
    }

    @Test(expected=ConfigurationResolveException.class)
    public void testWithoutConfigName() {
        underTest.get(site1Page1).asAdaptable(SimpleSlingModel.class);
    }

    @Test
    public void testAdaptToConfigurationBuilder() {
        context.build().resource("/conf/content/site1/sling:configs/sampleName");

        // make sure not endless loop occurs
        ConfigurationBuilder model = underTest.get(site1Page1).name("sampleName").asAdaptable(ConfigurationBuilder.class);
        assertNull(model);
    }

}
