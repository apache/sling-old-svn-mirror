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
package org.apache.sling.api.resource.path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import org.apache.sling.api.resource.path.Path;
import org.apache.sling.api.resource.path.PathSet;
import org.junit.Test;

public class PathSetTest {

    private void assertEqualSets(final PathSet set, final String...paths) {
        final Set<String> stringSet = new HashSet<String>();
        for(final String p : paths) {
            stringSet.add(p);
        }
        final Set<Path> pathSet = new HashSet<Path>();
        for(final Path p : set) {
            pathSet.add(p);
        }
        assertEquals(stringSet.size(), pathSet.size());
        for(final Path p : pathSet) {
            assertTrue(stringSet.contains(p.getPath()));
        }
    }

    @Test public void testStringSet() {
        final Set<String> strings = new HashSet<String>();
        strings.add("/a");
        strings.add("/x/y");
        strings.add("/z");
        final PathSet set = PathSet.fromStringCollection(strings);
        assertNotNull(set);
        assertEqualSets(set, "/a", "/x/y", "/z");
    }

    @Test public void testStrings() {
        final PathSet set = PathSet.fromStrings("/z", "/x/y", "/a");
        assertNotNull(set);
        assertEqualSets(set, "/a", "/x/y", "/z");
    }

    @Test public void testPathSet() {
        final Set<Path> paths = new HashSet<Path>();
        paths.add(new Path("/a"));
        paths.add(new Path("/x/y"));
        paths.add(new Path("/z"));
        final PathSet set = PathSet.fromPathCollection(paths);
        assertNotNull(set);
        assertEqualSets(set, "/a", "/x/y", "/z");
    }

    @Test public void testPaths() {
        final PathSet set = PathSet.fromPaths(new Path("/z"), new Path("/x/y"), new Path("/a"));
        assertNotNull(set);
        assertEqualSets(set, "/a", "/x/y", "/z");
    }

    @Test public void testOptimize() {
        final PathSet set1 = PathSet.fromStrings("/", "/a", "/z", "/x/y");
        assertEqualSets(set1, "/");

        final PathSet set2 = PathSet.fromStrings("/a", "/a/x/y", "/z", "/x/y", "/a2");
        assertEqualSets(set2, "/a", "/z", "/a2", "/x/y");
    }

    @Test public void testMatching() {
        final PathSet set = PathSet.fromStrings("/a", "/x/y");
        assertNull(set.matches("/"));

        assertEquals(new Path("/a"), set.matches("/a"));
        assertEquals(new Path("/a"), set.matches("/a/b"));
        assertEquals(new Path("/a"), set.matches("/a/b/c"));

        assertNull(set.matches("/a2"));

        assertNull(set.matches("/c"));
        assertNull(set.matches("/d/g"));

        assertNull(set.matches("/x"));
        assertNull(set.matches("/x/y2"));

        assertEquals(new Path("/x/y"), set.matches("/x/y"));
        assertEquals(new Path("/x/y"), set.matches("/x/y/1"));
        assertEquals(new Path("/x/y"), set.matches("/x/y/g/e"));
    }

    @Test public void testToStringSet() {
        final PathSet set = PathSet.fromStrings("/a", "/x/y");

        final Set<String> stringSet = set.toStringSet();
        assertEquals(2, stringSet.size());
        assertTrue(stringSet.contains("/a"));
        assertTrue(stringSet.contains("/x/y"));

        try {
            stringSet.add("foo");
            fail();
        } catch ( final UnsupportedOperationException uoe) {
            // expected
        }
    }
}
