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
package org.apache.sling.osgi.installer.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

public class BundleDigestsStorageTest {
    private BundleDigestsStorage storage;
    private File testFile;
    private TreeSet<String> installedBundles;
    
    @Before
    public void setUp() throws Exception {
        testFile = File.createTempFile(getClass().getSimpleName(), "properties");
        testFile.deleteOnExit();
        storage = new BundleDigestsStorage(new MockOsgiInstallerContext(), testFile);
        installedBundles = new TreeSet<String>();
    }
    
    @Test
    public void testCloseAndReopen() throws IOException {
        storage.putDigest("foo", "bar");
        assertEquals("Before save, expecting bar digest", "bar", storage.getDigest("foo"));
        installedBundles.add("foo");
        storage.purgeAndSave(installedBundles);
        assertEquals("After save, expecting bar digest", "bar", storage.getDigest("foo"));
        storage.putDigest("foo", "wii");
        assertEquals("After change, expecting wii digest", "wii", storage.getDigest("foo"));
        storage = null;
        final BundleDigestsStorage copy = new BundleDigestsStorage(new MockOsgiInstallerContext(), testFile);
        assertEquals("In copy saved before change, expecting bar digest", "bar", copy.getDigest("foo"));
    }
    
    @Test
    public void testPurge() throws IOException {
        for(int i=0; i < 50; i++) {
            storage.putDigest("foo" + i, "bar" + i);
            if(i % 2 == 0) {
                installedBundles.add("foo" + i);
            }
        }
        for(int i=0; i < 50; i++) {
            assertEquals("Before save, expecting digest to match at step " + i, "bar" + i, storage.getDigest("foo" + i));
        }
        storage.purgeAndSave(installedBundles);
        for(int i=0; i < 50; i++) {
            if(i % 2 != 0) {
                assertNull("After purge, expecting null digest at step " + i, storage.getDigest("foo" + i));
            } else {
                assertEquals("After purge, expecting digest to match at step " + i, "bar" + i, storage.getDigest("foo" + i));
            }
        }
        storage = null;
        final BundleDigestsStorage copy = new BundleDigestsStorage(new MockOsgiInstallerContext(), testFile);
        for(int i=0; i < 50; i++) {
            if(i % 2 != 0) {
                assertNull("In copy, expecting null digest at step " + i, copy.getDigest("foo" + i));
            } else {
                assertEquals("In copy, expecting digest to match at step " + i, "bar" + i, copy.getDigest("foo" + i));
            }
        }
    }
 }
