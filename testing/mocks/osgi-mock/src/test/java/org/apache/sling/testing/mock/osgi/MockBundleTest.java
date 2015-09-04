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
package org.apache.sling.testing.mock.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableMap;

public class MockBundleTest {

    private MockBundle bundle;

    @Before
    public void setUp() {
        bundle = (MockBundle) MockOsgi.newBundleContext().getBundle();
    }

    @Test
    public void testBundleId() {
        assertTrue(bundle.getBundleId() > 0);
    }

    @Test
    public void testBundleContxt() {
        assertNotNull(bundle.getBundleContext());
    }

    @Test
    public void testGetEntry() {
        assertNotNull(bundle.getEntry("/META-INF/test.txt"));
        assertNotNull(bundle.getEntry("META-INF/test.txt"));
        assertNull(bundle.getEntry("/invalid"));
    }

    @Test
    public void testGetStatie() {
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testGetHeaders() {
        bundle.setHeaders(ImmutableMap.of("prop1", "value1"));
        assertEquals("value1", bundle.getHeaders().get("prop1"));
        assertEquals("value1", bundle.getHeaders("en").get("prop1"));
    }

    @Test
    public void testGetSymbolicName() throws Exception {
        bundle.setSymbolicName("name-1");
        assertEquals("name-1", bundle.getSymbolicName());
    }

    @Test
    public void testGetLastModified() {
        bundle.setLastModified(42);
        assertEquals(42, bundle.getLastModified());
    }
    
    @Test
    public void getEntryPaths_noMatches() {
        assertNull(bundle.getEntryPaths("resources"));
    }    
    
    @Test
    public void getEntryPaths() {
        
        Enumeration<String> entryPaths = bundle.getEntryPaths("bundleData");
        
        List<String> paths = Collections.list(entryPaths);
        
        assertThat(paths.size(), CoreMatchers.is(1));
        assertTrue(paths.contains("bundleData/nested/"));
    }

    @Test
    public void getEntryPaths_leadingSlash() {
        
        Enumeration<String> entryPaths = bundle.getEntryPaths("bundleData");
        
        List<String> paths = Collections.list(entryPaths);
        
        assertThat(paths.size(), CoreMatchers.is(1));
        assertTrue(paths.contains("bundleData/nested/"));
    }

    @Test
    public void getEntryPaths_slash() {
        
        Enumeration<String> entryPaths = bundle.getEntryPaths("/");
        
        List<String> paths = Collections.list(entryPaths);
        
        // intentionally less precise as we don't want to be broken when e.g. test resources change 
        assertTrue(paths.size() >= 3);
        assertTrue(paths.contains("bundleData/"));
        assertTrue(paths.contains("OSGI-INF/"));
        assertTrue(paths.contains("META-INF/"));
    }

    @Test
    public void getEntryPaths_empty() {
        
        Enumeration<String> entryPaths = bundle.getEntryPaths("/");
        
        List<String> paths = Collections.list(entryPaths);
        
        // intentionally less precise as we don't want to be broken when e.g. test resources change 
        assertTrue(paths.size() >= 3);
        assertTrue(paths.contains("bundleData/"));
        assertTrue(paths.contains("OSGI-INF/"));
        assertTrue(paths.contains("META-INF/"));
    }
    
    @Test
    public void getEntryPaths_noMatch() {
        
        assertNull(bundle.getEntryPaths("/EMPTY"));
        assertNull(bundle.getEntryPaths("EMPTY"));
    }

    @Test
    public void getEntryPaths_Nested() {

        Enumeration<String> entryPaths = bundle.getEntryPaths("bundleData/nested");
        
        List<String> paths = Collections.list(entryPaths);
        
        assertThat(paths.size(), CoreMatchers.is(2));
        assertTrue(paths.contains("bundleData/nested/first.txt"));
        assertTrue(paths.contains("bundleData/nested/second.txt"));
    }
}
