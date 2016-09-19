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

import static org.apache.sling.contextaware.config.resource.impl.ConfigurationResourceTestUtils.assetResourcePaths;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.contextaware.config.resource.impl.def.DefaultContextPathStrategy;
import org.apache.sling.contextaware.config.resource.spi.ContextPathStrategy;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ContextPathStrategyMultiplexerTest {

    @Rule
    public SlingContext context = new SlingContext();
    
    private ContextPathStrategyMultiplexer underTest;
    
    private Resource site1Page1;
    private Resource site2Page1;

    @Before
    public void setUp() {
        underTest = context.registerInjectActivateService(new ContextPathStrategyMultiplexer());

        // content resources that form a deeper hierarchy
        context.build()
            .resource("/content/tenant1", "sling:config-ref", "/conf/tenant1")
            .resource("/content/tenant1/region1", "sling:config-ref", "/conf/tenant1/region1")
            .resource("/content/tenant1/region1/site1", "sling:config-ref", "/conf/tenant1/region1/site1")
            .resource("/content/tenant1/region1/site2", "sling:config-ref", "/conf/tenant1/region1/site2");
        site1Page1 = context.create().resource("/content/tenant1/region1/site1/page1");
        site2Page1 = context.create().resource("/content/tenant1/region1/site2/page1");
    }
    
    @Test
    public void testWithNoStrategies() {
        Iterator<Resource> resources = underTest.findContextResources(site1Page1);
        assertFalse(resources.hasNext());
    }

    @Test
    public void testWithDefaultStrategy() {
        context.registerInjectActivateService(new DefaultContextPathStrategy());

        assetResourcePaths(new String[] {
                "/content/tenant1/region1/site1",
                "/content/tenant1/region1",
                "/content/tenant1"
        }, underTest.findContextResources(site1Page1));

        assetResourcePaths(new String[] {
                "/content/tenant1/region1/site2",
                "/content/tenant1/region1",
                "/content/tenant1"
        }, underTest.findContextResources(site2Page1));
    }
    
    @Test
    public void testWithNonoverlappingStrategies() {
        registerContextPathStrategy("/content/tenant1");
        registerContextPathStrategy("/content/tenant1/region1/site1", "/content/tenant1/region1");
        
        assetResourcePaths(new String[] {
                "/content/tenant1/region1/site1",
                "/content/tenant1/region1",
                "/content/tenant1"
        }, underTest.findContextResources(site1Page1));
    }
    
    @Test
    public void testWithOverlappingStrategies() {
        registerContextPathStrategy("/content/tenant1", "/content/tenant1/region1");
        registerContextPathStrategy("/content/tenant1/region1/site1", "/content/tenant1/region1");
        
        assetResourcePaths(new String[] {
                "/content/tenant1/region1/site1",
                "/content/tenant1/region1",
                "/content/tenant1"
        }, underTest.findContextResources(site1Page1));
    }
    
    private void registerContextPathStrategy(String... paths) {
        final List<Resource> resources = new ArrayList<>();
        for (String path : paths) {
            Resource resource = context.resourceResolver().getResource(path);
            if (resource != null) {
                resources.add(resource);
            }
        }
        context.registerService(ContextPathStrategy.class, new ContextPathStrategy() {
            @Override
            public Iterator<Resource> findContextResources(Resource resource) {
                return resources.iterator();
            }
        });
    }

}
