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

@RunWith(JUnit4TestRunner.class)
public class BundleInstallTest extends OsgiInstallerTestBase {

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
    	final String uri = symbolicName + JAR_EXT;
    	final String BUNDLE_VERSION = "Bundle-Version";
    	
    	assertNull("Test bundle must not be present before test", findBundle(symbolicName));
    	
    	// Install first test bundle and check version
    	long bundleId = 0;
    	{
            assertNull("Test bundle must be absent before installing", findBundle(symbolicName));
    	    resetCounters();
    	    installer.addResource(getInstallableResource(
    	            getTestBundle("org.apache.sling.osgi.installer.it-" + POM_VERSION + "-testbundle-1.1.jar")));
    	    // wait for two tasks: install and start
    	    waitForInstallerAction(OsgiInstaller.OSGI_TASKS_COUNTER, 2);
    	    final Bundle b = findBundle(symbolicName);
        	assertNotNull("Test bundle 1.1 must be found after waitForInstallerAction", b);
        	bundleId = b.getBundleId();
        	assertEquals("Installed bundle must be started", Bundle.ACTIVE, b.getState());
        	assertEquals("Version must be 1.1", "1.1", b.getHeaders().get(BUNDLE_VERSION));
    	}

    	// Upgrade to later version, verify
    	{
    	    resetCounters();
            installer.addResource(getInstallableResource(
                    getTestBundle("org.apache.sling.osgi.installer.it-" + POM_VERSION + "-testbundle-1.2.jar")));
            // wait for two tasks: update (includes stop) and start
            waitForInstallerAction(OsgiInstaller.OSGI_TASKS_COUNTER, 2);
        	final Bundle b = findBundle(symbolicName);
        	assertNotNull("Test bundle 1.2 must be found after waitForInstallerAction", b);
        	assertEquals("Installed bundle must be started", Bundle.ACTIVE, b.getState());
        	assertEquals("Version must be 1.2 after update", "1.2", b.getHeaders().get(BUNDLE_VERSION));
        	assertEquals("Bundle ID must not change after update", bundleId, b.getBundleId());
    	}

    	// Downgrade to lower version, installed bundle must not change
        {
            resetCounters();
            installer.addResource(getInstallableResource(
                    getTestBundle("org.apache.sling.osgi.installer.it-" + POM_VERSION + "-testbundle-1.0.jar")));
            
            // wait for two cycles to make sure no updates happen
            waitForInstallerAction(OsgiInstaller.INSTALLER_CYCLES_COUNTER, 2);
            final Bundle b = findBundle(symbolicName);
            assertNotNull("Test bundle 1.2 must still be found after waitForInstallerAction", b);
            assertEquals("Installed bundle must be started", Bundle.ACTIVE, b.getState());
            assertEquals("Version must be 1.2 after ignored downgrade", "1.2", b.getHeaders().get(BUNDLE_VERSION));
            assertEquals("Bundle ID must not change after ignored downgrade", bundleId, b.getBundleId());
        }
    	
