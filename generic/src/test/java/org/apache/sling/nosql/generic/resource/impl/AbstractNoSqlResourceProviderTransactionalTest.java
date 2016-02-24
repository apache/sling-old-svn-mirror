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
package org.apache.sling.nosql.generic.resource.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test basic ResourceResolver and ValueMap with different data types.
 */
public abstract class AbstractNoSqlResourceProviderTransactionalTest {
    
    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);
    
    protected abstract void registerResourceProviderFactory();

    protected abstract Resource testRoot();

    @Before
    public void setUp() throws Exception {
        registerResourceProviderFactory();
    }
    
    @After
    public void tearDown() {
        context.resourceResolver().revert();
        try {
            context.resourceResolver().delete(testRoot());
            context.resourceResolver().commit();
        }
        catch (PersistenceException ex) {
            // ignore
        }        
    }

    @Test
    public void testRootNode() {
        assertTrue(testRoot() instanceof NoSqlResource);
    }

    @Test
    public void testAddDeleteNodesPartialCommit() throws PersistenceException {
        context.resourceResolver().create(testRoot(), "node0", ImmutableMap.<String, Object>of());
        context.resourceResolver().create(testRoot(), "node1", ImmutableMap.<String, Object>of());
        context.resourceResolver().commit();

        assertFalse(context.resourceResolver().hasChanges());
        
        context.resourceResolver().create(testRoot(), "node2", ImmutableMap.<String, Object>of());
        context.resourceResolver().create(testRoot(), "node3", ImmutableMap.<String, Object>of());

        assertTrue(context.resourceResolver().hasChanges());
        
        assertNotNull(testRoot().getChild("node0"));
        assertNotNull(testRoot().getChild("node1"));
        assertNotNull(testRoot().getChild("node2"));
        assertNotNull(testRoot().getChild("node3"));
        
        context.resourceResolver().delete(testRoot().getChild("node0"));
        context.resourceResolver().delete(testRoot().getChild("node2"));

        assertNull(testRoot().getChild("node0"));
        assertNotNull(testRoot().getChild("node1"));
        assertNull(testRoot().getChild("node2"));
        assertNotNull(testRoot().getChild("node3"));
        
        Iterator<Resource> children = testRoot().listChildren();
        assertEquals("node1", children.next().getName());
        assertEquals("node3", children.next().getName());
        assertFalse(children.hasNext());
        
        assertTrue(context.resourceResolver().hasChanges());
        
        context.resourceResolver().revert();

        assertFalse(context.resourceResolver().hasChanges());
        
        assertNotNull(testRoot().getChild("node1"));
        assertNull(testRoot().getChild("node2"));
        assertNull(testRoot().getChild("node3"));
        
        children = testRoot().listChildren();
        assertEquals("node0", children.next().getName());
        assertEquals("node1", children.next().getName());
        assertFalse(children.hasNext());
    }

    @Test
    public void testRecursiveDeleteWithoutCommit() throws PersistenceException {
        Resource node1 = context.resourceResolver().create(testRoot(), "node1", ImmutableMap.<String, Object>of());
        Resource node11 = context.resourceResolver().create(node1, "node11", ImmutableMap.<String, Object>of());
        context.resourceResolver().create(node11, "node111", ImmutableMap.<String, Object>of());

        assertNotNull(testRoot().getChild("node1"));
        assertNotNull(testRoot().getChild("node1/node11"));
        assertNotNull(testRoot().getChild("node1/node11/node111"));
        
        context.resourceResolver().delete(node1);

        assertNull(testRoot().getChild("node1"));
        assertNull(testRoot().getChild("node1/node11"));
        assertNull(testRoot().getChild("node1/node11/node111"));
    }

    @Test
    public void testRecursiveDeleteWithCommit() throws PersistenceException {
        Resource node1 = context.resourceResolver().create(testRoot(), "node1", ImmutableMap.<String, Object>of());
        Resource node11 = context.resourceResolver().create(node1, "node11", ImmutableMap.<String, Object>of());
        context.resourceResolver().create(node11, "node111", ImmutableMap.<String, Object>of());
        
        assertTrue(context.resourceResolver().hasChanges());
        
        context.resourceResolver().commit();

        assertFalse(context.resourceResolver().hasChanges());
        
        assertNotNull(testRoot().getChild("node1"));
        assertNotNull(testRoot().getChild("node1/node11"));
        assertNotNull(testRoot().getChild("node1/node11/node111"));
        
        context.resourceResolver().delete(node1);

        assertNull(testRoot().getChild("node1"));
        assertNull(testRoot().getChild("node1/node11"));
        assertNull(testRoot().getChild("node1/node11/node111"));

        assertTrue(context.resourceResolver().hasChanges());

        context.resourceResolver().commit();

        assertFalse(context.resourceResolver().hasChanges());

        assertNull(testRoot().getChild("node1"));
        assertNull(testRoot().getChild("node1/node11"));
        assertNull(testRoot().getChild("node1/node11/node111"));
    }

    @Test(expected = PersistenceException.class)
    public void testCreateAlreadyExistWithoutCommit() throws PersistenceException {
        context.resourceResolver().create(testRoot(), "node1", ImmutableMap.<String, Object>of());
        context.resourceResolver().create(testRoot(), "node1", ImmutableMap.<String, Object>of());
    }
    
    @Test(expected = PersistenceException.class)
    public void testCreateAlreadyExistWithCommit() throws PersistenceException {
        context.resourceResolver().create(testRoot(), "node1", ImmutableMap.<String, Object>of());
        context.resourceResolver().commit();
        context.resourceResolver().create(testRoot(), "node1", ImmutableMap.<String, Object>of());
    }
    
    @Test
    public void testCreateAlreadyExistDeletedWithoutCommit() throws PersistenceException {
        context.resourceResolver().create(testRoot(), "node1", ImmutableMap.<String, Object>of());
        context.resourceResolver().delete(testRoot().getChild("node1"));
        context.resourceResolver().create(testRoot(), "node1", ImmutableMap.<String, Object>of());
    }
    
    @Test
    public void testCreateAlreadyExistDeletedWithCommit() throws PersistenceException {
        context.resourceResolver().create(testRoot(), "node1", ImmutableMap.<String, Object>of());
        context.resourceResolver().commit();
        context.resourceResolver().delete(testRoot().getChild("node1"));
        context.resourceResolver().commit();
        context.resourceResolver().create(testRoot(), "node1", ImmutableMap.<String, Object>of());
    }
    
    @Test
    public void testUpdateWithoutCommit() throws PersistenceException {
        Resource node1 = context.resourceResolver().create(testRoot(), "node1", ImmutableMap.<String, Object>of("prop1", "value1"));
        assertEquals("value1", node1.getValueMap().get("prop1", String.class));
        
        ModifiableValueMap props = node1.adaptTo(ModifiableValueMap.class);
        props.put("prop1", "value2");
        
        node1 = testRoot().getChild("node1");
        assertEquals("value2", node1.getValueMap().get("prop1", String.class));
    }
    
    @Test
    public void testUpdateWithCommit() throws PersistenceException {
        Resource node1 = context.resourceResolver().create(testRoot(), "node1", ImmutableMap.<String, Object>of("prop1", "value1"));
        assertEquals("value1", node1.getValueMap().get("prop1", String.class));
        context.resourceResolver().commit();
        
        ModifiableValueMap props = node1.adaptTo(ModifiableValueMap.class);
        props.put("prop1", "value2");
        context.resourceResolver().commit();
        
        node1 = testRoot().getChild("node1");
        assertEquals("value2", node1.getValueMap().get("prop1", String.class));
    }
    
}
