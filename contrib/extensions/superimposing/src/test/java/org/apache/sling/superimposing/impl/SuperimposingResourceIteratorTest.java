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
package org.apache.sling.superimposing.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SuperimposingResourceIteratorTest {

    @Mock
    private Iterator<Resource> originalResourceIterator;
    @Mock
    private Resource originalResource1;
    @Mock
    private Resource originalResource2;

    private SuperimposingResourceProviderImpl superimposingResourceProvider;

    private static final String ORIGINAL_PATH = "/root/path1";
    private static final String SUPERIMPOSED_PATH = "/root/path2";

    @Before
    public void setUp() {
        this.superimposingResourceProvider = new SuperimposingResourceProviderImpl(SUPERIMPOSED_PATH, ORIGINAL_PATH, false);
        when(this.originalResource1.getPath()).thenReturn(ORIGINAL_PATH + "/node1");
        when(this.originalResource2.getPath()).thenReturn(ORIGINAL_PATH + "/node2");
    }

    @Test
    public void testEmpty() {
        when(this.originalResourceIterator.hasNext()).thenReturn(false);
        Iterator<Resource> underTest = new SuperimposingResourceIterator(this.superimposingResourceProvider, this.originalResourceIterator);
        assertFalse(underTest.hasNext());
    }

    @Test(expected=NoSuchElementException.class)
    public void testEmptyGetNext() {
        when(this.originalResourceIterator.hasNext()).thenReturn(false);
        Iterator<Resource> underTest = new SuperimposingResourceIterator(this.superimposingResourceProvider, this.originalResourceIterator);
        underTest.next();
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testRemove() {
        Iterator<Resource> underTest = new SuperimposingResourceIterator(this.superimposingResourceProvider, this.originalResourceIterator);
        underTest.remove();
    }

    @Test
    public void testWith2Elements() {
        when(this.originalResourceIterator.hasNext()).thenReturn(true, true, false);
        when(this.originalResourceIterator.next()).thenReturn(this.originalResource1, this.originalResource2, null);
        Iterator<Resource> underTest = new SuperimposingResourceIterator(this.superimposingResourceProvider, this.originalResourceIterator);

        assertTrue(underTest.hasNext());
        assertEquals(SUPERIMPOSED_PATH + "/node1", underTest.next().getPath());

        assertTrue(underTest.hasNext());
        assertEquals(SUPERIMPOSED_PATH + "/node2", underTest.next().getPath());

        assertFalse(underTest.hasNext());
    }

}
