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
package org.apache.sling.launchpad.webapp.integrationtest.ujax;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import org.apache.sling.launchpad.webapp.integrationtest.HttpTestBase;

/** Test node move via the MicrojaxPostServlet */
public class PostServletMoveTest extends HttpTestBase {
    public static final String TEST_BASE_PATH = "/ujax-tests";
    private String testPath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testPath = TEST_BASE_PATH + "/" + System.currentTimeMillis();
    }

    public void XtestMoveNodeAbsolute() throws IOException {
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put("ujax:moveSrc", testPath + "/src");
        props.put("ujax:moveDest", testPath + "/dest");
        testClient.createNode(HTTP_BASE_URL + testPath, props);
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }

    public void XtestMoveNodeRelative() throws IOException {
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put("ujax:moveSrc", "src");
        props.put("ujax:moveDest", "dest");
        testClient.createNode(HTTP_BASE_URL + testPath, props);
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }

    public void XtestMoveNodeNew() throws IOException {
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put("ujax:moveSrc", testPath + "/src");
        props.put("ujax:moveDest", "new");
        // special case - need force creation of the new node 
        props.put("jcr:created", "");
        String newNode = testClient.createNode(HTTP_BASE_URL + testPath + "/*", props);
        String content = getContent(newNode + "/new.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }

    public void testMoveNodeExistingFail() throws IOException {
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // create dest node
        props.put("text", "Hello Destination");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        props.clear();
        props.put("ujax:moveSrc", testPath + "/src");
        props.put("ujax:moveDest", testPath + "/dest");
        try {
            testClient.createNode(HTTP_BASE_URL + testPath, props);
        } catch (IOException ioe) {
            // if we do not get the status code 200 message, fail
            if (!ioe.getMessage().startsWith("Expected status code 302 for POST, got 200, URL=")) {
                throw ioe;
            }
        }
        
        // expect unmodified dest
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello Destination", content, "out.println(data.text)");
    }

    public void testMoveNodeExistingReplace() throws IOException {
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // create dest node
        props.put("text", "Hello Destination");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        props.clear();
        props.put("ujax:moveSrc", testPath + "/src");
        props.put("ujax:moveDest", testPath + "/dest");
        props.put("ujax:moveFlags", "replace");  // replace dest
        testClient.createNode(HTTP_BASE_URL + testPath, props);
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }

 }