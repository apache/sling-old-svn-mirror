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
package org.apache.sling.testing.samples.integrationtests.serverside.sling;

import static org.junit.Assert.assertEquals;

import org.apache.sling.junit.remote.testrunner.SlingRemoteTestParameters;
import org.apache.sling.junit.remote.testrunner.SlingRemoteTestRunner;
import org.apache.sling.junit.remote.testrunner.SlingTestsCountChecker;
import org.junit.runner.RunWith;

/** Run some server-side tests using the Sling JUnit servlet */
@RunWith(SlingRemoteTestRunner.class)
public class SlingServerSideTest extends SlingServerSideTestsBase 
implements SlingRemoteTestParameters, SlingTestsCountChecker {
    
    public static final String TEST_SELECTOR = "org.apache.sling.testing.samples.sampletests";
    public static final int TESTS_AT_THIS_PATH = 12;
    
    public void checkNumberOfTests(int numberOfTestsExecuted) {
        assertEquals(TESTS_AT_THIS_PATH, numberOfTestsExecuted);
    }
    
    public String getJunitServletUrl() {
        return getServerBaseUrl() + SLING_JUNIT_SERVLET_PATH;
    }

    public String getTestClassesSelector() {
        return TEST_SELECTOR;
    }

    public String getTestMethodSelector() {
        return null;
    }
}