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

import static org.apache.sling.caconfig.impl.def.ConfigurationDefNameConstants.PROPERTY_CONFIG_PROPERTY_INHERIT;
import static org.apache.sling.caconfig.resource.impl.def.ConfigurationResourceNameConstants.PROPERTY_CONFIG_COLLECTION_INHERIT;
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
import org.apache.sling.caconfig.example.NestedConfig;
import org.apache.sling.caconfig.example.SimpleConfig;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link ConfigurationResolver} with annotation classes for reading the config.
 */
public class ConfigurationResolverAnnotationClassTest {

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
        context.build().resource("/conf/content/site1/sling:configs/org.apache.sling.caconfig.example.SimpleConfig",
                "stringParam", "configValue1",
                "intParam", 111,
                "boolParam", true);

        SimpleConfig cfg = underTest.get(site1Page1).as(SimpleConfig.class);

        assertEquals("configValue1", cfg.stringParam());
        assertEquals(111, cfg.intParam());
        assertEquals(true, cfg.boolParam());
    }

    @Test
    public void testConfig_Simple_PropertyInheritance() {
        context.build()
            .resource("/conf/global/sling:configs/org.apache.sling.caconfig.example.SimpleConfig",
                "stringParam", "configValue1",
                "intParam", 111)
            .resource("/conf/content/site1/sling:configs/org.apache.sling.caconfig.example.SimpleConfig",
                "stringParam", "configValue2",
                "intParam", 222,
                "boolParam", true,
                PROPERTY_CONFIG_PROPERTY_INHERIT, true);

        SimpleConfig cfg = underTest.get(site1Page1).as(SimpleConfig.class);

        assertEquals("configValue2", cfg.stringParam());
        assertEquals(222, cfg.intParam());
        assertEquals(true, cfg.boolParam());
    }

    @Test
    public void testConfig_SimpleWithName() {
        context.build().resource("/conf/content/site1/sling:configs/sampleName",
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
        context.build().resource("/conf/content/site1/sling:configs/org.apache.sling.caconfig.example.ListConfig")
            .siblingsMode()
            .resource("1", "stringParam", "configValue1.1")
            .resource("2", "stringParam", "configValue1.2")
            .resource("3", "stringParam", "configValue1.3");

        Collection<ListConfig> cfgList = underTest.get(site1Page1).asCollection(ListConfig.class);

        assertEquals(3, cfgList.size());
        Iterator<ListConfig> cfgIterator = cfgList.iterator();
        assertEquals("configValue1.1", cfgIterator.next().stringParam());
        assertEquals("configValue1.2", cfgIterator.next().stringParam());
        assertEquals("configValue1.3", cfgIterator.next().stringParam());
    }

    @Test
    public void testConfig_List_CollectionPropertyInheritance() {
        context.build()
            .resource("/conf/global/sling:configs/org.apache.sling.caconfig.example.ListConfig")
                .siblingsMode()
                .resource("1", "stringParam", "configValue1.1", "intParam", "111")
                .resource("2", "stringParam", "configValue1.2", "intParam", "222")
            .resource("/conf/content/site1/sling:configs/org.apache.sling.caconfig.example.ListConfig",                    
                    PROPERTY_CONFIG_COLLECTION_INHERIT, true)
                .siblingsMode()
                .resource("2", "stringParam", "configValue2.2", PROPERTY_CONFIG_PROPERTY_INHERIT, true)
                .resource("3", "stringParam", "configValue2.3", "intParam", "333", PROPERTY_CONFIG_PROPERTY_INHERIT, true);

        List<ListConfig> cfgList = ImmutableList.copyOf(underTest.get(site1Page1).asCollection(ListConfig.class));

        assertEquals(3, cfgList.size());
        assertEquals("configValue2.2", cfgList.get(0).stringParam());
        assertEquals(222, cfgList.get(0).intParam());
        assertEquals("configValue2.3", cfgList.get(1).stringParam());
        assertEquals(333, cfgList.get(1).intParam());
        assertEquals("configValue1.1", cfgList.get(2).stringParam());
        assertEquals(111, cfgList.get(2).intParam());
    }

    @Test
    public void testConfig_Nested() {
        context.build().resource("/conf/content/site1/sling:configs/org.apache.sling.caconfig.example.NestedConfig",
                "stringParam", "configValue3")
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

    @Test
    public void testConfig_Nested_PropertyInheritance() {
        context.build()
            .resource("/conf/global/sling:configs/org.apache.sling.caconfig.example.NestedConfig")
                .resource("subConfig", "stringParam", "configValue1", "intParam", 111, "boolParam", true)
            .resource("/conf/content/site1/sling:configs/org.apache.sling.caconfig.example.NestedConfig",
                    "stringParam", "configValue3")
                .resource("subConfig", "stringParam", "configValue4", PROPERTY_CONFIG_PROPERTY_INHERIT, true);

        NestedConfig cfg = underTest.get(site1Page1).as(NestedConfig.class);

        assertEquals("configValue3", cfg.stringParam());
        
        SimpleConfig subConfig = cfg.subConfig();
        assertEquals("configValue4", subConfig.stringParam());
        assertEquals(111, subConfig.intParam());
        assertEquals(true, subConfig.boolParam());
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
