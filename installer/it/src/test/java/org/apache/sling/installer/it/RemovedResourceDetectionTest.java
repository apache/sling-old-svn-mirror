/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.installer.it;

import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.installer.api.InstallableResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;

/** Verify that the removal of resources is detected even if
 *  the OsgiController was stopped when resource was removed.
 */
@RunWith(PaxExam.class)
public class RemovedResourceDetectionTest extends OsgiInstallerTestBase {

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return defaultConfiguration();
    }
    
    @Before
    public void setUp() {
        setupInstaller();
    }

    @After
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testRemoveResourceAndRestart() throws Exception {

        final String symbolicNameA = "osgi-installer-testbundle";
        final String symbolicNameB = "osgi-installer-testB";

        // Install two bundles and verify
        assertNull(symbolicNameA + " must be absent before installing", findBundle(symbolicNameA));

        Object listener = this.startObservingBundleEvents();
        installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")), null);
        this.waitForBundleEvents("Bundle must be installed", listener,
                new BundleEvent(symbolicNameA, "1.1", org.osgi.framework.BundleEvent.INSTALLED),
                new BundleEvent(symbolicNameA, "1.1", org.osgi.framework.BundleEvent.STARTED));
        assertBundle("After initial install", symbolicNameA, "1.1", Bundle.ACTIVE);

        assertNull(symbolicNameB + " must be absent before installing", findBundle(symbolicNameB));
        listener = this.startObservingBundleEvents();
        installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testB-1.0.jar")), null);
        this.waitForBundleEvents("Bundle must be installed", listener,
                new BundleEvent(symbolicNameB, "1.0", org.osgi.framework.BundleEvent.INSTALLED),
                new BundleEvent(symbolicNameB, "1.0", org.osgi.framework.BundleEvent.STARTED));
        assertBundle("After initial install", symbolicNameB, "1.0", Bundle.ACTIVE);
        assertBundle("After initial install", symbolicNameA, "1.1", Bundle.ACTIVE);

        // Restart installer, register only second bundle and verify that first one is gone
        restartInstaller();
        listener = this.startObservingBundleEvents();
        final List<InstallableResource> data = new ArrayList<InstallableResource>();
        data.add(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testB-1.0.jar"))[0]);
        installer.registerResources(URL_SCHEME, data.toArray(new InstallableResource[data.size()]));
        sleep(500);
        this.waitForBundleEvents("Bundle must be installed", listener,
                new BundleEvent(symbolicNameA, org.osgi.framework.BundleEvent.STOPPED),
                new BundleEvent(symbolicNameA, org.osgi.framework.BundleEvent.UNINSTALLED));
        assertBundle("After installer restart", symbolicNameB, "1.0", Bundle.ACTIVE);
        assertNull("Bundle not in second list should be removed", findBundle(symbolicNameA));
    }
}