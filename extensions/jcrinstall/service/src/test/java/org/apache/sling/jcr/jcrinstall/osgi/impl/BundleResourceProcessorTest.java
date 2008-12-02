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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.sling.jcr.jcrinstall.jcr.impl.MockStartLevel;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

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
        final TestStorage s = new TestStorage(Utilities.getTestFile());
        Utilities.setStorage(c, s);
        final Bundle b = mockery.mock(Bundle.class);
        final long bundleId = 1234;
        final String uri = "/test/bundle.jar";
        final MockInstallableData data = new MockInstallableData(uri);
        final InputStream is = data.adaptTo(InputStream.class);

        // We'll try installing a bundle, re-installing to cause
        // it to be updated, and removing
        mockery.checking(new Expectations() {{
            allowing(pa).refreshPackages(null);
            allowing(pa).resolveBundles(null);
            allowing(b).getBundleId() ;
            will(returnValue(bundleId));
            allowing(bc).addFrameworkListener(with(any(FrameworkListener.class)));

            one(bc).installBundle(OsgiControllerImpl.getResourceLocation(uri), is);
            inSequence(sequence);
            will(returnValue(b));

            allowing(bc).getBundle(bundleId);
            inSequence(sequence);
            will(returnValue(b));

            one(b).stop();
            inSequence(sequence);
            
            one(b).update(is);
            inSequence(sequence);

            one(b).uninstall();
            inSequence(sequence);
        }});

        // Do the calls and check some stuff on the way
        final BundleResourceProcessor p = new BundleResourceProcessor(bc, pa, new MockStartLevel());
        Utilities.setProcessors(c, p);
        assertFalse("Before install, uri must not be in list", c.getInstalledUris().contains(uri));

        assertEquals("First install returns INSTALLED", INSTALLED, c.scheduleInstallOrUpdate(uri, data));
        assertTrue("After install, uri must be in list", c.getInstalledUris().contains(uri));
        assertEquals("Digest must have been stored", data.getDigest(), c.getDigest(uri));
        assertEquals("Storage data has been saved during install", 1, s.saveCounter);

        data.setDigest("digest is now different");
        assertEquals("Second install returns UPDATED", UPDATED, c.scheduleInstallOrUpdate(uri, data));
        assertTrue("After update, uri must be in list", c.getInstalledUris().contains(uri));
        assertEquals("Digest must have been updated", data.getDigest(), c.getDigest(uri));
        assertEquals("Storage data has been saved during update", 2, s.saveCounter);

        c.scheduleUninstall(uri);
        assertFalse("After uninstall, uri must not be in list", c.getInstalledUris().contains(uri));
        assertEquals("Digest must be gone", null, c.getDigest(uri));
        assertFalse("After getLastModified, uri must not be in list", c.getInstalledUris().contains(uri));
        assertEquals("Storage data has been saved during uninstall", 3, s.saveCounter);

        final String nonJarUri = "no_jar_extension";
        assertEquals(nonJarUri + " must be ignored", c.scheduleInstallOrUpdate("", data), IGNORED);

        // And verify expectations
        mockery.assertIsSatisfied();
    }
}
