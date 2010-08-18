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


public abstract class WorkerThreadIdleTest {
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
    public void testWorkerThreadBecomesIdle() throws Exception {
        waitForInstallerAction("Worker thread should become idle soon after controller starts",
                OsgiInstallerStatistics.WORKER_THREAD_IS_IDLE_COUNTER, -1);

        final String symbolicName = "osgi-installer-testbundle";
        assertNull("Test bundle must be absent before installing", findBundle(symbolicName));
        resetCounters();
        installer.addResource(URL_SCHEME, getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testbundle-1.1.jar")));
        // wait for two tasks: install and start
        waitForInstallerAction(OsgiInstallerStatistics.OSGI_TASKS_COUNTER, 2);
        assertBundle("After installing", symbolicName, "1.1", Bundle.ACTIVE);

        waitForInstallerAction("Worker thread should become idle after installing bundle",
                OsgiInstallerStatistics.WORKER_THREAD_IS_IDLE_COUNTER, -1);
    }*/
}