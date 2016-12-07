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

import java.util.UUID;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class PathTest {

    @Test public void testRootMatching() {
        final Path path = new Path("/");

        assertTrue(path.matches("/"));
        assertTrue(path.matches("/foo"));
        assertTrue(path.matches("/foo/bar"));
    }

    @Test public void testPathMatching() {
        final Path path = new Path("/content");

        assertFalse(path.matches("/"));
        assertFalse(path.matches("/foo"));
        assertFalse(path.matches("/foo/bar"));

        assertTrue(path.matches("/content"));
        assertTrue(path.matches("/content/a"));
        assertTrue(path.matches("/content/a/b"));

        assertFalse(path.matches("/content1"));
    }

    @Test public void testPatternMatching() {
        final Path path_1 = new Path("glob:/apps/**/*.html");
        assertTrue(path_1.matches("/apps/project/a.html"));
        assertTrue(path_1.matches("/apps/project/1/a.html"));
        assertTrue(path_1.matches("/apps/project/1/2/a.html"));
        assertFalse(path_1.matches("/apps/a.html"));
        assertFalse(path_1.matches("/apps/project/a.html/b"));

        final Path path_2 = new Path("glob:/apps/*.html");
        assertTrue(path_2.matches("/apps/a.html"));
        assertFalse(path_2.matches("/apps/a/a.html"));

        final Path path_3 = new Path("glob:/a/m-p/$structure/**/[cmp]/*.html");
        assertTrue(path_3.matches("/a/m-p/$structure/1/2/3/[cmp]/test.html"));
        assertTrue(path_3.matches("/a/m-p/$structure/1/[cmp]/test.html"));
        assertTrue(path_3.matches("/a/m-p/$structure/1/[cmp]/te-[s]t$.html"));
        assertTrue(path_3.matches("/a/m-p/$structure/1/[cmp]/.html"));
        assertFalse(path_3.matches("/a/m-p/$structure/1/[cmp]/html"));
        assertFalse(path_3.matches("/a/m-p/$structure/[cmp]/test.html"));
    }

    @Test public void testPatternRootMatching() {
        final Path path = new Path("/");
        assertTrue(path.matches("glob:/apps/myproject/components/**/*.html"));
        assertTrue(path.matches("glob:/apps/**/*.html"));
    }

    @Test public void testMatchesWithGlobPattern() {
        final Path path = new Path("/apps/myproject");
        assertTrue(path.matches("glob:/apps/myproject/components/**/*.html"));
        assertTrue(path.matches("glob:/apps/**/components/**/*.html"));
        assertFalse(path.matches("glob:/*/foo"));
        assertTrue(path.matches("glob:/*/myproject"));
        assertTrue(path.matches("glob:/*/*project"));
        assertTrue(path.matches("glob:/*/*project/**.html"));
    }

    @Test public void testIllegalArgumentException() {
        final Path path = new Path("/libs/foo");
        
        try {
            new Path("foo");
            fail();
        } catch ( final IllegalArgumentException iae) {
            // this should happen!
        }
        try {
            new Path("glob:foo");
            fail();
        } catch ( final IllegalArgumentException iae) {
            // this should happen!
        }
        
        try {
            path.matches("invalid-no-slash");
            fail();
        } catch ( final IllegalArgumentException iae) {
            // this should happen!
        }
    }

    @Test public void testPathMatchingTrailingSlash() {
        final Path path = new Path("/libs/");
        assertTrue(path.matches("/libs"));
        assertTrue(path.matches("/libs/foo"));
        assertFalse(path.matches("/lib"));
    }
    @Test public void testGlobPatternsMatch() {
        final Path glob = new Path("glob:/*/foo");
        assertTrue(glob.matches("glob:/*/foo"));
        assertFalse(glob.matches("glob:/*/**/foo"));
    }
    
    @Test public void testIsPatternWithGlob() {
        assertTrue(new Path("glob:/*/foo").isPattern());
    }
    
    @Test public void testIsPatternWithPath() {
        assertFalse(new Path("/libs/foo").isPattern());
    }
    
    @Test public void testEquals() {
        final Path path = new Path("/foo/bar");
        assertTrue(path.equals(path));
        assertFalse(path.equals(null));
        assertFalse(path.equals("a string"));
    }
    
    @Test public void testCompareTo() {
        final Path p1 = new Path("/1");
        final Path p2 = new Path("/2");
        assertTrue(p1.compareTo(p2) < 0);
        assertTrue(p2.compareTo(p1) > 0);
        assertTrue(p1.compareTo(p1) == 0);
    }
    
    @Test public void testToString() {
        final Path path = new Path("/" + UUID.randomUUID());
        assertTrue(path.toString().contains(path.getPath()));
    }
}
