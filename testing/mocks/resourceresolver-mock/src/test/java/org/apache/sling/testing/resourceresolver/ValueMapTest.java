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
package org.apache.sling.testing.resourceresolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test different ValueMap variants.
 */
public class ValueMapTest {

    private ResourceResolver resourceResolver;
    private Resource testRoot;

    @Before
    public final void setUp() throws IOException, LoginException {
        resourceResolver = new MockResourceResolverFactory().getResourceResolver(null);

        Resource root = resourceResolver.getResource("/");
        testRoot = resourceResolver.create(root, "test", ValueMap.EMPTY);

        resourceResolver.create(testRoot, "node1",
            ImmutableMap.<String, Object>builder()
                .put("prop1", "value1")
                .build());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMap() throws IOException {
        Resource resource1 = resourceResolver.getResource(testRoot.getPath() + "/node1");
        
        Map<String, Object> map = resource1.adaptTo(Map.class);
        assertTrue(map instanceof ValueMap && !(map instanceof ModifiableValueMap));
        
        assertEquals("value1", map.get("prop1"));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = UnsupportedOperationException.class)
    public void testMap_Readonly() throws IOException {
        Resource resource1 = resourceResolver.getResource(testRoot.getPath() + "/node1");
        
        Map<String, Object> map = resource1.adaptTo(Map.class);
        map.put("prop1", "value2");
    }

    @Test
    public void testValueMap() throws IOException {
        Resource resource1 = resourceResolver.getResource(testRoot.getPath() + "/node1");
        
        ValueMap map = resource1.adaptTo(ValueMap.class);
        assertTrue(map instanceof ValueMap && !(map instanceof ModifiableValueMap));
        
        assertEquals("value1", map.get("prop1"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testValueMapMap_Readonly() throws IOException {
        Resource resource1 = resourceResolver.getResource(testRoot.getPath() + "/node1");
        
        ValueMap map = resource1.adaptTo(ValueMap.class);
        map.put("prop1", "value2");
    }

    @Test
    public void testModifiableValueMap() throws IOException {
        Resource resource1 = resourceResolver.getResource(testRoot.getPath() + "/node1");
        
        ValueMap map = resource1.adaptTo(ModifiableValueMap.class);
        assertTrue(map instanceof ValueMap && map instanceof ModifiableValueMap);
        
        assertEquals("value1", map.get("prop1"));
        map.put("prop1", "value2");
    }

}
