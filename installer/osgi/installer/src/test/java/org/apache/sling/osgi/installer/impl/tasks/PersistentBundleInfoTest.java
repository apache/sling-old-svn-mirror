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
package org.apache.sling.osgi.installer.impl.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.TreeSet;

import org.apache.sling.osgi.installer.impl.tasks.PersistentBundleInfo;
import org.junit.Before;
import org.junit.Test;

public class PersistentBundleInfoTest {
    private PersistentBundleInfo storage;
    private File testFile;
    private TreeSet<String> installedBundles;
    private static final String NO_VERSION = "";

    @Before
    public void setUp() throws Exception {
        testFile = File.createTempFile(getClass().getSimpleName(), "properties");
        testFile.deleteOnExit();
        storage = new PersistentBundleInfo(testFile);
        installedBundles = new TreeSet<String>();
    }

    @Test
    public void testCloseAndReopen() throws IOException {
        storage.putInfo("foo", "bar", NO_VERSION);
        assertEquals("Before save, expecting bar digest", "bar", storage.getDigest("foo"));
        installedBundles.add("foo");
        storage.purgeAndSave(installedBundles);
        assertEquals("After save, expecting bar digest", "bar", storage.getDigest("foo"));
        storage.putInfo("foo", "wii", NO_VERSION);
        assertEquals("After change, expecting wii digest", "wii", storage.getDigest("foo"));
        storage = null;
        final PersistentBundleInfo copy = new PersistentBundleInfo(testFile);
        assertEquals("In copy saved before change, expecting bar digest", "bar", copy.getDigest("foo"));
    }

    @Test
    public void testPurge() throws IOException {
        for(int i=0; i < 50; i++) {
            storage.putInfo("foo" + i, "bar" + i, "1." + i);
            if(i % 2 == 0) {
                installedBundles.add("foo" + i);
            }
        }
        for(int i=0; i < 50; i++) {
            assertEquals("Before save, expecting digest to match at step " + i, "bar" + i,
                    storage.getDigest("foo" + i));
            assertEquals("Before save, expecting version to match at step " + i, "1." + i,
                    storage.getInstalledVersion("foo" + i));
        }
        storage.purgeAndSave(installedBundles);
        for(int i=0; i < 50; i++) {
            if(i % 2 != 0) {
                assertNull("After purge, expecting null digest at step " + i,
                        storage.getDigest("foo" + i));
                assertNull("After purge, expecting null version at step " + i,
                        storage.getInstalledVersion("foo" + i));
            } else {
                assertNotNull("After purge, expecting non-null digest at step " + i,
                        storage.getDigest("foo" + i));
                assertEquals("After purge, expecting digest to match at step " + i, "bar" + i,
                        storage.getDigest("foo" + i));
                assertNotNull("After purge, expecting non-null version at step " + i,
                        storage.getInstalledVersion("foo" + i));
                assertEquals("After purge, expecting version to match at step " + i, "1." + i,
                        storage.getInstalledVersion("foo" + i));
            }
        }
        storage = null;
        final PersistentBundleInfo copy = new PersistentBundleInfo(testFile);
        for(int i=0; i < 50; i++) {
            if(i % 2 != 0) {
                assertNull("In copy, expecting null digest at step " + i,
                        copy.getDigest("foo" + i));
                assertNull("In copy, expecting null version at step " + i,
                        copy.getInstalledVersion("foo" + i));
            } else {
                assertNotNull("In copy, expecting non-null digest at step " + i,
                        copy.getDigest("foo" + i));
                assertEquals("In copy, expecting digest to match at step " + i, "bar" + i,
                        copy.getDigest("foo" + i));
                assertNotNull("In copy, expecting non-null version at step " + i,
                        copy.getInstalledVersion("foo" + i));
                assertEquals("In copy, expecting version to match at step " + i, "1." + i,
                        copy.getInstalledVersion("foo" + i));
            }
        }
    }
 }
