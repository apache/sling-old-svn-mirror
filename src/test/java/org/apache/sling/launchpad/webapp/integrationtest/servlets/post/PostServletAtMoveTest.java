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

import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Test item move support by @MoveFrom suffix (SLING-455) */
public class PostServletAtMoveTest extends HttpTestBase {

    public static final String TEST_BASE_PATH = "/sling-at-move-tests";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testMoveNodeAbsolute() throws IOException {
        final String testPath = TEST_BASE_PATH + "/abs/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // assert content at source location
        final String oldContent = getContent(HTTP_BASE_URL + testPath + "/src.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", oldContent, "out.println(data.text)");

        // create dest with text set from src/text
        props.clear();
        props.put("src@MoveFrom", testPath + "/src");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // assert content at new location
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.src.text)");

        // assert no content at old location
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src.json",
            HttpServletResponse.SC_NOT_FOUND,
        "Expected Not_Found for old content");
    }

    public void testMoveNodeRelative() throws IOException {
        final String testPath = TEST_BASE_PATH + "/rel/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // assert content at source location
        final String oldContent = getContent(HTTP_BASE_URL + testPath + "/src.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", oldContent, "out.println(data.text)");

        // create dest with text set from src/text
        props.clear();
        props.put("src@MoveFrom", "../src");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // assert content at new location
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.src.text)");

        // assert no content at old location
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src.json",
            HttpServletResponse.SC_NOT_FOUND,
        "Expected Not_Found for old content");
    }

    public void testMovePropertyAbsolute() throws IOException {
        final String testPath = TEST_BASE_PATH + "/abs/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // assert content at source location
        final String oldContent = getContent(HTTP_BASE_URL + testPath + "/src.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", oldContent, "out.println(data.text)");

        // create dest with text set from src/text
        props.clear();
        props.put("text@MoveFrom", testPath + "/src/text");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // assert content at new location
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");

        // assert no content at old location
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src.json",
            HttpServletResponse.SC_OK, "Expected source parent existing");
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src/text.json",
            HttpServletResponse.SC_NOT_FOUND,
            "Expected Not_Found for old content");
    }

    public void testMovePropertyRelative() throws IOException {
        final String testPath = TEST_BASE_PATH + "/rel/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // assert content at source location
        final String oldContent = getContent(HTTP_BASE_URL + testPath + "/src.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", oldContent, "out.println(data.text)");

        // create dest with text set from src/text
        props.clear();
        props.put("text@MoveFrom", "../src/text");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // assert content at new location
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");

        // assert no content at old location
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src.json",
            HttpServletResponse.SC_OK, "Expected source parent existing");
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src/text.json",
            HttpServletResponse.SC_NOT_FOUND,
            "Expected Not_Found for old content");
    }

    public void testMoveNodeSourceMissing() throws IOException {
        final String testPath = TEST_BASE_PATH + "/exist/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();

        // create dest node
        props.clear();
        props.put("text", "Hello Destination");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest/src", props);

        props.clear();
        props.put("src@MoveFrom", testPath + "/non_existing_source");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // expect unmodified dest
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello Destination", content, "out.println(data.src.text)");
    }

    public void testMoveNodeExistingReplace() throws IOException {
        final String testPath = TEST_BASE_PATH + "/replace/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // create dest node
        props.clear();
        props.put("text", "Hello Destination");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest/src", props);

        props.clear();
        props.put("src@MoveFrom", "../src");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // expect unmodified dest
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.src.text)");
    }

    public void testMoveNodeDeepRelative() throws IOException {
        final String testPath = TEST_BASE_PATH + "/new/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put("deep/new@MoveFrom", "../../src");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // expect new data
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.deep['new'].text)");
    }

    public void testMoveNodeDeepAbsolute() throws IOException {
        final String testPath = TEST_BASE_PATH + "/new_fail/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put(testPath + "/some/not/existing/structure@MoveFrom", testPath + "/src");
        testClient.createNode(HTTP_BASE_URL + testPath + "/*", props);

        // expect new data
        String content = getContent(HTTP_BASE_URL + testPath + "/some/not/existing/structure.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }

 }