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
package org.apache.sling.jcr.jcrinstall.osgi.impl;

import static org.apache.sling.jcr.jcrinstall.osgi.InstallResultCode.IGNORED;
import static org.apache.sling.jcr.jcrinstall.osgi.InstallResultCode.INSTALLED;
import static org.apache.sling.jcr.jcrinstall.osgi.InstallResultCode.UPDATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;

/** Test the BundleResourceProcessor */
public class BundleResourceProcessorTest {

    private Mockery mockery;
    private Sequence sequence;

    static class TestStorage extends Storage {
        int saveCounter;

        TestStorage(File f) throws IOException {
            super(f);
        }

        @Override
        protected void saveToFile() throws IOException {
            super.saveToFile();
            saveCounter++;
        }
    }

    @org.junit.Before public void setup() {
        mockery = new Mockery();
        sequence = mockery.sequence(getClass().getSimpleName());
    }

    @org.junit.Test public void testInstall() throws Exception {

        final OsgiControllerImpl c = new OsgiControllerImpl();
        final BundleContext bc = mockery.mock(BundleContext.class);
        final PackageAdmin pa = mockery.mock(PackageAdmin.class);
        final BundleResourceProcessor p = new BundleResourceProcessor(bc, pa);
        Utilities.setProcessors(c, p);
        final TestStorage s = new TestStorage(Utilities.getTestFile());
        Utilities.setStorage(c, s);
        final Bundle b = mockery.mock(Bundle.class);
        final long bundleId = 1234;
        final String uri = "/test/bundle.jar";
        final InputStream data = new ByteArrayInputStream(uri.getBytes());
        long lastModified = System.currentTimeMillis();

        // We'll try installing a bundle, re-installing to cause
        // it to be updated, and removing
        mockery.checking(new Expectations() {{
            allowing(pa).refreshPackages(null);
            allowing(pa).resolveBundles(null);
            allowing(b).getBundleId() ;
            will(returnValue(bundleId));

            one(bc).installBundle(OsgiControllerImpl.getResourceLocation(uri), data);
            inSequence(sequence);
            will(returnValue(b));

            allowing(bc).getBundle(bundleId);
            inSequence(sequence);
            will(returnValue(b));

            one(b).update(data);
            inSequence(sequence);

            one(b).uninstall();
            inSequence(sequence);
        }});

        // Do the calls and check some stuff on the way
        assertFalse("Before install, uri must not be in list", c.getInstalledUris().contains(uri));

        assertEquals("First install returns INSTALLED", INSTALLED, c.installOrUpdate(uri, lastModified, data));
        assertTrue("After install, uri must be in list", c.getInstalledUris().contains(uri));
        assertEquals("LastModified must have been stored", lastModified, c.getLastModified(uri));
        assertEquals("Storage data has been saved during install", 1, s.saveCounter);

        lastModified = System.currentTimeMillis();
        assertEquals("Second install returns UPDATED", UPDATED, c.installOrUpdate(uri, lastModified, data));
        assertTrue("After update, uri must be in list", c.getInstalledUris().contains(uri));
        assertEquals("LastModified must have been updated", lastModified, c.getLastModified(uri));
        assertEquals("Storage data has been saved during update", 2, s.saveCounter);

        c.uninstall(uri);
        assertFalse("After uninstall, uri must not be in list", c.getInstalledUris().contains(uri));
        assertEquals("LastModified must be gone", -1, c.getLastModified(uri));
        assertFalse("After getLastModified, uri must not be in list", c.getInstalledUris().contains(uri));
        assertEquals("Storage data has been saved during uninstall", 3, s.saveCounter);

        final String nonJarUri = "no_jar_extension";
        assertEquals(nonJarUri + " must be ignored", c.installOrUpdate("", lastModified, data), IGNORED);

        // And verify expectations
        mockery.assertIsSatisfied();
    }

    @org.junit.Test public void testBundleProcessingQueue() throws Exception {

        // Fill the pending bundles queue with one bundle in each of the
        // possible states, process the queue and verify results
        final PackageAdmin pa = mockery.mock(PackageAdmin.class);
        final BundleContext bc = mockery.mock(BundleContext.class);
        final Bundle [] b = new Bundle[6];
        for(int i = 0; i < b.length; i++) {
            b[i] = mockery.mock(Bundle.class);
        };

        mockery.checking(new Expectations() {{
            allowing(pa).refreshPackages(null);
            allowing(pa).resolveBundles(with(any(Bundle[].class)));

            allowing(bc).getBundle(0L); will(returnValue(b[0]));
            allowing(bc).getBundle(1L); will(returnValue(b[1]));
            allowing(bc).getBundle(2L); will(returnValue(b[2]));
            allowing(bc).getBundle(3L); will(returnValue(b[3]));
            allowing(bc).getBundle(4L); will(returnValue(b[4]));
            allowing(bc).getBundle(5L); will(returnValue(b[5]));

            allowing(b[0]).getBundleId(); will(returnValue(0L));
            allowing(b[1]).getBundleId(); will(returnValue(1L));
            allowing(b[2]).getBundleId(); will(returnValue(2L));
            allowing(b[3]).getBundleId(); will(returnValue(3L));
            allowing(b[4]).getBundleId(); will(returnValue(4L));
            allowing(b[5]).getBundleId(); will(returnValue(5L));

            allowing(b[0]).getState(); will(returnValue(Bundle.ACTIVE));
            allowing(b[1]).getState(); will(returnValue(Bundle.STARTING));
            allowing(b[2]).getState(); will(returnValue(Bundle.STOPPING));
            allowing(b[3]).getState(); will(returnValue(Bundle.UNINSTALLED));
            allowing(b[4]).getState(); will(returnValue(Bundle.INSTALLED));
            allowing(b[5]).getState(); will(returnValue(Bundle.RESOLVED));

            allowing(b[0]).getLocation();
            allowing(b[1]).getLocation();
            allowing(b[2]).getLocation();
            allowing(b[3]).getLocation();
            allowing(b[4]).getLocation();
            allowing(b[5]).getLocation();

            one(b[5]).start();
        }});

        final BundleResourceProcessor p = new BundleResourceProcessor(bc, pa);
        final Map<Long, Bundle> pendingBundles = new HashMap<Long, Bundle>();
        Utilities.setField(p, "pendingBundles", pendingBundles);

        for(Bundle bu : b) {
            pendingBundles.put(new Long(bu.getBundleId()), bu);
        }
        p.processResourceQueue();

        assertEquals("Only 4 bundles must be left in queue", 4, pendingBundles.size());
        assertTrue("STARTING bundle must be left in queue", pendingBundles.containsKey(b[1].getBundleId()));
        assertTrue("STOPPING bundle must be left in queue", pendingBundles.containsKey(b[2].getBundleId()));
        assertTrue("INSTALLED bundle must be left in queue", pendingBundles.containsKey(b[4].getBundleId()));
        assertTrue("RESOLVED bundle must be left in queue", pendingBundles.containsKey(b[5].getBundleId()));
    }

}
