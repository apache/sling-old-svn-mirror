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
package org.apache.sling.contextaware.config.resource.impl;

import static org.apache.sling.contextaware.config.resource.impl.TestUtils.assetResourcePaths;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collection;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.contextaware.config.resource.ConfigurationResourceResolver;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Tests with content and configurations that form a deeper nested hierarchy.
 */
public class ConfigurationResourceResolverImplHierarchyTest {
    
    private static final String BUCKET = "sling:test";

    @Rule
    public SlingContext context = new SlingContext();

    private ConfigurationResourceResolver underTest;

    private Resource site1Page1;
    private Resource site2Page1;

    @Before
    public void setUp() {
        underTest = context.registerInjectActivateService(new ConfigurationResourceResolverImpl());

        // content resources that form a deeper hierarchy
        context.create().resource("/content/tenant1", ImmutableMap.<String, Object>builder()
                .put("sling:config-ref", "/conf/tenant1")
                .build());
        context.create().resource("/content/tenant1/region1", ImmutableMap.<String, Object>builder()
                .put("sling:config-ref", "/conf/tenant1/region1")
                .build());
        context.create().resource("/content/tenant1/region1/site1", ImmutableMap.<String, Object>builder()
                .put("sling:config-ref", "/conf/tenant1/region1/site1")
                .build());
        site1Page1 = context.create().resource("/content/tenant1/region1/site1/page1");
        context.create().resource("/content/tenant1/region1/site2", ImmutableMap.<String, Object>builder()
                .put("sling:config-ref", "/conf/tenant1/region1/site2")
                .build());
        site2Page1 = context.create().resource("/content/tenant1/region1/site2/page1");

        // configuration
        context.create().resource("/conf/tenant1/region1/site1/sling:test/cfgSite1");
        context.create().resource("/conf/tenant1/region1/site1/sling:test/cfgCol/site1");
        context.create().resource("/conf/tenant1/region1/sling:test/cfgRegion1");
        context.create().resource("/conf/tenant1/region1/sling:test/cfgCol/region1");
        context.create().resource("/conf/tenant1/sling:test/cfgTenant1");
        context.create().resource("/conf/tenant1/sling:test/cfgCol/tenant1");
        context.create().resource("/conf/tenant1/sling:test/test");
        context.create().resource("/conf/global/sling:test/cfgGlobal");
        context.create().resource("/conf/global/sling:test/cfgCol/confGlobal");
        context.create().resource("/conf/global/sling:test/test");
        context.create().resource("/apps/conf/sling:test/cfgAppsGlobal");
        context.create().resource("/apps/conf/sling:test/cfgCol/appsGlobal");
        context.create().resource("/apps/conf/sling:test/test");
        context.create().resource("/libs/conf/sling:test/cfgLibsGlobal");
        context.create().resource("/libs/conf/sling:test/cfgCol/libsGlobal1");
        context.create().resource("/libs/conf/sling:test/cfgCol/libsGlobal2");
        context.create().resource("/libs/conf/sling:test/test");
    }

    @Test
    public void testGetResource() {
        assertEquals("/conf/tenant1/region1/site1/sling:test/cfgSite1", underTest.getResource(site1Page1, BUCKET, "cfgSite1").getPath());
        assertEquals("/conf/tenant1/region1/sling:test/cfgRegion1", underTest.getResource(site1Page1, BUCKET, "cfgRegion1").getPath());
        assertEquals("/conf/tenant1/sling:test/cfgTenant1", underTest.getResource(site1Page1, BUCKET, "cfgTenant1").getPath());
        assertEquals("/conf/global/sling:test/cfgGlobal", underTest.getResource(site1Page1, BUCKET, "cfgGlobal").getPath());
        assertEquals("/apps/conf/sling:test/cfgAppsGlobal", underTest.getResource(site1Page1, BUCKET, "cfgAppsGlobal").getPath());
        assertEquals("/libs/conf/sling:test/cfgLibsGlobal", underTest.getResource(site1Page1, BUCKET, "cfgLibsGlobal").getPath());
        assertEquals("/conf/tenant1/sling:test/test", underTest.getResource(site1Page1, BUCKET, "test").getPath());

        assertNull(underTest.getResource(site2Page1, BUCKET, "cfgSite1"));
        assertEquals("/conf/tenant1/region1/sling:test/cfgRegion1", underTest.getResource(site2Page1, BUCKET, "cfgRegion1").getPath());
        assertEquals("/conf/tenant1/sling:test/cfgTenant1", underTest.getResource(site2Page1, BUCKET, "cfgTenant1").getPath());
        assertEquals("/conf/global/sling:test/cfgGlobal", underTest.getResource(site2Page1, BUCKET, "cfgGlobal").getPath());
        assertEquals("/apps/conf/sling:test/cfgAppsGlobal", underTest.getResource(site2Page1, BUCKET, "cfgAppsGlobal").getPath());
        assertEquals("/libs/conf/sling:test/cfgLibsGlobal", underTest.getResource(site2Page1, BUCKET, "cfgLibsGlobal").getPath());
        assertEquals("/conf/tenant1/sling:test/test", underTest.getResource(site2Page1, BUCKET, "test").getPath());
    }

    @Test
    public void testGetResourceCollection() {
        Collection<Resource> col1 = underTest.getResourceCollection(site1Page1, BUCKET, "cfgCol");
        assetResourcePaths(new String[] {
                "/conf/tenant1/region1/site1/sling:test/cfgCol/site1",
                "/conf/tenant1/region1/sling:test/cfgCol/region1", 
                "/conf/tenant1/sling:test/cfgCol/tenant1", 
                "/conf/global/sling:test/cfgCol/confGlobal", 
                "/apps/conf/sling:test/cfgCol/appsGlobal", 
                "/libs/conf/sling:test/cfgCol/libsGlobal1", 
                "/libs/conf/sling:test/cfgCol/libsGlobal2" },
                col1);

        Collection<Resource> col2 = underTest.getResourceCollection(site2Page1, BUCKET, "cfgCol");
        assetResourcePaths(new String[] {
                "/conf/tenant1/region1/sling:test/cfgCol/region1", 
                "/conf/tenant1/sling:test/cfgCol/tenant1", 
                "/conf/global/sling:test/cfgCol/confGlobal", 
                "/apps/conf/sling:test/cfgCol/appsGlobal", 
                "/libs/conf/sling:test/cfgCol/libsGlobal1", 
                "/libs/conf/sling:test/cfgCol/libsGlobal2" },
                col2);
    }

}
