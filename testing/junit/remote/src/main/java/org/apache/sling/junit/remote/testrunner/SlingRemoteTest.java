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
package org.apache.sling.junit.remote.testrunner;

import java.io.IOException;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.runner.Description;

/** Info about a remote tests, as provided by the Sling JUnit servlet */
class SlingRemoteTest {
    private final Class<?> testClass;
    private final String description;
    private final String failure;
    private final String trace;
    
    public static final String DESCRIPTION = "description";
    public static final String FAILURE = "failure";
    public static final String TRACE = "trace";
    
    SlingRemoteTest(Class<?> testClass, JSONObject json) throws JSONException {
        this.testClass = testClass;
        description = json.getString(DESCRIPTION);
        failure = json.has(FAILURE) ? json.getString(FAILURE) : null;
        if (failure != null) {
            trace = json.has(TRACE) ? json.getString(TRACE) : null;
        } else {
            trace = null;
        }
    }
    
    Description describe() {
        return Description.createTestDescription(testClass, description);
    }
    
    void run() {
        if(failure != null && failure.trim().length() > 0) {
            try {
                throw new RemoteExecutionException(failure, trace);
            } catch (NumberFormatException e) {
                // error reading stack
            } catch (IOException e) {
                // error reading stack
            }
            // TODO: distinguish between assumption failures and regular exceptions
        }
    }
}
