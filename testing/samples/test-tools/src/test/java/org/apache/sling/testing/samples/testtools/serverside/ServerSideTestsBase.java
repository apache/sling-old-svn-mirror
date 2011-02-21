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
package org.apache.sling.testing.samples.testtools.serverside;

import org.apache.sling.testing.samples.testtools.SlingTestBase;
import org.apache.stanbol.commons.testing.http.RetryLoop;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Base class for tests that require the server-side test bundles
 *  to be active.
 */
public class ServerSideTestsBase extends SlingTestBase {
    public static final String JUNIT_SERVLET_PATH = "/system/sling/junit";
    
    private static boolean junitServletOk;
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    // TODO compute those timeouts based on a configured factor
    // to cope with slower testing systems??
    public static final int JUNIT_SERVLET_TIMEOUT_SECONDS = 60;
    
    @Before
    public void checkJunitServletPresent() throws Exception {
        if(junitServletOk) {
            return;
        }

        // Retry accessing the junit servlet until it responds or timeout
        // (as we might just have installed the required bundles)
        final int expectedStatus = 200;
        final RetryLoop.Condition c = new RetryLoop.Condition() {
            public String getDescription() {
                return "Checking that " + JUNIT_SERVLET_PATH + " returns " + expectedStatus;
            }

            public boolean isTrue() throws Exception {
                executor.execute(
                        builder.buildGetRequest(JUNIT_SERVLET_PATH))
                .assertStatus(expectedStatus);
                return true;
            }
                
        };
        
        log.info(c.getDescription());
        new RetryLoop(c, JUNIT_SERVLET_TIMEOUT_SECONDS, 500); 
        junitServletOk = true;
    }
}
