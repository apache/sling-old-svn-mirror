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

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Testcase for {@link JcrPropertyMapCacheEntry}
 */
public class JcrPropertyMapCacheEntryTest {

    @Test
    public void testByteArray() throws Exception {
        assertNotNull(new JcrPropertyMapCacheEntry(new Byte[0], null));
        assertNotNull(new JcrPropertyMapCacheEntry(new byte[0], null));
    }

    @Test
    public void testShortArray() throws Exception {
        assertNotNull(new JcrPropertyMapCacheEntry(new Short[0], null));
        assertNotNull(new JcrPropertyMapCacheEntry(new short[0], null));
    }

    @Test
    public void testIntArray() throws Exception {
        assertNotNull(new JcrPropertyMapCacheEntry(new Integer[0], null));
        assertNotNull(new JcrPropertyMapCacheEntry(new int[0], null));
    }

    @Test
    public void testLongArray() throws Exception {
        assertNotNull(new JcrPropertyMapCacheEntry(new Long[0], null));
        assertNotNull(new JcrPropertyMapCacheEntry(new long[0], null));
    }

    @Test
    public void testFloatArray() throws Exception {
        assertNotNull(new JcrPropertyMapCacheEntry(new Float[0], null));
        assertNotNull(new JcrPropertyMapCacheEntry(new float[0], null));
    }

    @Test
    public void testDoubleArray() throws Exception {
        assertNotNull(new JcrPropertyMapCacheEntry(new Double[0], null));
        assertNotNull(new JcrPropertyMapCacheEntry(new double[0], null));
    }

    @Test
    public void testBooleanArray() throws Exception {
        assertNotNull(new JcrPropertyMapCacheEntry(new Boolean[0], null));
        assertNotNull(new JcrPropertyMapCacheEntry(new boolean[0], null));
    }

    @Test
    public void testCharArray() throws Exception {
        assertNotNull(new JcrPropertyMapCacheEntry(new Character[0], null));
        assertNotNull(new JcrPropertyMapCacheEntry(new char[0], null));
    }
}