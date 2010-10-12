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
package org.apache.sling.launchpad.webapp.integrationtest.auth;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.commons.testing.integration.HttpTestBase;

/**
 * Test of the response code from an authentication request depending on various
 * request parameters and headers including j_validate, Ajax detection, and
 * non-browser client detection.
 */
public class AuthenticationResponseCodeTest extends HttpTestBase {

    public void testValidatingCorrectFormCredentials() throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair("j_username", "admin"));
        params.add(new NameValuePair("j_password", "admin"));
        params.add(new NameValuePair("j_validate", "true"));
        HttpMethod post = assertPostStatus(HTTP_BASE_URL + "/j_security_check", HttpServletResponse.SC_OK, params, null);
        assertTrue(post.getResponseBodyAsString().length() == 0);

        List<NameValuePair> params2 = new ArrayList<NameValuePair>();
        params2.add(new NameValuePair("j_validate", "true"));
        HttpMethod post2 = assertPostStatus(HTTP_BASE_URL + "/j_security_check", HttpServletResponse.SC_OK, params2, null);
        assertTrue(post2.getResponseBodyAsString().length() == 0);
    }

    public void testValidatingCorrectHttpBasicCredentials() throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair("j_validate", "true"));
        HttpMethod post = assertPostStatus(HTTP_BASE_URL + "/j_security_check", HttpServletResponse.SC_OK, params, null);
        assertTrue(post.getResponseBodyAsString().length() == 0);

        HttpMethod get = assertHttpStatus(HTTP_BASE_URL + "?j_validate=true", HttpServletResponse.SC_OK);
        assertTrue(get.getResponseBodyAsString().length() == 0);
    }

    public void testValidatingIncorrectCredentials() throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair("j_username", "garbage"));
        params.add(new NameValuePair("j_password", "garbage"));
        params.add(new NameValuePair("j_validate", "true"));
        HttpMethod post = assertPostStatus(HTTP_BASE_URL + "/j_security_check", HttpServletResponse.SC_FORBIDDEN, params, null);
        assertNotNull(post.getResponseHeader("X-Reason"));
    }

    public void testValidatingIncorrectCookie() throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair("j_validate", "true"));

        List<Header> headers = new ArrayList<Header>();
        headers.add(new Header("Cookie", "sling.formauth=garbage"));

        HttpMethod post = assertPostStatus(HTTP_BASE_URL + "/j_security_check", HttpServletResponse.SC_FORBIDDEN, params, headers, null);
        assertXReason(post);
    }

    public void testValidatingIncorrectHttpBasicCredentials() throws Exception {

        // assume http and webdav are on the same host + port
        URL url = new URL(HTTP_BASE_URL);
        Credentials defaultcreds = new UsernamePasswordCredentials("garbage", "garbage");
        httpClient.getState().setCredentials(new AuthScope(url.getHost(), url.getPort(), AuthScope.ANY_REALM), defaultcreds);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair("j_validate", "true"));
        HttpMethod post = assertPostStatus(HTTP_BASE_URL + "/j_security_check", HttpServletResponse.SC_FORBIDDEN, params, null);
        assertXReason(post);

        HttpMethod get = assertHttpStatus(HTTP_BASE_URL + "?j_validate=true", HttpServletResponse.SC_FORBIDDEN);
        assertXReason(get);
    }

    public void testPreventLoopIncorrectHttpBasicCredentials() throws Exception {

        // assume http and webdav are on the same host + port
        URL url = new URL(HTTP_BASE_URL);
        Credentials defaultcreds = new UsernamePasswordCredentials("garbage", "garbage");
        httpClient.getState().setCredentials(new AuthScope(url.getHost(), url.getPort(), AuthScope.ANY_REALM), defaultcreds);

        final String requestUrl = HTTP_BASE_URL + "/junk?param1=1";
        HttpMethod get = new GetMethod(requestUrl);
        get.setRequestHeader("Referer", requestUrl);
        get.setRequestHeader("Accept", "text/*"); // simulate a browser request
        int status = httpClient.executeMethod(get);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, status);
    }

    public void testXRequestedWithIncorrectCredentials() throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair("j_username", "garbage"));
        params.add(new NameValuePair("j_password", "garbage"));

        List<Header> headers = new ArrayList<Header>();
        headers.add(new Header("X-Requested-With", "XMLHttpRequest"));
        headers.add(new Header("Accept", "text/html"));

        HttpMethod post = assertPostStatus(HTTP_BASE_URL + "/j_security_check", HttpServletResponse.SC_FORBIDDEN,
                params, headers, null);
        assertNotNull(post.getResponseHeader("X-Reason"));
        assertEquals("Username and Password do not match", post.getResponseHeader("X-Reason").getValue());
    }

    // this method assumes the use of the selector auth bundle
    public void testWithAcceptHeaderIncorrectCredentials() throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair("j_username", "garbage"));
        params.add(new NameValuePair("j_password", "garbage"));

        List<Header> headers = new ArrayList<Header>();
        headers.add(new Header("Accept", "text/html"));

        HttpMethod post = assertPostStatus(HTTP_BASE_URL + "/j_security_check",
                HttpServletResponse.SC_MOVED_TEMPORARILY, params, headers, null);

        final String location = post.getResponseHeader("Location").getValue();
        assertNotNull(location);
        assertTrue(location.startsWith(HTTP_BASE_URL + "/system/sling/selector/login?"));
        assertTrue(location.contains("resource=%2F"));
        assertTrue(location.contains("j_reason=INVALID_CREDENTIALS"));
    }

    public void testWithoutAcceptHeaderIncorrectCredentials() throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair("j_username", "garbage"));
        params.add(new NameValuePair("j_password", "garbage"));

        assertPostStatus(HTTP_BASE_URL + "/j_security_check", HttpServletResponse.SC_UNAUTHORIZED, params, null);
    }

    public void testWithNonHtmlAcceptHeaderIncorrectCredentials() throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair("j_username", "garbage"));
        params.add(new NameValuePair("j_password", "garbage"));

        List<Header> headers = new ArrayList<Header>();
        headers.add(new Header("Accept", "application/xml"));

        assertPostStatus(HTTP_BASE_URL + "/j_security_check", HttpServletResponse.SC_UNAUTHORIZED, params, null);
    }

    // TODO - move this method into commons.testing
    protected HttpMethod assertPostStatus(String url, int expectedStatusCode, List<NameValuePair> postParams,
            List<Header> headers, String assertMessage) throws IOException {
        final PostMethod post = new PostMethod(url);
        post.setFollowRedirects(false);

        if (headers != null) {
            for (Header header : headers) {
                post.addRequestHeader(header);
            }
        }

        if (postParams != null) {
            final NameValuePair[] nvp = {};
            post.setRequestBody(postParams.toArray(nvp));
        }

        final int status = httpClient.executeMethod(post);
        if (assertMessage == null) {
            assertEquals(expectedStatusCode, status);
        } else {
            assertEquals(assertMessage, expectedStatusCode, status);
        }
        return post;
    }

    private void assertXReason(final HttpMethod method) throws IOException {
        // expected the X-Reason header
        final Header reason = method.getResponseHeader("X-Reason");
        assertNotNull(reason);

        // expect the response to be the same as the reason (SLING-1831)
        assertEquals(reason.getValue(), method.getResponseBodyAsString().trim());
    }
}
