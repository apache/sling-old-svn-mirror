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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.launchpad.webapp.integrationtest.HttpTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;

/** Test node creation via the MicrojaxPostServlet */
public class PostServletCreateTest extends HttpTestBase {
    public static final String TEST_BASE_PATH = "/sling-tests";
    private String postUrl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
    }

   public void testPostPathIsUnique() throws IOException {
        assertHttpStatus(postUrl, HttpServletResponse.SC_NOT_FOUND,
                "Path must not exist before test: " + postUrl);
    }

    public void testCreateNode() throws IOException {
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        assertHttpStatus(location, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));
    }

    public void testCreateNodeWithExtension() throws IOException {
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX + ".html", null);
        assertHttpStatus(location, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));
    }

    public void testCreateNodeAtSpecificUrl() throws IOException {
        final String specifiedLocation = postUrl + "/specified-location";
        final String location = testClient.createNode(specifiedLocation, null);
        assertHttpStatus(location, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must be created at given URL (" + specifiedLocation + ")",
                location.equals(specifiedLocation));
    }

    public void testCreateNodeAtDeepUrl() throws IOException {
        final long id = System.currentTimeMillis();
        final String specifiedLocation = postUrl + "/specified-location" + id + "/deepA/deepB/" + id;
        final String location = testClient.createNode(specifiedLocation, null);
        assertHttpStatus(location, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must be created (deep) at given URL (" + specifiedLocation + ")",
                location.equals(specifiedLocation));
    }

    /** Create a node with some data, and check that data */
    public void testCreateWithData() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("a","123");
        props.put("b","456");
        props.put("c","some words");
        final String createdNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        final String content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);
        assertJavascript("123456", content, "out.println(data.a + data.b)");
        assertJavascript("some words", content, "out.println(data.c)");
    }

    /** Create a node with a propery in a subnode, and check (SLING-223) */
    public void testCreateSubnodeProperty() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("a","123");
        props.put("subnode/b","456");
        props.put("c","some words");
        final String createdNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        final String content = getContent(createdNodeUrl + ".2.json", CONTENT_TYPE_JSON);
        assertJavascript("123", content, "out.println(data.a)");
        assertJavascript("456", content, "out.println(data.subnode.b)");
        assertJavascript("some words", content, "out.println(data.c)");
    }

    /** Use the default "save prefix" on some parameters, and check that only those
     *  who have the prefix are saved.
     */
    public void testDefaultSavePrefix() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("./a","123");
        props.put("./b","456");
        props.put("c","not saved");
        final String createdNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        final String content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);
        assertJavascript("123456", content, "out.println(data.a + data.b)");
        assertJavascript("undefined", content, "out.println(typeof data.c)");
    }

    /** Use a custom "save prefix" on some parameters, and check that only those
     *  who have the prefix are saved.
     */
    public void testCustomSavePrefix() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("STUFF_a","123");
        props.put("STUFF_b","456");
        props.put("c","not saved");
        props.put(":saveParamPrefix","STUFF_");
        final String createdNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props,null,false);
        final String content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);
        assertJavascript("123456", content, "out.println(data.a + data.b)");
        assertJavascript("undefined", content, "out.println(typeof data.c)");
    }

    public void TODO_FAILS_testCustomSavePrefixPlusPlus() throws IOException {
        // for some reason, ++ as a custom save prefix fails
        // might indicate a weirdness in parameters processing
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("++a","123");
        props.put("++b","456");
        props.put("c","not saved");
        props.put(":saveParamPrefix","++");
        final String createdNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props,null,false);
        final String content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);
        assertJavascript("123456", content, "out.println(data.a + data.b)");
        assertJavascript("undefined", content, "out.println(typeof data.c)");
    }
 }