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
package org.apache.sling.installer.provider.jcr.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class FolderNameFilterTest {
    public static final String DEFAULT_REGEXP =  ".*/install$";
    public static final String CONFIG_REGEXP =  ".*/config$";
    public static final String [] ROOTS = JcrInstaller.DEFAULT_SEARCH_PATH;

    @Test
    public void testParseRootPaths() {
    	{
    		final String [] paths = { "a", "b/" };
            final FolderNameFilter f = new FolderNameFilter(paths, DEFAULT_REGEXP, new HashSet<String>());
            assertEquals("/a", f.getRootPaths()[0]);
            assertEquals("/b", f.getRootPaths()[1]);
            assertEquals(FolderNameFilter.DEFAULT_ROOT_PRIORITY, f.getRootPriority("/a/foo"));
            assertEquals(FolderNameFilter.DEFAULT_ROOT_PRIORITY, f.getRootPriority("/b/foo"));
            assertEquals(0, f.getRootPriority("/notInThoseRoots"));
    	}
    	{
    		final String [] paths = { "a:100", "/b/: 200 " };
            final FolderNameFilter f = new FolderNameFilter(paths, DEFAULT_REGEXP, new HashSet<String>());
            assertEquals("/b", f.getRootPaths()[0]);
            assertEquals("/a", f.getRootPaths()[1]);
            assertEquals(100, f.getRootPriority("/a/foo"));
            assertEquals(200, f.getRootPriority("/b/foo"));
    	}
    	{
    		final String [] paths = { "a/:NOT_AN_INTEGER", "/b/: 200 " };
            final FolderNameFilter f = new FolderNameFilter(paths, DEFAULT_REGEXP, new HashSet<String>());
            assertEquals("/b", f.getRootPaths()[0]);
            assertEquals("/a", f.getRootPaths()[1]);
            assertEquals(FolderNameFilter.DEFAULT_ROOT_PRIORITY, f.getRootPriority("/a/foo"));
            assertEquals(200, f.getRootPriority("/b/foo"));
    	}
    }

    @Test
    public void testNoRunMode() {
        final FolderNameFilter f = new FolderNameFilter(ROOTS, DEFAULT_REGEXP, new HashSet<String>());
        assertTrue("Test 1", f.getPriority("/libs/install") > 0);
        assertFalse("Test 2", f.getPriority("/libs/install.bar") > 0);
    }

    @Test
    public void testSingleMode() {
        final Set<String> m = new HashSet<String>();
        m.add("dev");
        final FolderNameFilter f = new FolderNameFilter(ROOTS, DEFAULT_REGEXP, m);
        assertTrue("Test 1", f.getPriority("/libs/install") > 0);
        assertFalse("Test 2", f.getPriority("/libs/install.bar") > 0);
        assertTrue("Test 3", f.getPriority("/libs/install.dev") > 0);
        assertFalse("Test 4", f.getPriority("/libs/install/dev") > 0);
        assertTrue("Test 5", f.getPriority("/libs/install.dev.dev") > 0);
        assertFalse("Test 6", f.getPriority("/libs/install.dev.bar") > 0);
        assertFalse("Test 7", f.getPriority("/libs/install.bar.dev") > 0);
    }

    @Test
    public void testThreeModes() {
        final Set<String> m = new HashSet<String>();
        m.add("dev");
        m.add("web");
        m.add("staging");
        final FolderNameFilter f = new FolderNameFilter(ROOTS, DEFAULT_REGEXP, m);
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

    @Test
    public void testRootPriorities() {
        final Set<String> m = new HashSet<String>();
        m.add("dev");
        final FolderNameFilter f = new FolderNameFilter(ROOTS, DEFAULT_REGEXP, m);
    	assertEquals("/libs root", new Integer(100), (Integer)f.getPriority("/libs/install"));
    	assertEquals("/apps root", new Integer(200), (Integer)f.getPriority("/apps/install"));
    }

    @Test
    public void testRunModePriorities() {
        final Set<String> m = new HashSet<String>();
        m.add("dev");
        m.add("prod");
        m.add("staging");
        final FolderNameFilter f = new FolderNameFilter(ROOTS, DEFAULT_REGEXP, m);
    	assertEquals("Matches no runmode", new Integer(100), (Integer)f.getPriority("/libs/install"));
    	assertEquals("Matches dev runmode", new Integer(201), (Integer)f.getPriority("/apps/install.dev"));
    	assertEquals("Matches staging runmode", new Integer(201), (Integer)f.getPriority("/apps/install.staging"));
    	assertEquals("Matches three runmodes (A)", new Integer(203), (Integer)f.getPriority("/apps/install.dev.staging.prod"));
    	assertEquals("Matches three runmodes (B)", new Integer(203), (Integer)f.getPriority("/apps/install.dev.prod.staging"));
    	assertEquals("Matches three runmodes (C)", new Integer(103), (Integer)f.getPriority("/libs/install.dev.prod.staging"));
    }

    @Test
    public void testAuthorPriorities() {
        final Set<String> m = new HashSet<String>();
        m.add("author");
        m.add("mycompany");
        m.add("dev");
        final FolderNameFilter f = new FolderNameFilter(ROOTS, CONFIG_REGEXP, m);
        assertEquals("", new Integer(201), (Integer)f.getPriority("/apps/somewhere/runmodes/config.author"));
        assertEquals("", new Integer(202), (Integer)f.getPriority("/apps/somewhere/runmodes/config.author.dev"));
        assertEquals("", new Integer(203), (Integer)f.getPriority("/apps/somewhere/runmodes/config.author.dev.mycompany"));
    }

    @Test
    public void testDotsInPath() {
        final Set<String> m = new HashSet<String>();
        m.add("dev");
        m.add("prod");
        m.add("staging");
        final FolderNameFilter f = new FolderNameFilter(ROOTS, DEFAULT_REGEXP, m);
    	assertEquals("Matches no runmode", new Integer(100), (Integer)f.getPriority("/libs/foo.bar/install"));
    	assertEquals("Matches dev runmode", new Integer(201), (Integer)f.getPriority("/apps/foo.bar/install.dev"));
    	assertEquals("Matches staging runmode", new Integer(201), (Integer)f.getPriority("/apps/foo.bar/install.staging"));
    	assertEquals("Matches three runmodes (A)", new Integer(203), (Integer)f.getPriority("/apps/foo.bar/install.dev.staging.prod"));
    	assertEquals("Matches three runmodes (B)", new Integer(203), (Integer)f.getPriority("/apps/foo.bar/install.dev.prod.staging"));
    	assertEquals("Matches three runmodes (C)", new Integer(103), (Integer)f.getPriority("/libs/foo.bar/install.dev.prod.staging"));
    }
}
