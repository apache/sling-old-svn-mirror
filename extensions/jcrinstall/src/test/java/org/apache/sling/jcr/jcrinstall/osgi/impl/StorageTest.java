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

import java.io.File;
import java.util.Map;

import static org.junit.Assert.*;

/** Test the Storage class */
public class StorageTest {

    @org.junit.Test public void testEmptyDataFile() throws Exception {
        final Storage s = new Storage(Utilities.getTestFile());
        assertEquals("Storage is initially empty", 0, s.getKeys().size());
    }
    
    @org.junit.Test public void testFileCreation() throws Exception {

        // Get a temp file and delete it so that Storage must
        // create it (i.e. we use just the filename)
        final File f = Utilities.getTestFile();
        f.delete();
        
        {
            final Storage s = new Storage(f);
            s.getMap("one").put("two", "twodata");
            s.saveToFile();
        }
        
        {
            final Storage s = new Storage(f);
            assertTrue("Retrieved Map contains 'two'", s.getMap("one").containsKey("two"));
        }
    }
    
    @org.junit.Test public void testStoreAndRetrieve() throws Exception {

        final File f = Utilities.getTestFile();
        final String [] keys = { "one", "two" };
        
        {
            final Storage s = new Storage(f);
            
            for(String key : keys) {
                final Map<String, Object> m = s.getMap(key);
                m.put(key + ".len", Integer.valueOf(key.length()));
            }
            assertEquals("Before saving, number of keys in storage matches", keys.length, s.getKeys().size());
            s.saveToFile();
        }
        
        {
            final Storage s = new Storage(f);
            assertEquals("After retrieving, number of keys in storage matches", keys.length, s.getKeys().size());
            for(String key : keys) {
                final Map<String, Object> m = s.getMap(key);
                assertEquals("Map for " + key + " has one entry", 1, m.size());
                final Integer len = (Integer)m.get(key + ".len");
                assertNotNull("Integer len found for key " + key, len);
                assertEquals("Integer len matches for key " + key, key.length(), len.intValue());
            }
        }
        
    }
    
    @org.junit.Test public void testContains() throws Exception {
        final Storage s = new Storage(Utilities.getTestFile());
        final String uri = "TEST_URI";
        assertFalse("Storage must initially be empty", s.contains(uri));
        s.contains(uri);
        assertFalse("Storage must be empty after contains call", s.contains(uri));
        s.getMap(uri);
        assertTrue("Storage contains key after getMap call", s.contains(uri));
    }
    
    @org.junit.Test public void testRemove() throws Exception {
        final File f = Utilities.getTestFile();
        {
            final Storage s = new Storage(f);
            s.getMap("one");
            assertEquals("After adding one entry, size is 1", 1, s.getKeys().size());
            s.remove("one");
            assertEquals("After removing entry, size is 0", 0, s.getKeys().size());
            s.saveToFile();
        }
        
        {
            final Storage s = new Storage(f);
            assertEquals("After save/restore, size is 0", 0, s.getKeys().size());
        }
    }
}