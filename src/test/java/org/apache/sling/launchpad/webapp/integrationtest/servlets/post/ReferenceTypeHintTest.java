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

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.commons.testing.integration.NameValuePairList;
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
        final NameValuePairList props = new NameValuePairList();
        props.add("a", "");
        props.add("jcr:mixinTypes", "mix:referenceable");

        final String firstCreatedNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props, null, false);
        final String firstUuid = getProperty(firstCreatedNodeUrl, "jcr:uuid");
        final String firstPath = getPath(firstCreatedNodeUrl);

        final String secondCreatedNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props, null, false);
        final String secondUuid = getProperty(secondCreatedNodeUrl, "jcr:uuid");
        final String secondPath = getPath(secondCreatedNodeUrl);

        props.clear();
        props.add("r", firstPath);
        props.add("r@TypeHint", "Reference");
        props.add("w", firstPath);
        props.add("w@TypeHint", "WeakReference");
        props.add("rs", firstPath);
        props.add("rs", secondPath);
        props.add("rs@TypeHint", "Reference");
        props.add("ws", firstPath);
        props.add("ws", secondPath);
        props.add("ws@TypeHint", "WeakReference");
        final String referencingNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX,
                props, null, false);

        String refCreatedValue = getProperty(referencingNodeUrl, "r");
        String weakrefCreatedValue = getProperty(referencingNodeUrl, "w");

        String[] refCreatedValues = getPropertyArray(referencingNodeUrl, "rs");
        String[] weakrefCreatedValues = getPropertyArray(referencingNodeUrl, "ws");

        assertEquals(firstUuid, refCreatedValue);
        assertEquals(firstUuid, weakrefCreatedValue);
        assertEquals(firstUuid, refCreatedValues[0]);
        assertEquals(firstUuid, weakrefCreatedValues[0]);
        assertEquals(secondUuid, refCreatedValues[1]);
        assertEquals(secondUuid, weakrefCreatedValues[1]);
    }

    private String getPath(String url) {
        return url.substring(HTTP_BASE_URL.length());
    }

    private String getProperty(String url, String name) throws Exception {
        return getContent(url + "/" + name + ".txt", CONTENT_TYPE_PLAIN);
    }

    private String[] getPropertyArray(String url, String name) throws Exception {
        JSONObject jo = new JSONObject(getContent(url + "/" + name + ".json", CONTENT_TYPE_JSON));
        JSONArray arr = jo.getJSONArray(name);
        String[] result = new String[arr.length()];
        for (int i = 0; i < arr.length(); i++) {
            result[i] = arr.getString(i);
        }
        return result;
    }
}
