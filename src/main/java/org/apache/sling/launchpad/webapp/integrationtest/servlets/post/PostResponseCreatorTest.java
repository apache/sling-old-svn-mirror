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
package org.apache.sling.launchpad.webapp.integrationtest.servlets.post;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;

public class PostResponseCreatorTest extends HttpTestBase {
    public static final String TEST_BASE_PATH = "/sling-tests";
    private String postUrl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
    }

    public void testCustomPostResponseCreator() throws Exception {
        final PostMethod post = new PostMethod(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX);
        post.addParameter(":responseType", "custom");
        
        post.setFollowRedirects(false);

        final int status = httpClient.executeMethod(post);
        assertEquals("Unexpected status response", 201, status);

        assertEquals("Thanks!", post.getResponseBodyAsString());

        post.releaseConnection();
    }
}
