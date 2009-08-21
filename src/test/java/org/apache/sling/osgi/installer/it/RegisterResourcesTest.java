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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.OsgiInstaller;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;

@RunWith(JUnit4TestRunner.class)
/** Test the {@link OsgiInstaller.registerResources} method, which lets a client
 *  supply a new list of resources.
 */
public class RegisterResourcesTest extends OsgiInstallerTestBase {

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
    public void initialRegistrationTest() throws IOException {
        resetCounters();
        final List<InstallableResource> r = new ArrayList<InstallableResource>();
        r.add(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testB-1.0.jar")));
        r.add(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-needsB.jar")));
        r.add(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
        r.add(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.2.jar")));
        
        installer.registerResources(r, URL_SCHEME);
        
        // Wait for worker thread to wake up and become idle once
        waitForInstallerAction(OsgiInstaller.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);
        
        final String info = "After initial registration";
        assertBundle(info, "osgi-installer-testB", "1.0", Bundle.ACTIVE);
        assertBundle(info, "osgi-installer-needsB", "1.0", Bundle.ACTIVE);
        assertBundle(info, "osgi-installer-testbundle", "1.2", Bundle.ACTIVE);
    }
    
    @Test
    public void removeAndReaddBundlesTest() throws IOException, BundleException {
        {
            resetCounters();
            final List<InstallableResource> r = new ArrayList<InstallableResource>();
            r.add(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testB-1.0.jar")));
            r.add(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-needsB.jar")));
            r.add(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar")));
            r.add(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
            
            installer.registerResources(r, URL_SCHEME);
            waitForInstallerAction(OsgiInstaller.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);
            
            final String info = "After initial registration";
            assertBundle(info, "osgi-installer-testB", "1.0", Bundle.ACTIVE);
            assertBundle(info, "osgi-installer-needsB", "1.0", Bundle.ACTIVE);
            assertBundle(info, "osgi-installer-testbundle", "1.1", Bundle.ACTIVE);
        }
        
        {
            // Add test 1.2 in between, to make sure it disappears in next registerResources call
            resetCounters();
            installer.addResource(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.2.jar")));
            waitForInstallerAction(OsgiInstaller.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);
            assertBundle("After adding testbundle V1.2", "osgi-installer-testbundle", "1.2", Bundle.ACTIVE);
        }
        
        {
            // Add a bundle with different URL scheme - must not be removed by registerResources
            resetCounters();
            installer.addResource(new InstallableResource(
                    "anotherscheme:testA.jar", 
                    new FileInputStream(getTestBundle(BUNDLE_BASE_NAME + "-testA-1.0.jar")), 
                    "digest1"));
            waitForInstallerAction(OsgiInstaller.OSGI_TASKS_COUNTER, 2);
            assertBundle("testA bundle added", "osgi-installer-testA", "1.0", Bundle.ACTIVE);
        }
        
        {
            // Simulate later registration where some bundles have disappeared
            // the installer must mark them "not installable" and act accordingly
            final List<InstallableResource> r = new ArrayList<InstallableResource>();
            r.add(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-needsB.jar")));
            r.add(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.0.jar")));
            r.add(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-snap.jar"), "digest1"));
            
            installer.registerResources(r, URL_SCHEME);
            waitForInstallerAction(OsgiInstaller.INSTALLER_CYCLES_COUNTER, 2);
            
            assertBundle("Snapshot bundle must be started", 
                    "osgi-installer-snapshot-test", "1.0.0.SNAPSHOT", Bundle.ACTIVE);
            assertNull("Bundle testB must be gone", findBundle("osgi-installer-testB"));
            final Bundle b = assertBundle("Bundle needsB must still be present", 
                    "osgi-installer-needsB", "1.0", -1);
            final int state = b.getState();
            assertFalse("Bundle needsB must be stopped as testB is gone (" + state + ")", Bundle.ACTIVE == state);
            assertBundle("Testbundle must be back to 1.0 as 1.1 and 1.2 is gone", 
                    "osgi-installer-testbundle", "1.0", Bundle.ACTIVE);
            assertBundle("testA bundle should still be present", "osgi-installer-testA", "1.0", Bundle.ACTIVE);
        }
        
        {
            // Re-add the missing bundles and recheck
            installer.addResource(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testB-1.0.jar")));
            installer.addResource(getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.2.jar")));
            
            waitForInstallerAction(OsgiInstaller.INSTALLER_CYCLES_COUNTER, 2);
            
            final String info = "After re-adding missing bundles";
            assertBundle(info, "osgi-installer-testB", "1.0", Bundle.ACTIVE);
            assertBundle(info, "osgi-installer-testbundle", "1.2", Bundle.ACTIVE);
            assertBundle(info, "osgi-installer-snapshot-test", "1.0.0.SNAPSHOT", Bundle.ACTIVE);
            
            final Bundle b = findBundle("osgi-installer-needsB");
            b.start();
            assertBundle("After reinstalling testB, needsB must be startable, ",
            		"osgi-installer-needsB", "1.0", Bundle.ACTIVE);
            assertBundle("testA bundle should still be present", "osgi-installer-testA", "1.0", Bundle.ACTIVE);
        }
    }
}
