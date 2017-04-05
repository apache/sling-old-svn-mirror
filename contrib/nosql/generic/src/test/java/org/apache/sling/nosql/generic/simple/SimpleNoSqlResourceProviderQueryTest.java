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
package org.apache.sling.nosql.generic.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.nosql.generic.simple.provider.SimpleNoSqlResourceProviderFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test basic ResourceResolver and ValueMap with different data types.
 */
public class SimpleNoSqlResourceProviderQueryTest {
    
    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);
    
    private Resource testRoot;

    @Before
    public void setUp() throws Exception {
        context.registerInjectActivateService(new SimpleNoSqlResourceProviderFactory(), ImmutableMap.<String, Object>builder()
                .put(ResourceProvider.ROOTS, "/nosql-simple")
                .build());
        
        // prepare some test data using Sling CRUD API
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        final Resource root = context.resourceResolver().getResource("/");
        Resource noSqlRoot = context.resourceResolver().create(root, "nosql-simple", props);
        this.testRoot = context.resourceResolver().create(noSqlRoot, "test", props);
        
        context.resourceResolver().create(testRoot, "node1", ImmutableMap.<String, Object>of("prop1", "value1"));
        context.resourceResolver().create(testRoot, "node2", ImmutableMap.<String, Object>of("prop1", "value2"));
        
        context.resourceResolver().commit();
    }

    @Test
    public void testFindResources_ValidQuery() {
        Iterator<Resource> result = context.resourceResolver().findResources("all", "simple");
        assertEquals("/nosql-simple", result.next().getPath());
        assertEquals("/nosql-simple/test", result.next().getPath());
        assertEquals("/nosql-simple/test/node1", result.next().getPath());
        assertEquals("/nosql-simple/test/node2", result.next().getPath());
        assertFalse(result.hasNext());
    }

    @Test
    public void testFindResources_InvalidQuery() {
        Iterator<Resource> result = context.resourceResolver().findResources("all", "invalid");
        assertFalse(result.hasNext());
    }

    @Test
    public void testQueryResources_ValidQuery() {
        Iterator<Map<String, Object>> result = context.resourceResolver().queryResources("all", "simple");
        assertNull(result.next().get("prop1"));
        assertNull(result.next().get("prop1"));
        assertEquals("value1", result.next().get("prop1"));
        assertEquals("value2", result.next().get("prop1"));
        assertFalse(result.hasNext());
    }

    @Test
    public void testQueryResources_InvalidQuery() {
        Iterator<Map<String, Object>> result = context.resourceResolver().queryResources("all", "invalid");
        assertFalse(result.hasNext());
    }

}
