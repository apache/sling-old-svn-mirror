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
package org.apache.sling.launchpad.webapp.integrationtest.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONTokener;
import org.apache.sling.junit.remote.httpclient.RemoteTestHttpClient;
import org.apache.sling.testing.tools.http.RequestExecutor;
import org.apache.sling.testing.tools.jarexec.JarExecutor;
import org.apache.sling.testing.tools.sling.SlingClient;
import org.apache.sling.testing.tools.sling.SlingTestBase;

/** Configurable SlingClient for server-side tests.
 *  We can't inherit from SlingTestBase as
 *  that class tries to start the JarExecutor which will fail in some
 *  situations (TODO: split that class into smaller utilities to avoid 
 *  this problem)
 */
public class ServerSideTestClient extends SlingClient {

    private final static String configuredUrl = System.getProperty(
            SlingTestBase.TEST_SERVER_URL_PROP,
            System.getProperty("launchpad.http.server.url"));
    private final static String serverBaseUrl = getServerBaseUrl();
    private final static String serverUsername = getUsername();
    private final static String serverPassword = getPassword();

    public static class TestResults {
        int testCount;
        List<String> failures = new ArrayList<String>();
        
        public int getTestCount() {
            return testCount;
        }
        
        public List<String> getFailures() {
            return failures;
        }
    }
    
    public ServerSideTestClient() {
        super(getServerBaseUrl(), getUsername(), getPassword());
    }
    
    public TestResults runTests(String testPackageOrClassName) throws Exception {
        final RemoteTestHttpClient testClient = new RemoteTestHttpClient(serverBaseUrl + "/system/sling/junit",serverUsername,serverPassword,true);
        final TestResults r = new TestResults();
        final Map<String, String> options = new HashMap<String, String>();
        options.put("forceReload", "true");
        final RequestExecutor executor = testClient.runTests(testPackageOrClassName, null, "json", options);
        executor.assertContentType("application/json");
        String content = executor.getContent();
        final JSONArray json = new JSONArray(new JSONTokener(content));

        for(int i = 0 ; i < json.length(); i++) {
            final JSONObject obj = json.getJSONObject(i);
            if("test".equals(obj.getString("INFO_TYPE"))) {
                r.testCount++;
                if(obj.has("failure")) {
                    r.failures.add(obj.get("failure").toString());
                }
            }
        }

        return r;
    }
    
    /** Run server-side test(s)
     * @param testPackageOrClassName selects which tests to run
     * @param expectedTestsCount Use a negative -N value to mean "at least N tests"
     * @throws Exception
     */
    public void assertTestsPass(String testPackageOrClassName, int expectedTestsCount) throws Exception {
        TestResults results = runTests(testPackageOrClassName);
        if(expectedTestsCount < 0) {
            assertTrue("Expecting at least " + -expectedTestsCount + " test(s) for " + testPackageOrClassName, 
                    results.getTestCount() >= -expectedTestsCount);
        } else {
            assertEquals("Expecting " + expectedTestsCount + " test(s) for " + testPackageOrClassName, 
                    expectedTestsCount, results.getTestCount());
        }
        if(!results.getFailures().isEmpty()) {
            fail(results.getFailures().size() + " tests failed:" + results.getFailures());
        }
    }
    
    private static String getServerBaseUrl() {
        String serverBaseUrl = null;
        if (configuredUrl != null) {
            if ( configuredUrl.endsWith("/") ) {
                serverBaseUrl = configuredUrl.substring(0, configuredUrl.length() - 1);
            } else {
                serverBaseUrl = configuredUrl;
            }
        } else {
            String serverHost = System.getProperty(SlingTestBase.SERVER_HOSTNAME_PROP);
            if (serverHost == null || serverHost.trim().length() == 0) {
                serverHost = "localhost";
            }
            final String portStr = System.getProperty(JarExecutor.PROP_SERVER_PORT);
            final int serverPort = portStr == null ? JarExecutor.DEFAULT_PORT : Integer.valueOf(portStr);
            serverBaseUrl = "http://" + serverHost + ":" + String.valueOf(serverPort);
        }
        return serverBaseUrl;
    }

    private static String getPassword() {
        // Set configured password using "admin" as default credential
        final String configuredPassword = System
                .getProperty(SlingTestBase.TEST_SERVER_PASSWORD);
        if (configuredPassword != null
                && configuredPassword.trim().length() > 0) {
            return configuredPassword;
        } else {
            return SlingTestBase.ADMIN;
        }
    }

    private static String getUsername() {
        // Set configured username using "admin" as default credential
        final String configuredUsername = System
                .getProperty(SlingTestBase.TEST_SERVER_USERNAME);
        if (configuredUsername != null
                && configuredUsername.trim().length() > 0) {
            return configuredUsername;
        } else {
            return SlingTestBase.ADMIN;
        }
    }
}