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

import org.apache.sling.osgi.installer.OsgiInstallerStatistics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;

@RunWith(JUnit4TestRunner.class)
public class WorkerThreadIdleTest extends OsgiInstallerTestBase {

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
    }
}