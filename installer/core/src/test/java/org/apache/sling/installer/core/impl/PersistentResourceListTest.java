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
package org.apache.sling.installer.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Iterator;

import org.apache.sling.installer.core.impl.EntityResourceList;
import org.apache.sling.installer.core.impl.PersistentResourceList;
import org.apache.sling.installer.core.impl.RegisteredResource;
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
        PersistentResourceList p = new PersistentResourceList(f);
        // check if data is available - we call a method which would result in an NPE otherwise!
        p.getEntityIds();
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
        PersistentResourceList p = new PersistentResourceList(f);
        // check if data is available - we call a method which would result in an NPE otherwise!
        p.getEntityIds();
        assertEquals("Constructor must fail gracefully with invalid data file", 0, p.getEntityIds().size());
    }

    @Test
    public void testTestData() throws IOException {
        File f = new File("NONEXISTENT");
        PersistentResourceList p = new PersistentResourceList(f);
        // check if data is available - we call a method which would result in an NPE otherwise!
        p.getEntityIds();
        addTestData(p);
        assertTestData(p);
    }

    @Test
    public void testSaveAndRetrieve() throws IOException {
        File f  = File.createTempFile(getClass().getSimpleName(), ".ser");
        f.deleteOnExit();
        {
            PersistentResourceList p = new PersistentResourceList(f);
            addTestData(p);
            p.save();
        }
        {
            PersistentResourceList p = new PersistentResourceList(f);
            assertTestData(p);
        }
    }

    private void addTestData(PersistentResourceList p) {
        for(int i = 0; i < TEST_SCALE; i++) {
            final String symbolicName = FAKE + i;
            for(int j= TEST_SCALE - 2; j >= 1; j--) {
                p.addOrUpdate(new MockBundleResource(symbolicName, getFakeVersion(i, j)));
            }
        }
    }

    private void assertTestData(PersistentResourceList p) {
        for(int i = 0; i < TEST_SCALE; i++) {
            final String symbolicName = FAKE + i;

            final EntityResourceList s = p.getEntityResourceList("bundle:" + symbolicName);
            assertNotNull(symbolicName + " must be found", s);

            final Iterator<RegisteredResource> it = s.getResources().iterator();
            for(int j= TEST_SCALE - 2; j >= 1; j--) {
                RegisteredResource r = it.next();
                assertNotNull("RegisteredResource " + j + " must be found", r);
                String sn = (String)r.getAttributes().get(Constants.BUNDLE_SYMBOLICNAME);
                assertEquals("RegisteredResource " + j + " symbolic name must match", symbolicName, sn);
                Version v = new Version((String)r.getAttributes().get(Constants.BUNDLE_VERSION));
                assertEquals("RegisteredResource " + j + " version must match", getFakeVersion(i, j), v.toString());
            }
            p.addOrUpdate(new MockBundleResource(symbolicName, "2." + i));
            p.addOrUpdate(new MockBundleResource(symbolicName, "3." + i));
        }
    }

    private String getFakeVersion(int i, int j) {
        return j + "." + i + ".0";
    }
}
