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
public class BundleStatePreservedTest extends OsgiInstallerTestBase {

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
    public void testBundleStatePreserved() throws Exception {
    	// Install two bundles, one started, one stopped
    	{
            resetCounters();
            installer.addResource(getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testA-1.0.jar")));
            waitForInstallerAction(OsgiInstaller.OSGI_TASKS_COUNTER, 2);
    	}
        {
            resetCounters();
            installer.addResource(getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-testB-1.0.jar")));
            waitForInstallerAction(OsgiInstaller.OSGI_TASKS_COUNTER, 2);
            final Bundle b = findBundle("osgi-installer-testB");
            assertNotNull("Test bundle B must be found", b);
            b.stop();
        }
    	
        assertBundle("Bundle A must be started", "osgi-installer-testA", null, Bundle.ACTIVE);
        assertBundle("Bundle B must be stopped", "osgi-installer-testB", null, Bundle.RESOLVED);
    	
    	// Execute some OsgiController operations
        installer.addResource(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar")));
        installer.addResource(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.2.jar")));
        resetCounters();
        installer.addResource(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
        waitForInstallerAction(OsgiInstaller.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);
        assertBundle("After installing testbundle", "osgi-installer-testbundle", "1.2", Bundle.ACTIVE);
    	
        // Verify number of registered resources and groups
        assertCounter(OsgiInstaller.REGISTERED_RESOURCES_COUNTER, 5);
        assertCounter(OsgiInstaller.REGISTERED_GROUPS_COUNTER, 3);
    	
        installer.removeResource(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar")));
        installer.removeResource(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
        resetCounters();
        installer.removeResource(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.2.jar")));
        waitForInstallerAction(OsgiInstaller.OSGI_TASKS_COUNTER, 1);
        assertNull("testbundle must be gone at end of test", findBundle("osgi-installer-testbundle"));
        
    	// Now check that bundles A and B have kept their states
        assertBundle("Bundle A must still be started", "osgi-installer-testA", null, Bundle.ACTIVE);
        assertBundle("Bundle B must still be stopped", "osgi-installer-testB", null, Bundle.RESOLVED);
    }
}