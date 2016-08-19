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

import static org.apache.sling.contextaware.config.example.AllTypesDefaults.BOOL_DEFAULT;
import static org.apache.sling.contextaware.config.example.AllTypesDefaults.BOOL_DEFAULT_2;
import static org.apache.sling.contextaware.config.example.AllTypesDefaults.DOUBLE_DEFAULT;
import static org.apache.sling.contextaware.config.example.AllTypesDefaults.DOUBLE_DEFAULT_2;
import static org.apache.sling.contextaware.config.example.AllTypesDefaults.INT_DEFAULT;
import static org.apache.sling.contextaware.config.example.AllTypesDefaults.INT_DEFAULT_2;
import static org.apache.sling.contextaware.config.example.AllTypesDefaults.LONG_DEFAULT;
import static org.apache.sling.contextaware.config.example.AllTypesDefaults.LONG_DEFAULT_2;
import static org.apache.sling.contextaware.config.example.AllTypesDefaults.STRING_DEFAULT;
import static org.apache.sling.contextaware.config.example.AllTypesDefaults.STRING_DEFAULT_2;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.contextaware.config.ConfigurationResolver;
import org.apache.sling.contextaware.config.example.AllTypesConfig;
import org.apache.sling.contextaware.config.example.ListConfig;
import org.apache.sling.contextaware.config.example.NestedConfig;
import org.apache.sling.contextaware.config.example.SimpleConfig;
import org.apache.sling.contextaware.config.resource.impl.ConfigurationResourceResolverImpl;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.service.converter.ConversionException;
import org.osgi.service.converter.Converter;
import org.osgi.service.converter.util.ConverterFactory;

import com.google.common.collect.ImmutableMap;

public class ConfigurationResolverImplTest {

    @Rule
    public SlingContext context = new SlingContext();

    private ConfigurationResolver underTest;

    private Resource site1Page1;
    private Resource site2Page1;

