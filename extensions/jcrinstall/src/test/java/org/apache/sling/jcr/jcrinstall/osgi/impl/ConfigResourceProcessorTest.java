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

import java.io.ByteArrayInputStream;
import java.util.Dictionary;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ConfigResourceProcessorTest 
{
    @org.junit.Test public void testLoadDictionary() throws Exception {
        final String data =
            "one=\"a\"\n"
            + "two=[\"b\",\"c\",\"d\"]\n"
            + "three=\"z\"\n"
        ;
        
        final ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        final Dictionary<?, ?> dict = new ConfigResourceProcessor(null).loadDictionary(is);
        
        assertNotNull("one must be found", dict.get("one"));
        assertEquals("one must match", "a", dict.get("one"));
        
        assertNotNull("two must be found", dict.get("two"));
        assertNotNull("two must be a String[]", dict.get("two") instanceof String[]);
        assertEquals("two[0] must match", "b" , ((String[])dict.get("two"))[0]);
        assertEquals("two[1] must match", "c" , ((String[])dict.get("two"))[1]);
        assertEquals("two[2] must match", "d" , ((String[])dict.get("two"))[2]);
        
        assertNotNull("three must be found", dict.get("three"));
        assertEquals("three must match", "z" , dict.get("three"));
    }
    
    @org.junit.Test public void testComments() throws Exception {
        final String data =
            "one=\"a\"\n"
            + "# some comment\n"
            + "two=\"b\"\n"
            + "# another comment\n"
        ;
        
        final ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        final Dictionary<?, ?> dict = new ConfigResourceProcessor(null).loadDictionary(is);
        
        assertNotNull("one must be found", dict.get("one"));
        assertEquals("one must match", "a", dict.get("one"));
        assertNull("two is not found if following a comment", dict.get("two"));
    }
    
    @org.junit.Test public void testQuotes() throws Exception {
        final String data =
            "0=\"0\"\n"
            + "1=1\n"
            ;
        
        final ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        final Dictionary<?, ?> dict = new ConfigResourceProcessor(null).loadDictionary(is);
        
        assertNotNull("0 must be found", dict.get("0"));
        assertNull("1 must not be found due to missing quotes", dict.get("1"));
    }
    
    @org.junit.Test public void testMissingEOL() throws Exception {
        final String data =
            "0=\"0\"\n"
            + "1=\"1\""
            ;
        
        final ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        final Dictionary<?, ?> dict = new ConfigResourceProcessor(null).loadDictionary(is);
        
        assertNotNull("0 must be found", dict.get("0"));
        assertNotNull("1 must be found, although EOL is missing", dict.get("1"));
    }
    
    @org.junit.Test public void testWhitespace() throws Exception {
        final String data =
            "0 =\"0\"\n"
            + "1\t=\"1\"\n"
            + "2= \"2\"\n"
            + "3=\t\"3\"\n"
        ;
        
        final ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        final Dictionary<?, ?> dict = new ConfigResourceProcessor(null).loadDictionary(is);
        
        assertNotNull("0 must be found, whitespace before = sign works", dict.get("0"));
        assertNotNull("1 must be found, whitespace before = sign works", dict.get("1"));
        assertNull("2 must not be found, whitespace after = sign breaks syntax", dict.get("2"));
        assertNull("3 must not be found, whitespace after = sign breaks syntax", dict.get("3"));
    }
}
