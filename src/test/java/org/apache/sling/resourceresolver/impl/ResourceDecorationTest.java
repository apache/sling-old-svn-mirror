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
package org.apache.sling.resourceresolver.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.junit.Test;

/** Test ResourceDecorator handling in ResourceResolverImpl */
public class ResourceDecorationTest extends ResourceDecoratorTestBase {

    private static final String DECORATED_NAME = "decorated";
    
    /** Wrap any resource so that its name is DECORATED_NAME */
    protected Resource wrapResourceForTest(Resource resource) {
        return new ResourceWrapper(resource) {
            @Override
            public String getName() {
                return DECORATED_NAME;
            }
        };
    }
    
    private void assertDecorated(Resource r) {
        assertEquals("Expecting " + r + " to be decorated", DECORATED_NAME, r.getName());
    }
    
    private void assertDecorated(Iterator<Resource> it, int expectedCount) {
        assertNotNull(it);
        assertTrue("Expecting non-empty Iterator", it.hasNext());
        int count = 0;
        while(it.hasNext()) {
            count++;
            assertDecorated(it.next());
        }
        assertEquals("Expecting " + expectedCount + " items in Iterator", expectedCount, count);
    }
    
    @Test
    public void resolveRootIsDecorated() {
        final Resource r = resolver.resolve((String)null); 
        assertDecorated(r);
        assertExistent(r, true);
    }
    
    @Test
    public void getRootIsDecorated() {
        final Resource r = resolver.getResource("/"); 
        assertDecorated(r);
        assertExistent(r, true);
    }
    
    @Test
    public void getNonExistingIsNull() {
        assertNull(resolver.getResource("/non-existing/something")); 
    }
    
    @Test
    public void existentIsDecorated() {
        final Resource r = resolver.resolve("/tmp/C");
        assertDecorated(r);
        assertExistent(r, true);
    }
    
    @Test
    public void NonExistentIsDecorated() {
        final Resource r = resolver.resolve("/foo");
        assertDecorated(r);
        assertExistent(r, false);
    }
    
    @Test
    public void childrenAreDecorated() {
        final Resource root = resolver.resolve((String)null);
        final Iterator<Resource> it = resolver.listChildren(root);
        assertTrue("Expecting root to have children", it.hasNext());
        assertDecorated(it, 2);
    }
    
    @Test
    public void listChildrenDecorates() {
        final Resource testVar = resolver.resolve("/var");
        assertDecorated(resolver.listChildren(testVar), 3);
    }
    
    @Test
    public void findDecorates() {
        assertDecorated(resolver.findResources("foo", QUERY_LANGUAGE), 4);
    }
}