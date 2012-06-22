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

/** Test node copy via the MicrojaxPostServlet */
public class PostServletCopyTest extends HttpTestBase {

    public static final String TEST_BASE_PATH = "/sling-copy-tests";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testCopyNodeAbsolute() throws IOException {
        final String testPath = TEST_BASE_PATH + "/abs/"
            + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_COPY);
        props.put(SlingPostConstants.RP_DEST, testPath + "/dest");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // assert content at new location
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json",
            CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");

        // assert content at old location
        content = getContent(HTTP_BASE_URL + testPath + "/src.json",
            CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }

    public void testCopyNodeAbsoluteBelowDest() throws IOException {
        final String testPath = TEST_BASE_PATH + "/abs/"
            + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        // first test: failure because dest (parent) does not exist
        List<NameValuePair> nvPairs = new ArrayList<NameValuePair>();
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_COPY));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_DEST, testPath
            + "/dest/"));
        assertPostStatus(HTTP_BASE_URL + testPath + "/src",
            HttpServletResponse.SC_PRECONDITION_FAILED, nvPairs,
            "Expecting Copy Failure (dest must exist)");

        // create dest as parent
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", null);

        // copy now succeeds to below dest
        assertPostStatus(HTTP_BASE_URL + testPath + "/src",
            HttpServletResponse.SC_CREATED, nvPairs, "Expecting Copy Success");

        // assert content at new location
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.-1.json",
            CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.src.text)");

        // assert content at old location
        content = getContent(HTTP_BASE_URL + testPath + "/src.json",
            CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }

    public void testCopyNodeRelative() throws IOException {
        final String testPath = TEST_BASE_PATH + "/rel/"
            + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_COPY);
        props.put(SlingPostConstants.RP_DEST, "dest");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json",
            CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }

    public void testCopyNodeExistingFail() throws IOException {
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
            SlingPostConstants.OPERATION_COPY);
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

    public void testCopyNodeExistingReplace() throws IOException {
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
            SlingPostConstants.OPERATION_COPY);
        props.put(SlingPostConstants.RP_DEST, testPath + "/dest");
        props.put(SlingPostConstants.RP_REPLACE, "true");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json",
            CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
    }

    public void testCopyNodeDeepRelative() throws IOException {
        final String testPath = TEST_BASE_PATH + "/new/"
            + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_COPY);
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
        final String testPath = TEST_BASE_PATH + "/new_fail/"
            + System.currentTimeMillis();
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", props);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_COPY);
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

    public void testCopyNodeMultipleSourceValid() throws IOException {
        final String testPath = TEST_BASE_PATH + "/cpmult/"
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

        // copy the src? nodes
        List<NameValuePair> nvPairs = new ArrayList<NameValuePair>();
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_COPY));
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
            nvPairs, "Expecting Copy Failure: dest parent does not exist");

        // create destination parent
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // now dest exists, so we expect success
        assertPostStatus(testRoot, HttpServletResponse.SC_OK, nvPairs,
            "Expecting Copy Success");

        // assert existence of the src?/text properties
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src1/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src2/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src3/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src4/text",
            HttpServletResponse.SC_OK);

        testClient.delete(testRoot);
    }

    public void testCopyNodeMultipleSourceInValid() throws IOException {
        final String testPath = TEST_BASE_PATH + "/cpmult/"
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

        // copy the src? nodes
        List<NameValuePair> nvPairs = new ArrayList<NameValuePair>();
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_COPY));
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
            "Expecting Copy Failure (dest must have trailing slash)");

        // create destination parent
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // retest after creating test
        assertPostStatus(testRoot, HttpServletResponse.SC_PRECONDITION_FAILED,
            nvPairs, "Expecting Copy Failure (dest already exists)");

        // assert non-existence of the src?/text properties
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src1/text",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src2/text",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src3/text",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src4/text",
            HttpServletResponse.SC_NOT_FOUND);

        testClient.delete(testRoot);
    }

    public void testCopyNodeMultipleSourcePartial() throws IOException {
        final String testPath = TEST_BASE_PATH + "/cpmult/"
            + System.currentTimeMillis();
        final String testRoot = testClient.createNode(HTTP_BASE_URL + testPath,
            null);

        // create multiple source nodes
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src1", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/src3", props);

        // copy the src? nodes
        List<NameValuePair> nvPairs = new ArrayList<NameValuePair>();
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_COPY));
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
            nvPairs, "Expecting Copy Failure: dest parent does not exist");

        // create destination parent
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", props);

        // now dest exists, so we expect success
        assertPostStatus(testRoot, HttpServletResponse.SC_OK, nvPairs,
            "Expecting Copy Success");

        // assert partial existence of the src?/text properties
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src1/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src2/text",
            HttpServletResponse.SC_NOT_FOUND);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src3/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src4/text",
            HttpServletResponse.SC_NOT_FOUND);

        testClient.delete(testRoot);
    }

    public void testCopyNodeMultipleSourceReplace() throws Exception {
        final String testPath = TEST_BASE_PATH + "/cpmult/"
            + System.currentTimeMillis();
        final String testRoot = testClient.createNode(HTTP_BASE_URL + testPath,
            null);

        // create multiple source nodes
        Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src1", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/src2", props);

        // copy the src? nodes
        List<NameValuePair> nvPairs = new ArrayList<NameValuePair>();
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_COPY));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_DEST, testPath
            + "/dest/"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, testPath
            + "/src1"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, testPath
            + "/src2"));
        assertPostStatus(testRoot, HttpServletResponse.SC_PRECONDITION_FAILED,
            nvPairs, "Expecting Copy Failure: dest parent does not exist");

        // create destination parent
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", null);

        // now dest exists, so we expect success
        assertPostStatus(testRoot, HttpServletResponse.SC_OK, nvPairs,
            "Expecting Copy Success");

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
            HttpServletResponse.SC_OK, nvPairs, "Expect Content Update Success");

        // copy the src? nodes
        nvPairs.clear();
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_COPY));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_DEST, testPath
            + "/dest/"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, testPath
            + "/src1"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, testPath
            + "/src2"));
        assertPostStatus(testRoot, HttpServletResponse.SC_OK, nvPairs,
            "Expecting Copy Success");

        // assert content test
        String content2 = getContent(HTTP_BASE_URL + testPath
            + "/dest/src1.json", CONTENT_TYPE_JSON);
        JSONObject json2 = new JSONObject(content2);
        assertEquals("Modified Hello", json2.get("text"));

        // clean up
        testClient.delete(testRoot);
    }
    
    
    /**
     * Test for SLING-2415 Ability to move all child nodes, without the parent node
     * Using :applyTo value of "*"
     */
    public void testCopyAllChildren() throws IOException {
        final String testPath = TEST_BASE_PATH + "/cpmultwc/"
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

        // copy the src? nodes
        List<NameValuePair> nvPairs = new ArrayList<NameValuePair>();
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_COPY));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_DEST, testPath
            + "/dest/"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, "*"));
        // we expect success
        assertPostStatus(testRoot + "/test", HttpServletResponse.SC_OK, nvPairs,
            "Expecting Copy Success");

        // assert existence of the src?/text properties
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src1/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src2/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src3/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src4/text",
            HttpServletResponse.SC_OK);

        testClient.delete(testRoot);
    }
    
    /**
     * Test for SLING-2415 Ability to move all child nodes, without the parent node
     * Using :applyTo value of "/*"
     */
    public void testCopyAllChildrenByPath() throws IOException {
        final String testPath = TEST_BASE_PATH + "/cpmultwc/"
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

        // copy the src? nodes
        List<NameValuePair> nvPairs = new ArrayList<NameValuePair>();
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_COPY));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_DEST, testPath
            + "/dest/"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, "/*"));
        // we expect success
        assertPostStatus(testRoot + "/test", HttpServletResponse.SC_OK, nvPairs,
            "Expecting Copy Success");

        // assert existence of the src?/text properties
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src1/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src2/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src3/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src4/text",
            HttpServletResponse.SC_OK);

        testClient.delete(testRoot);
    }
    
    /**
     * Test for SLING-2415 Ability to copy all child nodes of a subnode, without the parent node
     * Using :applyTo value of "subnode_path/*"
     */
    public void testCopyAllChildrenOfSubNode() throws IOException {
        final String testPath = TEST_BASE_PATH + "/cpmultwc/"
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

        // copy the src? nodes
        List<NameValuePair> nvPairs = new ArrayList<NameValuePair>();
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_OPERATION,
            SlingPostConstants.OPERATION_COPY));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_DEST, testPath
            + "/dest/"));
        nvPairs.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, "test/*"));
        // we expect success
        assertPostStatus(testRoot, HttpServletResponse.SC_OK, nvPairs,
            "Expecting Copy Success");

        // assert existence of the src?/text properties
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src1/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src2/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src3/text",
            HttpServletResponse.SC_OK);
        assertHttpStatus(HTTP_BASE_URL + testPath + "/dest/src4/text",
            HttpServletResponse.SC_OK);

        testClient.delete(testRoot);
    }
    
    /** Copying siblings should work */
    public void testCopySibling() throws IOException {
        final String testPath = TEST_BASE_PATH + "/csp/" + System.currentTimeMillis();
        final Map<String, String> props = new HashMap<String, String>();
        props.put("text", "Hello csp");
        testClient.createNode(HTTP_BASE_URL + testPath + "/a/1", props);
        testClient.createNode(HTTP_BASE_URL + testPath + "/a/12", props);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_COPY);
        props.put(SlingPostConstants.RP_DEST, testPath + "/a/12/13");
        testClient.createNode(HTTP_BASE_URL + testPath + "/a/1", props);

        // assert content at old and new locations
        for(String path : new String[] { "/a/1", "/a/12", "/a/12/13" }) {
            final String content = getContent(HTTP_BASE_URL + testPath + path + ".json", CONTENT_TYPE_JSON);
            assertJavascript("Hello csp", content, "out.println(data.text)", "at path " + path);
        }
    }
    
    /** Copying an ancestor to a descendant should fail */
    public void testCopyAncestor() throws IOException {
        final String testPath = TEST_BASE_PATH + "/tcanc/" + System.currentTimeMillis();
        final String testRoot = testClient.createNode(HTTP_BASE_URL + testPath, null);
        final String parentPath = testPath + "/a/b";
        final String childPath = parentPath + "/child";
        
        final String parentUrl = testClient.createNode(HTTP_BASE_URL + parentPath, null);
        assertFalse("child node must not exist before copy", 
                getContent(parentUrl + ".2.json", CONTENT_TYPE_JSON).contains("child"));
        
        final List<NameValuePair> opt = new ArrayList<NameValuePair>();
        opt.add(new NameValuePair(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_COPY));
        opt.add(new NameValuePair(SlingPostConstants.RP_DEST, childPath));
        
        final int expectedStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        assertPostStatus(testRoot, expectedStatus, opt, "Expecting status " + expectedStatus);
    }
}