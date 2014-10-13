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
package org.apache.sling.testing.mock.jcr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ResourceUtilTest {

    @Test
    public void testResolveRelativeSegments() {

        assertEquals("/", ResourceUtil.normalize("/"));
        assertEquals("/", ResourceUtil.normalize("///"));

        assertEquals("/a/b/c", ResourceUtil.normalize("/a//b/c"));
        assertEquals("/a/b/c", ResourceUtil.normalize("/a/b//c"));
        assertEquals("/a/b/c", ResourceUtil.normalize("/a///b///c"));
        assertEquals("/a/b/c", ResourceUtil.normalize("/a/b/c/"));
        assertEquals("/a/b/c", ResourceUtil.normalize("/a/b/c//"));
        assertEquals("/a/b/c", ResourceUtil.normalize("/a/b/c///"));

        assertEquals("/az/bz/cz", ResourceUtil.normalize("/az//bz/cz"));
        assertEquals("/az/bz/cz", ResourceUtil.normalize("/az/bz//cz"));
        assertEquals("/az/bz/cz", ResourceUtil.normalize("/az///bz///cz"));
        assertEquals("/az/bz/cz", ResourceUtil.normalize("/az/bz/cz/"));
        assertEquals("/az/bz/cz", ResourceUtil.normalize("/az/bz/cz//"));
        assertEquals("/az/bz/cz", ResourceUtil.normalize("/az/bz/cz///"));

        assertEquals("/a", ResourceUtil.normalize("/a"));
        assertEquals("/a", ResourceUtil.normalize("//a"));
        assertEquals("/a", ResourceUtil.normalize("///a"));

        assertEquals("/az", ResourceUtil.normalize("/az"));
        assertEquals("/az", ResourceUtil.normalize("//az"));
        assertEquals("/az", ResourceUtil.normalize("///az"));

        assertEquals("/", ResourceUtil.normalize("/."));
        assertEquals("/a", ResourceUtil.normalize("/a/."));
        assertEquals("/a", ResourceUtil.normalize("/./a"));
        assertEquals("/a/b", ResourceUtil.normalize("/a/./b"));
        assertEquals("/a/b", ResourceUtil.normalize("/a/b/."));
        assertEquals("/a/b", ResourceUtil.normalize("/a/./b/."));

        assertEquals("/", ResourceUtil.normalize("/."));
        assertEquals("/az", ResourceUtil.normalize("/az/."));
        assertEquals("/az", ResourceUtil.normalize("/./az"));
        assertEquals("/az/bz", ResourceUtil.normalize("/az/./bz"));
        assertEquals("/az/bz", ResourceUtil.normalize("/az/bz/."));
        assertEquals("/az/bz", ResourceUtil.normalize("/az/./bz/."));

        assertNull(ResourceUtil.normalize("/.."));
        assertNull(ResourceUtil.normalize("/.."));
        assertEquals("/", ResourceUtil.normalize("/a/.."));
        assertEquals("/a", ResourceUtil.normalize("/a/b/.."));
        assertEquals("/", ResourceUtil.normalize("/a/b/../.."));
        assertNull(ResourceUtil.normalize("/a/b/../../.."));

        assertNull(ResourceUtil.normalize("/.."));
        assertNull(ResourceUtil.normalize("/.."));
        assertEquals("/", ResourceUtil.normalize("/az/.."));
        assertEquals("/az", ResourceUtil.normalize("/az/bz/.."));
        assertEquals("/", ResourceUtil.normalize("/az/bz/../.."));
        assertNull(ResourceUtil.normalize("/az/bz/../../.."));

        assertEquals("/b", ResourceUtil.normalize("/a/../b"));
        assertEquals("/a/c", ResourceUtil.normalize("/a/b/../c"));
        assertEquals("/c", ResourceUtil.normalize("/a/b/../../c"));
        assertNull(ResourceUtil.normalize("/a/b/../../../c"));

        assertEquals("/bz", ResourceUtil.normalize("/az/../bz"));
        assertEquals("/az/cz", ResourceUtil.normalize("/az/bz/../cz"));
        assertEquals("/cz", ResourceUtil.normalize("/az/bz/../../cz"));
        assertNull(ResourceUtil.normalize("/az/bz/../../../cz"));

        assertEquals("/...", ResourceUtil.normalize("/..."));
        assertEquals("/a/...", ResourceUtil.normalize("/a/..."));
        assertEquals("/a/b/...", ResourceUtil.normalize("/a/b/..."));

        assertEquals("/az/...", ResourceUtil.normalize("/az/..."));
        assertEquals("/az/bz/...", ResourceUtil.normalize("/az/bz/..."));

        try {
            ResourceUtil.normalize(null);
            fail("Resolving null expects NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
    }

    @Test
    public void testResolveRelativeSegmentsRelative() {
        assertEquals("a/b", ResourceUtil.normalize("a/b"));
        assertEquals("a", ResourceUtil.normalize("a/b/.."));

        assertEquals("b", ResourceUtil.normalize("a/../b"));
        assertEquals("a/c", ResourceUtil.normalize("a/b/../c"));
        assertEquals("c", ResourceUtil.normalize("a/b/../../c"));
        assertEquals("", ResourceUtil.normalize("a/b/../.."));
        assertEquals("a/c/d", ResourceUtil.normalize("a/b/../c/d"));
        assertNull(ResourceUtil.normalize("a/b/../../../c"));

        assertEquals("a/b/c", ResourceUtil.normalize("a/b/c"));
        assertEquals("az/bz/cz", ResourceUtil.normalize("az/bz/cz"));
        assertEquals("", ResourceUtil.normalize(""));
    }

    @Test
    public void testGetParent() {
        assertNull(ResourceUtil.getParent("/"));
        assertNull(ResourceUtil.getParent("/.."));

        assertEquals("/", ResourceUtil.getParent("/b"));
        assertEquals("b/c", ResourceUtil.getParent("b/c/d"));
        assertEquals("/b/c", ResourceUtil.getParent("/b/c/d"));

        assertNull(ResourceUtil.getParent("b"));
        assertNull(ResourceUtil.getParent("/b/.."));

        assertEquals("security:/", ResourceUtil.getParent("security:/b"));
        assertEquals("security:/b", ResourceUtil.getParent("security:/b/c"));
        assertEquals("security:/b/c", ResourceUtil.getParent("security:/b/c/d"));
    }

    @Test
    public void testGetName() {
        assertEquals("", ResourceUtil.getName("/"));
        assertEquals("", ResourceUtil.getName("/a/.."));

        assertEquals("c", ResourceUtil.getName("c"));
        assertEquals("c", ResourceUtil.getName("/c"));

        assertEquals("c", ResourceUtil.getName("b/c"));
        assertEquals("c", ResourceUtil.getName("/b/c"));

        assertEquals("c", ResourceUtil.getName("b/c/"));
        assertEquals("c", ResourceUtil.getName("/b/c/"));

        assertEquals("b", ResourceUtil.getName("b/c/.."));
        assertEquals("b", ResourceUtil.getName("/b/c/.."));
        assertEquals("", ResourceUtil.getName("/b/c/../.."));
    }

    @Test
    public void testGetParentLevel() throws Exception {
        boolean caughtNullPointerException = false;
        try {
            ResourceUtil.getParent(null, 4);
        } catch (NullPointerException e) {
            // Expected exception
            caughtNullPointerException = true;
        } catch (Exception e) {
            fail("Expected NullPointerException, but caught " + e.getClass().getName() + " instead.");
        }
        if (!caughtNullPointerException) {
            fail("Expected NullPointerException, but no exception was thrown.");
        }

        boolean caughtIllegalArgumentException = false;
        try {
            ResourceUtil.getParent("/a/b", -2);
        } catch (IllegalArgumentException e) {
            // Expected exception
            caughtIllegalArgumentException = true;
        } catch (Exception e) {
            fail("Expected IllegalArgumentException, but caught " + e.getClass().getName() + " instead.");
        }
        if (!caughtIllegalArgumentException) {
            fail("Expected IllegalArgumentException, but no exception was thrown.");
        }

        assertNull(ResourceUtil.getParent("/a", 4));
        assertNull(ResourceUtil.getParent("/", 1));
        assertNull(ResourceUtil.getParent("b/c", 2));
        assertNull(ResourceUtil.getParent("/b/..", 1));
        assertNull(ResourceUtil.getParent("b", 1));
        assertNull(ResourceUtil.getParent("", 3));
        assertNull(ResourceUtil.getParent("/..", 1));
        assertNull(ResourceUtil.getParent("security:/b", 2));
        assertNull(ResourceUtil.getParent("/b///", 2));

        assertEquals("", ResourceUtil.getParent("", 0));
        assertEquals("b", ResourceUtil.getParent("b", 0));
        assertEquals("/", ResourceUtil.getParent("/", 0));
        assertEquals("/a/b", ResourceUtil.getParent("/a/b", 0));
        assertEquals("security:/b", ResourceUtil.getParent("security:/b", 0));

        assertEquals("/", ResourceUtil.getParent("/b", 1));
        assertEquals("b", ResourceUtil.getParent("b/c", 1));
        assertEquals("b/c", ResourceUtil.getParent("b/c/d", 1));
        assertEquals("/b/c", ResourceUtil.getParent("/b/c/d", 1));
        assertEquals("security:/", ResourceUtil.getParent("security:/b", 1));
        assertEquals("security:/b", ResourceUtil.getParent("security:/b/c", 1));
        assertEquals("security:/b/c", ResourceUtil.getParent("security:/b/c/d", 1));

        assertEquals("b", ResourceUtil.getParent("b/c/d", 2));
        assertEquals("b/c", ResourceUtil.getParent("b/c/d/e", 2));
        assertEquals("/", ResourceUtil.getParent("/b/c/d", 3));
        assertEquals("/", ResourceUtil.getParent("/b///", 1));
    }

}
