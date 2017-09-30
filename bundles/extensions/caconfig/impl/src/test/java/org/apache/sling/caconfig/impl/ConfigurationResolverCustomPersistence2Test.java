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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.ConfigurationResolveException;
import org.apache.sling.caconfig.ConfigurationResolver;
import org.apache.sling.caconfig.example.ListConfig;
import org.apache.sling.caconfig.example.ListDoubleNestedConfig;
import org.apache.sling.caconfig.example.ListNestedConfig;
import org.apache.sling.caconfig.example.NestedConfig;
import org.apache.sling.caconfig.example.SimpleConfig;
import org.apache.sling.caconfig.management.impl.CustomConfigurationPersistenceStrategy2;
import org.apache.sling.caconfig.spi.ConfigurationPersistenceStrategy2;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Constants;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link ConfigurationResolver} with annotation classes for reading the config.
 */
public class ConfigurationResolverCustomPersistence2Test {

    @Rule
    public SlingContext context = new SlingContext();

    private ConfigurationResolver underTest;

    private Resource site1Page1;

    @Before
    public void setUp() {
        // custom config with defines alternative bucket name "settings"
        underTest = ConfigurationTestUtils.registerConfigurationResolver(context,
                "configBucketNames", "settings");

        // custom strategy which redirects all config resources to a jcr:content subnode
        context.registerService(ConfigurationPersistenceStrategy2.class,
                new CustomConfigurationPersistenceStrategy2(), Constants.SERVICE_RANKING, 2000);
        
        // content resources
        context.build().resource("/content/site1", PROPERTY_CONFIG_REF, "/conf/content/site1");
        site1Page1 = context.create().resource("/content/site1/page1");
    }

    @Test
    public void testNonExistingConfig_Simple() {
        SimpleConfig cfg = underTest.get(site1Page1).as(SimpleConfig.class);

        assertNull(cfg.stringParam());
        assertEquals(5, cfg.intParam());
        assertEquals(false, cfg.boolParam());
    }

    @Test
    public void testNonExistingConfig_List() {
        Collection<ListConfig> cfgList = underTest.get(site1Page1).asCollection(ListConfig.class);
        assertTrue(cfgList.isEmpty());
    }

    @Test
    public void testNonExistingConfig_Nested() {
        NestedConfig cfg = underTest.get(site1Page1).as(NestedConfig.class);

        assertNull(cfg.stringParam());
        assertNotNull(cfg.subConfig());
        assertNotNull(cfg.subListConfig());
    }


    @Test
    public void testConfig_Simple() {
        context.build().resource("/conf/content/site1/settings/org.apache.sling.caconfig.example.SimpleConfig/jcr:content",
                "stringParam", "configValue1",
                "intParam", 111,
                "boolParam", true);

        SimpleConfig cfg = underTest.get(site1Page1).as(SimpleConfig.class);

        assertEquals("configValue1", cfg.stringParam());
        assertEquals(111, cfg.intParam());
        assertEquals(true, cfg.boolParam());
    }

    @Test
    public void testConfig_SimpleWithName() {
        context.build().resource("/conf/content/site1/settings/sampleName/jcr:content",
                "stringParam", "configValue1.1",
                "intParam", 1111,
                "boolParam", true);

        SimpleConfig cfg = underTest.get(site1Page1).name("sampleName").as(SimpleConfig.class);

        assertEquals("configValue1.1", cfg.stringParam());
        assertEquals(1111, cfg.intParam());
        assertEquals(true, cfg.boolParam());
    }

    @Test
    public void testConfig_List() {
        context.build().resource("/conf/content/site1/settings/org.apache.sling.caconfig.example.ListConfig/jcr:content")
            .siblingsMode()
            .resource("1", "stringParam", "value1")
            .resource("2", "stringParam", "value2")
            .resource("3", "stringParam", "value3");

        Collection<ListConfig> cfgList = underTest.get(site1Page1).asCollection(ListConfig.class);

        assertEquals(3, cfgList.size());
        Iterator<ListConfig> cfgIterator = cfgList.iterator();
        assertEquals("value1", cfgIterator.next().stringParam());
        assertEquals("value2", cfgIterator.next().stringParam());
        assertEquals("value3", cfgIterator.next().stringParam());
    }

    @Test
    public void testConfig_List_Nested() {
        context.build().resource("/conf/content/site1/sling:configs/org.apache.sling.caconfig.example.ListNestedConfig/jcr:content")
            .siblingsMode()
            .resource("1", "stringParam", "value1")
            .resource("2", "stringParam", "value2")
            .resource("3", "stringParam", "value3");
        context.build().resource("/conf/content/site1/sling:configs/org.apache.sling.caconfig.example.ListNestedConfig/jcr:content/1/subListConfig")
            .siblingsMode()
            .resource("1", "stringParam", "value11")
            .resource("2", "stringParam", "value12");
        context.build().resource("/conf/content/site1/sling:configs/org.apache.sling.caconfig.example.ListNestedConfig/jcr:content/2/subListConfig")
            .siblingsMode()
            .resource("1", "stringParam", "value21");

        List<ListNestedConfig> cfgList = ImmutableList.copyOf(underTest.get(site1Page1).asCollection(ListNestedConfig.class));

        assertEquals(3, cfgList.size());
        
        ListNestedConfig config1 = cfgList.get(0);
        assertEquals("value1", config1.stringParam());
        assertEquals(2, config1.subListConfig().length);
        assertEquals("value11", config1.subListConfig()[0].stringParam());
        assertEquals("value12", config1.subListConfig()[1].stringParam());
        
        ListNestedConfig config2 = cfgList.get(1);
        assertEquals("value2", config2.stringParam());
        assertEquals(1, config2.subListConfig().length);
        assertEquals("value21", config2.subListConfig()[0].stringParam());

        ListNestedConfig config3 = cfgList.get(2);
        assertEquals("value3", config3.stringParam());
        assertEquals(0, config3.subListConfig().length);
    }

