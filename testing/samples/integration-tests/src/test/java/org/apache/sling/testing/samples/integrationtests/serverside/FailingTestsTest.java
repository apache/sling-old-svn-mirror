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

import static org.junit.Assert.assertEquals;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONTokener;
import org.apache.sling.testing.tools.http.Request;
import org.junit.Test;

/** Verify that failures are correctly reported, using
 *  tests from the failingtests bundle.
 */
public class FailingTestsTest extends ServerSideTestsBase {
    
    public static final String FAILURE_FIELD = "failure";
    public static final String DESCRIPTION_FIELD = "description";
    
    /** Extract the "failure" field of given test in JSON
     *  response data. 
     */
    private String getFailure(JSONArray json, String testName) throws JSONException {
        String result = null;
        
        for(int i = 0 ; i < json.length(); i++) {
            final JSONObject obj = json.getJSONObject(i);
            if("test".equals(obj.getString("INFO_TYPE"))) {
                if(obj.getString(DESCRIPTION_FIELD).contains(testName)) {
                    if(obj.has(FAILURE_FIELD)) {
                        result = obj.getString(FAILURE_FIELD);
                        break;
                    }
                }
            }
        }
        
        return result;
    }
    
    @Test
    public void testFailures() throws Exception{
        
        // Execute tests from the failingtests bundle and verify response
        final Request r = getRequestBuilder().buildPostRequest(JUNIT_SERVLET_PATH 
                + "/org.apache.sling.testing.samples.failingtests.json").withCredentials(getServerUsername(), getServerPassword());
        getRequestExecutor().execute(r).assertStatus(200);
        
        final JSONArray json = new JSONArray(new JSONTokener((getRequestExecutor().getContent())));
        
        assertEquals(
                "initializationError(org.apache.sling.testing.samples.failingtests.EmptyTest): No runnable methods",
                getFailure(json, "org.apache.sling.testing.samples.failingtests.EmptyTest")
        );
        
        assertEquals(
                "testFailsEveryTime(org.apache.sling.testing.samples.failingtests.JUnit3FailingTest): This JUnit3 test fails every time",
                getFailure(json, "testFailsEveryTime(org.apache.sling.testing.samples.failingtests.JUnit3FailingTest")
        );
        
        assertEquals(
                "testAssertsEveryTime(org.apache.sling.testing.samples.failingtests.JUnit3FailingTest): This JUnit3 test asserts every time",
                getFailure(json, "testAssertsEveryTime(org.apache.sling.testing.samples.failingtests.JUnit3FailingTest")
        );
        
        assertEquals(
                "testFailsEveryTime(org.apache.sling.testing.samples.failingtests.JUnit4FailingTest): This JUnit4 test fails every time",
                getFailure(json, "testFailsEveryTime(org.apache.sling.testing.samples.failingtests.JUnit4FailingTest")
        );
        
        assertEquals(
                "testAssertsEveryTime(org.apache.sling.testing.samples.failingtests.JUnit4FailingTest): This JUnit4 test asserts every time",
                getFailure(json, "testAssertsEveryTime(org.apache.sling.testing.samples.failingtests.JUnit4FailingTest")
        );
    }
}