        /** TODO
    	// Uninstall
    	{
        	c.scheduleUninstall(uri);
        	c.waitForInstallerAction();
        	final Bundle b = findBundle(symbolicName);
        	assertNull("Test bundle 1.2 must be gone", b);
    	}
    	
    	// Install lower version, must work
    	{
        	c.scheduleInstallOrUpdate(uri, new FileInstallableResource(getTestBundle("org.apache.sling.osgi.installer.it-" + POM_VERSION + "-testbundle-1.0.jar")));
        	c.waitForInstallerAction();
        	final Bundle b = findBundle(symbolicName);
        	assertNotNull("Test bundle 1.0 must be found after waitForInstallerAction", b);
        	assertEquals("Installed bundle must be started", Bundle.ACTIVE, b.getState());
        	assertEquals("Version must be 1.0 after uninstall and downgrade", "1.0", b.getHeaders().get(BUNDLE_VERSION));
        	assertFalse("Bundle ID must have changed after uninstall and reinstall", bundleId == b.getBundleId());
    	}
    	*/
    }
	
	/** TODO
    
    @Test
    public void testBundleStatePreserved() throws Exception {
    	final OsgiInstaller c = getService(OsgiInstaller.class);
    	
    	// Install two bundles, one started, one stopped
    	{
        	c.scheduleInstallOrUpdate("otherBundleA.jar", 
        			new FileInstallableResource(getTestBundle("org.apache.sling.osgi.installer.it-" + POM_VERSION + "-testA-1.0.jar")));
        	c.waitForInstallerAction();
    	}
    	{
        	c.scheduleInstallOrUpdate("testB.jar", 
        			new FileInstallableResource(getTestBundle("org.apache.sling.osgi.installer.it-" + POM_VERSION + "-testB-1.0.jar")));
        	c.waitForInstallerAction();
        	final Bundle b = findBundle("osgi-installer-testB");
        	assertNotNull("Test bundle must be found", b);
        	b.stop();
    	}
    	
    	assertEquals("Bundle A must be started", Bundle.ACTIVE, findBundle("osgi-installer-testA").getState());
    	assertEquals("Bundle B must be stopped", Bundle.RESOLVED, findBundle("osgi-installer-testB").getState());
    	
    	// Execute some OsgiController operations
    	final String symbolicName = "osgi-installer-testbundle";
    	final String uri = symbolicName + JAR_EXT;
    	final String BUNDLE_VERSION = "Bundle-Version";
    	c.scheduleInstallOrUpdate(uri, 
    			new FileInstallableResource(getTestBundle("org.apache.sling.osgi.installer.it-" + POM_VERSION + "-testbundle-1.1.jar")));
    	c.waitForInstallerAction();
    	c.scheduleInstallOrUpdate(uri, 
    			new FileInstallableResource(getTestBundle("org.apache.sling.osgi.installer.it-" + POM_VERSION + "-testbundle-1.2.jar")));
    	c.waitForInstallerAction();
    	c.scheduleInstallOrUpdate(uri, 
    			new FileInstallableResource(getTestBundle("org.apache.sling.osgi.installer.it-" + POM_VERSION + "-testbundle-1.0.jar")));
    	c.waitForInstallerAction();
    	final Bundle b = findBundle(symbolicName);
    	assertNotNull("Installed bundle must be found", b);
    	assertEquals("Installed bundle must be started", Bundle.ACTIVE, b.getState());
    	assertEquals("Version must be 1.2", "1.2", b.getHeaders().get(BUNDLE_VERSION));
    	
    	// And check that bundles A and B have kept their states
    	assertEquals("Bundle A must be started", Bundle.ACTIVE, findBundle("osgi-installer-testA").getState());
    	assertEquals("Bundle B must be stopped", Bundle.RESOLVED, findBundle("osgi-installer-testB").getState());
    }
    
    // needsB bundle requires testB, try loading needsB first,
	// then testB, and verify that in the end needsB is started 	
    @Test
    public void testBundleDependencies() throws Exception {
    	final OsgiInstaller c = getService(OsgiInstaller.class);
    	
    	final String testB = "osgi-installer-testB";
    	final String needsB = "osgi-installer-needsB";
    	
    	{
        	final Bundle b = findBundle(testB);
        	if(b != null) {
        		c.scheduleUninstall(testB + JAR_EXT);
        		c.waitForInstallerAction();
        	}
        	assertNull(testB + " bundle must not be installed before test", findBundle(testB));
    	}
    	
    	// without testB, needsB must not start
    	{
        	c.scheduleInstallOrUpdate(needsB + JAR_EXT,
        			new FileInstallableResource(getTestBundle("org.apache.sling.osgi.installer.it-" + POM_VERSION + "-needsB.jar")));
        	c.waitForInstallerAction();
        	final Bundle b = findBundle(needsB);
        	assertNotNull(needsB + " must be installed", b);
        	assertFalse(needsB + " must not be started, testB not present", b.getState() == Bundle.ACTIVE);
    	}
    	
    	// Check SLING-1042 retry rules
    	assertTrue("OsgiController must implement OsgiControllerStatistics", c instanceof OsgiControllerStatistics);
    	final OsgiControllerStatistics stats = (OsgiControllerStatistics)c;
    	
    	{
    	    long n = stats.getExecutedTasksCount();
    	    c.waitForInstallerAction();
            assertTrue("First retry must not wait for an event", stats.getExecutedTasksCount() > n);
            n = stats.getExecutedTasksCount();
            c.waitForInstallerAction();
    	    assertEquals("Retrying before a bundle event happens must not execute any OsgiControllerTask", n, stats.getExecutedTasksCount());
    	    
            n = stats.getExecutedTasksCount();
    	    generateBundleEvent();
            c.waitForInstallerAction();
            assertTrue("Retrying after a bundle event must execute at least one OsgiControllerTask", stats.getExecutedTasksCount() > n);
    	}
    	
    	{
    	    // wait until no more events are received
            final long timeout = System.currentTimeMillis() + 2000L;
            while(System.currentTimeMillis() < timeout) {
                final long n = stats.getExecutedTasksCount();
                c.waitForInstallerAction();
                if(n == stats.getExecutedTasksCount()) {
                    break;
                }
                Thread.sleep(10L);
            }
            
            if(System.currentTimeMillis() >= timeout) {
                fail("Retries did not stop within specified time");
            }
    	}
    	
        {
            long n = stats.getExecutedTasksCount();
            c.waitForInstallerAction();
            assertEquals("Retrying before a framework event happens must not execute any OsgiControllerTask", n, stats.getExecutedTasksCount());
            refreshPackages();
            c.waitForInstallerAction();
            assertTrue("Retrying after framework event must execute at least one OsgiControllerTask", stats.getExecutedTasksCount() > n);
        }
        
    	// now install testB -> needsB must start
    	{
        	c.scheduleInstallOrUpdate(testB + JAR_EXT,
        			new FileInstallableResource(getTestBundle("org.apache.sling.osgi.installer.it-" + POM_VERSION + "-testB-1.0.jar")));
        	c.waitForInstallerAction();
        	final Bundle b = findBundle(needsB);
        	assertNotNull(needsB + " must be installed", b);
        	assertTrue(needsB + " must be started now that testB is installed", b.getState() == Bundle.ACTIVE);
    	}
    }
    */
}
