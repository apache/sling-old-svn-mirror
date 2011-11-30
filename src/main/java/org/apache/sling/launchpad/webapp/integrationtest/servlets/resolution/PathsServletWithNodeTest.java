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
package org.apache.sling.launchpad.webapp.integrationtest.servlets.resolution;

import java.util.Collections;

import org.apache.commons.httpclient.NameValuePair;

/**
 * Test that a servlet registered at a particular path is still executed when a
 * node at that path also exists.
 */
public class PathsServletWithNodeTest extends ResolutionTestBase {

    private final static String NODE_SERVLET_URL = HTTP_BASE_URL + "/testing/PathsServletNodeServlet";

    private final static String TEST_URL = HTTP_BASE_URL + "/testing/PathsServlet/foo";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertPostStatus(NODE_SERVLET_URL, 201, Collections.singletonList(new NameValuePair("action", "create")),
                "Unable to create node at " + TEST_PATH);
    }

    public void testGetCorrectPath() throws Exception {
        assertServlet(getContent(TEST_URL, CONTENT_TYPE_PLAIN), PATHS_SERVLET_SUFFIX);
    }

    @Override
    protected void tearDown() throws Exception {
        assertPostStatus(NODE_SERVLET_URL, 204, Collections.singletonList(new NameValuePair("action", "delete")),
                "Unable to delete node at " + TEST_PATH);
        super.tearDown();
    }

}
