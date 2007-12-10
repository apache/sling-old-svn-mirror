/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.microsling.resource;

import java.util.Iterator;

import junit.framework.TestCase;

/** Test the ResourcePathIterator class */

public class ResourcePathIteratorTest extends TestCase {
    
    public final static String GET = "GET";
    public final static String POST = "POST";
    
    public void testNullInput() {
        final Iterator<String> it = new ResourcePathIterator(null,POST);
        assertFalse(it.hasNext());
    }
    
    public void testEmptyInput() {
        final Iterator<String> it = new ResourcePathIterator("",POST);
        assertFalse("done iterating", it.hasNext());
    }
    
    public void testNoSeparators() {
        final Iterator<String> it = new ResourcePathIterator("MickeyMouseWasHere",POST);
        assertTrue(it.hasNext());
        assertEquals("MickeyMouseWasHere",it.next());
        assertFalse("done iterating", it.hasNext());
    }
    
    public void testSlashOnly() {
        final Iterator<String> it = new ResourcePathIterator("/MickeyMouseWasHere/ok",POST);
        assertTrue(it.hasNext());
        assertEquals("/MickeyMouseWasHere/ok",it.next());
        assertTrue(it.hasNext());
        assertEquals("/MickeyMouseWasHere",it.next());
        assertFalse("done iterating", it.hasNext());
    }
    
    public void testDotOnly() {
        final Iterator<String> it = new ResourcePathIterator("MickeyMouseWasHere.ok",POST);
        assertTrue(it.hasNext());
        assertEquals("MickeyMouseWasHere.ok",it.next());
        assertTrue(it.hasNext());
        assertEquals("MickeyMouseWasHere",it.next());
        assertFalse("done iterating", it.hasNext());
    }
    
    public void testRealisticPath() {
        final Iterator<String> it = new ResourcePathIterator("/some/stuff.print.a4/more",POST);
        assertTrue(it.hasNext());
        assertEquals("/some/stuff.print.a4/more",it.next());
        assertTrue(it.hasNext());
        assertEquals("/some/stuff.print.a4",it.next());
        assertTrue(it.hasNext());
        assertEquals("/some/stuff.print",it.next());
        assertTrue(it.hasNext());
        assertEquals("/some/stuff",it.next());
        assertTrue(it.hasNext());
        assertEquals("/some",it.next());
        assertFalse("done iterating", it.hasNext());
    }
    
    public void testGetA() {
        final Iterator<String> it = new ResourcePathIterator("/some/stuff/more.a4.html",GET);
        assertTrue(it.hasNext());
        assertEquals("/some/stuff/more.a4.html",it.next());
        assertTrue(it.hasNext());
        assertEquals("/some/stuff/more.a4",it.next());
        assertTrue(it.hasNext());
        assertEquals("/some/stuff/more",it.next());
        assertFalse("done iterating", it.hasNext());
    }
    
    public void testGetB() {
        final Iterator<String> it = new ResourcePathIterator("/some/stuff/more.html",GET);
        assertTrue(it.hasNext());
        assertEquals("/some/stuff/more.html",it.next());
        assertTrue(it.hasNext());
        assertEquals("/some/stuff/more",it.next());
        assertFalse("done iterating", it.hasNext());
    }
    
    public void testGetC() {
        final Iterator<String> it = new ResourcePathIterator("/some/stuff/more",GET);
        assertTrue(it.hasNext());
        assertEquals("/some/stuff/more",it.next());
        assertFalse("done iterating", it.hasNext());
    }
    
    public void testGetD() {
        final Iterator<String> it = new ResourcePathIterator("/some/stuff.print/more.html",GET);
        assertTrue(it.hasNext());
        assertEquals("/some/stuff.print/more.html",it.next());
        assertTrue(it.hasNext());
        assertEquals("/some/stuff.print/more",it.next());
        assertFalse("done iterating", it.hasNext());
    }
    
    public void testRelativePath() {
        final Iterator<String> it = new ResourcePathIterator("some/stuff.print",POST);
        assertTrue(it.hasNext());
        assertEquals("some/stuff.print",it.next());
        assertTrue(it.hasNext());
        assertEquals("some/stuff",it.next());
        assertTrue(it.hasNext());
        assertEquals("some",it.next());
        assertFalse("done iterating", it.hasNext());
    }
}
