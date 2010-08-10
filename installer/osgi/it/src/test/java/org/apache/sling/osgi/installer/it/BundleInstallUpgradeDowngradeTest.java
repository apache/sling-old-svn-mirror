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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.sling.osgi.installer.OsgiInstallerStatistics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;

@RunWith(JUnit4TestRunner.class)
public class BundleInstallUpgradeDowngradeTest extends OsgiInstallerTestBase {

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
    public void testInstallUpgradeDowngradeBundle() throws Exception {
    	final String symbolicName = "osgi-installer-testbundle";
    	int testIndex = 0;

    	assertNull("Test bundle must not be present before test", findBundle(symbolicName));

    	// Install first test bundle and check version
    	long bundleId = 0;
    	{
            assertNull("Test bundle must be absent before installing", findBundle(symbolicName));
    	    resetCounters();
    	    installer.addResource(URL_SCHEME, getInstallableResource(
    	            getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
            waitForInstallerAction(OsgiInstallerStatistics.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);
    	    final Bundle b = assertBundle("After installing", symbolicName, "1.1", Bundle.ACTIVE);
    	    bundleId = b.getBundleId();
    	}

    	assertNoOsgiTasks("After test " + testIndex++);

    	// Upgrade to later version, verify
    	{
    	    resetCounters();
            installer.addResource(URL_SCHEME, getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.2.jar"), "digestA"));
            waitForInstallerAction(OsgiInstallerStatistics.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);
        	final Bundle b = assertBundle("After updating to 1.2", symbolicName, "1.2", Bundle.ACTIVE);
        	assertEquals("Bundle ID must not change after update", bundleId, b.getBundleId());
    	}

    	assertNoOsgiTasks("After test " + testIndex++);

    	// Downgrade to lower version, installed bundle must not change
        {
            resetCounters();
            installer.addResource(URL_SCHEME, getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar"), "digestA"));

            // make sure no updates happen
            waitForInstallerAction(OsgiInstallerStatistics.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);
            final Bundle b = assertBundle("After ignored downgrade", symbolicName, "1.2", Bundle.ACTIVE);
            assertEquals("Bundle ID must not change after ignored downgrade", bundleId, b.getBundleId());
        }

    	assertNoOsgiTasks("After test " + testIndex++);

    	// Verify number of registered resources and groups
        assertCounter(OsgiInstallerStatistics.REGISTERED_RESOURCES_COUNTER, 3);
        assertCounter(OsgiInstallerStatistics.REGISTERED_GROUPS_COUNTER, 1);

    	// Update to same version with different digest must be ignored
    	{
    	    resetCounters();
            installer.addResource(URL_SCHEME, getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar"), "digestB"));
        	assertNoOsgiTasks("Update to same version should generate no OSGi tasks");
    	}

    	// Uninstall
    	{
            resetCounters();
            installer.removeResource(URL_SCHEME, getNonInstallableResourceUrl(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar")));
            waitForInstallerAction(OsgiInstallerStatistics.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);

            resetCounters();
            installer.removeResource(URL_SCHEME, getNonInstallableResourceUrl(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
            waitForInstallerAction(OsgiInstallerStatistics.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);

            resetCounters();
            installer.removeResource(URL_SCHEME, getNonInstallableResourceUrl(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.2.jar")));
            waitForInstallerAction(OsgiInstallerStatistics.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);

            final Bundle b = findBundle(symbolicName);
            assertNull("Testbundle must be gone", b);
    	}

    	// No resources must be registered anymore
        assertCounter(OsgiInstallerStatistics.REGISTERED_RESOURCES_COUNTER, 0);
    	assertCounter(OsgiInstallerStatistics.REGISTERED_GROUPS_COUNTER, 0);

    	assertNoOsgiTasks("After test " + testIndex++);

    	// Reinstall lower version, must work
        {
            resetCounters();
            installer.addResource(URL_SCHEME, getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
            waitForInstallerAction(OsgiInstallerStatistics.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);
            assertBundle("After reinstalling 1.1", symbolicName, "1.1", Bundle.ACTIVE);
        }

        assertNoOsgiTasks("After test " + testIndex++);
	}

	   @Test
	    public void testRemoveAndReadd() throws Exception {
	        final String symbolicName = "osgi-installer-testbundle";
	        int testIndex = 0;

	        {
	            assertNull("Test bundle must be absent before installing", findBundle(symbolicName));
	            resetCounters();
	            installer.addResource(URL_SCHEME, getInstallableResource(
	                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
	            waitForInstallerAction(OsgiInstallerStatistics.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);
	            assertBundle("After installing", symbolicName, "1.1", Bundle.ACTIVE);
	        }

	        assertNoOsgiTasks("After test " + testIndex++);

            {
                resetCounters();
                installer.removeResource(URL_SCHEME, getNonInstallableResourceUrl(
                        getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
                waitForInstallerAction(OsgiInstallerStatistics.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);
                assertNull("Test bundle must be gone", findBundle(symbolicName));
            }

            assertNoOsgiTasks("After test " + testIndex++);

            {
                resetCounters();
                installer.addResource(URL_SCHEME, getInstallableResource(
                        getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
                waitForInstallerAction(OsgiInstallerStatistics.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);
                assertBundle("After reinstalling", symbolicName, "1.1", Bundle.ACTIVE);
            }

            assertNoOsgiTasks("After test " + testIndex++);
	   }
}