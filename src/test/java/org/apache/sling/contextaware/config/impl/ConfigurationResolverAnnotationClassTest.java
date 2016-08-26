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
package org.apache.sling.contextaware.config.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.contextaware.config.ConfigurationResolveException;
import org.apache.sling.contextaware.config.ConfigurationResolver;
import org.apache.sling.contextaware.config.example.ListConfig;
import org.apache.sling.contextaware.config.example.NestedConfig;
import org.apache.sling.contextaware.config.example.SimpleConfig;
import org.apache.sling.contextaware.config.resource.impl.ConfigurationResourceResolverImpl;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class ConfigurationResolverAnnotationClassTest {

    @Rule
    public SlingContext context = new SlingContext();

    private ConfigurationResolver underTest;

    private Resource site1Page1;
    private Resource site2Page1;

    @Before
    public void setUp() {
        context.registerInjectActivateService(new ConfigurationResourceResolverImpl());
        underTest = context.registerInjectActivateService(new ConfigurationResolverImpl());

        // config resources
        context.create().resource("/config/content/site2/sampleName", ImmutableMap.<String, Object>builder()
                .put("stringParam", "configValue1")
                .put("intParam", 111)
                .put("boolParam", true)
                .build());

        context.create().resource("/config/content/site2/sling:configs/org.apache.sling.contextaware.config.example.SimpleConfig", ImmutableMap.<String, Object>builder()
                .put("stringParam", "configValue1")
                .put("intParam", 111)
                .put("boolParam", true)
                .build());

        context.create().resource("/config/content/site2/sling:configs/org.apache.sling.contextaware.config.example.NestedConfig", ImmutableMap.<String, Object>builder()
                .put("stringParam", "configValue3")
                .build());
        context.create().resource("/config/content/site2/sling:configs/org.apache.sling.contextaware.config.example.NestedConfig/subConfig", ImmutableMap.<String, Object>builder()
                .put("stringParam", "configValue4")
                .put("intParam", 444)
                .put("boolParam", true)
                .build());
        context.create().resource("/config/content/site2/sling:configs/org.apache.sling.contextaware.config.example.NestedConfig/subListConfig/1", ImmutableMap.<String, Object>builder()
                .put("stringParam", "configValue2.1")
                .build());
        context.create().resource("/config/content/site2/sling:configs/org.apache.sling.contextaware.config.example.NestedConfig/subListConfig/2", ImmutableMap.<String, Object>builder()
                .put("stringParam", "configValue2.2")
                .build());
        context.create().resource("/config/content/site2/sling:configs/org.apache.sling.contextaware.config.example.NestedConfig/subListConfig/3", ImmutableMap.<String, Object>builder()
                .put("stringParam", "configValue2.3")
                .build());

        context.create().resource("/config/content/site2/sling:configs/org.apache.sling.contextaware.config.example.ListConfig/1", ImmutableMap.<String, Object>builder()
                .put("stringParam", "configValue1.1")
                .build());
        context.create().resource("/config/content/site2/sling:configs/org.apache.sling.contextaware.config.example.ListConfig/2", ImmutableMap.<String, Object>builder()
                .put("stringParam", "configValue1.2")
                .build());
        context.create().resource("/config/content/site2/sling:configs/org.apache.sling.contextaware.config.example.ListConfig/3", ImmutableMap.<String, Object>builder()
                .put("stringParam", "configValue1.3")
                .build());

        // content resources
        context.create().resource("/content/site1", ImmutableMap.<String, Object>builder()
                .put("sling:config", "/config/content/site1")
                .build());
        context.create().resource("/content/site2", ImmutableMap.<String, Object>builder()
                .put("sling:config", "/config/content/site2")
                .build());
        site1Page1 = context.create().resource("/content/site1/page1");
        site2Page1 = context.create().resource("/content/site2/page1");
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
        SimpleConfig cfg = underTest.get(site2Page1).as(SimpleConfig.class);

        assertEquals("configValue1", cfg.stringParam());
        assertEquals(111, cfg.intParam());
        assertEquals(true, cfg.boolParam());
    }

    @Test
    public void testConfig_List() {
        Collection<ListConfig> cfgList = underTest.get(site2Page1).asCollection(ListConfig.class);

        assertEquals(3, cfgList.size());
        Iterator<ListConfig> cfgIterator = cfgList.iterator();
        assertEquals("configValue1.1", cfgIterator.next().stringParam());
        assertEquals("configValue1.2", cfgIterator.next().stringParam());
        assertEquals("configValue1.3", cfgIterator.next().stringParam());
    }

    @Test
    public void testConfig_Nested() {
        NestedConfig cfg = underTest.get(site2Page1).as(NestedConfig.class);

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
        underTest.get(site2Page1).as(Rectangle2D.class);
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
