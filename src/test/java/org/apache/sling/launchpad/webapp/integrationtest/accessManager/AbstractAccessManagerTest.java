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
package org.apache.sling.launchpad.webapp.integrationtest.accessManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;

/**
 * Base class for AccessManager tests.
 */
public abstract class AbstractAccessManagerTest extends HttpTestBase {

	public static final String TEST_BASE_PATH = "/sling-tests";

    /** Execute a POST request and check status */
    protected void assertAuthenticatedPostStatus(Credentials creds, String url, int expectedStatusCode, List<NameValuePair> postParams, String assertMessage)
    throws IOException {
        final PostMethod post = new PostMethod(url);
        post.setFollowRedirects(false);
        
        URL baseUrl = new URL(HTTP_BASE_URL);
        AuthScope authScope = new AuthScope(baseUrl.getHost(), baseUrl.getPort(), AuthScope.ANY_REALM);
        post.setDoAuthentication(true);
        Credentials oldCredentials = httpClient.getState().getCredentials(authScope);
        try {
			httpClient.getState().setCredentials(authScope, creds);
	        
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
        } finally {
        	httpClient.getState().setCredentials(authScope, oldCredentials);
        }
    }

    /** Verify that given URL returns expectedStatusCode
     * @throws IOException */
    protected void assertAuthenticatedHttpStatus(Credentials creds, String urlString, int expectedStatusCode, String assertMessage) throws IOException {
        URL baseUrl = new URL(HTTP_BASE_URL);
        AuthScope authScope = new AuthScope(baseUrl.getHost(), baseUrl.getPort(), AuthScope.ANY_REALM);
        GetMethod getMethod = new GetMethod(urlString);
        getMethod.setDoAuthentication(true);
        
        Credentials oldCredentials = httpClient.getState().getCredentials(authScope);
    	try {
			httpClient.getState().setCredentials(authScope, creds);

			final int status = httpClient.executeMethod(getMethod);
            if(assertMessage == null) {
                assertEquals(urlString,expectedStatusCode, status);
            } else {
                assertEquals(assertMessage, expectedStatusCode, status);
            }
    	} finally {
        	httpClient.getState().setCredentials(authScope, oldCredentials);
    	}
    }

    
    /** retrieve the contents of given URL and assert its content type
     * @param expectedContentType use CONTENT_TYPE_DONTCARE if must not be checked 
     * @throws IOException
     * @throws HttpException */
    protected String getAuthenticatedContent(Credentials creds, String url, String expectedContentType, List<NameValuePair> params, int expectedStatusCode) throws IOException {
        final GetMethod get = new GetMethod(url);

        URL baseUrl = new URL(HTTP_BASE_URL);
        AuthScope authScope = new AuthScope(baseUrl.getHost(), baseUrl.getPort(), AuthScope.ANY_REALM);
        get.setDoAuthentication(true);
        Credentials oldCredentials = httpClient.getState().getCredentials(authScope);
    	try {
			httpClient.getState().setCredentials(authScope, creds);
			
	        if(params != null) {
	            final NameValuePair [] nvp = new NameValuePair[0];
	            get.setQueryString(params.toArray(nvp));
	        }
	        final int status = httpClient.executeMethod(get);
	        final InputStream is = get.getResponseBodyAsStream();
	        final StringBuffer content = new StringBuffer();
	        final String charset = get.getResponseCharSet();
	        final byte [] buffer = new byte[16384];
	        int n = 0;
	        while( (n = is.read(buffer, 0, buffer.length)) > 0) {
	            content.append(new String(buffer, 0, n, charset));
	        }
	        assertEquals("Expected status " + expectedStatusCode + " for " + url + " (content=" + content + ")",
	                expectedStatusCode,status);
	        final Header h = get.getResponseHeader("Content-Type");
	        if(expectedContentType == null) {
	            if(h!=null) {
	                fail("Expected null Content-Type, got " + h.getValue());
	            }
	        } else if(CONTENT_TYPE_DONTCARE.equals(expectedContentType)) {
	            // no check
	        } else if(h==null) {
	            fail(
	                    "Expected Content-Type that starts with '" + expectedContentType
	                    +" but got no Content-Type header at " + url
	            );
	        } else {
	            assertTrue(
	                "Expected Content-Type that starts with '" + expectedContentType
	                + "' for " + url + ", got '" + h.getValue() + "'",
	                h.getValue().startsWith(expectedContentType)
	            );
	        }
	        return content.toString();
			
    	} finally {
        	httpClient.getState().setCredentials(authScope, oldCredentials);
    	}
    }
    
    
    protected static int counter = 1;
    
	protected String createTestUser() throws IOException {
        String postUrl = HTTP_BASE_URL + "/system/userManager/user.create.html";

		String testUserId = "testUser" + (counter++);
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":name", testUserId));
		postParams.add(new NameValuePair("pwd", "testPwd"));
		postParams.add(new NameValuePair("pwdConfirm", "testPwd"));
		assertPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		return testUserId;
	}
    
	protected String createTestGroup() throws IOException {
        String postUrl = HTTP_BASE_URL + "/system/userManager/group.create.html";

		String testGroupId = "testGroup" + (counter++);
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(":name", testGroupId));
		
		//success would be a redirect to the welcome page of the webapp
        Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);
		
		return testGroupId;
	}
	
	protected String createTestFolder() throws IOException {
        String postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + "testFolder" + (counter++);

        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));

        return location;
	}
}
