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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.contextaware.config.resource.ConfigurationResourceResolver;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class ConfigurationResourceResolverImplTest {

    @Rule
    public SlingContext context = new SlingContext();

    private ConfigurationResourceResolver underTest;

    private Resource site1Page1;
    private Resource site2Page1;

    @Before
    public void setUp() {
        underTest = context.registerInjectActivateService(new ConfigurationResourceResolverImpl());

        // content resources
        site1Page1 = context.create().resource("/content/site1/page1", ImmutableMap.<String, Object>builder()
                .put("sling:config", "/config/site1")
                .build());
        site2Page1 = context.create().resource("/content/site2/page1", ImmutableMap.<String, Object>builder()
                .put("sling:config", "/config/site2")
                .build());

        // configuration
        context.create().resource("/libs/test");
        context.create().resource("/config/site1/test");
        context.create().resource("/apps/feature/a");
        context.create().resource("/libs/feature/b");
        context.create().resource("/config/site1/feature/c");
        context.create().resource("/config/site2/feature/c");
        context.create().resource("/config/site2/feature/d");
    }

    @Test
    public void testGetValueMapContextPath() {
        assertEquals("/content/site1", underTest.getContextPath(site1Page1));
        assertEquals("/content/site2", underTest.getContextPath(site2Page1));
    }

    @Test
    public void testGetResource() {
        assertEquals("/config/site1/test", underTest.getResource(site1Page1, "test").getPath());
        assertEquals("/libs/test", underTest.getResource(site2Page1, "test").getPath());
    }

    @Test
    public void testGetResourceCollection() {
        final Collection<Resource> col1 = underTest.getResourceCollection(site1Page1, "feature");
        assertEquals(3, col1.size());
        final Set<String> expectedPaths = new HashSet<>();
        expectedPaths.add("/config/site1/feature/c");
        expectedPaths.add("/apps/feature/a");
        expectedPaths.add("/libs/feature/b");

        for(final Resource rsrc : col1) {
            assertTrue(expectedPaths.remove(rsrc.getPath()));
        }

        final Collection<Resource> col2 = underTest.getResourceCollection(site2Page1, "feature");
        assertEquals(4, col2.size());
        expectedPaths.add("/config/site2/feature/d");
        expectedPaths.add("/config/site2/feature/c");
        expectedPaths.add("/apps/feature/a");
        expectedPaths.add("/libs/feature/b");

        for(final Resource rsrc : col2) {
            assertTrue(expectedPaths.remove(rsrc.getPath()));
        }
    }
}
