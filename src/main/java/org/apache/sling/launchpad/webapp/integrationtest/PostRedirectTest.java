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
package org.apache.sling.launchpad.webapp.integrationtest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;

/** Test the various redirect options for POST, SLING-126 */
public class PostRedirectTest extends HttpTestBase {

    private String postPath = "CreateNodeTest/" + System.currentTimeMillis();

    private String postUrl = HTTP_BASE_URL + "/" + postPath
        + SlingPostConstants.DEFAULT_CREATE_SUFFIX;

    public void testEncodedRedirect() throws IOException {
        final Map<String, String> params = new HashMap<String, String>();
        params.put(":redirect", "*");
        params.put(":name", "\u0414\u0440\u0443\u0433\u0430");
        final Map<String, String> headers = new HashMap<String, String>();
        headers.put("Referer", "http://referer/");

        final String location = testClient.createNode(postUrl, params, headers,
            false);
        assertTrue("With UTF-8 in path, redirect must be encoded :"+location,
            location.contains(postPath + "/%D0%94%D1%80%D1%83%D0%B3%D0%B0"));
    }

    public void testForcedRedirect() throws IOException {
        final Map<String, String> params = new HashMap<String, String>();
        params.put(":redirect", "http://forced");
        final Map<String, String> headers = new HashMap<String, String>();
        headers.put("Referer", "http://referer/");

        final String location = testClient.createNode(postUrl, params, headers,
            false);
        assertEquals(
            "With forced redirect and Referer, redirect must be forced",
            "http://forced", location);
    }

    public void testDefaultRedirect() throws IOException {
        final Map<String, String> params = new HashMap<String, String>();
        params.put(":redirect", null);
        final String location = testClient.createNode(postUrl, null);
        assertTrue("With no headers or parameters, redirect (" + location
            + ") must point to created node (path=" + postPath + ")",
            location.contains(postPath));
    }

    public void testMagicStarRedirect() throws IOException {
        final Map<String, String> params = new HashMap<String, String>();
        params.put(":redirect", "*");
        final Map<String, String> headers = new HashMap<String, String>();
        headers.put("Referer", "http://referer/");
        final String location = testClient.createNode(postUrl, params, headers,
            false);
        assertTrue("With magic star, redirect (" + location
            + ") must point to created node (path=" + postPath + ")",
            location.contains(postPath));
    }

    public void testMagicStarRedirectPrefix() throws IOException {
        String prefix = "xyz/";
        final Map<String, String> params = new HashMap<String, String>();
        params.put(":redirect", prefix + "*");
        final Map<String, String> headers = new HashMap<String, String>();
        headers.put("Referer", "http://referer/");
        final String location = testClient.createNode(postUrl, params, headers,
            false);
        assertTrue("With magic star, redirect (" + location
            + ") must start with prefix " + prefix, location.contains(prefix));
    }

    public void testMagicStarRedirectSuffix() throws IOException {
        String suffix = "/xyz.html";
        final Map<String, String> params = new HashMap<String, String>();
        params.put(":redirect", "*" + suffix);
        final Map<String, String> headers = new HashMap<String, String>();
        headers.put("Referer", "http://referer/");
        final String location = testClient.createNode(postUrl, params, headers,
            false);
        assertTrue("With magic star, redirect (" + location
            + ") must end with suffix " + suffix, location.endsWith(suffix));
    }

    public void testMagicStarRedirectPrefixSuffix() throws IOException {
        String prefix = "xyz/";
        String suffix = "/xyz.html";
        final Map<String, String> params = new HashMap<String, String>();
        params.put(":redirect", prefix + "*" + suffix);
        final Map<String, String> headers = new HashMap<String, String>();
        headers.put("Referer", "http://referer/");
        final String location = testClient.createNode(postUrl, params, headers,
            false);
        assertTrue("With magic star, redirect (" + location
            + ") must start with prefix " + prefix + " and end with suffix "
            + suffix, location.contains(prefix) && location.endsWith(suffix));
    }
}