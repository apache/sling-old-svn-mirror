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
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.microsling.integration.helpers.MicroslingIntegrationTestClient;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/** Base class for HTTP-based microsling integration tests */
public class MicroslingHttpTestBase extends TestCase {
    public static final String HTTP_BASE_URL = System.getProperty("microsling.http.server.url");
    public static final String WEBDAV_BASE_URL = System.getProperty("microsling.webdav.server.url");
    
    /** base path for test files */
    public static final String TEST_PATH = "/microsling-integration-tests";
    
    public static final String CONTENT_TYPE_HTML = "text/html";
    public static final String CONTENT_TYPE_XML = "text/xml";
    public static final String CONTENT_TYPE_PLAIN = "text/plain";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_JS = "application/x-javascript";
    public static final String CONTENT_TYPE_CSS = "text/css";
    
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
    
    /** Execute a POST request and check status */
    protected void assertPostStatus(String url, int expectedStatusCode, List<NameValuePair> postParams, String assertMessage)
    throws IOException {
        final PostMethod post = new PostMethod(url);
        post.setFollowRedirects(false);
        
        if(postParams!=null) {
            final NameValuePair [] nvp = {};
            post.setRequestBody(postParams.toArray(nvp));
        }
        
        final int status = httpClient.executeMethod(post);
        if(assertMessage == null) {
            assertEquals(expectedStatusCode, status);
        } else {
            assertEquals(assertMessage, expectedStatusCode, status);
        }
    }
    
    /** retrieve the contents of given URL and assert its content type
     * @throws IOException
     * @throws HttpException */
    protected String getContent(String url, String expectedContentType) throws IOException {
        final GetMethod get = new GetMethod(url);
        final int status = httpClient.executeMethod(get);
        assertEquals("Expected status 200 for " + url,200,status);
        final Header h = get.getResponseHeader("Content-Type");
        if(expectedContentType == null) {
            if(h!=null) {
                fail("Expected null Content-Type, got " + h.getValue());
            }
        } else {
            assertTrue(
                "Expected Content-Type that starts with '" + expectedContentType 
                + "' for " + url + ", got '" + h.getValue() + "'",
                h.getValue().startsWith(expectedContentType)
            );
        }
        return get.getResponseBodyAsString();
    }

    /** upload rendering test script, and return its URL for future deletion */
    protected String uploadTestScript(String scriptPath, String localFilename,String filenameOnServer) throws IOException {
        final String url = WEBDAV_BASE_URL + scriptPath + "/" + filenameOnServer;
        final String testFile = "/integration-test/" + localFilename;
        final InputStream data = getClass().getResourceAsStream(testFile);
        try {
            testClient.upload(url, data);
        } finally {
            if(data!=null) {
                data.close();
            }
        }
        return url;
    }

    /** Evaluate given code using given jsonData as the "data" object */ 
    protected void assertJavascript(String expectedOutput, String jsonData, String code) throws IOException {
        // build the code, something like
        //  data = <jsonData> ;
        //  <code>
        final String jsCode = "data=" + jsonData + ";\n" + code;
        final Context rhinoContext = Context.enter();
        final ScriptableObject scope = rhinoContext.initStandardObjects();

        // execute the script, out script variable maps to sw 
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        ScriptableObject.putProperty(scope, "out", Context.javaToJS(pw, scope));
        final int lineNumber = 1;
        final Object securityDomain = null;
        rhinoContext.evaluateString(scope, jsCode, getClass().getSimpleName(), 
                lineNumber, securityDomain);
        
        // check script output
        pw.flush();
        final String result = sw.toString().trim();
        if(!result.equals(expectedOutput)) {
            fail("Expected '" + expectedOutput + "' but got '" + result + "' for script='" + jsCode + "'");
        }
    }
}