    @Before
    public void setUp() {
        context.registerInjectActivateService(new ConfigurationResourceResolverImpl());
        context.registerService(Converter.class, ConverterFactory.standardConverter());
        underTest = context.registerInjectActivateService(new ConfigurationResolverImpl());

        // config resources
        context.create().resource("/config/content/site2/sling:configs/org.apache.sling.contextaware.config.example.SimpleConfig", ImmutableMap.<String, Object>builder()
                .put("stringParam", "configValue1")
                .put("intParam", 111)
                .put("boolParam", true)
                .build());
        context.create().resource("/config/content/site2/sling:configs/org.apache.sling.contextaware.config.example.AllTypesConfig", ImmutableMap.<String, Object>builder()
                .put("stringParam", "configValue2")
                .put("intParam", 222)
                .put("longParam", 3456L)
                .put("doubleParam", 0.123d)
                .put("boolParam", true)
                .put("stringArrayParam", new String[] {STRING_DEFAULT_2,STRING_DEFAULT})
                .put("intArrayParam", new int[] {INT_DEFAULT_2})
                .put("longArrayParam", new long[] {LONG_DEFAULT_2,LONG_DEFAULT})
                .put("doubleArrayParam", new double[] {DOUBLE_DEFAULT_2})
                .put("boolArrayParam", new boolean[] {BOOL_DEFAULT_2,BOOL_DEFAULT})
                .build());

        context.create().resource("/config/content/site2/sling:configs/org.apache.sling.contextaware.config.example.NestedConfig", ImmutableMap.<String, Object>builder()
                .put("stringParam", "configValue3")
                .build());
        context.create().resource("/config/content/site2/sling:configs/org.apache.sling.contextaware.config.example.NestedConfig/subConfig", ImmutableMap.<String, Object>builder()
                .put("stringParam", "configValue4")
                .put("intParam", 444)
                .put("boolParam", true)
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
        assertEquals(0, cfg.intParam());
        assertEquals(false, cfg.boolParam());
    }

    @Test
    public void testNonExistingConfig_AllTypes() {
        AllTypesConfig cfg = underTest.get(site1Page1).as(AllTypesConfig.class);

        assertNull(cfg.stringParam());
        assertEquals(STRING_DEFAULT, cfg.stringParamWithDefault());
        assertEquals(0, cfg.intParam());
        assertEquals(INT_DEFAULT, cfg.intParamWithDefault());
        assertEquals(0L, cfg.longParam());
        assertEquals(LONG_DEFAULT, cfg.longParamWithDefault());
        assertEquals(0d, cfg.doubleParam(), 0.001d);
        assertEquals(DOUBLE_DEFAULT, cfg.doubleParamWithDefault(), 0.001d);
        assertEquals(false, cfg.boolParam());
        assertEquals(BOOL_DEFAULT, cfg.boolParamWithDefault());

        assertNull(cfg.stringArrayParam());
        assertArrayEquals(new String[] { STRING_DEFAULT, STRING_DEFAULT_2 }, cfg.stringArrayParamWithDefault());
        assertNull(cfg.intArrayParam());
        assertArrayEquals(new int[] { INT_DEFAULT, INT_DEFAULT_2 }, cfg.intArrayParamWithDefault());
        assertNull(cfg.longArrayParam());
        assertArrayEquals(new long[] { LONG_DEFAULT, LONG_DEFAULT_2 }, cfg.longArrayParamWithDefault());
        assertNull(cfg.doubleArrayParam());
        assertArrayEquals(new double[] { DOUBLE_DEFAULT, DOUBLE_DEFAULT_2 }, cfg.doubleArrayParamWithDefault(), 0.001d);
        assertNull(cfg.boolArrayParam());
        assertArrayEquals(new boolean[] { BOOL_DEFAULT, BOOL_DEFAULT_2 }, cfg.boolArrayParamWithDefault());
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
        assertNull(cfg.subConfig());
        assertNull(cfg.subListConfig());
    }


    @Test
    public void testConfig_Simple() {
        SimpleConfig cfg = underTest.get(site2Page1).as(SimpleConfig.class);

        assertEquals("configValue1", cfg.stringParam());
        assertEquals(111, cfg.intParam());
        assertEquals(true, cfg.boolParam());
    }

    @Test
    public void testConfig_AllTypes() {
        AllTypesConfig cfg = underTest.get(site2Page1).as(AllTypesConfig.class);

        assertEquals("configValue2", cfg.stringParam());
        assertEquals(STRING_DEFAULT, cfg.stringParamWithDefault());
        assertEquals(222, cfg.intParam());
        assertEquals(INT_DEFAULT, cfg.intParamWithDefault());
        assertEquals(3456L, cfg.longParam());
        assertEquals(LONG_DEFAULT, cfg.longParamWithDefault());
        assertEquals(0.123d, cfg.doubleParam(), 0.001d);
        assertEquals(DOUBLE_DEFAULT, cfg.doubleParamWithDefault(), 0.001d);
        assertEquals(true, cfg.boolParam());
        assertEquals(BOOL_DEFAULT, cfg.boolParamWithDefault());

        assertArrayEquals(new String[] {STRING_DEFAULT_2,STRING_DEFAULT}, cfg.stringArrayParam());
        assertArrayEquals(new String[] { STRING_DEFAULT, STRING_DEFAULT_2 }, cfg.stringArrayParamWithDefault());
        assertArrayEquals(new int[] {INT_DEFAULT_2}, cfg.intArrayParam());
        assertArrayEquals(new int[] { INT_DEFAULT, INT_DEFAULT_2 }, cfg.intArrayParamWithDefault());
        assertArrayEquals(new long[] {LONG_DEFAULT_2,LONG_DEFAULT}, cfg.longArrayParam());
        assertArrayEquals(new long[] { LONG_DEFAULT, LONG_DEFAULT_2 }, cfg.longArrayParamWithDefault());
        assertArrayEquals(new double[] {DOUBLE_DEFAULT_2}, cfg.doubleArrayParam(), 0.001d);
        assertArrayEquals(new double[] { DOUBLE_DEFAULT, DOUBLE_DEFAULT_2 }, cfg.doubleArrayParamWithDefault(), 0.001d);
        assertArrayEquals(new boolean[] {BOOL_DEFAULT_2,BOOL_DEFAULT}, cfg.boolArrayParam());
        assertArrayEquals(new boolean[] { BOOL_DEFAULT, BOOL_DEFAULT_2 }, cfg.boolArrayParamWithDefault());
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

        // FIXME: nested configurations do not work currently
    }

    @Test(expected=ConversionException.class)
    public void testInvalidClassConversion() {
        // osgi converter cannot convert map to Rectangle2D class
        underTest.get(site2Page1).as(Rectangle2D.class);
    }

}
