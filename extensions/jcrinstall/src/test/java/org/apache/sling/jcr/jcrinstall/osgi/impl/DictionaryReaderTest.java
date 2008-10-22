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
    
    @org.junit.Test public void testConvertValue() {
        assertArray("one two", new String[] { "one", "two" }, DictionaryReader.convertValue("one, two\t"));
        assertArray("one", new String[] { "one" }, DictionaryReader.convertValue("\t one "));
        assertArray("empty array", new String[] { }, DictionaryReader.convertValue("\t \n"));
    }
    
    @org.junit.Test public void testConvertValueWithEscapes() {
        assertArray("one two", new String[] { "one", "two,three" }, DictionaryReader.convertValue("one, two\\,three"));
        assertArray("one", new String[] { "one,two,three" }, DictionaryReader.convertValue("one\\,two\\,three"));
    }
    
    @org.junit.Test public void testSplitWithEscapes() {
        assertArray("empty", new String[0], DictionaryReader.splitWithEscapes("", ','));
        assertArray("a", new String[] { "a" }, DictionaryReader.splitWithEscapes("a", ','));
        assertArray("multi", new String[] { "a\\,,b\\" }, DictionaryReader.splitWithEscapes("a\\\\,\\,b\\", ','));
        assertArray("a,b", new String[] { "a", "b" }, DictionaryReader.splitWithEscapes("a, b\t", ','));
        assertArray("a,b,c", new String[] { "a", "b, c" }, DictionaryReader.splitWithEscapes("a, b\\, c\t", ','));
        assertArray("a,b,c,d", new String[] { "a", "b, c ,", "d" }, DictionaryReader.splitWithEscapes("a, b\\, c \\,,d ", ','));
    }
     
    @org.junit.Test public void testConvertProperties() {
        final Properties p = new Properties();
        p.setProperty("a", "1");
        p.setProperty("b", "2");
        p.setProperty("c[]", "1, 2, 3");
        p.setProperty("d []", "4, 5, 6");
        
        final Dictionary<?, ?> d = DictionaryReader.convert(p);
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
        Dictionary<?, ?> d = null;
        try {
            d = DictionaryReader.load(is);
        } finally {
            is.close();
        }
        
        assertEquals("Number of entries must match", 4, d.size());
        assertEquals("a", d.get("a"), "1");
        assertEquals("b", d.get("b"), "this is B");
        
        assertArray("c", new String[] { "1", "2,A", "3" }, d.get("c"));
        assertEquals("d", d.get("d"), "12");
    }
}
