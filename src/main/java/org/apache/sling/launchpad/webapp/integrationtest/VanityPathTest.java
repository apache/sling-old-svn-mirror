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
package org.apache.sling.launchpad.webapp.integrationtest;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.launchpad.webapp.integrationtest.util.EventsCounterUtil;
import org.apache.sling.servlets.post.SlingPostConstants;

/**
 * Tests of the sling:VanityPath mixin support.
 *
 */
public class VanityPathTest extends HttpTestBase {
    private String postUrl;
    private String vanityPath;
    private String vanityUrl;
    private int mappingEventCount;
    public static final String MAPPING_UPDATE_TOPIC = "org/apache/sling/api/resource/ResourceResolverMapping/CHANGED";

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create the test node, under a path that's specific to this class to
        // allow collisions
        postUrl = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "_"
            + System.currentTimeMillis()
            + SlingPostConstants.DEFAULT_CREATE_SUFFIX;
        vanityPath = "/" + getClass().getSimpleName() + "_" + System.currentTimeMillis() + "/vanity";
        vanityUrl = HTTP_BASE_URL + vanityPath;

        resetMappingEventCount();
    }

    private void resetMappingEventCount() throws JSONException, IOException {
        mappingEventCount = EventsCounterUtil.getEventsCount(this, MAPPING_UPDATE_TOPIC);
    }

    /** test vanity path with internal redirect */
    public void testInternalRedirect() throws IOException {
        // create a node with a vanity path
        Map<String, String> props = new HashMap<String, String>();
        props.put("jcr:mixinTypes", "sling:VanityPath");
        props.put("sling:vanityPath", vanityPath);
        String createdNodeUrl = testClient.createNode(postUrl, props);
        String createdPath = createdNodeUrl.substring(HTTP_BASE_URL.length());

        waitForMapReload();

        // get the created node without following redirects
        GetMethod get = new GetMethod(vanityUrl);
        get.setFollowRedirects(false);
        int status = httpClient.executeMethod(get);

        // expect a 200, not a redirect
        assertEquals(200, status);

        assertTrue(get.getResponseBodyAsString().contains(createdPath));
    }

    /** test vanity path with redirect */
    public void test302Redirect() throws IOException {
        // create a node with a vanity path
        Map<String, String> props = new HashMap<String, String>();
        props.put("jcr:mixinTypes", "sling:VanityPath");
        props.put("sling:vanityPath", vanityPath);
        props.put("sling:redirect", "true");
        String createdNodeUrl = testClient.createNode(postUrl, props);

        waitForMapReload();

        // get the created node's vanity path without following redirects
        GetMethod get = new GetMethod(vanityUrl);
        get.setFollowRedirects(false);
        int status = httpClient.executeMethod(get);

        // expect temporary redirect ...
        assertEquals(302, status);

        // ... to the created node
        String location = get.getResponseHeader("Location").getValue();
        assertNotNull(location);
        assertEquals(removeHttpBase(createdNodeUrl) + ".html", location);
    }

    /** test vanity path with 301 redirect */
    public void test301Redirect() throws IOException {
        // create a node with a vanity path
        Map<String, String> props = new HashMap<String, String>();
        props.put("jcr:mixinTypes", "sling:VanityPath");
        props.put("sling:vanityPath", vanityPath);
        props.put("sling:redirect", "true");
        props.put("sling:redirectStatus", "301");
        String createdNodeUrl = testClient.createNode(postUrl, props);

        waitForMapReload();

        // get the created node without following redirects
        GetMethod get = new GetMethod(vanityUrl);
        get.setFollowRedirects(false);
        int status = httpClient.executeMethod(get);

        // expect permanent redirect ...
        assertEquals(301, status);

        // ... to the created node
        String location = get.getResponseHeader("Location").getValue();
        assertNotNull(location);
        assertEquals(removeHttpBase(createdNodeUrl) + ".html", location);
    }

    /** test vanity path with redirect using a non-html extension and a selector */
    public void testRedirectKeepingExtensionAndSelector() throws IOException {
        // create a node with a vanity path
        Map<String, String> props = new HashMap<String, String>();
        props.put("jcr:mixinTypes", "sling:VanityPath");
        props.put("sling:vanityPath", vanityPath);
        props.put("sling:redirect", "true");
        String createdNodeUrl = testClient.createNode(postUrl, props);

        waitForMapReload();

        // get the created node's vanity path without following redirects
        GetMethod get = new GetMethod(vanityUrl + ".test.json");
        get.setFollowRedirects(false);
        int status = httpClient.executeMethod(get);

        // expect temporary redirect ...
        assertEquals(302, status);

        // ... to the created node
        String location = get.getResponseHeader("Location").getValue();
        assertNotNull(location);
        assertEquals(removeHttpBase(createdNodeUrl) + ".test.json", location);
    }

    /** test vanity path on a path with an extension with a redirect */
    public void testRedirectOnPathWithExtension() throws IOException, JSONException {
        // create a node with a vanity path
        Map<String, String> props = new HashMap<String, String>();
        props.put("jcr:mixinTypes", "sling:VanityPath");
        props.put("sling:vanityPath", vanityPath);
        props.put("sling:redirect", "true");
        String createdNodeUrl = testClient.createNode(postUrl, props);

        waitForMapReload();
        resetMappingEventCount();

        String pathWithExtension = removeHttpBase(createdNodeUrl) + ".ext";
        List<NameValuePair> moveParams = Arrays.asList(
                new NameValuePair(":dest", pathWithExtension),
                new NameValuePair(":operation", "move"));
        assertPostStatus(createdNodeUrl, 201, moveParams, "Could not move created node from " + createdNodeUrl);
        createdNodeUrl = createdNodeUrl + ".ext";

        waitForMapReload();

        // get the created node's vanity path without following redirects
        GetMethod get = new GetMethod(vanityUrl);
        get.setFollowRedirects(false);
        int status = httpClient.executeMethod(get);

        assertEquals("Expecting temporary redirect", 302, status);

        // ... to the created node
        String location = get.getResponseHeader("Location").getValue();
        assertNotNull("Expecting non-null Location", location);
        assertEquals("Expecting redirect to the specified extension", pathWithExtension, location);
    }

    private String removeHttpBase(String url) {
        return url.startsWith(HTTP_BASE_URL) ? url.substring(HTTP_BASE_URL.length()) : url;
    }

    /**
     * Wait for a MAPPING_UPDATE_TOPIC events, assuming mappingEventCount was reset
     * before making the corresponding content changes.
     */
    private void waitForMapReload() {
        EventsCounterUtil.waitForEvent(this, MAPPING_UPDATE_TOPIC, 5, mappingEventCount);
    }
}
