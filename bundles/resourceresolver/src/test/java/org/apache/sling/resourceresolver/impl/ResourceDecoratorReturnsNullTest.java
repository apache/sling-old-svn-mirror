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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;

/** Verify what happens when ResourceDecorator returns null */
public class ResourceDecoratorReturnsNullTest extends ResourceDecoratorTestBase {

    private Set<String> pathsThatReturnNull = new HashSet<String>();
    
    /** Return null for resources that have a path in pathsThatReturnNull.
     *  Will cause our test ResourceDecorator to return null for these resources
     */
    protected Resource wrapResourceForTest(Resource r) {
        return isReturnNull(r) ? null : r;
    }
    
    private boolean isReturnNull(Resource r) {
        return pathsThatReturnNull.contains(r.getPath());
    }
    
    private void assertResources(Iterator<Resource> it, String ...paths) {
        assertNotNull("Expecting non-null Iterator", it);
        final List<String> actual = new ArrayList<String>();
        while(it.hasNext()) {
            final Resource r = it.next();
            assertNotNull("Expecting no null Resources in iterator", r);
            actual.add(r.getPath());
        }
        for(String path : paths) {
            assertTrue("Expecting path " + path + " in " + actual, actual.contains(path));
        }
        if(actual.size() != paths.length) {
            fail("Expecting the same number of items in " + Arrays.asList(paths) + " and " + actual);
        }
    }
    
    @Before
    public void setup() {
        super.setup();
        pathsThatReturnNull.add("/tmp/D");
        pathsThatReturnNull.add("/var/two");
    }
    
    @Test
    public void testResolveNotNull() {
        assertExistent(resolver.resolve("/tmp/A"), true);
    }
    
    public void testGetNotNull() {
        assertExistent(resolver.getResource("/tmp/A"), true);
    }
    
    @Test
    public void testGetNull() {
        assertExistent(resolver.getResource("/tmp/D"), true);
    }
    
    @Test
    public void testResolveNull() {
        assertExistent(resolver.resolve("/tmp/D"), true);
    }
    
    @Test
    public void testRootChildren() {
        final Resource root = resolver.resolve("/");
        assertNotNull(root);
        assertResources(resolver.listChildren(root), "/tmp", "/var");
    }
    
    @Test
    public void testVarChildren() {
        final Resource var = resolver.resolve("/var");
        assertNotNull(var);
        assertResources(resolver.listChildren(var), "/var/one", "/var/two", "/var/three");
    }
    
    @Test
    public void testFind() {
        assertResources(resolver.findResources("foo", QUERY_LANGUAGE), "/tmp/C", "/tmp/D", "/var/one", "/var/two");
    }
}