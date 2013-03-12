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
package org.apache.sling.api.resource;

import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class ResourceMetadataTest {
    
    private static final Map<String, Object> TEST_MAP = new HashMap<String, Object>();
    static {
        TEST_MAP.put("first", "one");
        TEST_MAP.put("second", Integer.MAX_VALUE);
    }
    
    @Test
    public void testLockedPut() {
        final ResourceMetadata m = new ResourceMetadata();
        m.lock();
        try {
            m.put("after", "locking");
            fail("put() should fail after locking");
        } catch(UnsupportedOperationException uoe) {
            // all good
        }
    }
    
    @Test
    public void testLockedClear() {
        final ResourceMetadata m = new ResourceMetadata();
        m.lock();
        try {
            m.clear();
            fail("clear() should fail after locking");
        } catch(UnsupportedOperationException uoe) {
            // all good
        }
    }
    
    @Test
    public void testLockedPutAll() {
        final ResourceMetadata m = new ResourceMetadata();
        m.lock();
        try {
            m.putAll(TEST_MAP);
            fail("putAll() should fail after locking");
        } catch(UnsupportedOperationException uoe) {
            // all good
        }
    }
    
    @Test
    public void testLockedRemove() {
        final ResourceMetadata m = new ResourceMetadata();
        m.lock();
        try {
            m.remove("foo");
            fail("remove() should fail after locking");
        } catch(UnsupportedOperationException uoe) {
            // all good
        }
    }
    
    @Test(expected=StackOverflowError.class)
    public void testLockedEntrySet() {
        final ResourceMetadata m = new ResourceMetadata();
        m.lock();
        m.entrySet().toString();
    }
    
    @Test(expected=StackOverflowError.class)
    public void testLockedKeySet() {
        final ResourceMetadata m = new ResourceMetadata();
        m.lock();
        m.keySet().toString();
    }
    
    @Test(expected=StackOverflowError.class)
    public void testLockedValues() {
        final ResourceMetadata m = new ResourceMetadata();
        m.lock();
        m.values().toString();
    }
}
