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
package org.apache.sling.jcr.jcrinstall.osgi.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Properties;

import org.apache.sling.jcr.jcrinstall.osgi.impl.propertyconverter.PropertyConverter;
import org.apache.sling.jcr.jcrinstall.osgi.impl.propertyconverter.ValueConverterException;

/** Test the DictionaryReader */
public class DictionaryReaderTest {
    
    private void assertArray(String info, String [] expected, Object obj) {
        assertTrue(info + ":obj (" + obj.getClass().getName() + ") must be a String[]", obj instanceof String[]);
        final String [] actual = (String[])obj;
        assertEquals(info + ": array sizes must match", expected.length, actual.length);
        
        for(int i=0; i < expected.length; i++) {
            assertEquals(info + " at index " + i, expected[i], actual[i]);
        }
    }
    
    @org.junit.Test public void testConvertValue() throws ValueConverterException {
        final PropertyConverter c = new PropertyConverter();
        assertArray("one two", new String[] { "one", "two" }, c.convert("x[]", "one, two\t").getValue());
        assertArray("one", new String[] { "one" }, c.convert("x[]", "\t one ").getValue());
        assertArray("empty array", new String[] { }, c.convert("x []", "\t \n").getValue());
    }
    
    @org.junit.Test public void testConvertValueWithEscapes() throws ValueConverterException {
        final PropertyConverter c = new PropertyConverter();
        assertArray("one two", new String[] { "one", "two,three" }, c.convert("x[]", "one, two\\,three").getValue());
        assertArray("one", new String[] { "one,two,three" }, c.convert("x[]", "one\\,two\\,three").getValue());
    }
    
    @org.junit.Test public void testSplitWithEscapes() throws ValueConverterException {
        final PropertyConverter c = new PropertyConverter();
        assertArray("empty", new String[0], c.convert("x[]", "").getValue());
        assertArray("a", new String[] { "a" }, c.convert("x[]", "a").getValue());
        assertArray("multi", new String[] { "a\\,,b\\" }, c.convert("x[]", "a\\\\,\\,b\\").getValue());
        assertArray("a,b", new String[] { "a", "b" }, c.convert("x[]", "a, b\t").getValue());
        assertArray("a,b,c", new String[] { "a", "b, c" }, c.convert("x[]", "a, b\\, c\t").getValue());
        assertArray("a,b,c,d", new String[] { "a", "b, c ,", "d" }, c.convert("x[]", "a, b\\, c \\,,d ").getValue());
    }
     
    @org.junit.Test public void testConvertProperties() throws ValueConverterException {
        final Properties p = new Properties();
        p.setProperty("a", "1");
        p.setProperty("b", "2");
        p.setProperty("c[]", "1, 2, 3");
        p.setProperty("d []", "4, 5, 6");
        
        final DictionaryReader r = new DictionaryReader();
        final Dictionary<?, ?> d = r.convert(p);
        assertEquals("a", d.get("a"), "1");
        assertEquals("b", d.get("b"), "2");
        
        assertArray("c", new String[] { "1", "2", "3" }, d.get("c"));
        assertArray("d", new String[] { "4", "5", "6" }, d.get("d"));
    }
    
    @org.junit.Test public void testFromStream() throws IOException {
        final String data =
            "a = 1\n"
            + "b = this is B\n"
            + "# a comment\n"
            + "! another comment\n"
            + "c[] = 1,2\\\\,A , 3 \n"
            + "d=12\n"
            ;
        
        final ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        final DictionaryReader r = new DictionaryReader();
        Dictionary<?, ?> d = null;
        try {
            d = r.load(is);
        } finally {
            is.close();
        }
        
        assertEquals("Number of entries must match", 4, d.size());
        assertEquals("a", d.get("a"), "1");
        assertEquals("b", d.get("b"), "this is B");
        
        assertArray("c", new String[] { "1", "2,A", "3" }, d.get("c"));
        assertEquals("d", d.get("d"), "12");
    }
    
    @org.junit.Test public void testDataTypes() throws IOException {
        final String data =
            "a = 1\n"
            + "b(integer) = 242\n"
            + "# a comment\n"
            + "c(boolean) = true\n"
            + "d(double) = 14.23\n"
            + "e = My Value\n"
            ;
        
        final ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        final DictionaryReader r = new DictionaryReader();
        Dictionary<?, ?> d = null;
        try {
            d = r.load(is);
        } finally {
            is.close();
        }
        
        assertEquals("Number of entries must match", 5, d.size());
        assertEquals("String value matches", "1", d.get("a"));
        assertEquals("Integer value matches", new Integer(242), d.get("b"));
        assertEquals("Boolean value matches", new Boolean(true), d.get("c"));
        assertEquals("Double value matches", new Double(14.24), d.get("d"));
        assertEquals("String value matches", "My Value", d.get("e"));
    }
}