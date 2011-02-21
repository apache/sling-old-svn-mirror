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

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONTokener;
import org.apache.stanbol.commons.testing.http.Request;
import org.apache.stanbol.commons.testing.http.RetryLoop;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Test the JSON list of tests returned by the JUnit servlet, as an
 *  example of testing JSON responses.
 */
public class JSONResponseTest extends ServerSideTestsBase {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    // TODO compute those timeouts based on a configured factor
    // to cope with slower testing systems??
    public static final int TEST_LIST_TIMEOUT_SECONDS = 30;
    
    @Test
    @Ignore // TODO: fails in mvn build, why??
    public void testWithRetries() {
        // Need a retry loop as the tests might still be registering
        // when this test runs...we'd need to make the readyness detection
        // more extensive to avoid this
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
        new RetryLoop(c, TEST_LIST_TIMEOUT_SECONDS, 500);
    }

    private void testJsonListOfTests() throws Exception {
        Request r = builder.buildGetRequest(JUNIT_SERVLET_PATH + "/.json");
        
        // Get list of tests in JSON format
        executor.execute(r)
        .assertStatus(200)
        .assertContentType("application/json");
        
        // Parse JSON response for more precise testing
        final JSONArray json = new JSONArray(new JSONTokener((executor.getContent())));
        
        // Verify that all our test names are in the response
        final List<String> expectedTestNames = Arrays.asList(new String []{
                "org.apache.sling.junit.scriptable.ScriptableTestsProvider",
                "org.apache.sling.junit.testbundle.tests.JUnit3Test",
                "org.apache.sling.junit.testbundle.tests.JUnit4Test",
                "org.apache.sling.junit.testbundle.tests.MissingTest",
                "org.apache.sling.junit.testbundle.tests.OsgiAwareTest"
        });

        // Response contains an array of objects identified by 
        // their INFO_TYPE and INFO_SUBTYPE: check the one
        // that has type=list and subtype=testNames
        boolean dataFound = false;
        for(int i = 0 ; i < json.length(); i++) {
            final JSONObject obj = json.getJSONObject(i);
            if("list".equals(obj.getString("INFO_TYPE")) && "testNames".equals(obj.getString("INFO_SUBTYPE"))) {
                dataFound = true;
                final JSONArray data = obj.getJSONArray("data");
                assertEquals("Expecting correct number of tests", expectedTestNames.size(), data.length());
                
                int matched = 0;
                for(int j=0; j < data.length(); j++) {
                    if(expectedTestNames.contains(data.getString(j))) {
                        matched++;
                    }
                }
                assertEquals("Expecting to find all test names in data array", expectedTestNames.size(), matched);
            }
        }
        
        if(!dataFound) {
            fail("Test names object not found in response");
        }
    }
}