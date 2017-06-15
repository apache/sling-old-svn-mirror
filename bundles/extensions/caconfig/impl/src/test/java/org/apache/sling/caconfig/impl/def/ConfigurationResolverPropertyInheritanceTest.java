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
package org.apache.sling.caconfig.impl.def;

import static org.apache.sling.caconfig.impl.def.ConfigurationDefNameConstants.PROPERTY_CONFIG_PROPERTY_INHERIT;
import static org.apache.sling.caconfig.resource.impl.def.ConfigurationResourceNameConstants.PROPERTY_CONFIG_REF;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.ConfigurationResolver;
import org.apache.sling.caconfig.impl.ConfigurationTestUtils;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test {@link ConfigurationResolver} with property inheritance and merging.
 */
public class ConfigurationResolverPropertyInheritanceTest {

    @Rule
    public SlingContext context = new SlingContext();

    private ConfigurationResolver underTest;

    private Resource site1Page1;
    private Resource site2Page1;

    @Before
    public void setUp() {
        underTest = ConfigurationTestUtils.registerConfigurationResolver(context);

        // content resources that form a deeper hierarchy
        context.build()
            .resource("/content/tenant1", PROPERTY_CONFIG_REF, "/conf/brand1/tenant1")
            .resource("/content/tenant1/region1", PROPERTY_CONFIG_REF, "/conf/brand1/tenant1/region1")
            .resource("/content/tenant1/region1/site1", PROPERTY_CONFIG_REF, "/conf/brand1/tenant1/region1/site1")
            .resource("/content/tenant1/region1/site2", PROPERTY_CONFIG_REF, "/conf/brand1/tenant1/region1/site2");
        site1Page1 = context.create().resource("/content/tenant1/region1/site1/page1");
        site2Page1 = context.create().resource("/content/tenant1/region1/site2/page1");
    }

    @Test
    public void testInheritanceWithoutMerging() {
        context.build()
            .resource("/conf/global/sling:configs/test", "param1", "value1", "param2", "value2")
            .resource("/conf/brand1/tenant1/region1/site1/sling:configs/test", "param1", "value1a")
            .resource("/conf/brand1/tenant1/region1/site2/sling:configs/test", "param1", "value1b");
        
        assertThat(underTest.get(site1Page1).name("test").asValueMap(), allOf(
                        hasEntry("param1", (Object)"value1a"),
                        not(hasKey("param2"))));
        assertThat(underTest.get(site2Page1).name("test").asValueMap(), allOf(
                        hasEntry("param1", (Object)"value1b"),
                        not(hasKey("param2"))));
    }

    @Test
    public void testInheritanceMerging() {
        context.build()
            .resource("/conf/global/sling:configs/test", "param1", "value1", "param2", "value2")
            .resource("/conf/brand1/tenant1/region1/site1/sling:configs/test", "param1", "value1a",
                    PROPERTY_CONFIG_PROPERTY_INHERIT, true)
            .resource("/conf/brand1/tenant1/region1/site2/sling:configs/test", "param1", "value1b");
        
        assertThat(underTest.get(site1Page1).name("test").asValueMap(), allOf(
                hasEntry("param1", (Object)"value1a"),
                hasEntry("param2", (Object)"value2")));        
        assertThat(underTest.get(site2Page1).name("test").asValueMap(), allOf(
                hasEntry("param1", (Object)"value1b"),
                not(hasKey("param2"))));
    }

    @Test
    public void testInheritanceMergingMultipleLevels() {
        context.build()
            .resource("/conf/global/sling:configs/test", "param1", "value1", "param4", "value4")
            .resource("/conf/brand1/tenant1/sling:configs/test", "param1", "value1a", "param3", "value3",
                    PROPERTY_CONFIG_PROPERTY_INHERIT, true)
            .resource("/conf/brand1/tenant1/region1/sling:configs/test", "param1", "value1b", "param2", "value2",
                    PROPERTY_CONFIG_PROPERTY_INHERIT, true)
            .resource("/conf/brand1/tenant1/region1/site1/sling:configs/test", "param1", "value1c",
                    PROPERTY_CONFIG_PROPERTY_INHERIT, true)
            .resource("/conf/brand1/tenant1/region1/site2/sling:configs/test", "param1", "value1d");
        
        assertThat(underTest.get(site1Page1).name("test").asValueMap(), allOf(
                hasEntry("param1", (Object)"value1c"),
                hasEntry("param2", (Object)"value2"),
                hasEntry("param3", (Object)"value3"),
                hasEntry("param4", (Object)"value4")));        
        assertThat(underTest.get(site2Page1).name("test").asValueMap(), allOf(
                hasEntry("param1", (Object)"value1d"),
                not(hasKey("param2")),
                not(hasKey("param3")),
                not(hasKey("param4"))));
    }

