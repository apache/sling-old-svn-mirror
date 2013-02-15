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

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONTokener;
import org.apache.sling.testing.tools.http.Request;
import org.apache.sling.testing.tools.retry.RetryLoop;
import org.apache.sling.testing.tools.sling.TimeoutsProvider;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Test the JSON list of tests returned by the JUnit servlet, as an
 *  example of testing JSON responses.
 */
public class JSONResponseTest extends ServerSideTestsBase {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final int TEST_LIST_TIMEOUT_SECONDS = TimeoutsProvider.getInstance().getTimeout(30);
    
    @Test
    public void testWithRetries() {
        // Need a retry loop as the tests might still be registering
        // when this test runs...we'd need to make the readyness detection
        // more extensive to avoid this
        // TODO we could probably use a JUnit Rule to retry tests.
        final RetryLoop.Condition c = new RetryLoop.Condition() {

            public String getDescription() {
                return "Checking JSON list of server-side tests";
            }

            public boolean isTrue() throws Exception {
                testJsonListOfTests();
                return true;
            }
        };

        log.info("{} (timeout={} seconds)", c.getDescription(), TEST_LIST_TIMEOUT_SECONDS);
        new RetryLoop(c, TEST_LIST_TIMEOUT_SECONDS, TimeoutsProvider.getInstance().getTimeout(500));
    }
    
    private boolean findTestName(JSONArray json, String name) throws JSONException {
        for(int i = 0 ; i < json.length(); i++) {
            final JSONObject obj = json.getJSONObject(i);
            if("list".equals(obj.getString("INFO_TYPE")) && "testNames".equals(obj.getString("INFO_SUBTYPE"))) {
                final JSONArray data = obj.getJSONArray("data");
                for(int j=0; j < data.length(); j++) {
                    if(name.equals(data.getString(j))) {
                        return true;
                    }
                }
            }
        }
        log.info("Test name not found in JSON response: {}", name);
        return false;
    }

    private void testJsonListOfTests() throws Exception {
        Request r = getRequestBuilder().buildGetRequest(JUNIT_SERVLET_PATH + "/.json")
                .withCredentials(getServerUsername(), getServerPassword());
        
        // Get list of tests in JSON format
        getRequestExecutor().execute(r)
        .assertStatus(200)
        .assertContentType("application/json");
        
        // Parse JSON response for more precise testing
        final JSONArray json = new JSONArray(new JSONTokener((getRequestExecutor().getContent())));
        
        // Verify that some test names are present in the response
        final List<String> expectedTestNames = Arrays.asList(new String []{
                "org.apache.sling.testing.samples.failingtests.EmptyTest",
                "org.apache.sling.testing.samples.failingtests.JUnit3FailingTest",
                "org.apache.sling.testing.samples.failingtests.JUnit4FailingTest",
                "org.apache.sling.testing.samples.sampletests.JUnit3Test",
                "org.apache.sling.testing.samples.sampletests.JUnit4Test",
                "org.apache.sling.testing.samples.sampletests.OsgiAwareTest",
                "org.apache.sling.junit.scriptable.ScriptableTestsProvider"
        });
        
        for(String name : expectedTestNames) {
            assertTrue(
                    "Expecting test name " + name + " in json response", 
                    findTestName(json, name));
        }
    }
}
