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
package org.apache.sling.jcr.jcrinstall.jcr.impl;

import junit.framework.TestCase;

public class RunModeRegexpFilterTest extends TestCase {
    
    public static final String DEFAULT_REGEXP =  ".*/install$";
    
    public void testNoRunMode() {
        final RunModeRegexpFilter f = new RunModeRegexpFilter(DEFAULT_REGEXP, new MockRunMode(new String[0]));
        assertTrue(f.accept("/foo/install"));
        assertFalse(f.accept("/foo/install.bar"));
    }
    
    public void testSingleMode() {
        final String [] m = { "dev" };
        final RunModeRegexpFilter f = new RunModeRegexpFilter(DEFAULT_REGEXP, new MockRunMode(m));
        assertTrue("Test 1", f.accept("/foo/install"));
        assertFalse("Test 2", f.accept("/foo/install.bar"));
        assertTrue("Test 3", f.accept("/foo/install.dev"));
        assertFalse("Test 4", f.accept("/foo/install/dev"));
        assertTrue("Test 5", f.accept("/foo/install.dev.dev"));
        assertFalse("Test 6", f.accept("/foo/install.dev.bar"));
        assertFalse("Test 7", f.accept("/foo/install.bar.dev"));
    }
    
    public void testThreeModes() {
        final String [] m = { "dev", "web", "staging" };
        final RunModeRegexpFilter f = new RunModeRegexpFilter(DEFAULT_REGEXP, new MockRunMode(m));
        assertTrue("Test 1",f.accept("/foo/install"));
        assertFalse("Test 2",f.accept("/foo/install.bar"));
        assertTrue("Test 3",f.accept("/foo/install.dev"));
        assertTrue("Test 4",f.accept("/foo/install.web"));
        assertTrue("Test 5",f.accept("/foo/install.staging"));
        assertFalse("Test 6",f.accept("/foo/install/dev"));
        assertTrue("Test 7",f.accept("/foo/install.dev.web.staging"));
        assertTrue("Test 8",f.accept("/foo/install.web.dev.staging"));
        assertTrue("Test 9",f.accept("/foo/install.staging.web.dev"));
        assertTrue("Test 10",f.accept("/foo/install.staging.dev.web"));
        assertTrue("Test 11",f.accept("/foo/install.dev.dev.web.staging.web"));
        assertFalse("Test 12",f.accept("/foo/install.dev.bar"));
        assertFalse("Test 13",f.accept("/foo/install.bar.dev"));
    }
}
