/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.installer.it;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;

@RunWith(PaxExam.class)
public class FragmentInstallTest extends OsgiInstallerTestBase {

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
    public void testInstallFragment() throws Exception {
        final String hostSymbolicName = "osgi-installer-testbundle";
        assertNull("Test host bundle must be absent before installing", findBundle(hostSymbolicName));
        final String fragmentSymbolicName = "osgi-installer-testfragment";
        assertNull("Test fragment bundle must be absent before installing", findBundle(fragmentSymbolicName));

        Object listener = this.startObservingBundleEvents();
        installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.2.jar"), "digest1"), null);
        // wait for two tasks: install and start
        this.waitForBundleEvents("Test bundle 1.2 must be found after waitForInstallerAction", listener,
                new BundleEvent(hostSymbolicName, "1.2", org.osgi.framework.BundleEvent.INSTALLED),
                new BundleEvent(hostSymbolicName, "1.2", org.osgi.framework.BundleEvent.STARTED));
        this.assertBundle("Bundle version 1.2 must be installed.", hostSymbolicName, "1.2", Bundle.ACTIVE);

        listener = this.startObservingBundleEvents();
        installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testfragment-1.0.jar"), "digest2"), null);
        this.waitForBundleEvents("Fragment bundle 1.0 must be found after waitForInstallerAction", listener,
                new BundleEvent(fragmentSymbolicName, "1.0", org.osgi.framework.BundleEvent.RESOLVED),
                new BundleEvent(hostSymbolicName, "1.2", org.osgi.framework.BundleEvent.STARTED));
        this.assertBundle("Host bundle must be still be active.", hostSymbolicName, "1.2", Bundle.ACTIVE);

        // Verify that package provided by fragment is exported by host
        final Bundle host = findBundle(hostSymbolicName);
        assertTrue("Expecting host bundle to export its own package", isPackageExported(host, "com.example.testbundle.somepackage"));
        assertTrue("Expecting host bundle to export the fragment's package", isPackageExported(host, "com.example.testfragment.fragmentpackage"));
    }
}
