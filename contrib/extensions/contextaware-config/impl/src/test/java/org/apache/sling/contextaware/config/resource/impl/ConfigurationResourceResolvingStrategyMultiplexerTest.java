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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.contextaware.config.resource.spi.ConfigurationResourceResolvingStrategy;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Constants;

import com.google.common.collect.ImmutableList;

public class ConfigurationResourceResolvingStrategyMultiplexerTest {

    private static final String BUCKET = "sling:test";
    
    @Rule
    public SlingContext context = new SlingContext();
    
    private ConfigurationResourceResolvingStrategyMultiplexer underTest;
    
    private Resource site1Page1;

    @Before
    public void setUp() {
        context.registerInjectActivateService(new DefaultContextPathStrategy());
        context.registerInjectActivateService(new ContextPathStrategyMultiplexer());
        underTest = context.registerInjectActivateService(new ConfigurationResourceResolvingStrategyMultiplexer());

        // content resources
        context.build()
            .resource("/content/site1", "sling:config-ref", "/conf/site1")
            .resource("/content/site2", "sling:config-ref", "/conf/site2");
        site1Page1 = context.create().resource("/content/site1/page1");
        
        // configuration
        context.build()
            .resource("/conf/site1/sling:test/test")
            .resource("/conf/site1/sling:test/feature/c")
            .resource("/conf/site2/sling:test/feature/c")
            .resource("/conf/site2/sling:test/feature/d")
            .resource("/apps/conf/sling:test/feature/a")
            .resource("/libs/conf/sling:test/test")
            .resource("/libs/conf/sling:test/feature/b");
    }
    
    @Test
    public void testWithNoStrategies() {
        assertNull(underTest.getResource(site1Page1, BUCKET, "test"));
        
        Collection<Resource> col1 = underTest.getResourceCollection(site1Page1, BUCKET, "feature");
        assertTrue(col1.isEmpty());
    }

    @Test
    public void testWithDefaultStrategy() {
        context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy());

        assertEquals("/conf/site1/sling:test/test", underTest.getResource(site1Page1, BUCKET, "test").getPath());

        Collection<Resource> col1 = underTest.getResourceCollection(site1Page1, BUCKET, "feature");
        assetResourcePaths(new String[] {
                "/conf/site1/sling:test/feature/c",
                "/apps/conf/sling:test/feature/a", 
                "/libs/conf/sling:test/feature/b" },
                col1);
    }
    
    @Test
    public void testMultipleStrategies() {
        
        // strategy 1
        context.registerService(ConfigurationResourceResolvingStrategy.class, new ConfigurationResourceResolvingStrategy() {
            @Override
            public Resource getResource(Resource resource, String bucketName, String configName) {
                return context.resourceResolver().getResource("/conf/site1/sling:test/test");
            }
            @Override
            public Collection<Resource> getResourceCollection(Resource resource, String bucketName, String configName) {
                return ImmutableList.copyOf(context.resourceResolver().getResource("/conf/site1/sling:test/feature").listChildren());
            }
        }, Constants.SERVICE_RANKING, 2000);
        
        // strategy 2
        context.registerService(ConfigurationResourceResolvingStrategy.class, new ConfigurationResourceResolvingStrategy() {
            @Override
            public Resource getResource(Resource resource, String bucketName, String configName) {
                return context.resourceResolver().getResource("/libs/conf/sling:test/test");
            }
            @Override
            public Collection<Resource> getResourceCollection(Resource resource, String bucketName, String configName) {
                return ImmutableList.copyOf(context.resourceResolver().getResource("/libs/conf/sling:test/feature").listChildren());
            }
        }, Constants.SERVICE_RANKING, 1000);
        
        assertEquals("/conf/site1/sling:test/test", underTest.getResource(site1Page1, BUCKET, "test").getPath());

        Collection<Resource> col1 = underTest.getResourceCollection(site1Page1, BUCKET, "feature");
        assetResourcePaths(new String[] {
                "/conf/site1/sling:test/feature/c",
                "/libs/conf/sling:test/feature/b" },
                col1);
    }
    
    
}
