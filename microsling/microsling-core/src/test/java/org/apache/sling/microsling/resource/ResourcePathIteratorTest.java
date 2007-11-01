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
    
    public void testNullInput() {
        final Iterator<String> it = new ResourcePathIterator(null);
        assertFalse(it.hasNext());
    }
    
    public void testEmptyInput() {
        final Iterator<String> it = new ResourcePathIterator("");
        assertFalse(it.hasNext());
    }
    
    public void testNoSeparators() {
        final Iterator<String> it = new ResourcePathIterator("MickeyMouseWasHere");
        assertTrue(it.hasNext());
        assertEquals("MickeyMouseWasHere",it.next());
        assertFalse(it.hasNext());
    }
    
    public void testSlashOnly() {
        final Iterator<String> it = new ResourcePathIterator("/MickeyMouseWasHere/ok");
        assertTrue(it.hasNext());
        assertEquals("/MickeyMouseWasHere/ok",it.next());
        assertTrue(it.hasNext());
        assertEquals("/MickeyMouseWasHere",it.next());
        assertFalse(it.hasNext());
    }
    
    public void testDotOnly() {
        final Iterator<String> it = new ResourcePathIterator("MickeyMouseWasHere.ok");
        assertTrue(it.hasNext());
        assertEquals("MickeyMouseWasHere.ok",it.next());
        assertTrue(it.hasNext());
        assertEquals("MickeyMouseWasHere",it.next());
        assertFalse(it.hasNext());
    }
    
    public void testRealisticPath() {
        final Iterator<String> it = new ResourcePathIterator("/some/stuff.print.a4/more");
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
        assertFalse(it.hasNext());
    }
    
    public void testRelativePath() {
        final Iterator<String> it = new ResourcePathIterator("some/stuff.print");
        assertTrue(it.hasNext());
        assertEquals("some/stuff.print",it.next());
        assertTrue(it.hasNext());
        assertEquals("some/stuff",it.next());
        assertTrue(it.hasNext());
        assertEquals("some",it.next());
        assertFalse(it.hasNext());
    }
}
