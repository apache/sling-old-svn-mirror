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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;

/** Verify that snapshot bundles are updated even if
 *  their version number does not change.
 *  TODO: digests should be taken into account, i.e. no action
 *  if digest does not change
 */
@RunWith(PaxExam.class)
public class BundleSnapshotUpdateTest extends OsgiInstallerTestBase {

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
    public void testNonSnapshot() throws Exception {
        final String symbolicName = "osgi-installer-testbundle";
        assertNull("Test bundle must be absent before installing", findBundle(symbolicName));

        Object listener = this.startObservingBundleEvents();
        installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar"), "digest1"), null);
        // wait for two tasks: install and start
        this.waitForBundleEvents("Test bundle 1.1 must be found after waitForInstallerAction", listener,
                new BundleEvent(symbolicName, "1.1", org.osgi.framework.BundleEvent.INSTALLED),
                new BundleEvent(symbolicName, "1.1", org.osgi.framework.BundleEvent.STARTED));
        this.assertBundle("Bundle version 1.1 must be installed.", symbolicName, "1.1", Bundle.ACTIVE);

        // Update with same digest must be ignored
        listener = this.startObservingBundleEvents();
        installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar"), "digest1"), null);
        sleep(100);
        this.assertNoBundleEvents("Update with same digest must be ignored.", listener, symbolicName);
        this.assertBundle("Bundle version 1.1 must be installed.", symbolicName, "1.1", Bundle.ACTIVE);

        // Update with different digest must be ignored
        listener = this.startObservingBundleEvents();
        installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar"), "digest2"), null);
        sleep(100);
        this.assertNoBundleEvents("Update with different digest must be ignored.", listener, symbolicName);
        this.assertBundle("Bundle version 1.1 must be installed.", symbolicName, "1.1", Bundle.ACTIVE);
    }

    private void testSnapshotPrimitive(boolean restartInstaller) throws Exception {

        // Install test bundle
        final String symbolicName = "osgi-installer-snapshot-test";
        assertNull("Snapshot test bundle must be absent before installing", findBundle(symbolicName));

        Object listener = this.startObservingBundleEvents();
        installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-snap.jar"), "digest1"), null);
        // wait for two tasks: install and start
        this.waitForBundleEvents("Test bundle must be found after waitForInstallerAction", listener,
                new BundleEvent(symbolicName, org.osgi.framework.BundleEvent.INSTALLED),
                new BundleEvent(symbolicName, org.osgi.framework.BundleEvent.STARTED));
        final Bundle b = findBundle(symbolicName);
        assertNotNull("Snapshot bundle must be found after waitForInstallerAction", b);

        if (restartInstaller) {
            restartInstaller();
        }

        // Update with same digest must be ignored
        listener = this.startObservingBundleEvents();
        installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-snap.jar"), "digest1"), null);
        sleep(100);
        this.assertNoBundleEvents("Update with same digest must be ignored.", listener, symbolicName);

        // Update with different digest must generate an OSGi update
        listener = this.startObservingBundleEvents();
        installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-snap.jar"), "digest2"), null);
        sleep(100);
        this.waitForBundleEvents("Test bundle must be updated with different digest and snapshot", listener,
                new BundleEvent(symbolicName, org.osgi.framework.BundleEvent.STOPPED),
                new BundleEvent(symbolicName, org.osgi.framework.BundleEvent.UPDATED),
                new BundleEvent(symbolicName, org.osgi.framework.BundleEvent.STARTED));
    }

    @Test
    public void testSnapshot() throws Exception {
        testSnapshotPrimitive(false);
    }

    @Test
    public void testSnapshotWithInstallerRestart() throws Exception {
        testSnapshotPrimitive(true);
    }
}