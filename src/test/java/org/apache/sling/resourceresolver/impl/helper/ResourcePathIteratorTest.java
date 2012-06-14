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
package org.apache.sling.resourceresolver.impl.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Test;

public class ResourcePathIteratorTest {

    @Test public void testNull() {
        ResourcePathIterator rpi = new ResourcePathIterator(null);
        assertFinished(rpi);
    }

    @Test public void testEmpty() {
        ResourcePathIterator rpi = new ResourcePathIterator("");
        assertFinished(rpi);
    }

    @Test public void testRoot() {
        ResourcePathIterator rpi = new ResourcePathIterator("/");
        assertNext("/", rpi);
        assertFinished(rpi);
    }

    @Test public void testSlashed() {
        ResourcePathIterator rpi = new ResourcePathIterator("/root/child");
        assertNext("/root/child", rpi);
        assertFinished(rpi);
    }

    @Test public void testSlashedTrailingSlash1() {
        ResourcePathIterator rpi = new ResourcePathIterator("/root/child/");
        assertNext("/root/child", rpi);
        assertFinished(rpi);
    }

    @Test public void testSlashedTrailingSlash2() {
        ResourcePathIterator rpi = new ResourcePathIterator("/root/child//");
        assertNext("/root/child", rpi);
        assertFinished(rpi);
    }

    @Test public void testDotted() {
        ResourcePathIterator rpi = new ResourcePathIterator("/root.child");
        assertNext("/root.child", rpi);
        assertNext("/root", rpi);
        assertFinished(rpi);
    }

    @Test public void testMixed() {
        ResourcePathIterator rpi = new ResourcePathIterator(
            "/root/child.print.a4.html/with/suffix");
        assertNext("/root/child.print.a4.html/with/suffix", rpi);
        assertNext("/root/child.print.a4", rpi);
        assertNext("/root/child.print", rpi);
        assertNext("/root/child", rpi);
        assertFinished(rpi);
    }

    @Test public void testNoSeparators() {
        final Iterator<String> rpi = new ResourcePathIterator(
            "MickeyMouseWasHere");
        assertNext("MickeyMouseWasHere", rpi);
        assertFinished(rpi);
    }

    @Test public void testGetA() {
        final Iterator<String> rpi = new ResourcePathIterator(
            "/some/stuff/more.a4.html");
        assertNext("/some/stuff/more.a4.html", rpi);
        assertNext("/some/stuff/more.a4", rpi);
        assertNext("/some/stuff/more", rpi);
        assertFinished(rpi);
    }

    @Test public void testGetB() {
        final Iterator<String> rpi = new ResourcePathIterator(
            "/some/stuff/more.html");
        assertNext("/some/stuff/more.html", rpi);
        assertNext("/some/stuff/more", rpi);
        assertFinished(rpi);
    }

    @Test public void testHeadB() {
        final Iterator<String> rpi = new ResourcePathIterator(
            "/some/stuff/more.html");
        assertNext("/some/stuff/more.html", rpi);
        assertNext("/some/stuff/more", rpi);
        assertFinished(rpi);
    }

    @Test public void testGetC() {
        final Iterator<String> it = new ResourcePathIterator("/some/stuff/more");
        assertNext("/some/stuff/more", it);
        assertFinished(it);
    }

    @Test public void testGetD() {
        final Iterator<String> it = new ResourcePathIterator(
            "/some/stuff.print/more.html");
        assertNext("/some/stuff.print/more.html", it);
        assertNext("/some/stuff.print/more", it);
        assertNext("/some/stuff", it);
        assertFinished(it);
    }

    @Test public void testRelativePathGet() {
        final Iterator<String> it = new ResourcePathIterator("some/stuff.print");
        assertNext("some/stuff.print", it);
        assertNext("some/stuff", it);
        assertFinished(it);
    }

    private void assertNext(String expected, Iterator<String> iterator) {
        assertTrue("Iterator must have next", iterator.hasNext());
        assertEquals("Incorrect next value", expected, iterator.next());
    }

    private void assertFinished(Iterator<String> iterator) {

        assertFalse("Iterator must not have next", iterator.hasNext());

        try {
            iterator.next();
            fail("Iterator should throw NoSuchElementException");
        } catch (NoSuchElementException nsee) {
            // expected
        }

    }
}