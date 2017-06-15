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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.Test;

public class PathTest {

    private void assertMatchOrNot(boolean expected, Path p, String ... pathOrPattern) {
        for(String s : pathOrPattern) {
            final boolean actual = p.matches(s);
            if(actual != expected) {
                fail("Expected "
                        + (expected ? "match" : "no match")
                        + " between " + p.getPath() + " and " + s
                        + " but got match=" + actual
                );
            }
        }
    }

    private void assertMatch(Path p, String ... pathOrPattern) {
        assertMatchOrNot(true, p, pathOrPattern);
    }

    private void assertNoMatch(Path p, String ... pathOrPattern) {
        assertMatchOrNot(false, p, pathOrPattern);
    }

    @Test public void testRootMatching() {
        final Path p = new Path("/");

        assertMatch(p,
                "/",
                "/foo",
                "/foo/bar"
        );
    }

    @Test public void testPathMatching() {
        final Path p  = new Path("/content");
        assertNoMatch(p,
                "/",
                "/foo",
                "/foo/bar",
                "/content1"
        );

        assertMatch(p,
                "/content",
                "/content/a",
                "/content/a/b"
        );
    }

    @Test public void testPatternMatchingA() {
        final Path p = new Path("glob:/apps/**/*.html");

        assertMatch(p,
                "/apps/project/a.html",
                "/apps/project/1/a.html",
                "/apps/project/1/2/a.html",
                "/apps/project/1/2/3/4/5/6/7/8/9/a.html"
        );

        assertNoMatch(p,
                "/apps/a.html",
                "/apps/project/a.html/b"
        );
    }

    @Test public void testPatternMatchingB() {
        final Path p = new Path("glob:/apps/*.html");
        assertMatch(p, "/apps/a.html");
        assertNoMatch(p, "/apps/a/a.html");
    }

    @Test public void testPatternMatchingC() {
        final Path p = new Path("glob:/a/m-p/$structure/**/[cmp]/*.html");

        assertMatch(p,
                "/a/m-p/$structure/1/2/3/[cmp]/test.html",
                "/a/m-p/$structure/1/2/3/4/5/6/[cmp]/test.html",
                "/a/m-p/$structure/1/[cmp]/test.html",
                "/a/m-p/$structure/1/[cmp]/te-[s]t$.html",
                "/a/m-p/$structure/1/[cmp]/.html");

        assertNoMatch(p,
                "/a/m-p/$structure/1/[cmp]/html",
                "/a/m-p/$structure/[cmp]/test.html"
        );
    }

    @Test public void testPatternRootMatching() {
        final Path p = new Path("/");
        assertMatch(p,
                "glob:/apps/myproject/components/**/*.html",
                "glob:/apps/**/*.html"
        );
    }

    @Test public void testMatchesWithGlobPattern() {
        final Path p = new Path("/apps/myproject");
        assertMatch(p,
                "glob:/apps/myproject/components/**/*.html",
                "glob:/apps/**/components/**/*.html",
                "glob:/*/myproject",
                "glob:/*/*project",
                "glob:/*/*project/**.html",
                "glob:/**/myproject",
                "glob:/**/myproject/*.jsp",
                "glob:/**",
                "glob:/**/*.jsp"
        );

        assertNoMatch(p,
                "glob:/*/foo"
        );
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
        final Path p = new Path("/libs/");

        assertMatch(p,
                "/libs",
                "/libs/foo"
        );

        assertNoMatch(p,"/lib");
    }
    @Test public void testGlobPatternsMatch() {
        final Path p = new Path("glob:/*/foo");
        assertMatch(p, "glob:/*/foo");
        assertNoMatch(p, "glob:/*/**/foo");
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

    @Test public void testMorePathSlashesThenInGlobExp() {
        Path path = new Path("/libs/sling/resource-editor");
        assertTrue(path.matches("glob:/libs/**/*.jsp"));
        path = new Path("/libs/sling/resource-editor/sub-path");
        assertTrue(path.matches("glob:/libs/**/*.jsp"));
    }

    @Test public void testEnclosedPatternMatch() {
        Path path = new Path("/a/b/c");
        assertFalse(path.matches("glob:/a/*/d"));
        assertTrue(path.matches("glob:/a/**/d"));
        assertTrue(path.matches("glob:/a/b/*"));
        assertTrue(path.matches("glob:/a/b/*/d"));
    }
}