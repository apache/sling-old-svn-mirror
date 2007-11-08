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
package org.apache.sling.jcr.resource;

import org.apache.sling.jcr.resource.JcrResourceUtil;

import junit.framework.TestCase;

public class JcrResourceUtilTest extends TestCase {

    public void testResolveRelativeSegments() {

        assertEquals("/", JcrResourceUtil.normalize("/"));
        assertEquals("/", JcrResourceUtil.normalize("///"));

        assertEquals("/a/b/c", JcrResourceUtil.normalize("/a//b/c"));
        assertEquals("/a/b/c", JcrResourceUtil.normalize("/a/b//c"));
        assertEquals("/a/b/c", JcrResourceUtil.normalize("/a///b///c"));
        assertEquals("/a/b/c", JcrResourceUtil.normalize("/a/b/c/"));
        assertEquals("/a/b/c", JcrResourceUtil.normalize("/a/b/c//"));
        assertEquals("/a/b/c", JcrResourceUtil.normalize("/a/b/c///"));

        assertEquals("/az/bz/cz", JcrResourceUtil.normalize("/az//bz/cz"));
        assertEquals("/az/bz/cz", JcrResourceUtil.normalize("/az/bz//cz"));
        assertEquals("/az/bz/cz", JcrResourceUtil.normalize("/az///bz///cz"));
        assertEquals("/az/bz/cz", JcrResourceUtil.normalize("/az/bz/cz/"));
        assertEquals("/az/bz/cz", JcrResourceUtil.normalize("/az/bz/cz//"));
        assertEquals("/az/bz/cz", JcrResourceUtil.normalize("/az/bz/cz///"));

        assertEquals("/a", JcrResourceUtil.normalize("/a"));
        assertEquals("/a", JcrResourceUtil.normalize("//a"));
        assertEquals("/a", JcrResourceUtil.normalize("///a"));

        assertEquals("/az", JcrResourceUtil.normalize("/az"));
        assertEquals("/az", JcrResourceUtil.normalize("//az"));
        assertEquals("/az", JcrResourceUtil.normalize("///az"));

        assertEquals("/", JcrResourceUtil.normalize("/."));
        assertEquals("/a", JcrResourceUtil.normalize("/a/."));
        assertEquals("/a", JcrResourceUtil.normalize("/./a"));
        assertEquals("/a/b", JcrResourceUtil.normalize("/a/./b"));
        assertEquals("/a/b", JcrResourceUtil.normalize("/a/b/."));
        assertEquals("/a/b", JcrResourceUtil.normalize("/a/./b/."));

        assertEquals("/", JcrResourceUtil.normalize("/."));
        assertEquals("/az", JcrResourceUtil.normalize("/az/."));
        assertEquals("/az", JcrResourceUtil.normalize("/./az"));
        assertEquals("/az/bz", JcrResourceUtil.normalize("/az/./bz"));
        assertEquals("/az/bz", JcrResourceUtil.normalize("/az/bz/."));
        assertEquals("/az/bz", JcrResourceUtil.normalize("/az/./bz/."));

        assertNull(JcrResourceUtil.normalize("/.."));
        assertNull(JcrResourceUtil.normalize("/.."));
        assertEquals("/", JcrResourceUtil.normalize("/a/.."));
        assertEquals("/a", JcrResourceUtil.normalize("/a/b/.."));
        assertEquals("/", JcrResourceUtil.normalize("/a/b/../.."));
        assertNull(JcrResourceUtil.normalize("/a/b/../../.."));

        assertNull(JcrResourceUtil.normalize("/.."));
        assertNull(JcrResourceUtil.normalize("/.."));
        assertEquals("/", JcrResourceUtil.normalize("/az/.."));
        assertEquals("/az", JcrResourceUtil.normalize("/az/bz/.."));
        assertEquals("/", JcrResourceUtil.normalize("/az/bz/../.."));
        assertNull(JcrResourceUtil.normalize("/az/bz/../../.."));

        assertEquals("/b", JcrResourceUtil.normalize("/a/../b"));
        assertEquals("/a/c", JcrResourceUtil.normalize("/a/b/../c"));
        assertEquals("/c", JcrResourceUtil.normalize("/a/b/../../c"));
        assertNull(JcrResourceUtil.normalize("/a/b/../../../c"));

        assertEquals("/bz", JcrResourceUtil.normalize("/az/../bz"));
        assertEquals("/az/cz", JcrResourceUtil.normalize("/az/bz/../cz"));
        assertEquals("/cz", JcrResourceUtil.normalize("/az/bz/../../cz"));
        assertNull(JcrResourceUtil.normalize("/az/bz/../../../cz"));

        assertEquals("/...", JcrResourceUtil.normalize("/..."));
        assertEquals("/a/...", JcrResourceUtil.normalize("/a/..."));
        assertEquals("/a/b/...", JcrResourceUtil.normalize("/a/b/..."));

        assertEquals("/az/...", JcrResourceUtil.normalize("/az/..."));
        assertEquals("/az/bz/...", JcrResourceUtil.normalize("/az/bz/..."));

        try {
            JcrResourceUtil.normalize(null);
            fail("Resolving null expects NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
    }

    public void testResolveRelativeSegmentsRelative() {
        assertEquals("a/b", JcrResourceUtil.normalize("a/b"));
        assertEquals("a", JcrResourceUtil.normalize("a/b/.."));

        assertEquals("b", JcrResourceUtil.normalize("a/../b"));
        assertEquals("a/c", JcrResourceUtil.normalize("a/b/../c"));
        assertEquals("c", JcrResourceUtil.normalize("a/b/../../c"));
        assertEquals("", JcrResourceUtil.normalize("a/b/../.."));
        assertEquals("a/c/d", JcrResourceUtil.normalize("a/b/../c/d"));
        assertNull(JcrResourceUtil.normalize("a/b/../../../c"));

        assertEquals("a/b/c", JcrResourceUtil.normalize("a/b/c"));
        assertEquals("az/bz/cz", JcrResourceUtil.normalize("az/bz/cz"));
        assertEquals("", JcrResourceUtil.normalize(""));
    }
}
