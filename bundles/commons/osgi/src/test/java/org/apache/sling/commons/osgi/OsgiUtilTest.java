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
package org.apache.sling.commons.osgi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class OsgiUtilTest extends TestCase {

    public void testToDouble() {
        // we test getProperty which calls toDouble - so we can test both
        // methods in one go
        assertEquals(2.0, OsgiUtil.getProperty(null, 2.0));
        assertEquals(1.0, OsgiUtil.getProperty(1.0, 2.0));
        assertEquals(1.0, OsgiUtil.getProperty(new Double(1.0), 2.0));
        assertEquals(5.0, OsgiUtil.getProperty(new Long(5), 2.0));
        assertEquals(2.0, OsgiUtil.getProperty("abc", 2.0));
    }

    public void testToBoolean() {
        assertEquals(true, OsgiUtil.toBoolean(null, true));
        assertEquals(false, OsgiUtil.toBoolean(1.0, true));
        assertEquals(false, OsgiUtil.toBoolean(false, true));
        assertEquals(false, OsgiUtil.toBoolean("false", true));
        assertEquals(false, OsgiUtil.toBoolean("abc", true));
    }

    public void testToInteger() {
        assertEquals(2, OsgiUtil.toInteger(null, 2));
        assertEquals(2, OsgiUtil.toInteger(1.0, 2));
        assertEquals(2, OsgiUtil.toInteger(new Double(1.0), 2));
        assertEquals(5, OsgiUtil.toInteger(new Long(5), 2));
        assertEquals(5, OsgiUtil.toInteger(new Integer(5), 2));
        assertEquals(2, OsgiUtil.toInteger("abc", 2));
    }

    public void testToLong() {
        assertEquals(2, OsgiUtil.toLong(null, 2));
        assertEquals(2, OsgiUtil.toLong(1.0, 2));
        assertEquals(2, OsgiUtil.toLong(new Double(1.0), 2));
        assertEquals(5, OsgiUtil.toLong(new Long(5), 2));
        assertEquals(5, OsgiUtil.toLong(new Integer(5), 2));
        assertEquals(2, OsgiUtil.toLong("abc", 2));
    }

    public void testToObject() {
        assertEquals("hallo", OsgiUtil.toObject("hallo"));
        assertEquals("1", OsgiUtil.toObject(new String[] {"1", "2"}));
        assertEquals(null, OsgiUtil.toObject(null));
        assertEquals(null, OsgiUtil.toObject(new String[] {}));
        final List<String> l = new ArrayList<String>();
        assertEquals(null, OsgiUtil.toObject(l));
        l.add("1");
        assertEquals("1", OsgiUtil.toObject(l));
        l.add("2");
        assertEquals("1", OsgiUtil.toObject(l));
        final Map<String, Object> m = new HashMap<String, Object>();
        assertEquals(m, OsgiUtil.toObject(m));
    }

    public void testToString() {
        assertEquals("hallo", OsgiUtil.toString("hallo", null));
        assertEquals(this.toString(), OsgiUtil.toString(null, this.toString()));
        final Map<String, Object> m = new HashMap<String, Object>();
        m.put("1", 5);
        assertEquals(m.toString(), OsgiUtil.toString(m, this.toString()));
    }

    public void testToStringArray() {
        final String[] defaultValue = new String[] {"1"};
        assertEquals(null, OsgiUtil.toStringArray(5));
        assertEquals(null, OsgiUtil.toStringArray(null));
        assertEquals(defaultValue, OsgiUtil.toStringArray(5, defaultValue));
        assertEquals(defaultValue, OsgiUtil.toStringArray(null, defaultValue));
        equals(new String[] {"hallo"}, OsgiUtil.toStringArray("hallo", defaultValue));
        equals(new String[] {"hallo"}, OsgiUtil.toStringArray(new String[] {"hallo"}, defaultValue));
        equals(new String[] {"hallo", "you"}, OsgiUtil.toStringArray(new String[] {"hallo", "you"}, defaultValue));
        equals(new String[] {"5", "1"}, OsgiUtil.toStringArray(new Integer[] {5, 1}, defaultValue));
        equals(new String[] {"5", "1"}, OsgiUtil.toStringArray(new Integer[] {5, null, 1}, defaultValue));
        final List<String> l = new ArrayList<String>();
        equals(new String[] {}, OsgiUtil.toStringArray(l, defaultValue));
        l.add("1");
        l.add("2");
        equals(new String[] {"1", "2"}, OsgiUtil.toStringArray(l, defaultValue));
        l.add(null);
        equals(new String[] {"1", "2"}, OsgiUtil.toStringArray(l, defaultValue));
        final Map<String, Object> m = new HashMap<String, Object>();
        m.put("1", 5);
        assertEquals(defaultValue, OsgiUtil.toStringArray(m, defaultValue));
    }

    private void equals(final String[] a, final String[] b) {
        if ( a == null && b == null ) {
            return;
        }
        if ( a == null ) {
            fail("Array is not null: " + b);
        }
        if ( b == null ) {
            fail("Array is null, expected is: " + a);
        }
        if ( a.length != b.length ) {
            fail("Length differs: expect " + a .length + ", received " + b.length);
        }
        for(int i=0; i < a.length; i++) {
            if ( ! a[i].equals(b[i])) {
                fail("Expected " + a[i] + " at index " + i + ", but is " + b[i]);
            }
        }
    }
}
