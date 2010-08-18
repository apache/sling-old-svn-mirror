/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.osgi.installer.impl;


/** Test the bundle start retries logic of SLING-1042 */
public abstract class BundleStartRetriesTest {
    /*
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
    public void testBundleStartRetries() throws Exception {
        final String testB = "osgi-installer-testB";
        final String needsB = "osgi-installer-needsB";

        assertNull("TestB bundle must not be present at beginning of test", findBundle(testB));

        // without testB, needsB must not start
        resetCounters();
        final long nOps = statistics.getCounters()[OsgiInstallerStatistics.OSGI_TASKS_COUNTER];
        installer.addResource(URL_SCHEME, getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-needsB.jar")));
        waitForInstallerAction(OsgiInstallerStatistics.OSGI_TASKS_COUNTER, 2);
        assertBundle(needsB + " must not be started, testB not present", needsB, null, Bundle.INSTALLED);

        // the bundle start task must be retried immediately
        // (== 3 tasks since last counters reset)
        waitForInstallerAction(OsgiInstallerStatistics.OSGI_TASKS_COUNTER, 3);

        // and no more retries must happen before receiving a bundle event
        sleep(1000L);
        assertEquals("Exactly 3 OSGi tasks must have been executed after a few installer cycles",
                nOps + 3, statistics.getCounters()[OsgiInstallerStatistics.OSGI_TASKS_COUNTER]);

        // generate a bundle event -> must trigger one retry
        resetCounters();
        generateBundleEvent();
        waitForInstallerAction(OsgiInstallerStatistics.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);
        waitForInstallerAction(OsgiInstallerStatistics.OSGI_TASKS_COUNTER, 1);
    }
*/
}
