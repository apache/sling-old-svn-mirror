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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;

@RunWith(PaxExam.class)
public class BundleInstallUpgradeDowngradeTest extends OsgiInstallerTestBase {

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
    public void testInstallUpgradeDowngradeBundle() throws Exception {
    	final String symbolicName = "osgi-installer-testbundle";

    	assertNull("Test bundle must not be present before test", findBundle(symbolicName));

    	// Install first test bundle and check version
    	long bundleId = 0;
    	{
            assertNull("Test bundle must be absent before installing", findBundle(symbolicName));
            final Object listener = this.startObservingBundleEvents();
    	    installer.updateResources(URL_SCHEME, getInstallableResource(
    	            getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")), null);
            this.waitForBundleEvents(symbolicName + " must be installed", listener,
                    new BundleEvent(symbolicName, "1.1", org.osgi.framework.BundleEvent.INSTALLED),
                    new BundleEvent(symbolicName, "1.1", org.osgi.framework.BundleEvent.STARTED));
    	    final Bundle b = assertBundle("After installing", symbolicName, "1.1", Bundle.ACTIVE);
    	    bundleId = b.getBundleId();
    	}

    	// Upgrade to later version, verify
    	{
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.2.jar"), "digestA"), null);
            this.waitForBundleEvents(symbolicName + " must be installed", listener,
                    new BundleEvent(symbolicName, "1.1", org.osgi.framework.BundleEvent.STOPPED),
                    new BundleEvent(symbolicName, "1.2", org.osgi.framework.BundleEvent.STARTED));
        	final Bundle b = assertBundle("After updating to 1.2", symbolicName, "1.2", Bundle.ACTIVE);
        	assertEquals("Bundle ID must not change after update", bundleId, b.getBundleId());
    	}

    	// Downgrade to lower version, installed bundle must not change
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar"), "digestA"), null);
            sleep(150);

            // make sure no updates happen
            this.assertNoBundleEvents("Older bundle should not be installed.", listener, symbolicName);
            final Bundle b = assertBundle("After ignored downgrade", symbolicName, "1.2", Bundle.ACTIVE);
            assertEquals("Bundle ID must not change after ignored downgrade", bundleId, b.getBundleId());
        }

    	// Update to same version with different digest must be ignored
    	{
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar"), "digestB"), null);
            sleep(150);
            this.assertNoBundleEvents("Update to same version should generate no OSGi tasks.", listener, symbolicName);
    	}

    	// Uninstall
    	{
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, null, getNonInstallableResourceUrl(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar")));
            sleep(150);
            this.assertNoBundleEvents("Older bundle remove should not cause a remove", listener, symbolicName);
    	}
    	{
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, null, getNonInstallableResourceUrl(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
            this.assertNoBundleEvents("Older bundle remove should not cause a remove", listener, symbolicName);
    	}
    	{
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, null, getNonInstallableResourceUrl(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.2.jar")));
            this.waitForBundleEvents(symbolicName + " must be installed", listener,
                    new BundleEvent(symbolicName, "1.2", org.osgi.framework.BundleEvent.STOPPED),
                    new BundleEvent(symbolicName, "1.2", org.osgi.framework.BundleEvent.UNINSTALLED));

            final Bundle b = findBundle(symbolicName);
            assertNull("Testbundle must be gone", b);
    	}

    	// Reinstall lower version, must work
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")), null);
            this.waitForBundleEvents(symbolicName + " reinstall with lower version", listener,
                    new BundleEvent(symbolicName, "1.1", org.osgi.framework.BundleEvent.INSTALLED),
                    new BundleEvent(symbolicName, "1.1", org.osgi.framework.BundleEvent.STARTED));
            assertBundle("After reinstalling 1.1", symbolicName, "1.1", Bundle.ACTIVE);
        }
	}

    @Test
    public void testRemoveAndReadd() throws Exception {
        final String symbolicName = "osgi-installer-testbundle";

        {
            assertNull("Test bundle must be absent before installing", findBundle(symbolicName));

            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")), null);
            this.waitForBundleEvents(symbolicName + " should be installed with version 1.1", listener,
                    new BundleEvent(symbolicName, "1.1", org.osgi.framework.BundleEvent.INSTALLED),
                    new BundleEvent(symbolicName, "1.1", org.osgi.framework.BundleEvent.STARTED));
            assertBundle("After installing", symbolicName, "1.1", Bundle.ACTIVE);
        }
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, null, getNonInstallableResourceUrl(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
            this.waitForBundleEvents(symbolicName + " should be gone", listener,
                    new BundleEvent(symbolicName, "1.1", org.osgi.framework.BundleEvent.STOPPED),
                    new BundleEvent(symbolicName, "1.1", org.osgi.framework.BundleEvent.UNINSTALLED));
            assertNull("Test bundle must be gone", findBundle(symbolicName));
        }
        // let's sleep a little bit to avoid sync problems with the file data store.
        this.sleep(300);
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")), null);
            this.waitForBundleEvents(symbolicName + " should be reinstalled with version 1.1", listener,
                    new BundleEvent(symbolicName, "1.1", org.osgi.framework.BundleEvent.INSTALLED),
                    new BundleEvent(symbolicName, "1.1", org.osgi.framework.BundleEvent.STARTED));
            assertBundle("After reinstalling", symbolicName, "1.1", Bundle.ACTIVE);
        }
    }

    /**
     * This test first installs the test bundle in version 1.0 and then removes this version while
     * at the same time adding version 1.1 of the bundle.
     * This test should ensure that this results in a bundle update and not in a bundle remove/install!
     */
    @Test
    public void testUpdateAndRemove() throws Exception {
        final String symbolicName = "osgi-installer-testbundle";
        assertNull("Test bundle must be absent before installing", findBundle(symbolicName));

        // install version 1.0
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar")), null);
            this.waitForBundleEvents(symbolicName + " should be installed with version 1.0", listener,
                    new BundleEvent(symbolicName, "1.0", org.osgi.framework.BundleEvent.INSTALLED),
                    new BundleEvent(symbolicName, "1.0", org.osgi.framework.BundleEvent.STARTED));
            assertBundle("After installing", symbolicName, "1.0", Bundle.ACTIVE);
        }
        // install version 1.1 and remove 1.0 in one go
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME,
                    getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")),
                    getNonInstallableResourceUrl(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar")));
            this.waitForBundleEvents(symbolicName + " should be updated", listener,
                    new BundleEvent(symbolicName, null, org.osgi.framework.BundleEvent.UPDATED),
                    new BundleEvent(symbolicName, "1.1", org.osgi.framework.BundleEvent.STARTED));
            assertBundle("After installing", symbolicName, "1.1", Bundle.ACTIVE);
        }

    }
}