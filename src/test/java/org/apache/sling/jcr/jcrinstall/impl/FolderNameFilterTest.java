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
package org.apache.sling.jcr.jcrinstall.impl;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

public class FolderNameFilterTest {
    public static final String DEFAULT_REGEXP =  ".*/install$";
    public static final String [] ROOTS = { "/libs", "/apps" };

    @Test
    public void testNoRunMode() {
        final FolderNameFilter f = new FolderNameFilter(ROOTS, DEFAULT_REGEXP, new MockRunMode(new String[0]));
        assertTrue("Test 1", f.getPriority("/libs/install") > 0);
        assertFalse("Test 2", f.getPriority("/libs/install.bar") > 0);
    }
    
    public void testSingleMode() {
        final String [] m = { "dev" };
        final FolderNameFilter f = new FolderNameFilter(ROOTS, DEFAULT_REGEXP, new MockRunMode(m));
        assertTrue("Test 1", f.getPriority("/libs/install") > 0);
        assertFalse("Test 2", f.getPriority("/libs/install.bar") > 0);
        assertTrue("Test 3", f.getPriority("/libs/install.dev") > 0);
        assertFalse("Test 4", f.getPriority("/libs/install/dev") > 0);
        assertTrue("Test 5", f.getPriority("/libs/install.dev.dev") > 0);
        assertFalse("Test 6", f.getPriority("/libs/install.dev.bar") > 0);
        assertFalse("Test 7", f.getPriority("/libs/install.bar.dev") > 0);
    }
    
    public void testThreeModes() {
        final String [] m = { "dev", "web", "staging" };
        final FolderNameFilter f = new FolderNameFilter(ROOTS, DEFAULT_REGEXP, new MockRunMode(m));
        assertTrue("Test 1",f.getPriority("/libs/install") > 0);
        assertFalse("Test 2",f.getPriority("/libs/install.bar") > 0);
        assertTrue("Test 3",f.getPriority("/libs/install.dev") > 0);
        assertTrue("Test 4",f.getPriority("/libs/install.web") > 0);
        assertTrue("Test 5",f.getPriority("/libs/install.staging") > 0);
        assertFalse("Test 6",f.getPriority("/libs/install/dev") > 0);
        assertTrue("Test 7",f.getPriority("/libs/install.dev.web.staging") > 0);
        assertTrue("Test 8",f.getPriority("/libs/install.web.dev.staging") > 0);
        assertTrue("Test 9",f.getPriority("/libs/install.staging.web.dev") > 0);
        assertTrue("Test 10",f.getPriority("/libs/install.staging.dev.web") > 0);
        assertTrue("Test 11",f.getPriority("/libs/install.dev.dev.web.staging.web") > 0);
        assertFalse("Test 12",f.getPriority("/libs/install.dev.bar") > 0);
        assertFalse("Test 13",f.getPriority("/libs/install.bar.dev") > 0);
    }
    
    public void testRootPriorities() {
        final String [] m = { "dev" };
        final FolderNameFilter f = new FolderNameFilter(ROOTS, DEFAULT_REGEXP, new MockRunMode(m));
    	assertEquals("/libs root", new Integer(100), f.getPriority("/libs/install"));
    	assertEquals("/apps root", new Integer(200), f.getPriority("/apps/install"));
    }
    
    public void testRunModePriorities() {
        final String [] m = { "dev", "prod", "staging" };
        final FolderNameFilter f = new FolderNameFilter(ROOTS, DEFAULT_REGEXP, new MockRunMode(m));
    	assertEquals("Matches no runmode", new Integer(100), f.getPriority("/libs/install"));
    	assertEquals("Matches dev runmode", new Integer(201), f.getPriority("/apps/install.dev"));
    	assertEquals("Matches staging runmode", new Integer(201), f.getPriority("/apps/install.staging"));
    	assertEquals("Matches three runmodes (A)", new Integer(203), f.getPriority("/apps/install.dev.staging.prod"));
    	assertEquals("Matches three runmodes (B)", new Integer(203), f.getPriority("/apps/install.dev.prod.staging"));
    	assertEquals("Matches three runmodes (C)", new Integer(103), f.getPriority("/libs/install.dev.prod.staging"));
    }
}
