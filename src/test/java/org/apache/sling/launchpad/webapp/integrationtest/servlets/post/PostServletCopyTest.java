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

import org.apache.sling.launchpad.webapp.integrationtest.HttpTestBase;
import org.apache.sling.launchpad.webapp.integrationtest.helpers.HttpStatusCodeException;
import org.apache.sling.servlets.post.SlingPostConstants;

/** Test node copy via the MicrojaxPostServlet */
public class PostServletCopyTest extends HttpTestBase {

    public static final String TEST_BASE_PATH = "/sling-copy-tests";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testCopyNodeAbsolute() throws IOException {
        final String testPath = TEST_BASE_PATH + "/abs/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_COPY);
        props.put(SlingPostConstants.RP_DEST, testPath + "/dest");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);
        
        // assert content at new location
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
        
        // assert content at old location
        content = getContent(HTTP_BASE_URL + testPath + "/src.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }

    public void testCopyNodeRelative() throws IOException {
        final String testPath = TEST_BASE_PATH + "/rel/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_COPY);
        props.put(SlingPostConstants.RP_DEST, "dest");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }

    public void testCopyNodeExistingFail() throws IOException {
        final String testPath = TEST_BASE_PATH + "/exist/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // create dest node
        props.put("text", "Hello Destination");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_COPY);
        props.put(SlingPostConstants.RP_DEST, testPath + "/dest");
        try {
            testClient.createNode(HTTP_BASE_URL + testPath, props);
        } catch (HttpStatusCodeException hsce) {
            // if we do not get the status code 302 message, fail
            if (hsce.getActualStatus() == 302) {
                throw hsce;
            }
        }

        // expect unmodified dest
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello Destination", content, "out.println(data.text)");
    }

    public void testCopyNodeExistingReplace() throws IOException {
        final String testPath = TEST_BASE_PATH + "/replace/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // create dest node
        props.put("text", "Hello Destination");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_COPY);
        props.put(SlingPostConstants.RP_DEST, testPath + "/dest");
        props.put(SlingPostConstants.RP_REPLACE, "true");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }

    public void testCopyNodeDeepRelative() throws IOException {
        final String testPath = TEST_BASE_PATH + "/new/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_COPY);
        props.put(SlingPostConstants.RP_DEST, "deep/new");
        
        try {
            testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);
            fail("Moving node to non existing parent location should fail.");
        } catch (HttpStatusCodeException hsce) {
            // actually the status is not 200, but we get "browser" clear stati
            if (hsce.getActualStatus() != 200) {
                throw hsce;
            }
        }
    }

    public void testCopyNodeDeepAbsolute() throws IOException {
        final String testPath = TEST_BASE_PATH + "/new_fail/" + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_COPY);
        props.put(SlingPostConstants.RP_DEST, "/some/not/existing/structure");
        try {
            testClient.createNode(HTTP_BASE_URL + testPath + "/*", props);
            // not quite correct. should check status response
            fail("Moving node to non existing parent location should fail.");
        } catch (HttpStatusCodeException hsce) {
            // actually the status is not 200, but we get "browser" clear stati
            if (hsce.getActualStatus() != 200) {
                throw hsce;
            }
        }
    }

 }