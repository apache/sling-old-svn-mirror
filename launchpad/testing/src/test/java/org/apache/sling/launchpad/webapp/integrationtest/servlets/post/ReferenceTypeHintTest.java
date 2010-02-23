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

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;

/**
 * Integration test of reference type hints in the post servlet.
 */
public class ReferenceTypeHintTest extends HttpTestBase {
    public static final String TEST_BASE_PATH = "/sling-tests";
    private String postUrl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
    }

    public void testReferenceTypes() throws Exception {
        final Map<String, String> props = new HashMap<String, String>();
        props.put("a", "");
        props.put("jcr:mixinTypes", "mix:referenceable");

        final String firstCreatedNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        final String firstUuid = getProperty(firstCreatedNodeUrl, "jcr:uuid");
        final String firstPath = getPath(firstCreatedNodeUrl);

        final String secondCreatedNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        final String secondUuid = getProperty(firstCreatedNodeUrl, "jcr:uuid");
        final String secondPath = getPath(firstCreatedNodeUrl);

        props.clear();
        props.put("a", firstPath);
        props.put("a@TypeHint", "Reference");
        props.put("b", firstPath);
        props.put("b@TypeHint", "WeakReference");
        props.put("as", firstPath);
        props.put("as@TypeHint", "Reference");
        props.put("bs", firstPath);
        props.put("bs@TypeHint", "WeakReference");
        final String referencingNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX,
                props);

        String refCreatedValue = getProperty(referencingNodeUrl, "a");
        String weakrefCreatedValue = getProperty(referencingNodeUrl, "b");

        assertEquals(firstUuid, refCreatedValue);
        assertEquals(firstUuid, weakrefCreatedValue);
    }

    private String getPath(String url) {
        return url.substring(HTTP_BASE_URL.length());
    }

    private String getProperty(String url, String name) throws Exception {
        return getContent(url + "/" + name + ".txt", CONTENT_TYPE_PLAIN);
    }
}
