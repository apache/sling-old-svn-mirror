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
import static org.junit.Assert.assertEquals;
import org.apache.sling.junit.remote.testrunner.SlingRemoteTestParameters;
import org.apache.sling.junit.remote.testrunner.SlingRemoteTestRunner;
import org.apache.sling.junit.remote.testrunner.SlingTestsCountChecker;
import org.apache.sling.testing.tools.http.Request;
import org.apache.sling.testing.tools.http.RequestCustomizer;
import org.junit.runner.RunWith;

/** Run some server-side tests, and verify that the RequestCustomizer
 *  interface is used by the test runner */
@RunWith(SlingRemoteTestRunner.class)
public class ServerSideSampleTest extends ServerSideTestsBase 
implements SlingRemoteTestParameters, SlingTestsCountChecker, RequestCustomizer {
    
    public static final String TEST_SELECTOR = "org.apache.sling.testing.samples.sampletests";
    public static final int TESTS_AT_THIS_PATH = 9;
    private int customizeCalled;
    
    public void checkNumberOfTests(int numberOfTestsExecuted) {
        // This assumes this method is called after customizeRequest, which
        // should be the case as the request must be executed to find out
        // how many tests are present
        if(customizeCalled == 0) {
            fail("customizeRequest not called?");
        }
        assertEquals(TESTS_AT_THIS_PATH, numberOfTestsExecuted);
    }

    public int getExpectedNumberOfTests() {
        return TESTS_AT_THIS_PATH;
    }
    
    public String getJunitServletUrl() {
        return getServerBaseUrl() + JUNIT_SERVLET_PATH;
    }

    public String getTestClassesSelector() {
        return TEST_SELECTOR;
    }

    public String getTestMethodSelector() {
        return null;
    }

    public void customizeRequest(Request r) {
        customizeCalled++;
    }
}