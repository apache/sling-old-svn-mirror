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
package org.apache.sling.microsling.scripting.helpers;

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;

/** Test the ScriptSearchPathsBuilder class */
public class ScriptSearchPathsBuilderTest extends TestCase {
    
    private final ScriptSearchPathsBuilder builder = new ScriptSearchPathsBuilder();
    
    private void testBuilder(String resourceType,String [] selectors, String [] paths) throws SlingException {
        final Resource r = new MockResource(resourceType);
        
        final List<String> actual = builder.getScriptSearchPaths(r, selectors);
        
        final List<String> expected = new LinkedList<String>();
        if(paths != null) {
            for(String path : paths) {
                expected.add(path);
            }
        }
        assertEquals(expected,actual);
    }
    
    public void testNoSelectorsA() throws SlingException {
        final String [] selectors = null;
        final String [] expected = { "/sling/scripts/rt" }; 
        testBuilder("rt", selectors, expected);
    }
    
    public void testNoSelectorsB() throws SlingException {
        final String [] selectors = null;
        final String [] expected = { "/sling/scripts/rt/something" }; 
        testBuilder("rt/something", selectors, expected);
    }
    
    public void testWithSelectorsA() throws SlingException {
        final String [] selectors = { "a4" };
        final String [] expected = { "/sling/scripts/rt/a4", "/sling/scripts/rt" }; 
        testBuilder("rt", selectors, expected);
    }
    
    public void testWithSelectorsB() throws SlingException {
        final String [] selectors = { "a4", "print" };
        final String [] expected = { "/sling/scripts/rt/x/a4/print", "/sling/scripts/rt/x/a4", "/sling/scripts/rt/x" }; 
        testBuilder("rt/x", selectors, expected);
    }
    
    static class MockResource implements Resource {

        private final String resourceType;
        
        MockResource(String resourceType) {
            this.resourceType = resourceType;
        }

        public String getResourceType() {
            return resourceType;
        }

        public String getURI() {
            throw new Error("MockResource does not implement this method");
        }

        public ResourceMetadata getResourceMetadata() {
            throw new Error("MockResource does not implement this method");
        }
        
        public <Type> Type adaptTo(Class<Type> type) {
            throw new Error("MockResource does not implement this method");
        }
    }
}

