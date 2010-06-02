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
package org.apache.sling.launchpad.webapp.integrationtest.servlets.resolution;

import org.apache.commons.httpclient.methods.PostMethod;

/** Test the RequestUriOptingServlet resolution */
public class RequestUriOptingServletTest extends ResolutionTestBase {
    public static final String TEST_URL_SUFFIX = ".RequestUriOptingServlet.html";
    
    public void testPostMethodExistingResource() throws Exception {
        final PostMethod post = new PostMethod(testNodeNORT.nodeUrl + TEST_URL_SUFFIX);
        final int status = httpClient.executeMethod(post);
        assertEquals("PUT should return 200", 200, status);
        final String content = post.getResponseBodyAsString();
        assertServlet(content, REQUEST_URI_OPTING_SERVLET_SUFFIX);
    }
    
    public void testPuttMethodNonExistingResource() throws Exception {
        final PostMethod post = new PostMethod(NONEXISTING_RESOURCE_URL + TEST_URL_SUFFIX);
        final int status = httpClient.executeMethod(post);
        assertEquals("PUT should return 200", 200, status);
        final String content = post.getResponseBodyAsString();
        assertServlet(content, REQUEST_URI_OPTING_SERVLET_SUFFIX);
    }
    
    public void testGetMethodExistingResource() throws Exception {
        assertServlet(
                getContent(testNodeNORT.nodeUrl + TEST_URL_SUFFIX, CONTENT_TYPE_PLAIN),
                REQUEST_URI_OPTING_SERVLET_SUFFIX);
    }
    
    public void testGetMethodNonExistingResource() throws Exception {
        assertServlet(
                getContent(NONEXISTING_RESOURCE_URL + TEST_URL_SUFFIX, CONTENT_TYPE_PLAIN),
                REQUEST_URI_OPTING_SERVLET_SUFFIX);
    }
}