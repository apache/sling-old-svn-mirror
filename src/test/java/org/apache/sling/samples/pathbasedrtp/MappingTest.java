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
package org.apache.sling.samples.pathbasedrtp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class MappingTest {
    
    @Test
    public void testSimpleMapping() {
        final Mapping m = new Mapping("/content:2");
        
        final String[][] testCases = {
                { "/content/foo", "nt:unstructured", "foo" },
                { "/content/foo/bar", "nt:unstructured", "foo" },
                { "/not/content/foo/bar", "nt:unstructured", null },
                { "/content/foo/bar", "nt:file",  null }
        };

        for(int i=0; i < testCases.length; i++) {
            final String [] tc = testCases[i];
            if(tc[2] == null) {
                assertNull("At index " + i, m.getResourceType(tc[0], tc[1]));
            } else {
                assertEquals("At index " + i, tc[2], m.getResourceType(tc[0], tc[1]));
            }
        }
    }
    
    @Test
    public void testLevel2() {
        final Mapping m = new Mapping("/content:3");
        assertEquals("bar1", m.getResourceType("/content/foo/bar1", "nt:unstructured"));
        assertEquals("bar2", m.getResourceType("/content/foo/bar2/wii", "nt:unstructured"));
        assertNull(m.getResourceType("/", "nt:unstructured"));
        assertNull(m.getResourceType("/content", "nt:unstructured"));
    }
    
    @Test
    public void testLevel3() {
        final Mapping m = new Mapping("/content:3");
        assertEquals("bar1", m.getResourceType("/content/foo/bar1", "nt:unstructured"));
        assertEquals("bar2", m.getResourceType("/content/foo/bar2/wii", "nt:unstructured"));
        assertEquals("bar3", m.getResourceType("/content/foo/bar3/wii2/xx", "nt:unstructured"));
        assertNull(m.getResourceType("/", "nt:unstructured"));
        assertNull(m.getResourceType("/content", "nt:unstructured"));
        assertNull(m.getResourceType("/content/foo", "nt:unstructured"));
    }
    
    @Test
    public void testNodetypeString() {
        final Mapping m = new Mapping("/content:2:nt:file");
        assertNotNull("nt:file must match", m.getResourceType("/content/foo1", "nt:file"));
        assertEquals("foo1", m.getResourceType("/content/foo1", "nt:file"));
        assertNull(m.getResourceType("/content/foo1", "nt:unstructured"));
        assertNull(m.getResourceType("/content/foo1", "some:type"));
    }

    @Test
    public void testNodetypeRegexp() {
        final Mapping m = new Mapping("/content:2:(nt:(file|test))");
        assertNotNull("nt:file must match", m.getResourceType("/content/foo1", "nt:file"));
        assertEquals("foo1", m.getResourceType("/content/foo1", "nt:file"));
        assertNotNull("nt:test must match", m.getResourceType("/content/foo1", "nt:test"));
        assertEquals("foo2", m.getResourceType("/content/foo2", "nt:test"));
        assertNull(m.getResourceType("/content/foo1", "nt:unstructured"));
        assertNull(m.getResourceType("/content/foo1", "some:type"));
    }
}