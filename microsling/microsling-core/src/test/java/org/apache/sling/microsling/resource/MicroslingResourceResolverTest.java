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
package org.apache.sling.microsling.resource;

import java.io.PrintWriter;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.SimpleLayout;

import junit.framework.TestCase;

public class MicroslingResourceResolverTest extends TestCase {

    private MicroslingResourceResolver resolver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        LogManager.resetConfiguration();
        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new PrintWriter(System.out, true));
        ca.setName("console");
        ca.setLayout(new SimpleLayout());
        ca.activateOptions();
        LogManager.getRootLogger().addAppender(ca);

        // may call with null, don't need repository for now
        resolver = new MicroslingResourceResolver(null);
    }

    public void testResolveRelativeSegments() {

        assertEquals("/", resolver.resolveRelativeSegments("/"));
        assertEquals("/", resolver.resolveRelativeSegments("///"));

        assertEquals("/a/b/c", resolver.resolveRelativeSegments("/a//b/c"));
        assertEquals("/a/b/c", resolver.resolveRelativeSegments("/a/b//c"));
        assertEquals("/a/b/c", resolver.resolveRelativeSegments("/a///b///c"));
        assertEquals("/a/b/c", resolver.resolveRelativeSegments("/a/b/c/"));
        assertEquals("/a/b/c", resolver.resolveRelativeSegments("/a/b/c//"));
        assertEquals("/a/b/c", resolver.resolveRelativeSegments("/a/b/c///"));

        assertEquals("/az/bz/cz", resolver.resolveRelativeSegments("/az//bz/cz"));
        assertEquals("/az/bz/cz", resolver.resolveRelativeSegments("/az/bz//cz"));
        assertEquals("/az/bz/cz", resolver.resolveRelativeSegments("/az///bz///cz"));
        assertEquals("/az/bz/cz", resolver.resolveRelativeSegments("/az/bz/cz/"));
        assertEquals("/az/bz/cz", resolver.resolveRelativeSegments("/az/bz/cz//"));
        assertEquals("/az/bz/cz", resolver.resolveRelativeSegments("/az/bz/cz///"));

        assertEquals("/a", resolver.resolveRelativeSegments("/a"));
        assertEquals("/a", resolver.resolveRelativeSegments("//a"));
        assertEquals("/a", resolver.resolveRelativeSegments("///a"));

        assertEquals("/az", resolver.resolveRelativeSegments("/az"));
        assertEquals("/az", resolver.resolveRelativeSegments("//az"));
        assertEquals("/az", resolver.resolveRelativeSegments("///az"));

        assertEquals("/", resolver.resolveRelativeSegments("/."));
        assertEquals("/a", resolver.resolveRelativeSegments("/a/."));
        assertEquals("/a", resolver.resolveRelativeSegments("/./a"));
        assertEquals("/a/b", resolver.resolveRelativeSegments("/a/./b"));
        assertEquals("/a/b", resolver.resolveRelativeSegments("/a/b/."));
        assertEquals("/a/b", resolver.resolveRelativeSegments("/a/./b/."));

        assertEquals("/", resolver.resolveRelativeSegments("/."));
        assertEquals("/az", resolver.resolveRelativeSegments("/az/."));
        assertEquals("/az", resolver.resolveRelativeSegments("/./az"));
        assertEquals("/az/bz", resolver.resolveRelativeSegments("/az/./bz"));
        assertEquals("/az/bz", resolver.resolveRelativeSegments("/az/bz/."));
        assertEquals("/az/bz", resolver.resolveRelativeSegments("/az/./bz/."));

        assertNull(resolver.resolveRelativeSegments("/.."));
        assertNull(resolver.resolveRelativeSegments("/.."));
        assertEquals("/", resolver.resolveRelativeSegments("/a/.."));
        assertEquals("/a", resolver.resolveRelativeSegments("/a/b/.."));
        assertEquals("/", resolver.resolveRelativeSegments("/a/b/../.."));
        assertNull(resolver.resolveRelativeSegments("/a/b/../../.."));

        assertNull(resolver.resolveRelativeSegments("/.."));
        assertNull(resolver.resolveRelativeSegments("/.."));
        assertEquals("/", resolver.resolveRelativeSegments("/az/.."));
        assertEquals("/az", resolver.resolveRelativeSegments("/az/bz/.."));
        assertEquals("/", resolver.resolveRelativeSegments("/az/bz/../.."));
        assertNull(resolver.resolveRelativeSegments("/az/bz/../../.."));

        assertEquals("/b", resolver.resolveRelativeSegments("/a/../b"));
        assertEquals("/a/c", resolver.resolveRelativeSegments("/a/b/../c"));
        assertEquals("/c", resolver.resolveRelativeSegments("/a/b/../../c"));
        assertNull(resolver.resolveRelativeSegments("/a/b/../../../c"));

        assertEquals("/bz", resolver.resolveRelativeSegments("/az/../bz"));
        assertEquals("/az/cz", resolver.resolveRelativeSegments("/az/bz/../cz"));
        assertEquals("/cz", resolver.resolveRelativeSegments("/az/bz/../../cz"));
        assertNull(resolver.resolveRelativeSegments("/az/bz/../../../cz"));

        assertEquals("/...", resolver.resolveRelativeSegments("/..."));
        assertEquals("/a/...", resolver.resolveRelativeSegments("/a/..."));
        assertEquals("/a/b/...", resolver.resolveRelativeSegments("/a/b/..."));

        assertEquals("/az/...", resolver.resolveRelativeSegments("/az/..."));
        assertEquals("/az/bz/...", resolver.resolveRelativeSegments("/az/bz/..."));

        try {
            resolver.resolveRelativeSegments(null);
            fail("Resolving null expects NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
    }

    public void testResolveRelativeSegmentsRelative() {
        assertEquals("a/b", resolver.resolveRelativeSegments("a/b"));
        assertEquals("a", resolver.resolveRelativeSegments("a/b/.."));

        assertEquals("b", resolver.resolveRelativeSegments("a/../b"));
        assertEquals("a/c", resolver.resolveRelativeSegments("a/b/../c"));
        assertEquals("c", resolver.resolveRelativeSegments("a/b/../../c"));
        assertEquals("", resolver.resolveRelativeSegments("a/b/../.."));
        assertEquals("a/c/d", resolver.resolveRelativeSegments("a/b/../c/d"));
        assertNull(resolver.resolveRelativeSegments("a/b/../../../c"));

        assertEquals("a/b/c", resolver.resolveRelativeSegments("a/b/c"));
        assertEquals("az/bz/cz", resolver.resolveRelativeSegments("az/bz/cz"));
        assertEquals("", resolver.resolveRelativeSegments(""));
    }
}
