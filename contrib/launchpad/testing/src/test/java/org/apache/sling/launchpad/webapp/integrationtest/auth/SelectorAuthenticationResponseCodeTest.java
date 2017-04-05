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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.commons.testing.integration.HttpTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test of the response code from an authentication request depending on various
 * request parameters and headers including j_validate, Ajax detection, and
 * non-browser client detection.
 */
// SLING-2760 auth/selector not deployed to avoid missing artifact dependency on http://dyuproject.googlecode.com/svn/repos/maven2
@Ignore
public class SelectorAuthenticationResponseCodeTest {
    
    private final HttpTest H = new HttpTest(); 

    @Before
    public void setup() throws Exception {
        H.setUp();
    }
    
    @After
    public void cleanup() throws Exception {
        H.tearDown();
    }

    // this method assumes the use of the selector auth bundle
    @Test 
    public void testWithAcceptHeaderIncorrectCredentials() throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair("j_username", "garbage"));
        params.add(new NameValuePair("j_password", "garbage"));

        // simulate a browser request
        List<Header> headers = new ArrayList<Header>();
        headers.add(new Header("User-Agent", "Mozilla/5.0 Sling Integration Test"));

        HttpMethod post = assertPostStatus(HttpTest.HTTP_BASE_URL + "/j_security_check",
                HttpServletResponse.SC_MOVED_TEMPORARILY, params, headers, null);

        final String location = post.getResponseHeader("Location").getValue();
        assertNotNull(location);
        assertTrue(location.startsWith(HttpTest.HTTP_BASE_URL + "/system/sling/selector/login?"));
        assertTrue(location.contains("resource=%2F"));
        assertTrue(location.contains("j_reason=INVALID_CREDENTIALS"));
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

        final int status = H.getHttpClient().executeMethod(post);
        if (assertMessage == null) {
            assertEquals(expectedStatusCode, status);
        } else {
            assertEquals(assertMessage, expectedStatusCode, status);
        }
        return post;
    }
}
