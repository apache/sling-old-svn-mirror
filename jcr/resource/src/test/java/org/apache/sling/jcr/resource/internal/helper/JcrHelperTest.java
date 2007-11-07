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
package org.apache.sling.jcr.resource.internal.helper;

import junit.framework.TestCase;

public class JcrHelperTest extends TestCase {

    public void testResolveRelativeSegments() {

        assertEquals("/", JcrHelper.resolveRelativeSegments("/"));
        assertEquals("/", JcrHelper.resolveRelativeSegments("///"));

        assertEquals("/a/b/c", JcrHelper.resolveRelativeSegments("/a//b/c"));
        assertEquals("/a/b/c", JcrHelper.resolveRelativeSegments("/a/b//c"));
        assertEquals("/a/b/c", JcrHelper.resolveRelativeSegments("/a///b///c"));
        assertEquals("/a/b/c", JcrHelper.resolveRelativeSegments("/a/b/c/"));
        assertEquals("/a/b/c", JcrHelper.resolveRelativeSegments("/a/b/c//"));
        assertEquals("/a/b/c", JcrHelper.resolveRelativeSegments("/a/b/c///"));

        assertEquals("/az/bz/cz", JcrHelper.resolveRelativeSegments("/az//bz/cz"));
        assertEquals("/az/bz/cz", JcrHelper.resolveRelativeSegments("/az/bz//cz"));
        assertEquals("/az/bz/cz", JcrHelper.resolveRelativeSegments("/az///bz///cz"));
        assertEquals("/az/bz/cz", JcrHelper.resolveRelativeSegments("/az/bz/cz/"));
        assertEquals("/az/bz/cz", JcrHelper.resolveRelativeSegments("/az/bz/cz//"));
        assertEquals("/az/bz/cz", JcrHelper.resolveRelativeSegments("/az/bz/cz///"));

        assertEquals("/a", JcrHelper.resolveRelativeSegments("/a"));
        assertEquals("/a", JcrHelper.resolveRelativeSegments("//a"));
        assertEquals("/a", JcrHelper.resolveRelativeSegments("///a"));

        assertEquals("/az", JcrHelper.resolveRelativeSegments("/az"));
        assertEquals("/az", JcrHelper.resolveRelativeSegments("//az"));
        assertEquals("/az", JcrHelper.resolveRelativeSegments("///az"));

        assertEquals("/", JcrHelper.resolveRelativeSegments("/."));
        assertEquals("/a", JcrHelper.resolveRelativeSegments("/a/."));
        assertEquals("/a", JcrHelper.resolveRelativeSegments("/./a"));
        assertEquals("/a/b", JcrHelper.resolveRelativeSegments("/a/./b"));
        assertEquals("/a/b", JcrHelper.resolveRelativeSegments("/a/b/."));
        assertEquals("/a/b", JcrHelper.resolveRelativeSegments("/a/./b/."));

        assertEquals("/", JcrHelper.resolveRelativeSegments("/."));
        assertEquals("/az", JcrHelper.resolveRelativeSegments("/az/."));
        assertEquals("/az", JcrHelper.resolveRelativeSegments("/./az"));
        assertEquals("/az/bz", JcrHelper.resolveRelativeSegments("/az/./bz"));
        assertEquals("/az/bz", JcrHelper.resolveRelativeSegments("/az/bz/."));
        assertEquals("/az/bz", JcrHelper.resolveRelativeSegments("/az/./bz/."));

        assertNull(JcrHelper.resolveRelativeSegments("/.."));
        assertNull(JcrHelper.resolveRelativeSegments("/.."));
        assertEquals("/", JcrHelper.resolveRelativeSegments("/a/.."));
        assertEquals("/a", JcrHelper.resolveRelativeSegments("/a/b/.."));
        assertEquals("/", JcrHelper.resolveRelativeSegments("/a/b/../.."));
        assertNull(JcrHelper.resolveRelativeSegments("/a/b/../../.."));

        assertNull(JcrHelper.resolveRelativeSegments("/.."));
        assertNull(JcrHelper.resolveRelativeSegments("/.."));
        assertEquals("/", JcrHelper.resolveRelativeSegments("/az/.."));
        assertEquals("/az", JcrHelper.resolveRelativeSegments("/az/bz/.."));
        assertEquals("/", JcrHelper.resolveRelativeSegments("/az/bz/../.."));
        assertNull(JcrHelper.resolveRelativeSegments("/az/bz/../../.."));

        assertEquals("/b", JcrHelper.resolveRelativeSegments("/a/../b"));
        assertEquals("/a/c", JcrHelper.resolveRelativeSegments("/a/b/../c"));
        assertEquals("/c", JcrHelper.resolveRelativeSegments("/a/b/../../c"));
        assertNull(JcrHelper.resolveRelativeSegments("/a/b/../../../c"));

        assertEquals("/bz", JcrHelper.resolveRelativeSegments("/az/../bz"));
        assertEquals("/az/cz", JcrHelper.resolveRelativeSegments("/az/bz/../cz"));
        assertEquals("/cz", JcrHelper.resolveRelativeSegments("/az/bz/../../cz"));
        assertNull(JcrHelper.resolveRelativeSegments("/az/bz/../../../cz"));

        assertEquals("/...", JcrHelper.resolveRelativeSegments("/..."));
        assertEquals("/a/...", JcrHelper.resolveRelativeSegments("/a/..."));
        assertEquals("/a/b/...", JcrHelper.resolveRelativeSegments("/a/b/..."));

        assertEquals("/az/...", JcrHelper.resolveRelativeSegments("/az/..."));
        assertEquals("/az/bz/...", JcrHelper.resolveRelativeSegments("/az/bz/..."));

        try {
            JcrHelper.resolveRelativeSegments(null);
            fail("Resolving null expects NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
    }

    public void testResolveRelativeSegmentsRelative() {
        assertEquals("a/b", JcrHelper.resolveRelativeSegments("a/b"));
        assertEquals("a", JcrHelper.resolveRelativeSegments("a/b/.."));

        assertEquals("b", JcrHelper.resolveRelativeSegments("a/../b"));
        assertEquals("a/c", JcrHelper.resolveRelativeSegments("a/b/../c"));
        assertEquals("c", JcrHelper.resolveRelativeSegments("a/b/../../c"));
        assertEquals("", JcrHelper.resolveRelativeSegments("a/b/../.."));
        assertEquals("a/c/d", JcrHelper.resolveRelativeSegments("a/b/../c/d"));
        assertNull(JcrHelper.resolveRelativeSegments("a/b/../../../c"));

        assertEquals("a/b/c", JcrHelper.resolveRelativeSegments("a/b/c"));
        assertEquals("az/bz/cz", JcrHelper.resolveRelativeSegments("az/bz/cz"));
        assertEquals("", JcrHelper.resolveRelativeSegments(""));
    }
}
