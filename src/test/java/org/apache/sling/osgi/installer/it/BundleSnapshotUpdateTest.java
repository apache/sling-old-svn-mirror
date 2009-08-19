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
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.sling.osgi.installer.OsgiInstaller;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;

/** Verify that snapshot bundles are updated even if
 *  their version number does not change.
 *  TODO: digests should be taken into account, i.e. no action
 *  if digest does not change
 */
@RunWith(JUnit4TestRunner.class)
public class BundleSnapshotUpdateTest extends OsgiInstallerTestBase {
    
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
    public void testNonSnapshot() throws IOException {
        final String symbolicName = "osgi-installer-testbundle";
        assertNull("Test bundle must be absent before installing", findBundle(symbolicName));
        resetCounters();
        installer.addResource(getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
        // wait for two tasks: install and start
        waitForInstallerAction(OsgiInstaller.OSGI_TASKS_COUNTER, 2);
        final Bundle b = findBundle(symbolicName);
        assertNotNull("Test bundle 1.1 must be found after waitForInstallerAction", b);
        
        final long nOps = installer.getCounters()[OsgiInstaller.OSGI_TASKS_COUNTER];
        installer.addResource(getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
        waitForInstallerAction(OsgiInstaller.INSTALLER_CYCLES_COUNTER, 1);
        assertEquals("Updating non-snapshot bundle must not generate any OSGi tasks",
                nOps, installer.getCounters()[OsgiInstaller.OSGI_TASKS_COUNTER]);
    }
    
    @Test
    public void testSnapshot() throws IOException {
        final String symbolicName = "osgi-installer-snapshot-test";
        assertNull("Snapshot test bundle must be absent before installing", findBundle(symbolicName));
        resetCounters();
        installer.addResource(getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-snap.jar")));
        // wait for two tasks: install and start
        waitForInstallerAction(OsgiInstaller.OSGI_TASKS_COUNTER, 2);
        final Bundle b = findBundle(symbolicName);
        assertNotNull("Snapshot bundle must be found after waitForInstallerAction", b);

        resetCounters();
        installer.addResource(getInstallableResource(
                getTestBundle(BUNDLE_BASE_NAME + "-snap.jar")));
        waitForInstallerAction(OsgiInstaller.INSTALLER_CYCLES_COUNTER, 1);
        // If this fails it means bundle has not been updated
        waitForInstallerAction(OsgiInstaller.OSGI_TASKS_COUNTER, 1);
    }
}
