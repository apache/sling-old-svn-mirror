/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.resource.inventory.impl;

import java.util.Collections;

import javax.json.JsonObject;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.resourceresolver.MockHelper;
import org.apache.sling.testing.resourceresolver.MockResource;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ResourceTraversorTest {

    private ResourceResolver resolver;

    @Before
    public void setup() throws LoginException {
        resolver = new MockResourceResolverFactory()
                .getAdministrativeResourceResolver(null);
    }

    @Test
    public void testCollectResources() throws Exception {
        MockHelper.create(resolver)
                .resource("/some")
                .p("p1", "v1")
                .resource("/some/path")
                .p("p2", "v2")
                .commit();
        Resource resource = resolver.getResource("/some");
        ResourceTraversor traversor = new ResourceTraversor(resource);
        traversor.collectResources();
        JsonObject json = traversor.getJsonObject();
        assertEquals("v1", json.getString("p1"));
        JsonObject path = json.getJsonObject("path");
        assertNotNull(path);
        assertEquals("v2", path.getString("p2"));

    }

    @Test
    public void testGetJSONObject() throws Exception {
        Resource resource = new MockResource("/some/path", Collections.<String, Object>singletonMap("p1", "v1"), resolver);
        JsonObject json = new ResourceTraversor(resource).getJsonObject();
        assertEquals("v1", json.getString("p1"));

    }

}