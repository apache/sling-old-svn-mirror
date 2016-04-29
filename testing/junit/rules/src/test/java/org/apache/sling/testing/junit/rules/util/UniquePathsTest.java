/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.junit.rules.util;

import org.apache.sling.testing.clients.util.UniquePaths;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

public class UniquePathsTest {

    @Before
    public void setup() throws Exception {
        // Set known startTime and counter values for tests
        {
            final Field f = UniquePaths.class.getDeclaredField("startTime");
            f.setAccessible(true);
            f.set(UniquePaths.class, 1234L);
        }
        {
            final Field f = UniquePaths.class.getDeclaredField("counter");
            f.setAccessible(true);
            f.set(UniquePaths.class, new AtomicLong(9362L));
        }
    }
    
    @Test
    public void testNoUPattern() {
        assertEquals("/tmp/UniquePathsTest_1234_9363", UniquePaths.get(this, "/tmp/"));
        assertEquals("/bar/UniquePathsTest_1234_9364", UniquePaths.get(this, "/bar/"));
    }
    
    @Test
    public void testSingleUPattern() {
        assertEquals("/tmp/UniquePathsTest_1234_9363/foo", UniquePaths.get(this, "/tmp/_UNIQ_/foo"));
    }
    
    @Test
    public void testMultipleUPattern() {
        assertEquals(
                "/tmp/UniquePathsTest_1234_9363/foo/UniquePathsTest_1234_9363.html", 
                UniquePaths.get(this, "/tmp/_UNIQ_/foo/_UNIQ_.html"));
    }
    
    @Test
    public void testNullPattern() {
        assertEquals(
                "UniquePathsTest_1234_9363", 
                UniquePaths.get(this, null));
    }
    
    @Test
    public void testNoPattern() {
        assertEquals(
                "UniquePathsTest_1234_9363", 
                UniquePaths.get(this));
    }
}
