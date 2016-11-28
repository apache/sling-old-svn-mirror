/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.it.performance;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.performance.PerformanceRunner;
import org.apache.sling.performance.PerformanceRunner.Parameters;
import org.apache.sling.performance.PerformanceRunner.ReportLevel;
import org.apache.sling.performance.annotation.PerformanceTest;

import org.junit.FixMethodOrder;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import io.sightly.tck.http.Client;

/**
 * Performance Integration Tests for Sightly
 */
@RunWith(PerformanceRunner.class)
@Parameters(reportLevel= ReportLevel.MethodLevel, referenceMethod = PerformanceIT.REFERENCE_METHOD)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PerformanceIT {

    private static final int INVOCATIONS = 20;
    private static final int WARMUP_INVOCATIONS = 15;
    private static final int CONTENT_LOOP_COUNT = 20;
    public static final String REFERENCE_METHOD = "test1Jsp";

    private Client client;
    private String serverURL;

    private class Constants {
        public static final String SYS_PROP_SERVER_URL = "launchpad.http.server.url";
        public static final String SYS_PROP_USER = "launchpad.http.server.user";
        public static final String SYS_PROP_PASS = "launchpad.http.server.pass";
    }

    /**
     * Default constructor, initializes HTTP client
     */
    public PerformanceIT() {
        serverURL = System.getProperty(Constants.SYS_PROP_SERVER_URL);
        String user = System.getProperty(Constants.SYS_PROP_USER);
        String password = System.getProperty(Constants.SYS_PROP_PASS);
        if (StringUtils.isNotEmpty(user) && StringUtils.isNotEmpty(password)) {
            client = new Client(user, password);
        } else {
            client = new Client();
        }
    }

    /**
     * Helper method to read content from an URL
     *
     * @param url URL
     * @param expectedStatusCode Expected status code
     *
     * @return Actual string content
     */
    private String getStringContent(String url, int expectedStatusCode) {
        return client.getStringContent(serverURL + url, expectedStatusCode);
    }

    @PerformanceTest(runinvocations = INVOCATIONS, warmupinvocations = WARMUP_INVOCATIONS)
    public void test1Jsp() {
        getStringContent("/sightlyperf/loop.html?selector=jsp&count=" + CONTENT_LOOP_COUNT,
                HttpStatus.SC_OK);
    }

    @PerformanceTest(runinvocations = INVOCATIONS, warmupinvocations = WARMUP_INVOCATIONS)
    public void test2JspEL() {
        getStringContent("/sightlyperf/loop.html?selector=jsp-el&count=" + CONTENT_LOOP_COUNT,
                HttpStatus.SC_OK);
    }

    @PerformanceTest(runinvocations = INVOCATIONS, warmupinvocations = WARMUP_INVOCATIONS, threshold = 2)
    public void test3SlyJavaPojoRepo() {
        getStringContent("/sightlyperf/loop.html?selector=sly-java-pojo-repo&count=" + CONTENT_LOOP_COUNT,
                HttpStatus.SC_OK);
    }

    @PerformanceTest(runinvocations = INVOCATIONS, warmupinvocations = WARMUP_INVOCATIONS, threshold = 2)
    public void test4SlyJavaPojoBundle() {
        getStringContent("/sightlyperf/loop.html?selector=sly-java-pojo-bundle&count=" + CONTENT_LOOP_COUNT,
            HttpStatus.SC_OK);
    }

    @PerformanceTest(runinvocations = INVOCATIONS, warmupinvocations = WARMUP_INVOCATIONS, threshold = 3)
    public void test5SlyJavaSlingModels() {
        getStringContent("/sightlyperf/loop.html?selector=sly-java-slingmodels&count=" + CONTENT_LOOP_COUNT,
            HttpStatus.SC_OK);
    }

    @PerformanceTest(runinvocations = INVOCATIONS, warmupinvocations = WARMUP_INVOCATIONS, threshold = 4)
    public void test6SlyJSAsync() {
        getStringContent("/sightlyperf/loop.html?selector=sly-js-async&count=" + CONTENT_LOOP_COUNT,
                HttpStatus.SC_OK);
    }

    @PerformanceTest(runinvocations = INVOCATIONS, warmupinvocations = WARMUP_INVOCATIONS, threshold = 4)
    public void test7SlyJSSync() {
        getStringContent("/sightlyperf/loop.html?selector=sly-js-sync&count=" + CONTENT_LOOP_COUNT,
            HttpStatus.SC_OK);
    }

}
