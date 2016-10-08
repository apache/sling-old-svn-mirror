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
package org.apache.sling.contextaware.config.resource.impl.def;

import static org.apache.sling.contextaware.config.resource.impl.def.ConfigurationResourceNameConstants.PROPERTY_CONFIG_COLLECTION_INHERIT;
import static org.apache.sling.contextaware.config.resource.impl.def.ConfigurationResourceNameConstants.PROPERTY_CONFIG_REF;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.contextaware.config.resource.impl.ContextPathStrategyMultiplexer;
import org.apache.sling.contextaware.config.resource.spi.ConfigurationResourceResolvingStrategy;
import org.apache.sling.hamcrest.ResourceCollectionMatchers;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests with content and configurations that form a deeper nested hierarchy.
 */
public class DefaultConfigurationResourceResolvingStrategyHierarchyTest {
    
    private static final String BUCKET = "sling:test";

    @Rule
    public SlingContext context = new SlingContext();

    private ConfigurationResourceResolvingStrategy underTest;

    private Resource site1Page1;
    private Resource site2Page1;

    @Before
    public void setUp() {
        context.registerInjectActivateService(new DefaultContextPathStrategy());
        context.registerInjectActivateService(new ContextPathStrategyMultiplexer());
        underTest = context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy());

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
    public void testGetResource() {
        // build config resources
        context.build()
            .resource("/conf/brand1/tenant1/region1/site1/sling:test/cfgSite1")
            .resource("/conf/brand1/tenant1/region1/sling:test/cfgRegion1")
            .resource("/conf/brand1/tenant1/sling:test/cfgTenant1")
            .resource("/conf/brand1/tenant1/sling:test/test")
            .resource("/conf/brand1/sling:test/cfgBrand1")
            .resource("/conf/global/sling:test/cfgGlobal")
            .resource("/conf/global/sling:test/test")
            .resource("/apps/conf/sling:test/cfgAppsGlobal")
            .resource("/apps/conf/sling:test/test")
            .resource("/libs/conf/sling:test/cfgLibsGlobal")
            .resource("/libs/conf/sling:test/test");

        assertEquals("/conf/brand1/tenant1/region1/site1/sling:test/cfgSite1", underTest.getResource(site1Page1, BUCKET, "cfgSite1").getPath());
        assertEquals("/conf/brand1/tenant1/region1/sling:test/cfgRegion1", underTest.getResource(site1Page1, BUCKET, "cfgRegion1").getPath());
        assertEquals("/conf/brand1/tenant1/sling:test/cfgTenant1", underTest.getResource(site1Page1, BUCKET, "cfgTenant1").getPath());
        assertEquals("/conf/brand1/sling:test/cfgBrand1", underTest.getResource(site1Page1, BUCKET, "cfgBrand1").getPath());
        assertEquals("/conf/global/sling:test/cfgGlobal", underTest.getResource(site1Page1, BUCKET, "cfgGlobal").getPath());
        assertEquals("/apps/conf/sling:test/cfgAppsGlobal", underTest.getResource(site1Page1, BUCKET, "cfgAppsGlobal").getPath());
        assertEquals("/libs/conf/sling:test/cfgLibsGlobal", underTest.getResource(site1Page1, BUCKET, "cfgLibsGlobal").getPath());
        assertEquals("/conf/brand1/tenant1/sling:test/test", underTest.getResource(site1Page1, BUCKET, "test").getPath());

        assertNull(underTest.getResource(site2Page1, BUCKET, "cfgSite1"));
        assertEquals("/conf/brand1/tenant1/region1/sling:test/cfgRegion1", underTest.getResource(site2Page1, BUCKET, "cfgRegion1").getPath());
        assertEquals("/conf/brand1/tenant1/sling:test/cfgTenant1", underTest.getResource(site2Page1, BUCKET, "cfgTenant1").getPath());
        assertEquals("/conf/brand1/sling:test/cfgBrand1", underTest.getResource(site2Page1, BUCKET, "cfgBrand1").getPath());
        assertEquals("/conf/global/sling:test/cfgGlobal", underTest.getResource(site2Page1, BUCKET, "cfgGlobal").getPath());
        assertEquals("/apps/conf/sling:test/cfgAppsGlobal", underTest.getResource(site2Page1, BUCKET, "cfgAppsGlobal").getPath());
        assertEquals("/libs/conf/sling:test/cfgLibsGlobal", underTest.getResource(site2Page1, BUCKET, "cfgLibsGlobal").getPath());
        assertEquals("/conf/brand1/tenant1/sling:test/test", underTest.getResource(site2Page1, BUCKET, "test").getPath());
    }

