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
package org.apache.sling.junit.teleporter.customizers;

import static org.junit.Assert.fail;

import org.apache.sling.commons.testing.integration.HttpTest;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.launchpad.webapp.integrationtest.teleporter.TeleporterOptionsTest;
import org.apache.sling.testing.teleporter.client.ClientSideTeleporter;

/** TeleporterRule Customizer used for Sling launchpad integration tests.
 *  Waits for Sling to be ready and sets the appropriate parameters 
 *  on the ClientSideTeleporter. 
 */
public class LaunchpadCustomizer implements TeleporterRule.Customizer {

    private final static HttpTest H = new HttpTest();
    private final static int testReadyTimeout = Integer.getInteger("ClientSideTeleporter.testReadyTimeoutSeconds",12);
    
    @Override
    /** Customize the client-side TeleporterRule by first waiting
     *  for Sling to be ready and then setting it up with the test server
     *  URL, timeout etc.
     */
    public void customize(TeleporterRule t, String options) {
        // Used to test the options mechanism
        if(TeleporterOptionsTest.OPTIONS.equals(options)) {
            throw new TeleporterOptionsTest.OptionsException(options);
        }

        // Setup Sling and the ClientSideTeleporter
        try {
            H.setUp();
        } catch(Exception e) {
            fail("HttpTest setup failed: " + e);
        }
        final ClientSideTeleporter cst = (ClientSideTeleporter)t;
        cst.setBaseUrl(HttpTest.HTTP_BASE_URL);
        cst.setTestReadyTimeoutSeconds(testReadyTimeout);
        cst.includeDependencyPrefix("org.apache.sling.launchpad.webapp");
        cst.setServerCredentials("admin", "admin");
    }
    
}
