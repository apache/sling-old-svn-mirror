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
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Tests combinations of create and deletion of resources and conflict detection.
 */
public class CreateDeleteResourceResolverTest {
        
    private static final Map<String,Object> PROPS1 = ImmutableMap.<String, Object>builder()
            .put("prop1", "value1").build();
    private static final Map<String,Object> PROPS2 = ImmutableMap.<String, Object>builder()
            .put("prop2", "value2").build();

    private ResourceResolver resourceResolver;
    private Resource testRoot;

    @Before
    public final void setUp() throws IOException, LoginException {
        resourceResolver = new MockResourceResolverFactory().getResourceResolver(null);
        Resource root = resourceResolver.getResource("/");
        testRoot = resourceResolver.create(root, "test", ValueMap.EMPTY);
    }

    @Test
    public void testCreateDeleteCreate() throws PersistenceException {
        // create new node without commit
        Resource resource = resourceResolver.create(testRoot, "node", PROPS1);
        assertEquals(PROPS1, ResourceUtil.getValueMap(resource));
        
        // delete node without commit
        resourceResolver.delete(resource);
        assertNull(resourceResolver.getResource(testRoot.getPath() + "/node"));

        // create node again with different properties without commit
        resource = resourceResolver.create(testRoot, "node", PROPS2);
        assertEquals(PROPS2, ResourceUtil.getValueMap(resource));
    }

    @Test
    public void testCreateDeleteCreateCommit() throws PersistenceException {
        // create new node without commit
        Resource resource = resourceResolver.create(testRoot, "node", PROPS1);
        assertEquals(PROPS1, ResourceUtil.getValueMap(resource));
        
        // delete node without commit
        resourceResolver.delete(resource);
        assertNull(resourceResolver.getResource(testRoot.getPath() + "/node"));

        // create node again with different properties with commit
        resource = resourceResolver.create(testRoot, "node", PROPS2);
        assertEquals(PROPS2, ResourceUtil.getValueMap(resource));
        resourceResolver.commit();
    }

    @Test
    public void testCreateCommitDeleteCreateCommit() throws PersistenceException {
        // create new node with commit
        Resource resource = resourceResolver.create(testRoot, "node", PROPS1);
        assertEquals(PROPS1, ResourceUtil.getValueMap(resource));
        resourceResolver.commit();
        
        // delete node without commit
        resourceResolver.delete(resource);
        assertNull(resourceResolver.getResource(testRoot.getPath() + "/node"));

        // create node again with different properties with commit
        resource = resourceResolver.create(testRoot, "node", PROPS2);
        assertEquals(PROPS2, ResourceUtil.getValueMap(resource));
        resourceResolver.commit();
    }

    @Test
    public void testCreateCommitDeleteCommitCreateCommit() throws PersistenceException {
        // create new node with commit
        Resource resource = resourceResolver.create(testRoot, "node", PROPS1);
        assertEquals(PROPS1, ResourceUtil.getValueMap(resource));
        resourceResolver.commit();
        
        // delete node with commit
        resourceResolver.delete(resource);
        assertNull(resourceResolver.getResource(testRoot.getPath() + "/node"));
        resourceResolver.commit();

        // create node again with different properties with commit
        resource = resourceResolver.create(testRoot, "node", PROPS2);
        assertEquals(PROPS2, ResourceUtil.getValueMap(resource));
        resourceResolver.commit();
    }

    @Test(expected=PersistenceException.class)
    public void testCreatePathAlreadyExists() throws PersistenceException {
        resourceResolver.create(testRoot, "node", PROPS1);
        resourceResolver.create(testRoot, "node", PROPS2);
    }

    @Test(expected=PersistenceException.class)
    public void testCreateCommitPathAlreadyExists() throws PersistenceException {
        resourceResolver.create(testRoot, "node", PROPS1);
        resourceResolver.commit();
        resourceResolver.create(testRoot, "node", PROPS2);
    }

}
