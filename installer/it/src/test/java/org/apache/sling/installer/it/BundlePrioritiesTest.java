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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.sling.installer.api.InstallableResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;

@RunWith(PaxExam.class)
public class BundlePrioritiesTest extends OsgiInstallerTestBase {

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

    /**
     *  Use snapshots, it's the only bundles which are updated even if their version doesn't change
     */
    @Test
    public void testPrioritiesUsingSnapshots() throws Exception {
        // Install test bundle
        final String symbolicName = "osgi-installer-snapshot-test";
        assertNull("Snapshot test bundle must be absent before installing", findBundle(symbolicName));

        final int lowPriority = InstallableResource.DEFAULT_PRIORITY - 1;
        final int highPriority = InstallableResource.DEFAULT_PRIORITY + 1;

        // we install the bundle with header SNAPSHOT and value A
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-snap.jar"), "digest1"), null);
            this.waitForBundleEvents(symbolicName + " must be installed and active", listener,
                    new BundleEvent(symbolicName, org.osgi.framework.BundleEvent.INSTALLED),
                    new BundleEvent(symbolicName, org.osgi.framework.BundleEvent.STARTED));
            assertBundle("Initial install", symbolicName, null, Bundle.ACTIVE);
            final Bundle b = findBundle(symbolicName);
            assertEquals("A", b.getHeaders().get("SNAPSHOT"));
        }

        // we try to install the bundle with header SNAPSHOT and value B
        // it shouldn't be installed because of the low priority!
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-snapb.jar"), "digest2", lowPriority), null);
            sleep(1000L);
            this.assertNoBundleEvents("Low-priority snapshot updated must be ignored", listener, symbolicName);
            assertBundle("Update one", symbolicName, null, Bundle.ACTIVE);
            final Bundle b = findBundle(symbolicName);
            assertEquals("Low-priority snapshot updated must be ignored", "A", b.getHeaders().get("SNAPSHOT"));
        }

        // we try to install the bundle with header SNAPSHOT and value C
        // it should be installed because of the high priority
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-snapc.jar"), "digest3", highPriority), null);
            this.waitForBundleEvents(symbolicName + " must be installed and active", listener,
                    new BundleEvent(symbolicName, org.osgi.framework.BundleEvent.UPDATED),
                    new BundleEvent(symbolicName, org.osgi.framework.BundleEvent.STARTED));
            assertBundle("Update two", symbolicName, null, Bundle.ACTIVE);
            final Bundle b = findBundle(symbolicName);
            assertEquals("High-priority snapshot updated must be processed", "C", b.getHeaders().get("SNAPSHOT"));
        }
    }
}
