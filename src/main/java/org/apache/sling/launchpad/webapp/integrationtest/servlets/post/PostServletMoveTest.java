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
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.HttpStatusCodeException;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;

/** Test node move via the MicrojaxPostServlet */
public class PostServletMoveTest extends HttpTestBase {

    public static final String TEST_BASE_PATH = "/sling-move-tests";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testMoveNodeAbsolute() throws IOException {
        final String testPath = TEST_BASE_PATH + "/abs/"
            + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_MOVE);
        props.put(SlingPostConstants.RP_DEST, testPath + "/dest");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // assert content at new location
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json",
            CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");

        // assert no content at old location
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src.json",
            HttpServletResponse.SC_NOT_FOUND,
            "Expected Not_Found for old content");
    }

    public void testMoveNodeAbsoluteBelowDest() throws IOException {
        final String testPath = TEST_BASE_PATH + "/abs/"
            + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // first test: failure because dest (parent) does not exist
        List<NameValuePair> nvPairs = new ArrayList<NameValuePair>();
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_MOVE));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_DEST, testPath
            + "/dest/"));
        assertPostStatus(HTTP_BASE_URL + testPath + "/src",
            HttpServletResponse.SC_PRECONDITION_FAILED, nvPairs,
            "Expecting Move Failure (dest must exist)");

        // create dest as parent
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", null);

        // move now succeeds to below dest
        assertPostStatus(HTTP_BASE_URL + testPath + "/src",
            HttpServletResponse.SC_CREATED, nvPairs, "Expecting Move Success");

        // assert content at new location
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.-1.json",
            CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.src.text)");

        // assert content at old location
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src.json",
            HttpServletResponse.SC_NOT_FOUND);
    }

    public void testMoveNodeRelative() throws IOException {
        final String testPath = TEST_BASE_PATH + "/rel/"
            + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_MOVE);
        props.put(SlingPostConstants.RP_DEST, "dest");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json",
            CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }

    public void testMoveNodeExistingFail() throws IOException {
        final String testPath = TEST_BASE_PATH + "/exist/"
            + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // create dest node
        props.put("text", "Hello Destination");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_MOVE);
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
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json",
            CONTENT_TYPE_JSON);
        assertJavascript("Hello Destination", content, "out.println(data.text)");
    }

    public void testMoveNodeExistingReplace() throws IOException {
        final String testPath = TEST_BASE_PATH + "/replace/"
            + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // create dest node
        props.put("text", "Hello Destination");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_MOVE);
        props.put(SlingPostConstants.RP_DEST, testPath + "/dest");
        props.put(SlingPostConstants.RP_REPLACE, "true");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json",
            CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }

    public void testMoveNodeDeepRelative() throws IOException {
        final String testPath = TEST_BASE_PATH + "/new/"
            + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_MOVE);
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

    public void testMoveNodeDeepAbsolute() throws IOException {
        final String testPath = TEST_BASE_PATH + "/new_fail/"
            + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_MOVE);
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

    public void testMoveNodeMultipleSourceValid() throws IOException {
        final String testPath = TEST_BASE_PATH + "/mvmult/"
            + System.currentTimeMillis();
        final String testRoot = testClient.createNode(HTTP_BASE_URL + testPath,
            null);

        // create multiple source nodes
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src1", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/src2", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/src3", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/src4", props);

        // move the src? nodes
        List<NameValuePair> nvPairs = new ArrayList<NameValuePair>();
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_MOVE));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_DEST, testPath
            + "/dest/"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, testPath
            + "/src1"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, testPath
            + "/src2"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, testPath
            + "/src3"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, testPath
            + "/src4"));
        assertPostStatus(testRoot, HttpServletResponse.SC_PRECONDITION_FAILED,
            nvPairs, "Expecting Move Failure: dest parent does not exist");

        // create destination parent
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // now dest exists, so we expect success
        assertPostStatus(testRoot, HttpServletResponse.SC_OK, nvPairs,
            "Expecting Move Success");

        // assert existence of the src?/text properties
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src1/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src2/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src3/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src4/text",
            HttpServletResponse.SC_OK);

        // assert non-existence of src?
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src1.html",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src2.html",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src3.html",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src4.html",
            HttpServletResponse.SC_NOT_FOUND);

        testClient.delete(testRoot);
    }

    public void testMoveNodeMultipleSourceInValid() throws IOException {
        final String testPath = TEST_BASE_PATH + "/mvmult/"
            + System.currentTimeMillis();
        final String testRoot = testClient.createNode(HTTP_BASE_URL + testPath,
            null);

        // create multiple source nodes
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src1", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/src2", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/src3", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/src4", props);

        // move the src? nodes
        List<NameValuePair> nvPairs = new ArrayList<NameValuePair>();
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_MOVE));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_DEST, testPath
            + "/dest"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, testPath
            + "/src1"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, testPath
            + "/src2"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, testPath
            + "/src3"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, testPath
            + "/src4"));
        assertPostStatus(testRoot,
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR, nvPairs,
            "Expecting Move Failure (dest must have trailing slash)");

        // create destination parent
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // retest after creating test
        assertPostStatus(testRoot, HttpServletResponse.SC_PRECONDITION_FAILED,
            nvPairs, "Expecting Move Failure (dest already exists)");

        // assert non-existence of the src?/text properties
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src1/text",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src2/text",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src3/text",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src4/text",
            HttpServletResponse.SC_NOT_FOUND);

        // assert non-existence of src?
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src1.html",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src2.html",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src3.html",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src4.html",
            HttpServletResponse.SC_OK);

        testClient.delete(testRoot);
    }

    public void testMoveNodeMultipleSourcePartial() throws IOException {
        final String testPath = TEST_BASE_PATH + "/mvmult/"
            + System.currentTimeMillis();
        final String testRoot = testClient.createNode(HTTP_BASE_URL + testPath,
            null);

        // create multiple source nodes
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src1", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/src3", props);

        // move the src? nodes
        List<NameValuePair> nvPairs = new ArrayList<NameValuePair>();
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_MOVE));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_DEST, testPath
            + "/dest/"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, testPath
            + "/src1"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, testPath
            + "/src2"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, testPath
            + "/src3"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, testPath
            + "/src4"));
        assertPostStatus(testRoot, HttpServletResponse.SC_PRECONDITION_FAILED,
            nvPairs, "Expecting Move Failure: dest parent does not exist");

        // create destination parent
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // now dest exists, so we expect success
        assertPostStatus(testRoot, HttpServletResponse.SC_OK, nvPairs,
            "Expecting Move Success");

        // assert partial existence of the src?/text properties
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src1/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src2/text",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src3/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src4/text",
            HttpServletResponse.SC_NOT_FOUND);

        // assert non-existence of src?
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src1.html",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src2.html",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src3.html",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src4.html",
            HttpServletResponse.SC_NOT_FOUND);

        testClient.delete(testRoot);
    }

    public void testMoveNodeMultipleSourceReplace() throws Exception {
        final String testPath = TEST_BASE_PATH + "/mvmult/"
            + System.currentTimeMillis();
        final String testRoot = testClient.createNode(HTTP_BASE_URL + testPath,
            null);

        // create multiple source nodes
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src1", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/src2", props);

        // move the src? nodes
        List<NameValuePair> nvPairs = new ArrayList<NameValuePair>();
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_MOVE));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_DEST, testPath
            + "/dest/"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, testPath
            + "/src1"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, testPath
            + "/src2"));
        assertPostStatus(testRoot, HttpServletResponse.SC_PRECONDITION_FAILED,
            nvPairs, "Expecting Move Failure: dest parent does not exist");

        // create destination parent
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", null);

        // now dest exists, so we expect success
        assertPostStatus(testRoot, HttpServletResponse.SC_OK, nvPairs,
            "Expecting Move Success");

        // assert partial existence of the src?/text properties
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src1/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src2/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src3/text",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src4/text",
            HttpServletResponse.SC_NOT_FOUND);

        // assert content test
        String content = getContent(HTTP_BASE_URL + testPath
            + "/dest/src1.json", CONTENT_TYPE_JSON);
        JSONObject json = new JSONObject(content);
        assertEquals("Hello", json.get("text"));

        // modify src1 content
        nvPairs.clear();
        nvPairs.add(new NameValuePair("text", "Modified Hello"));
        assertPostStatus(HTTP_BASE_URL + testPath + "/src1",
            HttpServletResponse.SC_CREATED, nvPairs,
            "Expect Content Create Success");

        // move the src? nodes
        nvPairs.clear();
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_MOVE));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_DEST, testPath
            + "/dest/"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, testPath
            + "/src1"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, testPath
            + "/src2"));
        assertPostStatus(testRoot, HttpServletResponse.SC_OK, nvPairs,
            "Expecting Move Success");

        // assert content test
        String content2 = getContent(HTTP_BASE_URL + testPath
            + "/dest/src1.json", CONTENT_TYPE_JSON);
        JSONObject json2 = new JSONObject(content2);
        assertEquals("Modified Hello", json2.get("text"));

        // clean up
        testClient.delete(testRoot);
    }

    public void testMoveAtRoot() throws IOException {
        final String pathA = "/" + getClass().getSimpleName() + "_A";
        final String pathB = "/" + getClass().getSimpleName() + "_B";
        
        final String testText = "Hello." + Math.random();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", testText);
        
        // Create pathA
        testClient.delete(HTTP_BASE_URL + pathA);
        assertHttpStatus(HTTP_BASE_URL + pathA, HttpServletResponse.SC_NOT_FOUND);
        testClient.createNode(HTTP_BASE_URL + pathA, props);
        
        // Move to pathB
        testClient.delete(HTTP_BASE_URL + pathB);
        assertHttpStatus(HTTP_BASE_URL + pathB, HttpServletResponse.SC_NOT_FOUND);
        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_MOVE);
        props.put(SlingPostConstants.RP_DEST, pathB);
        props.put(SlingPostConstants.RP_REPLACE, "true");
        testClient.createNode(HTTP_BASE_URL + pathA, props);
        String content = getContent(HTTP_BASE_URL + pathB + ".json", CONTENT_TYPE_JSON);
        assertJavascript(testText, content, "out.println(data.text)");
        assertHttpStatus(HTTP_BASE_URL + pathA, HttpServletResponse.SC_NOT_FOUND);
    }

    
    /**
     * Test for SLING-2415 Ability to move all child nodes, without the parent node
     * Using :applyTo value of "*"
     */
    public void testMoveAllChildren() throws IOException {
        final String testPath = TEST_BASE_PATH + "/mvmultwc/"
            + System.currentTimeMillis();
        final String testRoot = testClient.createNode(HTTP_BASE_URL + testPath,
            null);

        // create multiple source nodes
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/test/src1", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/test/src2", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/test/src3", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/test/src4", props);

        // create destination parent
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // move the src? nodes
        List<NameValuePair> nvPairs = new ArrayList<NameValuePair>();
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_MOVE));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_DEST, testPath
            + "/dest/"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, "*"));
        // we expect success
        assertPostStatus(testRoot + "/test", HttpServletResponse.SC_OK, nvPairs,
            "Expecting Move Success");

        // assert existence of the src?/text properties
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src1/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src2/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src3/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src4/text",
            HttpServletResponse.SC_OK);

        // assert non-existence of src?
        assertHttpStatus(HTTP_BASE_URL + testPath + "/test/src1.html",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/test/src2.html",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/test/src3.html",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/test/src4.html",
            HttpServletResponse.SC_NOT_FOUND);

        testClient.delete(testRoot);
    }

    /**
     * Test for SLING-2415 Ability to move all child nodes, without the parent node
     * Using :applyTo value of "/*"
     */
    public void testMoveAllChildrenByPath() throws IOException {
        final String testPath = TEST_BASE_PATH + "/mvmultwc/"
            + System.currentTimeMillis();
        final String testRoot = testClient.createNode(HTTP_BASE_URL + testPath,
            null);

        // create multiple source nodes
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/test/src1", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/test/src2", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/test/src3", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/test/src4", props);

        // create destination parent
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // move the src? nodes
        List<NameValuePair> nvPairs = new ArrayList<NameValuePair>();
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_MOVE));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_DEST, testPath
            + "/dest/"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, "/*"));
        // we expect success
        assertPostStatus(testRoot + "/test", HttpServletResponse.SC_OK, nvPairs,
            "Expecting Move Success");

        // assert existence of the src?/text properties
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src1/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src2/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src3/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src4/text",
            HttpServletResponse.SC_OK);

        // assert non-existence of src?
        assertHttpStatus(HTTP_BASE_URL + testPath + "/test/src1.html",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/test/src2.html",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/test/src3.html",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/test/src4.html",
            HttpServletResponse.SC_NOT_FOUND);

        testClient.delete(testRoot);
    }
    
    /**
     * Test for SLING-2415 Ability to move all child nodes of a subnode, without the parent node
     * Using :applyTo value of "subnode_path/*"
     */
    public void testMoveAllChildrenOfSubNode() throws IOException {
        final String testPath = TEST_BASE_PATH + "/mvmultwc/"
            + System.currentTimeMillis();
        final String testRoot = testClient.createNode(HTTP_BASE_URL + testPath,
            null);

        // create multiple source nodes
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/test/src1", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/test/src2", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/test/src3", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/test/src4", props);

        // create destination parent
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // move the src? nodes
        List<NameValuePair> nvPairs = new ArrayList<NameValuePair>();
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_MOVE));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_DEST, testPath
            + "/dest/"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, "test/*"));
        // we expect success
        assertPostStatus(testRoot, HttpServletResponse.SC_OK, nvPairs,
            "Expecting Move Success");

        // assert existence of the src?/text properties
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src1/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src2/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src3/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src4/text",
            HttpServletResponse.SC_OK);

        // assert non-existence of src?
        assertHttpStatus(HTTP_BASE_URL + testPath + "/test/src1.html",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/test/src2.html",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/test/src3.html",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/test/src4.html",
            HttpServletResponse.SC_NOT_FOUND);

        testClient.delete(testRoot);
    }
}