    @Test
    public void testConfig_List_DoubleNested() {
        context.build().resource("/conf/content/site1/sling:configs/org.apache.sling.caconfig.example.ListDoubleNestedConfig/jcr:content")
            .siblingsMode()
            .resource("1", "stringParam", "value1")
            .resource("2", "stringParam", "value2")
            .resource("3", "stringParam", "value3");
        context.build().resource("/conf/content/site1/sling:configs/org.apache.sling.caconfig.example.ListDoubleNestedConfig/jcr:content/1/subListNestedConfig")
            .siblingsMode()
            .resource("1", "stringParam", "value11")
            .resource("2", "stringParam", "value12");
        context.build().resource("/conf/content/site1/sling:configs/org.apache.sling.caconfig.example.ListDoubleNestedConfig/jcr:content/1/subListNestedConfig/1/subListConfig")
            .siblingsMode()
            .resource("1", "stringParam", "value111")
            .resource("2", "stringParam", "value112");
        context.build().resource("/conf/content/site1/sling:configs/org.apache.sling.caconfig.example.ListDoubleNestedConfig/jcr:content/1/subListNestedConfig/2/subListConfig")
            .siblingsMode()
            .resource("1", "stringParam", "value121");
        context.build().resource("/conf/content/site1/sling:configs/org.apache.sling.caconfig.example.ListDoubleNestedConfig/jcr:content/2/subListNestedConfig")
            .siblingsMode()
            .resource("1", "stringParam", "value21");

        List<ListDoubleNestedConfig> cfgList = ImmutableList.copyOf(underTest.get(site1Page1).asCollection(ListDoubleNestedConfig.class));

        assertEquals(3, cfgList.size());
        
        ListDoubleNestedConfig config1 = cfgList.get(0);
        assertEquals("value1", config1.stringParam());
        assertEquals(2, config1.subListNestedConfig().length);
        assertEquals("value11", config1.subListNestedConfig()[0].stringParam());
        assertEquals(2, config1.subListNestedConfig()[0].subListConfig().length);
        assertEquals("value111", config1.subListNestedConfig()[0].subListConfig()[0].stringParam());
        assertEquals("value112", config1.subListNestedConfig()[0].subListConfig()[1].stringParam());
        assertEquals("value12", config1.subListNestedConfig()[1].stringParam());
        assertEquals(1, config1.subListNestedConfig()[1].subListConfig().length);
        assertEquals("value121", config1.subListNestedConfig()[1].subListConfig()[0].stringParam());
        
        ListDoubleNestedConfig config2 = cfgList.get(1);
        assertEquals("value2", config2.stringParam());
        assertEquals(1, config2.subListNestedConfig().length);
        assertEquals("value21", config2.subListNestedConfig()[0].stringParam());
        assertEquals(0, config2.subListNestedConfig()[0].subListConfig().length);

        ListDoubleNestedConfig config3 = cfgList.get(2);
        assertEquals("value3", config3.stringParam());
        assertEquals(0, config3.subListNestedConfig().length);
    }

    @Test
    public void testConfig_Nested() {
        context.build().resource("/conf/content/site1/settings/org.apache.sling.caconfig.example.NestedConfig")
            .resource("jcr:content", "stringParam", "configValue3")
                .siblingsMode()
                .resource("subConfig", "stringParam", "configValue4", "intParam", 444, "boolParam", true)
                .hierarchyMode()
                .resource("subListConfig")
                    .siblingsMode()
                    .resource("1", "stringParam", "configValue2.1")
                    .resource("2", "stringParam", "configValue2.2")
                    .resource("3", "stringParam", "configValue2.3");

        NestedConfig cfg = underTest.get(site1Page1).as(NestedConfig.class);

        assertEquals("configValue3", cfg.stringParam());
        
        SimpleConfig subConfig = cfg.subConfig();
        assertEquals("configValue4", subConfig.stringParam());
        assertEquals(444, subConfig.intParam());
        assertEquals(true, subConfig.boolParam());
        
        ListConfig[] listConfig = cfg.subListConfig();
        assertEquals(3, listConfig.length);
        assertEquals("configValue2.1", listConfig[0].stringParam());
        assertEquals("configValue2.2", listConfig[1].stringParam());
        assertEquals("configValue2.3", listConfig[2].stringParam());
    }

    @Test(expected=ConfigurationResolveException.class)
    public void testInvalidClassConversion() {
        // test with class not supported for configuration mapping
        underTest.get(site1Page1).as(Rectangle2D.class);
    }

    @Test
    public void testNonExistingContentResource_Simple() {
        SimpleConfig cfg = underTest.get(null).as(SimpleConfig.class);

        assertNull(cfg.stringParam());
        assertEquals(5, cfg.intParam());
        assertEquals(false, cfg.boolParam());
    }

    @Test
    public void testNonExistingContentResource_List() {
        Collection<ListConfig> cfgList = underTest.get(null).asCollection(ListConfig.class);
        assertTrue(cfgList.isEmpty());
    }

}
