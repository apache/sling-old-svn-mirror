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

import org.apache.sling.api.resource.path.Path;
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
    }
}
