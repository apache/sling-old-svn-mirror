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

import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;

/** Test the second SLING-1106 scenario: if a bundle is updated
 * 	via the BundleContext, the installer should not downgrade it
 * 	back to its own version.
 */
@RunWith(PaxExam.class)
public class ContextBundleUpdateTest extends OsgiInstallerTestBase {

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
	public void testContextUpdate() throws Exception {

		// Install V1.0 via installer
        final String symbolicName = "osgi-installer-testbundle";
        assertNull("Test bundle must be absent before installing", findBundle(symbolicName));

        Object listener = this.startObservingBundleEvents();
        installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar"), "digest0"), null);
        this.waitForBundleEvents("Bundle must be active", listener,
                new BundleEvent(symbolicName, "1.0", org.osgi.framework.BundleEvent.INSTALLED),
                new BundleEvent(symbolicName, "1.0", org.osgi.framework.BundleEvent.STARTED));
        final Bundle b = assertBundle("After initial install", symbolicName, "1.0", Bundle.ACTIVE);

        // Update to 1.1, directly via bundle context
        final InputStream is = new FileInputStream(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar"));
        try {
        	b.update(is);
        } finally {
        	is.close();
        }
        assertBundle("After direct update", symbolicName, "1.1", Bundle.ACTIVE);

        // Install another bundle (to trigger installer queue activity), wait
        // for installer to be idle and check version
        listener = this.startObservingBundleEvents();
        installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-snap.jar"), "digest1"), null);
        this.waitForBundleEvents("Bundle must be active", listener,
                new BundleEvent("osgi-installer-snapshot-test", org.osgi.framework.BundleEvent.INSTALLED),
                new BundleEvent("osgi-installer-snapshot-test", org.osgi.framework.BundleEvent.STARTED));
        assertBundle("-snap bundle install", "osgi-installer-snapshot-test", null, Bundle.ACTIVE);
        assertBundle("After installing another bundle", symbolicName, "1.1", Bundle.ACTIVE);
	}
}
