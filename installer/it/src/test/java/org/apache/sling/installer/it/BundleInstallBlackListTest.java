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

import java.io.File;

import org.apache.sling.installer.api.OsgiInstaller;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

@RunWith(PaxExam.class)
public class BundleInstallBlackListTest extends OsgiInstallerTestBase {

    final String symbolicName = "osgi-installer-testbundle";

    @Override
    protected String requiredServices() {
        return "";
    }

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return defaultConfiguration();
    }

    @Override
    @Before
    public void setup() {
        // switch to system bundle context
        bundleContext = bundleContext.getBundle(0).getBundleContext();
        super.setup();
        System.setProperty("sling.launchpad",new File("src/test/resources/blacklisttest").getAbsolutePath());
        try {
            updateInstallerBundle();
        } catch ( BundleException be) {
            throw new RuntimeException(be);
        }
    }

    @Override
    @After
    public void tearDown() {
        super.tearDown();
        System.clearProperty("sling.launchpad");
    }

    @Test
    public void testBlacklistBundleVersion() throws Exception {

        assertNull("Test bundle must not be present before test", findBundle(symbolicName));

        // Install first test bundle and check version
        long bundleId = 0;
        {
            assertNull("Test bundle must be absent before installing", findBundle(symbolicName));
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar")), null);
            this.waitForBundleEvents(symbolicName + " must be installed", listener,
                new BundleEvent(symbolicName, "1.0", org.osgi.framework.BundleEvent.INSTALLED),
                new BundleEvent(symbolicName, "1.0", org.osgi.framework.BundleEvent.STARTED));
            final Bundle b = assertBundle("After installing", symbolicName, "1.0", Bundle.ACTIVE);
            bundleId = b.getBundleId();
        }

        // Try to install blacklisted version of bundle
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar"), "digestA"), null);
            sleep(150);
            this.assertNoBundleEvents("Bundle install of blacklisted version should not cause any change.", listener,
                symbolicName);
            final Bundle b = assertBundle("After updating to 1.1", symbolicName, "1.0", Bundle.ACTIVE);
            assertEquals("Bundle ID must not change after update", bundleId, b.getBundleId());
        }

    }

    @Test
    public void testUninstallWithBlacklistedVersions() throws Exception {

        // Reroute launchpad home containing empty blacklist for ramp up
        System.setProperty("sling.launchpad",new File("src/test/resources/blacklisttestempty").getAbsolutePath());
        updateInstallerBundle();

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

        // configure blacklist bundle to ignore older version
        System.setProperty("sling.launchpad",new File("src/test/resources/blacklisttest").getAbsolutePath());
        updateInstallerBundle();

        // Try to uninstall current version and verify uninstall instead of
        // downgrade to blacklisted version
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

    }

    private void updateInstallerBundle() throws BundleException {
        // wait a little bit for updating bundle
        sleep(2000);
        bundleContext.getServiceReference(OsgiInstaller.class).getBundle().update();
        // wait a little bit after updating bundle
        sleep(2000);
        setupInstaller();
    }

    @Test
    public void testUninstallWithBlacklistedIntermediateVersion() throws Exception {

        // Reroute launchpad home containing empty blacklist for ramp up
        System.setProperty("sling.launchpad",new File("src/test/resources/blacklisttestempty").getAbsolutePath());
        updateInstallerBundle();

        assertNull("Test bundle must not be present before test",
            findBundle(symbolicName));

        // Install first test bundle and check version
        long bundleId = 0;
        {
            assertNull("Test bundle must be absent before installing",
                findBundle(symbolicName));
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar")), null);
            this.waitForBundleEvents(symbolicName + " must be installed", listener,
                new BundleEvent(symbolicName, "1.0",
                    org.osgi.framework.BundleEvent.INSTALLED),
                new BundleEvent(symbolicName, "1.0",
                    org.osgi.framework.BundleEvent.STARTED));
            final Bundle b = assertBundle("After installing", symbolicName, "1.0",
                Bundle.ACTIVE);
            bundleId = b.getBundleId();
        }

        // Upgrade to later version, verify
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar"), "digestA"),
                null);
            this.waitForBundleEvents(symbolicName + " must be installed", listener,
                new BundleEvent(symbolicName, "1.0",
                    org.osgi.framework.BundleEvent.STOPPED),
                new BundleEvent(symbolicName, "1.1",
                    org.osgi.framework.BundleEvent.STARTED));
            final Bundle b = assertBundle("After updating to 1.1", symbolicName,
                "1.1", Bundle.ACTIVE);
            assertEquals("Bundle ID must not change after update", bundleId,
                b.getBundleId());
        }

        // upgrade to 3rd version
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.2.jar"), "digestA"),
                null);
            this.waitForBundleEvents(symbolicName + " must be installed", listener,
                new BundleEvent(symbolicName, "1.1",
                    org.osgi.framework.BundleEvent.STOPPED),
                new BundleEvent(symbolicName, "1.2",
                    org.osgi.framework.BundleEvent.STARTED));
            final Bundle b = assertBundle("After updating to 1.2", symbolicName,
                "1.2", Bundle.ACTIVE);
            assertEquals("Bundle ID must not change after update", bundleId,
                b.getBundleId());
        }

        // configure blacklist bundle to ignore 1.1 version
        // configure blacklist bundle to ignore older version
        System.setProperty("sling.launchpad",new File("src/test/resources/blacklisttest").getAbsolutePath());
        updateInstallerBundle();

        // Try to uninstall current version and verify uninstall instead of
        // downgrade to blacklisted version
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, null, getNonInstallableResourceUrl(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.2.jar")));
            this.waitForBundleEvents(symbolicName + " must be installed", listener,
                new BundleEvent(symbolicName, "1.2",
                    org.osgi.framework.BundleEvent.STOPPED),
                new BundleEvent(symbolicName, "1.0",
                    org.osgi.framework.BundleEvent.STARTED));

            final Bundle b = assertBundle("After uninstalling 1.2", symbolicName,
                "1.0", Bundle.ACTIVE);
            assertEquals("Bundle ID must not change after update", bundleId,
                b.getBundleId());
        }

    }

}