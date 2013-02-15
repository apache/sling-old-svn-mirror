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
package org.apache.sling.testing.samples.integrationtests.serverside.scriptable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONTokener;
import org.apache.sling.junit.remote.httpclient.RemoteTestHttpClient;
import org.apache.sling.testing.samples.integrationtests.serverside.ServerSideTestsBase;
import org.apache.sling.testing.tools.http.RequestExecutor;
import org.apache.sling.testing.tools.sling.SlingClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Test server-side scriptable tests by creating test nodes and
 *  scripts, for both success and failure cases */
@RunWith(Parameterized.class)
public class ScriptableTestsTest extends ServerSideTestsBase {
    public static final String RESOURCE_TYPE = "testing/ScriptableTests";
    public static final String TEST_NODES_BASE = "/apps/" + RESOURCE_TYPE;
    public static final String TEST_NODE_PATH = TEST_NODES_BASE + "/testnode";
    
    private SlingClient slingClient;
    private RemoteTestHttpClient testClient;
    private final String testScript;
    private final int failureCount;

    public ScriptableTestsTest(String testScript, Integer failureCount) {
        this.testScript = testScript;
        this.failureCount = failureCount.intValue();
    }
    
    @Before
    public void setupContent() throws Exception {
        slingClient = new SlingClient(getServerBaseUrl(), getServerUsername(), getServerPassword());
        testClient = new RemoteTestHttpClient(getServerBaseUrl() + JUNIT_SERVLET_PATH, getServerUsername(), getServerPassword(), true);
        
        cleanupContent();

        // A test node has the sling:Test mixin and points
        // to a test script
        slingClient.createNode(TEST_NODE_PATH, 
                "sling:resourceType", RESOURCE_TYPE,
                "jcr:mixinTypes", "sling:Test");
        
        // Test script will be requested with .test.txt selector and extension
        slingClient.upload(TEST_NODES_BASE + "/test.txt.esp", 
                new ByteArrayInputStream(testScript.getBytes()), -1, true);
    }
    
    @After
    public void cleanupContent() throws Exception {
        if(slingClient.exists(TEST_NODES_BASE)) {
            slingClient.delete(TEST_NODES_BASE);
        }
    }
    
    @Test
    public void testScriptableTest() throws Exception {
        
        // Execute all available scriptable tests and count failures 
        final RequestExecutor executor = testClient.runTests(
                "org.apache.sling.junit.scriptable.ScriptableTestsProvider",
                null,
                "json"
                );
        executor.assertContentType("application/json");
        final JSONArray json = new JSONArray(new JSONTokener((executor.getContent())));
        
        int testsCount = 0;
        final List<String> failures = new ArrayList<String>();
        for(int i = 0 ; i < json.length(); i++) {
            final JSONObject obj = json.getJSONObject(i);
            if("test".equals(obj.getString("INFO_TYPE"))) {
                testsCount++;
                if(obj.has("failure")) {
                    failures.add(obj.get("failure").toString());
                }
            }
        }
        
        final int expectedTests = 1;
        assertEquals("Expecting " + expectedTests + " scriptable tests", expectedTests, testsCount);
        
        if(failures.size() != failureCount) {
            fail("Expected " + failureCount + " failing tests but got " + failures.size() + ": " + failures);
        }
    }
    
    /** Test both pass and fail cases */ 
    @Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> data = new ArrayList<Object[]>();
        data.add(new Object[] { "TEST_PASSED", 0});
        data.add(new Object[] { "TEST_FAILED", 1});
        return data;
    }
}
