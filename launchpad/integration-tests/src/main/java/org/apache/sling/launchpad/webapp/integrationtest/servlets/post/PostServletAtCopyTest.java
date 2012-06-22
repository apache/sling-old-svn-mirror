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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Test item copy support by @CopyFrom suffix (SLING-455) */
public class PostServletAtCopyTest extends HttpTestBase {

    public static final String TEST_BASE_PATH = "/sling-at-copy-tests";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testCopyNodeAbsolute() throws IOException {
        final String testPath = TEST_BASE_PATH + "/abs/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // assert content at source location
        final String oldContent = getContent(HTTP_BASE_URL + testPath + "/src.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", oldContent, "out.println(data.text)");

        // create dest with text set from src/text
        props.clear();
        props.put("src@CopyFrom", testPath + "/src");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // assert content at new location
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.src.text)");

        // assert content at old location
        String contentOld = getContent(HTTP_BASE_URL + testPath + "/src.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", contentOld, "out.println(data.text)");
    }

    public void testCopyNodeRelative() throws IOException {
        final String testPath = TEST_BASE_PATH + "/rel/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // assert content at source location
        final String oldContent = getContent(HTTP_BASE_URL + testPath + "/src.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", oldContent, "out.println(data.text)");

        // create dest with text set from src/text
        props.clear();
        props.put("src@CopyFrom", "../src");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // assert content at new location
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.src.text)");

        // assert content at old location
        String contentOld = getContent(HTTP_BASE_URL + testPath + "/src.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", contentOld, "out.println(data.text)");
    }

    public void testCopyPropertyAbsolute() throws IOException {
        final String testPath = TEST_BASE_PATH + "/abs/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // assert content at source location
        final String oldContent = getContent(HTTP_BASE_URL + testPath + "/src.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", oldContent, "out.println(data.text)");

        // create dest with text set from src/text
        props.clear();
        props.put("text@CopyFrom", testPath + "/src/text");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // assert content at new location
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");

        // assert content at old location
        String contentOld = getContent(HTTP_BASE_URL + testPath + "/src.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", contentOld, "out.println(data.text)");
    }

    public void testCopyPropertyRelative() throws IOException {
        final String testPath = TEST_BASE_PATH + "/rel/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // assert content at source location
        final String oldContent = getContent(HTTP_BASE_URL + testPath + "/src.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", oldContent, "out.println(data.text)");

        // create dest with text set from src/text
        props.clear();
        props.put("text@CopyFrom", "../src/text");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // assert content at new location
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");

        // assert content at old location
        String contentOld = getContent(HTTP_BASE_URL + testPath + "/src.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", contentOld, "out.println(data.text)");
    }

    public void testCopyNodeSourceMissing() throws IOException {
        final String testPath = TEST_BASE_PATH + "/exist/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();

        // create dest node
        props.clear();
        props.put("text", "Hello Destination");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest/src", props);

        props.clear();
        props.put("src@CopyFrom", testPath + "/non_existing_source");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // expect unmodified dest
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello Destination", content, "out.println(data.src.text)");
    }

    public void testCopyNodeExistingReplace() throws IOException {
        final String testPath = TEST_BASE_PATH + "/replace/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // create dest node
        props.clear();
        props.put("text", "Hello Destination");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest/src", props);

        props.clear();
        props.put("src@CopyFrom", "../src");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // expect unmodified dest
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.src.text)");
    }

    public void testCopyNodeDeepRelative() throws IOException {
        final String testPath = TEST_BASE_PATH + "/new/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put("deep/new@CopyFrom", "../../src");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // expect new data
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.deep['new'].text)");
    }

    public void testCopyNodeDeepAbsolute() throws IOException {
        final String testPath = TEST_BASE_PATH + "/new_fail/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put(testPath + "/some/not/existing/structure@CopyFrom", testPath + "/src");
        testClient.createNode(HTTP_BASE_URL + testPath + "/*", props);

        // expect new data
        String content = getContent(HTTP_BASE_URL + testPath + "/some/not/existing/structure.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }
    
    /** Copying siblings should work */
    public void testCopySibling() throws IOException {
        final String testPath = TEST_BASE_PATH + "/AT_sibling/" + System.currentTimeMillis();
        final Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello ATcsp");
        testClient.createNode(HTTP_BASE_URL + testPath + "/a/1", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/a/12", props);
    
        final List<NameValuePair> opt = new ArrayList<NameValuePair>();
        opt.add(new NameValuePair(testPath + "/a/12/13@CopyFrom", testPath + "/a/1"));
        
        final int expectedStatus = HttpServletResponse.SC_OK;
        assertPostStatus(HTTP_BASE_URL + testPath, expectedStatus, opt, "Expecting status " + expectedStatus);
        
        // assert content at old and new locations
        for(String path : new String[] { "/a/1", "/a/12", "/a/12/13" }) {
            final String content = getContent(HTTP_BASE_URL + testPath + path + ".json", CONTENT_TYPE_JSON);
            assertJavascript("Hello ATcsp", content, "out.println(data.text)", "at path " + path);
        }
    }
    
    public void testCopyAncestor() throws IOException {
        final String testPath = TEST_BASE_PATH + "/AT_tcanc/" + System.currentTimeMillis();
        
        final List<NameValuePair> opt = new ArrayList<NameValuePair>();
        opt.add(new NameValuePair(testPath + "./@CopyFrom", "../"));
        
        final int expectedStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        assertPostStatus(HTTP_BASE_URL + testPath, expectedStatus, opt, "Expecting status " + expectedStatus);
    }
 }