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

@RunWith(PaxExam.class)
public class BundleStatePreservedTest extends OsgiInstallerTestBase {

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
    public void testBundleStatePreserved() throws Exception {
    	// Install two bundles, one started, one stopped
    	{
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testA-1.0.jar")), null);
            this.waitForBundleEvents("Bundle must be installed", listener,
                    new BundleEvent("osgi-installer-testA", "1.0", org.osgi.framework.BundleEvent.INSTALLED),
                    new BundleEvent("osgi-installer-testA", "1.0", org.osgi.framework.BundleEvent.STARTED));
    	}
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testB-1.0.jar")), null);
            this.waitForBundleEvents("Bundle must be installed", listener,
                    new BundleEvent("osgi-installer-testB", "1.0", org.osgi.framework.BundleEvent.INSTALLED),
                    new BundleEvent("osgi-installer-testB", "1.0", org.osgi.framework.BundleEvent.STARTED));
            final Bundle b = findBundle("osgi-installer-testB");
            assertNotNull("Test bundle B must be found", b);
            b.stop();
        }

        assertBundle("Bundle A must be started", "osgi-installer-testA", null, Bundle.ACTIVE);
        assertBundle("Bundle B must be stopped", "osgi-installer-testB", null, Bundle.RESOLVED);

    	// Execute some OsgiController operations
        Object listener = this.startObservingBundleEvents();
        installer.updateResources(URL_SCHEME, getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar")), null);
        sleep(150);
        installer.updateResources(URL_SCHEME, getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.2.jar")), null);
        sleep(150);
        installer.updateResources(URL_SCHEME, getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")), null);
        this.waitForBundleEvents("Bundle must be installed", listener,
                new BundleEvent("osgi-installer-testbundle", "1.0", org.osgi.framework.BundleEvent.INSTALLED),
                new BundleEvent("osgi-installer-testbundle", "1.2", org.osgi.framework.BundleEvent.UPDATED),
                new BundleEvent("osgi-installer-testbundle", "1.2", org.osgi.framework.BundleEvent.STARTED));
        assertBundle("After installing testbundle", "osgi-installer-testbundle", "1.2", Bundle.ACTIVE);

        listener = this.startObservingBundleEvents();
        installer.updateResources(URL_SCHEME, null, getNonInstallableResourceUrl(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar")));
        sleep(150);
        this.assertNoBundleEvents("Update to same version should generate no OSGi tasks.", listener, "osgi-installer-testbundle");

        listener = this.startObservingBundleEvents();
        installer.updateResources(URL_SCHEME, null, getNonInstallableResourceUrl(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
        sleep(150);
        this.assertNoBundleEvents("Update to same version should generate no OSGi tasks.", listener, "osgi-installer-testbundle");

        listener = this.startObservingBundleEvents();
        installer.updateResources(URL_SCHEME, null, getNonInstallableResourceUrl(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.2.jar")));
        this.waitForBundleEvents("Bundle must be uninstalled", listener,
                new BundleEvent("osgi-installer-testbundle", "1.2", org.osgi.framework.BundleEvent.STOPPED),
                new BundleEvent("osgi-installer-testbundle", "1.2", org.osgi.framework.BundleEvent.UNINSTALLED));

        assertNull("testbundle must be gone at end of test", findBundle("osgi-installer-testbundle"));

    	// Now check that bundles A and B have kept their states
        assertBundle("Bundle A must still be started", "osgi-installer-testA", null, Bundle.ACTIVE);
        assertBundle("Bundle B must still be stopped", "osgi-installer-testB", null, Bundle.RESOLVED);
    }
}