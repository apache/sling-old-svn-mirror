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
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/** Test the PersistentResourceList */
public class PersistentResourceListTest {
    private final static int TEST_SCALE = 5;
    private final static String FAKE = "fakebundle.";

    @Test
    public void testFileNotFound() throws IOException {
        File f = new File("NONEXISTENT");
        PersistentResourceList p = new PersistentResourceList(null, f);
        assertNotNull(p.getData());
    }

    @Test
    public void testBadDataFile() throws IOException {
        File f  = File.createTempFile(getClass().getSimpleName(), ".ser");
        f.deleteOnExit();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
        try {
            oos.writeObject("Some data that's not what we expect");
        } finally {
            oos.close();
        }
        PersistentResourceList p = new PersistentResourceList(null, f);
        assertNotNull(p.getData());
        assertEquals("Constructor must fail gracefully with invalid data file", 0, p.getData().size());
    }

    @Test
    public void testTestData() throws IOException {
        File f = new File("NONEXISTENT");
        PersistentResourceList p = new PersistentResourceList(null, f);
        assertNotNull(p.getData());
        addTestData(p);
        assertTestData(p);
    }

    @Test
    public void testSaveAndRetrieve() throws IOException {
        File f  = File.createTempFile(getClass().getSimpleName(), ".ser");
        f.deleteOnExit();
        {
            PersistentResourceList p = new PersistentResourceList(null, f);
            addTestData(p);
            p.save();
        }
        {
            PersistentResourceList p = new PersistentResourceList(null, f);
            assertTestData(p);
        }
    }

    private void addTestData(PersistentResourceList p) {
        for(int i = 0; i < TEST_SCALE; i++) {
            final String symbolicName = FAKE + i;
            TreeSet<RegisteredResource> s = new TreeSet<RegisteredResource>();
            for(int j= TEST_SCALE - 2; j >= 1; j--) {
                s.add(new MockBundleResource(symbolicName, getFakeVersion(i, j)));
            }
            p.getData().put(symbolicName, s);
        }
    }

    private void assertTestData(PersistentResourceList p) {
        for(int i = 0; i < TEST_SCALE; i++) {
            final String symbolicName = FAKE + i;
            SortedSet<RegisteredResource> s = p.getData().get(symbolicName);
            assertNotNull(symbolicName + " must be found", s);
            Iterator<RegisteredResource> it = s.iterator();
            for(int j= TEST_SCALE - 2; j >= 1; j--) {
                RegisteredResource r = it.next();
                assertNotNull("RegisteredResource " + j + " must be found");
                String sn = (String)r.getAttributes().get(Constants.BUNDLE_SYMBOLICNAME);
                assertEquals("RegisteredResource " + j + " symbolic name must match", symbolicName, sn);
                Version v = new Version((String)r.getAttributes().get(Constants.BUNDLE_VERSION));
                assertEquals("RegisteredResource " + j + " version must match", getFakeVersion(i, j), v.toString());
            }
            s.add(new MockBundleResource(symbolicName, "2." + i));
            s.add(new MockBundleResource(symbolicName, "3." + i));
            p.getData().put(symbolicName, s);
        }
    }

    private String getFakeVersion(int i, int j) {
        return j + "." + i + ".0";
    }
}
