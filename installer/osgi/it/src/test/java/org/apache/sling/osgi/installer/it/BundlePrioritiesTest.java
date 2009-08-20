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

import java.io.IOException;

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.OsgiInstaller;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;

@RunWith(JUnit4TestRunner.class)
public class BundlePrioritiesTest extends OsgiInstallerTestBase {
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
    /** Use snapshots, it's the only bundles which are updated even if their version doesn't change */
    public void testPrioritiesUsingSnapshots() throws IOException {
        // Install test bundle
        final String symbolicName = "osgi-installer-snapshot-test";
        assertNull("Snapshot test bundle must be absent before installing", findBundle(symbolicName));
        
        final int lowPriority = InstallableResource.DEFAULT_PRIORITY - 1;
        final int highPriority = InstallableResource.DEFAULT_PRIORITY + 1;
        
        {
            resetCounters();
            installer.addResource(getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-snap.jar"), "digest1"));
            // wait for two tasks: install and start
            waitForInstallerAction(OsgiInstaller.OSGI_TASKS_COUNTER, 2);
            assertBundle("Initial install", symbolicName, null, Bundle.ACTIVE);
        }

        {
            installer.addResource(getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-snap.jar"), "digest2", lowPriority));
            assertNoOsgiTasks("Low-priority snapshot updated must be ignored");
        }

        {
            resetCounters();
            installer.addResource(getInstallableResource(
                    getTestBundle(BUNDLE_BASE_NAME + "-snap.jar"), "digest3", highPriority));
            // wait for two tasks: update and restart
            waitForInstallerAction(OsgiInstaller.OSGI_TASKS_COUNTER, 2);
        }
        
        assertNoOsgiTasks("At end of test");
    }
}
