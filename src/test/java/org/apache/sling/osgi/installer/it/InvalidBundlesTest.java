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
package org.apache.sling.osgi.installer.it;

import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.sling.osgi.installer.InstallableResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;

@RunWith(JUnit4TestRunner.class)
public class InvalidBundlesTest extends OsgiInstallerTestBase {

    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() {
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
    public void testRegisterInvalidBundles() throws Exception {
        final Collection<InstallableResource> data = new ArrayList<InstallableResource>();
        data.add(getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
        data.add(getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-notabundle.jar")));
        data.add(getInstallableResource(
                getTestBundle("test-classes/invalid-jar.jar")));
        data.add(getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testB-1.0.jar")));

        final Object listener = this.startObservingBundleEvents();
        installer.registerResources(URL_SCHEME, data);
        this.waitForBundleEvents("All valid bundles must be installed and started.", listener,
                new BundleEvent("osgi-installer-testbundle", "1.1", org.osgi.framework.BundleEvent.INSTALLED),
                new BundleEvent("osgi-installer-testbundle", "1.1", org.osgi.framework.BundleEvent.STARTED),
                new BundleEvent("osgi-installer-testB", "1.0", org.osgi.framework.BundleEvent.INSTALLED),
                new BundleEvent("osgi-installer-testB", "1.0", org.osgi.framework.BundleEvent.STARTED));

        final String info = "All valid bundles must be installed";
        assertBundle(info, "osgi-installer-testbundle", "1.1", Bundle.ACTIVE);
        assertBundle(info, "osgi-installer-testB", "1.0", Bundle.ACTIVE);
    }

	@Test
    public void testIndividualInvalidBundles() throws Exception {
    	final String symbolicName = "osgi-installer-testbundle";

    	assertNull("Test bundle must not be present before test", findBundle(symbolicName));

    	// Install first test bundle and check version
    	{
            assertNull("Test bundle must be absent before installing", findBundle(symbolicName));

            final Object listener = this.startObservingBundleEvents();
            installer.addResource(URL_SCHEME, getInstallableResource(
    	            getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
            this.waitForBundleEvents("Bundle must be installed and started.", listener,
                    new BundleEvent(symbolicName, "1.1", org.osgi.framework.BundleEvent.INSTALLED),
                    new BundleEvent(symbolicName, "1.1", org.osgi.framework.BundleEvent.STARTED));
    	    assertBundle("After installing", symbolicName, "1.1", Bundle.ACTIVE);
    	}

    	// Non-bundle must be ignored
    	{
            final Object listener = this.startObservingBundleEvents();
            installer.addResource(URL_SCHEME, getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-notabundle.jar")));
            this.assertNoBundleEvents("Invalid jar should be ignored", listener, null);
    	}

        // Invalid archive must be ignored
        {
            final Object listener = this.startObservingBundleEvents();
            installer.addResource(URL_SCHEME, getInstallableResource(getTestBundle("test-classes/invalid-jar.jar")));
            this.assertNoBundleEvents("Invalid archive should be ignored", listener, null);
        }

    	// Make sure controller is not blocked, by testing an upgrade
    	{
            final Object listener = this.startObservingBundleEvents();
            installer.addResource(URL_SCHEME, getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.2.jar")));
            this.waitForBundleEvents("Bundle must be updated.", listener,
                    new BundleEvent(symbolicName, "1.2", org.osgi.framework.BundleEvent.STARTED));
        	assertBundle("After updating to 1.2", symbolicName, "1.2", Bundle.ACTIVE);
    	}
	}
}