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
package org.apache.sling.commons.testing.integration;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;

/** Helper class for HTTP tests, extends HttpTestBase and adds
 *  a few utilities that we commonly use in our integration tests.
 *  
 *  Meant to be used as a helper class in JUnit4-style tests, as we
 *  gradually move away from JUnit3 style.  
 */
public class HttpTest extends HttpTestBase {
    protected String scriptPath;
    protected String testText;
    protected String displayUrl;
    
    public String uploadTestScript(String localFilename,String filenameOnServer) throws IOException {
        return uploadTestScript(scriptPath, localFilename, filenameOnServer);
    }
    
    public static void assertContains(String content, String expected) {
        if(!content.contains(expected)) {
            fail("Content does not contain '" + expected + "' (content=" + content + ")");
        }
    }
    
    public static void assertNotContains(String content, String notExpected) {
        if(content.contains(notExpected)) {
            fail("Content contains '" + notExpected + "' (content=" + content + ")");
        }
    }
    
    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }
    
    public String getScriptPath() {
        return scriptPath;
    }
    
    public SlingIntegrationTestClient getTestClient() {
        return testClient;
    }
    
    public HttpClient getHttpClient() {
        return httpClient;
    }
    
    /** Making this public here, changing the base class to public is not convenient
     *  as many derived classes override it as protected.
     */
    public void setUp() throws Exception {
        super.setUp();
    }
    
    /** Making this public here, changing the base class to public is not convenient
     *  as many derived classes override it as protected.
     */
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
