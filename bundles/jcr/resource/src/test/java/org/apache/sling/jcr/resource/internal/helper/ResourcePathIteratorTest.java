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

import java.util.Iterator;
import java.util.NoSuchElementException;

import junit.framework.TestCase;

public class ResourcePathIteratorTest extends TestCase {

    public void testNull() {
        ResourcePathIterator rpi = new ResourcePathIterator(null);
        assertFinished(rpi);
    }

    public void testEmpty() {
        ResourcePathIterator rpi = new ResourcePathIterator("");
        assertFinished(rpi);
    }

    public void testRoot() {
        ResourcePathIterator rpi = new ResourcePathIterator("/");
        assertNext("/", rpi);
        assertFinished(rpi);
    }

    public void testSlashed() {
        ResourcePathIterator rpi = new ResourcePathIterator("/root/child");
        assertNext("/root/child", rpi);
        assertFinished(rpi);
    }

    public void testSlashedTrailingSlash1() {
        ResourcePathIterator rpi = new ResourcePathIterator("/root/child/");
        assertNext("/root/child", rpi);
        assertFinished(rpi);
    }

    public void testSlashedTrailingSlash2() {
        ResourcePathIterator rpi = new ResourcePathIterator("/root/child//");
        assertNext("/root/child", rpi);
        assertFinished(rpi);
    }

    public void testDotted() {
        ResourcePathIterator rpi = new ResourcePathIterator("/root.child");
        assertNext("/root.child", rpi);
        assertNext("/root", rpi);
        assertFinished(rpi);
    }

    public void testMixed() {
        ResourcePathIterator rpi = new ResourcePathIterator(
            "/root/child.print.a4.html/with/suffix");
        assertNext("/root/child.print.a4.html/with/suffix", rpi);
        assertNext("/root/child.print.a4", rpi);
        assertNext("/root/child.print", rpi);
        assertNext("/root/child", rpi);
        assertFinished(rpi);
    }

    public void testNoSeparators() {
        final Iterator<String> rpi = new ResourcePathIterator(
            "MickeyMouseWasHere");
        assertNext("MickeyMouseWasHere", rpi);
        assertFinished(rpi);
    }

    public void testGetA() {
        final Iterator<String> rpi = new ResourcePathIterator(
            "/some/stuff/more.a4.html");
        assertNext("/some/stuff/more.a4.html", rpi);
        assertNext("/some/stuff/more.a4", rpi);
        assertNext("/some/stuff/more", rpi);
        assertFinished(rpi);
    }

    public void testGetB() {
        final Iterator<String> rpi = new ResourcePathIterator(
            "/some/stuff/more.html");
        assertNext("/some/stuff/more.html", rpi);
        assertNext("/some/stuff/more", rpi);
        assertFinished(rpi);
    }

    public void testHeadB() {
        final Iterator<String> rpi = new ResourcePathIterator(
            "/some/stuff/more.html");
        assertNext("/some/stuff/more.html", rpi);
        assertNext("/some/stuff/more", rpi);
        assertFinished(rpi);
    }

    public void testGetC() {
        final Iterator<String> it = new ResourcePathIterator("/some/stuff/more");
        assertNext("/some/stuff/more", it);
        assertFinished(it);
    }

    public void testGetD() {
        final Iterator<String> it = new ResourcePathIterator(
            "/some/stuff.print/more.html");
        assertNext("/some/stuff.print/more.html", it);
        assertNext("/some/stuff.print/more", it);
        assertNext("/some/stuff", it);
        assertFinished(it);
    }

    public void testRelativePathGet() {
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