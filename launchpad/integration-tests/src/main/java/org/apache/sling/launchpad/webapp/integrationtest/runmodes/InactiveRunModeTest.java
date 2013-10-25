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
package org.apache.sling.launchpad.webapp.integrationtest.runmodes;

import java.io.IOException;

import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Verify that a bundle that has a non-active run mode is not present.
 *  Uses a bundle provided by the test-bundles module with a runmode
 *  that's not active in our tests. 
 */
public class InactiveRunModeTest extends HttpTestBase {
    
    private void assertBundlePresent(String symbolicName, boolean present) throws IOException {
        final String bundlePath = "/system/console/bundles/" + symbolicName;
        assertHttpStatus(HTTP_BASE_URL + bundlePath, present ? 200 : 404);
    }
    
    /** Verify that assertBundlePresent works "*/
    public void testBundleDetection() throws IOException {
        assertBundlePresent("org.apache.sling.api", true);
    }
    
    /** Verify that our test bundle is absent - it should be present
     *  only if its specific run mode is active.
     */
    public void testBundleAbsent() throws IOException {
        assertBundlePresent("org.apache.sling.testing.samples.failingtests", false);
    }
    
}
