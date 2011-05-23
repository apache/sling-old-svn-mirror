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
package org.apache.sling.launchpad.webapp.integrationtest.issues;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.commons.testing.integration.HttpTestBase;

public class SLING2082Test extends HttpTestBase {
    
    private void runTest(HttpMethodBase m, int expectedStatus) throws Exception {
        m.setFollowRedirects(false);
        final int status = httpClient.executeMethod(m);
        assertEquals(expectedStatus, status);
        final String content = getResponseBodyAsStream(m, 0);
        final String scriptTag = "<script>";
        assertFalse("Content should not contain '" + scriptTag + "'", content.contains(scriptTag));
    }
    
    public void testPOST() throws Exception {
        final String path = "/" + getClass().getSimpleName() + "/" + Math.random() + ".html/%22%3e%3cscript%3ealert(29679)%3c/script%3e";
        final PostMethod post = new PostMethod(HTTP_BASE_URL + path);
        runTest(post, HttpServletResponse.SC_CREATED);
    }
    
    public void testOptingServletPost() throws Exception {
        final String path = "/testing/HtmlResponseServlet";
        final GetMethod post = new GetMethod(HTTP_BASE_URL + path);
        runTest(post, HttpServletResponse.SC_GATEWAY_TIMEOUT);
    }
}
