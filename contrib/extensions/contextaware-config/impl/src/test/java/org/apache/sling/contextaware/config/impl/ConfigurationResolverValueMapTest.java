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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.contextaware.config.ConfigurationResolveException;
import org.apache.sling.contextaware.config.ConfigurationResolver;
import org.apache.sling.contextaware.config.resource.impl.ConfigurationResourceResolverImpl;
import org.apache.sling.contextaware.config.resource.impl.ContextPathStrategyMultiplexer;
import org.apache.sling.contextaware.config.resource.impl.DefaultContextPathStrategy;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test {@link ConfigurationResolver} with ValueMap for reading the config.
 */
public class ConfigurationResolverValueMapTest {

    @Rule
    public SlingContext context = new SlingContext();

    private ConfigurationResolver underTest;

    private Resource site1Page1;
    private Resource site2Page1;

    @Before
    public void setUp() {
        context.registerInjectActivateService(new DefaultContextPathStrategy());
        context.registerInjectActivateService(new ContextPathStrategyMultiplexer());
        context.registerInjectActivateService(new ConfigurationResourceResolverImpl());
        underTest = context.registerInjectActivateService(new ConfigurationResolverImpl());

        // config resources
        context.build().resource("/conf/content/site2/sling:configs/sampleName", 
                "stringParam", "configValue1",
                "intParam", 111,
                "boolParam", true);

        context.build().resource("/conf/content/site2/sling:configs/sampleList")
            .siblingsMode()
            .resource("1", "stringParam", "configValue1.1")
            .resource("2", "stringParam", "configValue1.2")
            .resource("3", "stringParam", "configValue1.3");

        // content resources
        context.build().resource("/content/site1", "sling:config-ref", "/conf/content/site1")
            .resource("/content/site2", "sling:config-ref", "/conf/content/site2");
        site1Page1 = context.create().resource("/content/site1/page1");
        site2Page1 = context.create().resource("/content/site2/page1");
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
        ValueMap props = underTest.get(site2Page1).name("sampleName").asValueMap();

        assertEquals("configValue1", props.get("stringParam", String.class));
        assertEquals(111, (int)props.get("intParam", 0));
        assertEquals(true, props.get("boolParam", false));
    }

    @Test
    public void testConfigCollection() {
        Collection<ValueMap> propsList = underTest.get(site2Page1).name("sampleList").asValueMapCollection();

        Iterator<ValueMap> propsIterator = propsList.iterator();
        assertEquals("configValue1.1", propsIterator.next().get("stringParam", String.class));
        assertEquals("configValue1.2", propsIterator.next().get("stringParam", String.class));
        assertEquals("configValue1.3", propsIterator.next().get("stringParam", String.class));
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
        underTest.get(site2Page1).name(null).asValueMap();
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidConfigName() {
        underTest.get(site2Page1).name("/a/b/c").asValueMap();
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidConfigName2() {
        underTest.get(site2Page1).name("../a/b/c").asValueMap();
    }

    @Test(expected=ConfigurationResolveException.class)
    public void testWithoutConfigName() {
        underTest.get(site2Page1).asValueMap();
    }

}