    /**
     * Setting merging property only on parent has not effect => no inheritance.
     */
    @Test
    public void testInheritanceMergingOnParent() {
        context.build()
            .resource("/conf/global/sling:configs/test", "param1", "value1", "param2", "value2",
                    PROPERTY_CONFIG_PROPERTY_INHERIT, true)
            .resource("/conf/brand1/tenant1/region1/site1/sling:configs/test", "param1", "value1a")
            .resource("/conf/brand1/tenant1/region1/site2/sling:configs/test", "param1", "value1b");
        
        assertThat(underTest.get(site1Page1).name("test").asValueMap(), allOf(
                hasEntry("param1", (Object)"value1a"),
                not(hasKey("param2"))));        
        assertThat(underTest.get(site2Page1).name("test").asValueMap(), allOf(
                hasEntry("param1", (Object)"value1b"),
                not(hasKey("param2"))));
    }

    @Test
    public void testCollectionInheritanceWithoutMerging() {
        context.build()
            .resource("/conf/global/sling:configs/test")
                .resource("item1", "param1", "value1", "param2", "value2")
            .resource("/conf/brand1/tenant1/region1/site1/sling:configs/test")
                .resource("item1", "param1", "value1a")
            .resource("/conf/brand1/tenant1/region1/site2/sling:configs/test")
                .resource("item1", "param1", "value1b");
        
        assertThat(underTest.get(site1Page1).name("test").asValueMapCollection().iterator().next(), allOf(
                hasEntry("param1", (Object)"value1a"),
                not(hasKey("param2"))));
        assertThat(underTest.get(site2Page1).name("test").asValueMapCollection().iterator().next(), allOf(
                hasEntry("param1", (Object)"value1b"),
                not(hasKey("param2"))));
    }

    @Test
    public void testCollectionInheritanceMerging() {
        context.build()
            .resource("/conf/global/sling:configs/test")
                .resource("item1", "param1", "value1", "param2", "value2")
            .resource("/conf/brand1/tenant1/region1/site1/sling:configs/test")
                .resource("item1", "param1", "value1a", PROPERTY_CONFIG_PROPERTY_INHERIT, true)
            .resource("/conf/brand1/tenant1/region1/site2/sling:configs/test")
                .resource("item1", "param1", "value1b");
        
        assertThat(underTest.get(site1Page1).name("test").asValueMapCollection().iterator().next(), allOf(
                hasEntry("param1", (Object)"value1a"),
                hasEntry("param2", (Object)"value2")));        
        assertThat(underTest.get(site2Page1).name("test").asValueMapCollection().iterator().next(), allOf(
                hasEntry("param1", (Object)"value1b"),
                not(hasKey("param2"))));
    }

    @Test
    public void testCollectionInheritanceMergingMultipleLevels() {
        context.build()
            .resource("/conf/global/sling:configs/test")
                .resource("item1", "param1", "value1", "param4", "value4")
            .resource("/conf/brand1/tenant1/sling:configs/test")
                .resource("item1", "param1", "value1a", "param3", "value3", PROPERTY_CONFIG_PROPERTY_INHERIT, true)
            .resource("/conf/brand1/tenant1/region1/sling:configs/test")
                .resource("item1", "param1", "value1b", "param2", "value2", PROPERTY_CONFIG_PROPERTY_INHERIT, true)
            .resource("/conf/brand1/tenant1/region1/site1/sling:configs/test")
                .resource("item1", "param1", "value1c", PROPERTY_CONFIG_PROPERTY_INHERIT, true)
            .resource("/conf/brand1/tenant1/region1/site2/sling:configs/test")
                .resource("item1", "param1", "value1d");
        
        assertThat(underTest.get(site1Page1).name("test").asValueMapCollection().iterator().next(), allOf(
                hasEntry("param1", (Object)"value1c"),
                hasEntry("param2", (Object)"value2"),
                hasEntry("param3", (Object)"value3"),
                hasEntry("param4", (Object)"value4")));        
        assertThat(underTest.get(site2Page1).name("test").asValueMapCollection().iterator().next(), allOf(
                hasEntry("param1", (Object)"value1d"),
                not(hasKey("param2")),
                not(hasKey("param3")),
                not(hasKey("param4"))));
    }

    /**
     * Setting merging property only on parent has not effect => no inheritance.
     */
    @Test
    public void testCollectionInheritanceMergingOnParent() {
        context.build()
            .resource("/conf/global/sling:configs/test", PROPERTY_CONFIG_PROPERTY_INHERIT, true)
                .resource("item1", "param1", "value1", "param2", "value2")
            .resource("/conf/brand1/tenant1/region1/site1/sling:configs/test")
                .resource("item1", "param1", "value1a")
            .resource("/conf/brand1/tenant1/region1/site2/sling:configs/test")
                .resource("item1", "param1", "value1b");
        
        assertThat(underTest.get(site1Page1).name("test").asValueMapCollection().iterator().next(), allOf(
                hasEntry("param1", (Object)"value1a"),
                not(hasKey("param2"))));        
        assertThat(underTest.get(site2Page1).name("test").asValueMapCollection().iterator().next(), allOf(
                hasEntry("param1", (Object)"value1b"),
                not(hasKey("param2"))));
    }

}
