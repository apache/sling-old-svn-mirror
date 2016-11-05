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
package org.apache.sling.installer.factories.configuration.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.junit.Test;

public class ConfigUtilTest {

    @Test public void testIsSameDataEmptyAndNullDictionaries() throws Exception {
        final Dictionary<String, Object> a = new Hashtable<String, Object>();
        final Dictionary<String, Object> b = new Hashtable<String, Object>();

        assertTrue(ConfigUtil.isSameData(a, b));
        assertTrue(ConfigUtil.isSameData(b, a));
        assertFalse(ConfigUtil.isSameData(null, a));
        assertFalse(ConfigUtil.isSameData(null, null));
        assertFalse(ConfigUtil.isSameData(b, null));
    }

    @Test public void testIsSameDataSameDictionaries() throws Exception {
        final Dictionary<String, Object> a = new Hashtable<String, Object>();
        final Dictionary<String, Object> b = new Hashtable<String, Object>();

        a.put("a", "value");
        a.put("b", 1);
        a.put("c", 2L);
        a.put("d", true);
        a.put("e", 1.1);

        final Enumeration<String> e = a.keys();
        while ( e.hasMoreElements() ) {
            final String name = e.nextElement();
            b.put(name, a.get(name));
        }

        assertTrue(ConfigUtil.isSameData(a, b));
        assertTrue(ConfigUtil.isSameData(b, a));

        final Enumeration<String> e1 = a.keys();
        while ( e1.hasMoreElements() ) {
            final String name = e1.nextElement();
            b.put(name, a.get(name).toString());
        }

        assertTrue(ConfigUtil.isSameData(a, b));
        assertTrue(ConfigUtil.isSameData(b, a));
    }

    @Test public void testIsSameDataArrays() throws Exception {
        final Dictionary<String, Object> a = new Hashtable<String, Object>();
        final Dictionary<String, Object> b = new Hashtable<String, Object>();

        a.put("a", new String[] {"1", "2", "3"});
        b.put("a", a.get("a"));

        a.put("b", new Integer[] {1,2,3});
        b.put("b", a.get("b"));

        a.put("c", new Long[] {1L,2L,3L});
        b.put("c", a.get("c"));

        a.put("d", new Integer[] {1,2,3});
        b.put("d", new String[] {"1", "2", "3"});

        assertTrue(ConfigUtil.isSameData(a, b));
        assertTrue(ConfigUtil.isSameData(b, a));
    }

    @Test public void testIsSameDataWithPrimitiveArrays() throws Exception {
        final Dictionary<String, Object> a = new Hashtable<String, Object>();
        final Dictionary<String, Object> b = new Hashtable<String, Object>();

        a.put("b", new int[] {1,2,3});
        b.put("b", a.get("b"));

        a.put("c", new long[] {1L,2L,3L});
        b.put("c", a.get("c"));

        a.put("d", new int[] {1,2,3});
        b.put("d", new String[] {"1", "2", "3"});

        assertTrue(ConfigUtil.isSameData(a, b));
        assertTrue(ConfigUtil.isSameData(b, a));
    }
}