    @Test
    public void testGetResourceCollectionWithInheritance() {
        // build config resources
        context.build()
            .resource("/conf/brand1/tenant1/region1/site1/sling:test/cfgCol/site1")
            .resource("/conf/brand1/tenant1/region1/sling:test/cfgCol/region1")
            .resource("/conf/brand1/tenant1/sling:test/cfgCol/tenant1")
            .resource("/conf/brand1/sling:test/cfgCol/brand1")
            .resource("/conf/global/sling:test/cfgCol/confGlobal")
            .resource("/apps/conf/sling:test/cfgCol/appsGlobal")
            .resource("/libs/conf/sling:test/cfgCol", PROPERTY_CONFIG_COLLECTION_INHERIT, true)
            .resource("/libs/conf/sling:test/cfgCol/libsGlobal1")
            .resource("/libs/conf/sling:test/cfgCol/libsGlobal2");

        assertThat(underTest.getResourceCollection(site1Page1, BUCKET, "cfgCol"), ResourceCollectionMatchers.paths(
                "/conf/brand1/tenant1/region1/site1/sling:test/cfgCol/site1",
                "/conf/brand1/tenant1/region1/sling:test/cfgCol/region1", 
                "/conf/brand1/tenant1/sling:test/cfgCol/tenant1", 
                "/conf/brand1/sling:test/cfgCol/brand1", 
                "/conf/global/sling:test/cfgCol/confGlobal", 
                "/apps/conf/sling:test/cfgCol/appsGlobal", 
                "/libs/conf/sling:test/cfgCol/libsGlobal1", 
                "/libs/conf/sling:test/cfgCol/libsGlobal2"));

        assertThat(underTest.getResourceCollection(site2Page1, BUCKET, "cfgCol"), ResourceCollectionMatchers.paths( 
                "/conf/brand1/tenant1/region1/sling:test/cfgCol/region1", 
                "/conf/brand1/tenant1/sling:test/cfgCol/tenant1", 
                "/conf/brand1/sling:test/cfgCol/brand1", 
                "/conf/global/sling:test/cfgCol/confGlobal", 
                "/apps/conf/sling:test/cfgCol/appsGlobal", 
                "/libs/conf/sling:test/cfgCol/libsGlobal1", 
                "/libs/conf/sling:test/cfgCol/libsGlobal2"));
    }

    @Test
    public void testGetResourceCollectionContentConfigRefInheritanceAndConfigResourceInheritance() {
        
        context.build()
            .resource("/content/level1", PROPERTY_CONFIG_REF, "/conf/a1/a2")
            .resource("/content/level1/level2", PROPERTY_CONFIG_REF, "/conf/b1/b2")
            .resource("/conf/a1/sling:test/cfgCol/a1")
            .resource("/conf/a1/a2/sling:test/cfgCol/a1_a2")
            .resource("/conf/b1/sling:test/cfgCol/b1")
            .resource("/conf/b1/b2/sling:test/cfgCol/b1_b2")
            .resource("/conf/global/sling:test/cfgCol/confGlobal")
            .resource("/libs/conf/sling:test/cfgCol", PROPERTY_CONFIG_COLLECTION_INHERIT, true)
            .resource("/apps/conf/sling:test/cfgCol/appsGlobal")
            .resource("/libs/conf/sling:test/cfgCol/libsGlobal");
        
        Resource level1_2 = context.resourceResolver().getResource("/content/level1/level2");

        assertThat(underTest.getResourceCollection(level1_2, BUCKET, "cfgCol"), ResourceCollectionMatchers.paths( 
                "/conf/b1/b2/sling:test/cfgCol/b1_b2", 
                "/conf/b1/sling:test/cfgCol/b1", 
                "/conf/a1/a2/sling:test/cfgCol/a1_a2", 
                "/conf/a1/sling:test/cfgCol/a1", 
                "/conf/global/sling:test/cfgCol/confGlobal", 
                "/apps/conf/sling:test/cfgCol/appsGlobal", 
                "/libs/conf/sling:test/cfgCol/libsGlobal"));
    }
    
}
