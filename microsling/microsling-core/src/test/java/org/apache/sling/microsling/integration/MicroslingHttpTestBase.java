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
package org.apache.sling.microsling.integration;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.TestCase;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.sling.microsling.integration.helpers.MicroslingIntegrationTestClient;

/** Base class for HTTP-based microsling integration tests */
class MicroslingHttpTestBase extends TestCase {
    public static final String HTTP_BASE_URL = System.getProperty("microsling.http.server.url");
    public static final String WEBDAV_BASE_URL = System.getProperty("microsling.webdav.server.url");
    
    /** base path for test files */
    public static final String TEST_PATH = "/microsling-integration-tests";
    
    public static final String CONTENT_TYPE_HTML = "text/html";
    public static final String CONTENT_TYPE_XML = "text/xml";
    public static final String CONTENT_TYPE_PLAIN = "text/plain";
    
    protected MicroslingIntegrationTestClient testClient;
    protected HttpClient httpClient;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // assume http and webdav are on the same host + port
        URL url = null;
        try {
            url = new URL(HTTP_BASE_URL);
        } catch(MalformedURLException mfe) {
            // MalformedURLException doesn't tell us the URL by default
            throw new IOException("MalformedURLException: " + HTTP_BASE_URL);
        }
        
        // setup HTTP client, with authentication (using default Jackrabbit credentials)
        httpClient = new HttpClient();
        httpClient.getParams().setAuthenticationPreemptive(true);
        Credentials defaultcreds = new UsernamePasswordCredentials("admin", "admin");
        httpClient.getState().setCredentials(new AuthScope(url.getHost(), url.getPort(), AuthScope.ANY_REALM), defaultcreds);

        testClient = new MicroslingIntegrationTestClient(httpClient);
    }

    /** Verify that given URL returns expectedStatusCode 
     * @throws IOException */
    protected void assertHttpStatus(String urlString, int expectedStatusCode, String assertMessage) throws IOException {
        final int status = httpClient.executeMethod(new GetMethod(urlString));
        if(assertMessage == null) {
            assertEquals(expectedStatusCode, status);
        } else {
            assertEquals(assertMessage, expectedStatusCode, status);
        }
    }
    
    /** Verify that given URL returns expectedStatusCode 
     * @throws IOException */
    protected void assertHttpStatus(String urlString, int expectedStatusCode) throws IOException {
        assertHttpStatus(urlString, expectedStatusCode, null);
    }
}