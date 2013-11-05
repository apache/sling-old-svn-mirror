/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.launchpad.webapp.integrationtest;

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
import org.apache.sling.commons.testing.integration.HttpTest;

/**
 * Utility class for tests which use authentication.
 */
public class AuthenticatedTestUtil extends HttpTest {

    /** Execute a POST request and check status */
    public void assertAuthenticatedAdminPostStatus(String url, int expectedStatusCode, List<NameValuePair> postParams, String assertMessage)
    throws IOException {
        Credentials defaultcreds = new UsernamePasswordCredentials("admin", "admin");
        assertAuthenticatedPostStatus(defaultcreds, url, expectedStatusCode, postParams, assertMessage);
    }

    /** Execute a POST request and check status */
    public void assertAuthenticatedPostStatus(Credentials creds, String url, int expectedStatusCode, List<NameValuePair> postParams, String assertMessage)
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
    public void assertAuthenticatedHttpStatus(Credentials creds, String urlString, int expectedStatusCode, String assertMessage) throws IOException {
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
    public String getAuthenticatedContent(Credentials creds, String url, String expectedContentType, List<NameValuePair> params, int expectedStatusCode) throws IOException {
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

    /** retrieve the contents of given URL and assert its content type
     * @param expectedContentType use CONTENT_TYPE_DONTCARE if must not be checked
     * @throws IOException
     * @throws HttpException */
    public String getAuthenticatedPostContent(Credentials creds, String url, String expectedContentType, List<NameValuePair> postParams, int expectedStatusCode) throws IOException {
        final PostMethod post = new PostMethod(url);

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
	        final InputStream is = post.getResponseBodyAsStream();
	        final StringBuffer content = new StringBuffer();
	        final String charset = post.getResponseCharSet();
	        final byte [] buffer = new byte[16384];
	        int n = 0;
	        while( (n = is.read(buffer, 0, buffer.length)) > 0) {
	            content.append(new String(buffer, 0, n, charset));
	        }
	        assertEquals("Expected status " + expectedStatusCode + " for " + url + " (content=" + content + ")",
	                expectedStatusCode,status);
	        final Header h = post.getResponseHeader("Content-Type");
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


    private static long randomId = System.currentTimeMillis();

    private static synchronized long getNextInt() {
        final long val = randomId;
        randomId++;
        return val;
    }

    public String createTestUser() throws IOException {
        String postUrl = HTTP_BASE_URL + "/system/userManager/user.create.html";

        String testUserId = "testUser" + getNextInt();
        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new NameValuePair(":name", testUserId));
        postParams.add(new NameValuePair("pwd", "testPwd"));
        postParams.add(new NameValuePair("pwdConfirm", "testPwd"));
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		final String msg = "Unexpected status while attempting to create test user at " + postUrl; 
        assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, msg);
        
        final String sessionInfoUrl = HTTP_BASE_URL + "/system/sling/info.sessionInfo.json";
        assertAuthenticatedHttpStatus(creds, sessionInfoUrl, HttpServletResponse.SC_OK, 
                "session info failed for user " + testUserId);

        return testUserId;
    }

    public String createTestGroup() throws IOException {
        String postUrl = HTTP_BASE_URL + "/system/userManager/group.create.html";

        String testGroupId = "testGroup" + getNextInt();
        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new NameValuePair(":name", testGroupId));

        //success would be a redirect to the welcome page of the webapp
        assertAuthenticatedAdminPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);

        return testGroupId;
    }
}
