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
package org.apache.sling.launchpad.webapp.integrationtest.servlets.post;

import java.io.IOException;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;

/** Test the various status options for POST, SLING-422 */
public class PostStatusTest extends HttpTestBase {

    private String postPath = "PostStatusTest/" + System.currentTimeMillis();

    public void testStandardStatusNull() throws IOException {
        final String resPath = "/" + postPath + "/StandardNull";
        final String postUrl = HTTP_BASE_URL + resPath;
        simplePost(postUrl, null, 201, resPath);
    }
    
    public void testStandardStatusStandard() throws IOException {
        final String resPath = "/" + postPath + "/StandardStandard";
        final String postUrl = HTTP_BASE_URL + resPath;
        simplePost(postUrl, SlingPostConstants.STATUS_VALUE_STANDARD, 201, resPath);
    }
    
    public void testStandardStatusUnknown() throws IOException {
        final String resPath = "/" + postPath + "/StandardUnknown";
        final String postUrl = HTTP_BASE_URL + resPath;
        simplePost(postUrl, "Unknown Value", 201, resPath);
    }
    
    public void testStatusBrowser() throws IOException {
        final String resPath = "/" + postPath + "/Browser";
        final String postUrl = HTTP_BASE_URL + resPath;
        simplePost(postUrl, SlingPostConstants.STATUS_VALUE_BROWSER, 200, null);
    }

    private void simplePost(String url, String statusParam, int expectStatus,
            String expectLocation) throws IOException {

        final PostMethod post = new PostMethod(url);
        post.setFollowRedirects(false);

        if (statusParam != null) {
            post.addParameter(SlingPostConstants.RP_STATUS, statusParam);
        }

        final int status = httpClient.executeMethod(post);
        assertEquals("Unexpected status response", expectStatus, status);

        if (expectLocation != null) {
            String location = post.getResponseHeader("Location").getValue();

            assertNotNull("Expected location header", location);
            assertTrue(location.endsWith(expectLocation));
        }

        post.releaseConnection();
    }

}