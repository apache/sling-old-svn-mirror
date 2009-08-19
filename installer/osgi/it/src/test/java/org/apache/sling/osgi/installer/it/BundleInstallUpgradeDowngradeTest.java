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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.sling.osgi.installer.OsgiInstaller;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

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
    	final String BUNDLE_VERSION = "Bundle-Version";
    	int testIndex = 0;
    	
    	assertNull("Test bundle must not be present before test", findBundle(symbolicName));
    	
    	// Install first test bundle and check version
    	long bundleId = 0;
    	{
            assertNull("Test bundle must be absent before installing", findBundle(symbolicName));
    	    resetCounters();
    	    installer.addResource(getInstallableResource(
    	            getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
    	    // wait for two tasks: install and start
    	    waitForInstallerAction(OsgiInstaller.OSGI_TASKS_COUNTER, 2);
    	    final Bundle b = findBundle(symbolicName);
        	assertNotNull("Test bundle 1.1 must be found after waitForInstallerAction", b);
        	bundleId = b.getBundleId();
        	assertEquals("Installed bundle must be started", Bundle.ACTIVE, b.getState());
        	assertEquals("Version must be 1.1", "1.1", b.getHeaders().get(BUNDLE_VERSION));
    	}
    	
    	assertNoOsgiTasks("After test " + testIndex++);

    	// Upgrade to later version, verify
    	{
    	    resetCounters();
            installer.addResource(getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.2.jar")));
            // wait for two tasks: update (includes stop) and start
            waitForInstallerAction(OsgiInstaller.OSGI_TASKS_COUNTER, 2);
        	final Bundle b = findBundle(symbolicName);
        	assertNotNull("Test bundle 1.2 must be found after waitForInstallerAction", b);
        	assertEquals("Installed bundle must be started", Bundle.ACTIVE, b.getState());
        	assertEquals("Version must be 1.2 after update", "1.2", b.getHeaders().get(BUNDLE_VERSION));
        	assertEquals("Bundle ID must not change after update", bundleId, b.getBundleId());
    	}

    	assertNoOsgiTasks("After test " + testIndex++);
    	
    	// Downgrade to lower version, installed bundle must not change
        {
            resetCounters();
            installer.addResource(getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar")));
            
            // wait for two cycles to make sure no updates happen
            waitForInstallerAction(OsgiInstaller.INSTALLER_CYCLES_COUNTER, 2);
            final Bundle b = findBundle(symbolicName);
            assertNotNull("Test bundle 1.2 must still be found after waitForInstallerAction", b);
            assertEquals("Installed bundle must be started", Bundle.ACTIVE, b.getState());
            assertEquals("Version must be 1.2 after ignored downgrade", "1.2", b.getHeaders().get(BUNDLE_VERSION));
            assertEquals("Bundle ID must not change after ignored downgrade", bundleId, b.getBundleId());
        }
    	
    	assertNoOsgiTasks("After test " + testIndex++);
    	
    	// Uninstall
    	{
            resetCounters();
            installer.removeResource(getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar")));
            installer.removeResource(getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
            installer.removeResource(getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.2.jar")));
            
            // wait for one task: uninstall
            waitForInstallerAction(OsgiInstaller.OSGI_TASKS_COUNTER, 1);
            final Bundle b = findBundle(symbolicName);
            assertNull("Bundle must be gone", b);
            
            // uninstall task generates package refresh and a number of bundle start tasks, consume these
            waitForInstallerAction(OsgiInstaller.INSTALLER_CYCLES_COUNTER, 2);
    	}
    	
    	assertNoOsgiTasks("After test " + testIndex++);
    	
    	// Reinstall lower version, must work
        {
            resetCounters();
            installer.addResource(getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
            // wait for two tasks: install and start
            waitForInstallerAction(OsgiInstaller.OSGI_TASKS_COUNTER, 2);
            final Bundle b = findBundle(symbolicName);
            assertNotNull("Reinstalled test bundle 1.1 must be found after waitForInstallerAction", b);
            bundleId = b.getBundleId();
            assertEquals("Reinstalled bundle must be started", Bundle.ACTIVE, b.getState());
            assertEquals("Reinstalled bundle version must be 1.1", "1.1", b.getHeaders().get(Constants.BUNDLE_VERSION));
        }

        assertNoOsgiTasks("After test " + testIndex++);
	}
}