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
package org.apache.sling.bgservlets.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.regex.Pattern;

import junit.framework.AssertionFailedError;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.HttpTest;
import org.junit.Before;
import org.junit.Test;

public class BackgroundRequestIT {
    private final HttpTest T = new HttpTest();
    
    @Before
    public void setup() throws Exception {
        T.setUp();
    }
    
    /** Find out how many job state nodes are present */ 
    private int countStoredJobs() throws IOException {
        int jobs = 0;
        String content = "";
        try {
            content = T.getContent(HttpTest.HTTP_BASE_URL + "/var/bg.tidy.20.json", "application/json");
        } catch(AssertionFailedError ignore) {
            // Expected if no jobs have ever been created
        }
        final BufferedReader r = new BufferedReader(new StringReader(content));
        String line = null;
        while((line = r.readLine()) != null) {
            if(line.contains("sling:resourceType") && line.contains("sling/bg/job")) {
                jobs++;
            }
        }
        return jobs;
    }
    
    @Test
    public void testGetRequestFails() throws IOException,InterruptedException, JSONException {
        T.assertHttpStatus(HttpTest.HTTP_BASE_URL + "/tmp.json?sling:bg=true", 500);
    }
    
    @Test
    public void testTmpRequestCreatesJob() throws IOException,InterruptedException, JSONException {
        final int initialJobs = countStoredJobs();
        T.assertPostStatus(HttpTest.HTTP_BASE_URL + "/tmp.json?sling:bg=true", 302, null, null);

        // Request must have created a job
        final long timeout = System.currentTimeMillis() + 10000L;
        while(true) {
            if(countStoredJobs() >= initialJobs + 1) {
                break;
            }
            if(System.currentTimeMillis()  > timeout) {
                fail("Timeout waiting for background job creation");
            }
            Thread.sleep(50L);
        }
        
        // Verify that jobs are stored under a path that includes the Sling instance ID
        final String path = "/var/bg/jobs.tidy.1.json";
        final JSONObject json = new JSONObject(T.getContent(HttpTest.HTTP_BASE_URL + path, "application/json"));
        int matches = 0;
        final Pattern instanceIdPattern = Pattern.compile("([0-9a-fA-F\\\\-]){20,}");
        final Iterator<String> it = json.keys();
        while(it.hasNext()) {
            final String key = it.next();
            if(instanceIdPattern.matcher(key).matches()) {
                matches++;
            }
        }
        if(matches == 0) {
            fail("No instance ID node found under " + path);
        }
        
        // Wait a bit - no more jobs should be created
        Thread.sleep(2000L);
        assertEquals("Expecting no extra jobs", initialJobs + 1, countStoredJobs());
    }
}
