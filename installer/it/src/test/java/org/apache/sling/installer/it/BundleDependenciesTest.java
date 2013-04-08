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
package org.apache.sling.installer.it;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;

@RunWith(PaxExam.class)
public class BundleDependenciesTest extends OsgiInstallerTestBase {

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
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

    // needsB bundle requires testB, try loading needsB first,
    // then testB, and verify that in the end needsB is started
    @Test
    public void testBundleDependencies() throws Exception {
        final String testB = "osgi-installer-testB";
        final String needsB = "osgi-installer-needsB";

        assertNull("TestB bundle must not be present at beginning of test", findBundle(testB));

        // without testB, needsB must not start
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-needsB.jar")), null);
            this.waitForBundleEvents(needsB + " must be installed", listener,
                    new BundleEvent(needsB, org.osgi.framework.BundleEvent.INSTALLED));
            assertBundle(needsB + " must not be started, testB not present", needsB, null, Bundle.INSTALLED);
        }

       // now install testB -> needsB must start
        {
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME, getInstallableResource(getTestBundle(BUNDLE_BASE_NAME + "-testB-1.0.jar")), null);
            this.waitForBundleEvents(needsB + " and " + testB + " must be active", listener,
                    new BundleEvent(testB, org.osgi.framework.BundleEvent.INSTALLED),
                    new BundleEvent(testB, org.osgi.framework.BundleEvent.STARTED),
                    new BundleEvent(needsB, org.osgi.framework.BundleEvent.STARTED));
            assertNotNull(testB + " must be installed", findBundle(testB));
            assertBundle(needsB + " must be started now that testB is installed", needsB, null, Bundle.ACTIVE);
        }
    }
}