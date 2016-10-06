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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.contextaware.config.hamcrest.ResourceCollectionMatchers;
import org.apache.sling.contextaware.config.resource.impl.ContextPathStrategyMultiplexer;
import org.apache.sling.contextaware.config.resource.spi.ConfigurationResourceResolvingStrategy;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class DefaultConfigurationResourceResolvingStrategyTest {
    
    private static final String BUCKET = "sling:test";

    @Rule
    public SlingContext context = new SlingContext();

    private Resource site1Page1;
    private Resource site2Page1;

    @Before
    public void setUp() {
        context.registerInjectActivateService(new DefaultContextPathStrategy());
        context.registerInjectActivateService(new ContextPathStrategyMultiplexer());

        // content resources
        context.build()
            .resource("/content/site1", "sling:config-ref", "/conf/site1")
            .resource("/content/site2", "sling:config-ref", "/conf/site2");
        site1Page1 = context.create().resource("/content/site1/page1");
        site2Page1 = context.create().resource("/content/site2/page1");
        
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
    public void testGetResource() {
        ConfigurationResourceResolvingStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy());

        assertEquals("/conf/site1/sling:test/test", underTest.getResource(site1Page1, BUCKET, "test").getPath());
        assertEquals("/libs/conf/sling:test/test", underTest.getResource(site2Page1, BUCKET, "test").getPath());
    }

    @Test
    public void testGetResourceCollection() {
        ConfigurationResourceResolvingStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy());

        assertThat(underTest.getResourceCollection(site1Page1, BUCKET, "feature"), ResourceCollectionMatchers.paths(
                "/conf/site1/sling:test/feature/c",
                "/apps/conf/sling:test/feature/a", 
                "/libs/conf/sling:test/feature/b"));

        assertThat(underTest.getResourceCollection(site2Page1, BUCKET, "feature"), ResourceCollectionMatchers.paths( 
                "/conf/site2/sling:test/feature/c",
                "/conf/site2/sling:test/feature/d",
                "/apps/conf/sling:test/feature/a",
                "/libs/conf/sling:test/feature/b"));
    }

    @Test
    public void testGetResourcePath() throws Exception {
        ConfigurationResourceResolvingStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy());
        assertEquals("/conf/site1/sling:test/test", underTest.getResourcePath(site1Page1, BUCKET, "test"));
    }

    @Test
    public void testGetResourceCollectionParentPath() throws Exception {
        ConfigurationResourceResolvingStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy());
        assertEquals("/conf/site1/sling:test/feature", underTest.getResourceCollectionParentPath(site1Page1, BUCKET, "feature"));
    }

    @Test
    public void testDisabled() {
        ConfigurationResourceResolvingStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy(),
                "enabled", false);

        assertNull(underTest.getResource(site1Page1, BUCKET, "test"));
        assertTrue(underTest.getResourceCollection(site1Page1, BUCKET, "feature").isEmpty());
        assertNull(underTest.getResourcePath(site1Page1, BUCKET, "test"));
        assertNull(underTest.getResourceCollectionParentPath(site1Page1, BUCKET, "feature"));
    }

}
