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
package org.apache.sling.testing.samples.integrationtests.serverside;

import static org.junit.Assert.fail;

import org.apache.sling.testing.tools.http.RetryingContentChecker;
import org.apache.sling.testing.tools.sling.SlingTestBase;
import org.apache.sling.testing.tools.sling.TimeoutsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Base class for tests that require the server-side test bundles
 *  to be active.
 */
public class ServerSideTestsBase extends SlingTestBase {
    public static final String JUNIT_SERVLET_PATH = "/system/sling/junit";
    
    private RetryingContentChecker junitServletChecker;
    private static boolean junitServletOk;
    private static boolean junitServletCheckFailed;
    private final Logger log = LoggerFactory.getLogger(getClass());
    public static final int JUNIT_SERVLET_TIMEOUT_SECONDS = TimeoutsProvider.getInstance().getTimeout(60);

    /** Verify that JUnit servlet is started before running these tests */
    public ServerSideTestsBase() {
        if(junitServletCheckFailed) {
            fail("Previous check of JUnit servlet failed, cannot run tests");
        }
        
        try {
            if(!junitServletOk) {
                if(junitServletChecker == null) {
                    junitServletChecker = 
                        new RetryingContentChecker(getRequestExecutor(), getRequestBuilder(), getServerUsername(), getServerPassword()) {
                        @Override
                        public void onTimeout() {
                            junitServletCheckFailed = true;
                        }
                    };
                }
                
                final String path = JUNIT_SERVLET_PATH;
                final int status = 200;
                final int timeout = TimeoutsProvider.getInstance().getTimeout(JUNIT_SERVLET_TIMEOUT_SECONDS);
                final int intervalMsec = TimeoutsProvider.getInstance().getTimeout(500);
                
                log.info("Checking that {} returns status {}, timeout={} seconds",
                        new Object[] { path, status, timeout });
                junitServletChecker.check(path,status,timeout,intervalMsec);
                junitServletOk = true;
            }
            
        } catch(Exception e) {
            throw new IllegalStateException("JUnit Servlet not ready: ", e);
        }
    }
}
