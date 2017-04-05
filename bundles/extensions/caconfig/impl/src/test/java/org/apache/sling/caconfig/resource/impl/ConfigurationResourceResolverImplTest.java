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
package org.apache.sling.caconfig.resource.impl;

import static org.apache.sling.caconfig.resource.impl.def.ConfigurationResourceNameConstants.PROPERTY_CONFIG_COLLECTION_INHERIT;
import static org.apache.sling.caconfig.resource.impl.def.ConfigurationResourceNameConstants.PROPERTY_CONFIG_REF;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.apache.sling.hamcrest.ResourceCollectionMatchers;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class ConfigurationResourceResolverImplTest {
    
    private static final String BUCKET = "sling:test";

    @Rule
    public SlingContext context = new SlingContext();

    private ConfigurationResourceResolver underTest;

    private Resource site1Page1;
    private Resource site2Page1;

    @Before
    public void setUp() {
        underTest = ConfigurationResourceTestUtils.registerConfigurationResourceResolver(context);

        // content resources
        context.build()
            .resource("/content/site1", PROPERTY_CONFIG_REF, "/conf/site1")
            .resource("/content/site2", PROPERTY_CONFIG_REF, "/conf/site2");
        site1Page1 = context.create().resource("/content/site1/page1");
        site2Page1 = context.create().resource("/content/site2/page1");
        
        // configuration
        context.build()
            .resource("/conf/site1/sling:test/test")
            .resource("/conf/site1/sling:test/feature", PROPERTY_CONFIG_COLLECTION_INHERIT, true)
                .resource("c")
            .resource("/conf/site2/sling:test/feature", PROPERTY_CONFIG_COLLECTION_INHERIT, true)
                .siblingsMode()
                .resource("c")
                .resource("d")
            .resource("/apps/conf/sling:test/feature", PROPERTY_CONFIG_COLLECTION_INHERIT, true)
                .resource("a")
            .resource("/libs/conf/sling:test/test")
            .resource("/libs/conf/sling:test/feature")
                .resource("b");
    }

    @Test
    public void testGetResource() {
        assertEquals("/conf/site1/sling:test/test", underTest.getResource(site1Page1, BUCKET, "test").getPath());
        assertEquals("/libs/conf/sling:test/test", underTest.getResource(site2Page1, BUCKET, "test").getPath());
    }

    @Test
    public void testGetResourceCollection() {
        assertThat(underTest.getResourceCollection(site1Page1, BUCKET, "feature"), ResourceCollectionMatchers.paths( 
                "/conf/site1/sling:test/feature/c",
                "/apps/conf/sling:test/feature/a", 
                "/libs/conf/sling:test/feature/b"));
        
        assertThat(underTest.getResourceCollection(site2Page1, BUCKET, "feature"), ResourceCollectionMatchers.paths( 
                "/conf/site2/sling:test/feature/c",
                "/conf/site2/sling:test/feature/d",
                "/apps/conf/sling:test/feature/a",
                "/libs/conf/sling:test/feature/b" ));
    }

    @Test
    public void testGetContextPath() {
        assertEquals("/content/site1", underTest.getContextPath(site1Page1));
        assertEquals("/content/site2", underTest.getContextPath(site2Page1));
    }

    @Test
    public void testGetAllContextPaths() {
        assertEquals(ImmutableList.of("/content/site1"), underTest.getAllContextPaths(site1Page1));
        assertEquals(ImmutableList.of("/content/site2"), underTest.getAllContextPaths(site2Page1));
    }
    
}
