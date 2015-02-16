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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;

/** Test node creation via the PostServlet and versionable nodes */
public class PostServletVersionableTest extends HttpTestBase {

    public static final String TEST_BASE_PATH = "/sling-tests";
    private String postUrl;
    private Map<String,String> params;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
        params = new HashMap<String,String>();
        params.put("jcr:mixinTypes", "mix:versionable");
    }

   public void testPostPathIsUnique() throws IOException {
        assertHttpStatus(postUrl, HttpServletResponse.SC_NOT_FOUND,
                "Path must not exist before test: " + postUrl);
    }

    public void testCreatedNodeIsCheckedOut() throws IOException {
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, params);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));

        final String content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked out.",
                content.contains("jcr:isCheckedOut: true"));

    }

    public void testAddingVersionableMixInChecksOut() throws IOException {
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));

        String content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertFalse("Node (" + location + ") isn't versionable.",
                content.contains("jcr:isCheckedOut"));

        testClient.createNode(location, params);
        content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked out.",
                content.contains("jcr:isCheckedOut: true"));

    }

    public void testCreatedNodeIsCheckedInIfRequested() throws IOException {
        params.put(":checkinNewVersionableNodes", "true");
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, params);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));

        final String content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked in.",
                content.contains("jcr:isCheckedOut: false"));
    }

    public void testAddingVersionableMixInChecksInIfRequested() throws IOException {
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));

        String content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertFalse("Node (" + location + ") isn't versionable.",
                content.contains("jcr:isCheckedOut"));

        params.put(":checkinNewVersionableNodes", "true");
        testClient.createNode(location, params);
        content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked in.",
                content.contains("jcr:isCheckedOut: false"));

    }

    public void testModifyingACheckedOutNodeDoesntCheckItIn() throws IOException {
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, params);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));

        String content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked out.",
                content.contains("jcr:isCheckedOut: true"));

        params.clear();
        params.put("name", "value");
        testClient.createNode(location, params);

        content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node property should have been set.", content.contains("name: value"));
        assertTrue("Node (" + location + ") should (still) be checked out.",
                content.contains("jcr:isCheckedOut: true"));

    }

    public void testModifyingACheckedInNodeFailsWithoutAutoCheckout() throws IOException {
        params.put(":checkinNewVersionableNodes", "true");
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, params);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));

        String content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked in.",
                content.contains("jcr:isCheckedOut: false"));

        List<NameValuePair> testParams = Arrays.asList(new NameValuePair(":autoCheckout", "false"),
                new NameValuePair("name", "value"));
        assertPostStatus(location, 500, testParams, "Attempted modification with :autoCheckout=false should fail.");

    }

    public void testModifiedNodeIsCheckedInAfterModification() throws IOException {
        params.put(":checkinNewVersionableNodes", "true");
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, params);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));

        String content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked in.",
                content.contains("jcr:isCheckedOut: false"));

        params.clear();
        params.put(":autoCheckout", "true");
        params.put("name", "value");
        testClient.createNode(location, params);

        content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node property should have been set.", content.contains("name: value"));
        assertTrue("Node (" + location + ") should be checked in.",
                content.contains("jcr:isCheckedOut: false"));

    }

    public void testModifiedNodeIsCheckedOutIfRequested() throws IOException {
        params.put(":checkinNewVersionableNodes", "true");
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, params);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));

        String content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked in.",
                content.contains("jcr:isCheckedOut: false"));

        params.clear();
        params.put(":autoCheckout", "true");
        params.put("name", "value");
        params.put(":autoCheckin", "false");
        testClient.createNode(location, params);

        content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node property should have been set.", content.contains("name: value"));
        assertTrue("Node (" + location + ") should be checked out.",
                content.contains("jcr:isCheckedOut: true"));

    }

    public void testCheckingInACheckOutNode() throws IOException {
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, params);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));

        String content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked out.",
                content.contains("jcr:isCheckedOut: true"));

        params.clear();
        params.put(":operation", "checkin");
        testClient.createNode(location, params);

        content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked in.",
                content.contains("jcr:isCheckedOut: false"));

    }

    public void testRestoreVersion() throws IOException {
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, params);
        testClient.createNode(location, Collections.singletonMap("key", "valueForVersion1.0"));
        testClient.createNode(location, Collections.singletonMap(":operation", "checkin"));

        testClient.createNode(location, Collections.singletonMap(":operation", "checkout"));
        testClient.createNode(location, Collections.singletonMap("key", "valueForVersion1.1"));
        testClient.createNode(location, Collections.singletonMap(":operation", "checkin"));

        assertTrue(getContent(location + ".txt", CONTENT_TYPE_PLAIN).contains("key: valueForVersion1.1"));

        params.clear();
        params.put(":operation", "restore");
        params.put(":version", "1.0");
        testClient.createNode(location, params);

        assertTrue(getContent(location + ".txt", CONTENT_TYPE_PLAIN).contains("key: valueForVersion1.0"));

        params.clear();
        params.put(":operation", "restore");
        params.put(":version", "1.1");
        testClient.createNode(location, params);

        assertTrue(getContent(location + ".txt", CONTENT_TYPE_PLAIN).contains("key: valueForVersion1.1"));
    }

    public void testCheckingOutACheckedInNode() throws IOException {
        params.put(":checkinNewVersionableNodes", "true");
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, params);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));

        String content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked in.",
                content.contains("jcr:isCheckedOut: false"));

        params.clear();
        params.put(":operation", "checkout");
        testClient.createNode(location, params);

        content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked in.",
                content.contains("jcr:isCheckedOut: true"));

    }

    public void testCheckingOutAnAlreadyCheckedOutNodeIsANoOp() throws IOException {
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, params);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));

        String content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked out.",
                content.contains("jcr:isCheckedOut: true"));

        params.clear();
        params.put(":operation", "checkout");
        testClient.createNode(location, params);

        content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked out.",
                content.contains("jcr:isCheckedOut: true"));

    }

    public void testCheckingInAnAlreadyCheckedInNodeIsANoOp() throws IOException {
        params.put(":checkinNewVersionableNodes", "true");
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, params);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));

        String content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked in.",
                content.contains("jcr:isCheckedOut: false"));

        params.clear();
        params.put(":operation", "checkin");
        testClient.createNode(location, params);

        content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked in.",
                content.contains("jcr:isCheckedOut: false"));

    }

    public void testDeletingChildNodeOfACheckedInNode() throws IOException {
        params.put(":checkinNewVersionableNodes", "true");
        params.put("child/testprop", "testvalue");
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, params);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));

        String content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked in.",
                content.contains("jcr:isCheckedOut: false"));
        assertFalse("Node (" + location + ") shouldn't have a test property.",
                content.contains("testprop: testvalue"));

        content = getContent(location + "/child.txt", CONTENT_TYPE_PLAIN);
        assertFalse("Node (" + location + "/child) shouldn't be versionable be checked in.",
                content.contains("jcr:isCheckedOut: false"));
        assertTrue("Node (" + location + "/child) has a test property. ",
                content.contains("testprop: testvalue"));

        params.clear();
        params.put(":autoCheckout", "true");
        params.put("child@Delete", "");
        testClient.createNode(location, params);

        content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked in.",
                content.contains("jcr:isCheckedOut: false"));

        assertHttpStatus(location + "/child.txt", 404);

    }

    public void testDeletingChildNodeOfACheckedInNodeByOp() throws IOException {
        params.put(":checkinNewVersionableNodes", "true");
        params.put("child/testprop", "testvalue");
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, params);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));

        String content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked in.",
                content.contains("jcr:isCheckedOut: false"));
        assertFalse("Node (" + location + ") shouldn't have a test property.",
                content.contains("testprop: testvalue"));

        content = getContent(location + "/child.txt", CONTENT_TYPE_PLAIN);
        assertFalse("Node (" + location + "/child) shouldn't be versionable be checked in.",
                content.contains("jcr:isCheckedOut: false"));
        assertTrue("Node (" + location + "/child) has a test property. ",
                content.contains("testprop: testvalue"));

        params.clear();
        params.put(":autoCheckout", "true");
        params.put(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_DELETE);
        testClient.createNode(location+"/child", params);

        content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked in.",
                content.contains("jcr:isCheckedOut: false"));

        assertHttpStatus(location + "/child.txt", 404);

    }

    public void testDeletingAPropertyOfACheckedInNode() throws IOException {
        params.put(":checkinNewVersionableNodes", "true");
        params.put("testprop", "testvalue");
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, params);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));

        String content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked in.",
                content.contains("jcr:isCheckedOut: false"));
        assertTrue("Node (" + location + ") has a test property.",
                content.contains("testprop: testvalue"));

        params.clear();
        params.put(":autoCheckout", "true");
        params.put("testprop@Delete", "");
        testClient.createNode(location, params);

        content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked in.",
                content.contains("jcr:isCheckedOut: false"));
        assertFalse("Node (" + location + ") shouldn't have a test property.",
                content.contains("testprop: testvalue"));

    }

    public void testDeletingAPropertyOfACheckedInNodeFailsWithoutAutoCheckout() throws IOException {
        params.put(":checkinNewVersionableNodes", "true");
        params.put("testprop", "testvalue");
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, params);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));

        String content = getContent(location + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Node (" + location + ") should be checked in.",
                content.contains("jcr:isCheckedOut: false"));
        assertTrue("Node (" + location + ") has a test property.",
                content.contains("testprop: testvalue"));

        List<NameValuePair> testParams = Arrays.asList(new NameValuePair(":autoCheckout", "false"),
                new NameValuePair("testprop@Delete", ""));

        assertPostStatus(location, 500, testParams, "Attempted modification with :autoCheckout=false should fail.");

    }

    public void testMovingAPropertyOfACheckedInNodeToANewVersionableNode() throws IOException {
        params.put(":checkinNewVersionableNodes", "true");
        params.put("text", "Hello");
        final String testPath = TEST_BASE_PATH + "/abs/" + System.currentTimeMillis();
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", params);

        // assert content at source location
        String oldContent = getContent(HTTP_BASE_URL + testPath + "/src.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", oldContent, "out.println(data.text)");
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");

        // create dest with text set from src/text
        params.clear();
        params.put(":autoCheckout", "true");
        params.put("jcr:mixinTypes", "mix:versionable");
        params.put(":checkinNewVersionableNodes", "true");
        params.put("text@MoveFrom", testPath + "/src/text");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", params);

        // assert content at new location
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");

        // assert no content at old location
        oldContent = getContent(HTTP_BASE_URL + testPath + "/src.json", CONTENT_TYPE_JSON);
        assertJavascript("undefined", oldContent, "out.println(typeof(data.text))");
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");
    }

    public void testMovingAPropertyOfACheckedInNodeToACheckedInNode() throws IOException {
        params.put(":checkinNewVersionableNodes", "true");
        params.put("text", "Hello");
        final String testPath = TEST_BASE_PATH + "/abs/" + System.currentTimeMillis();
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", params);

        // assert content at source location
        String oldContent = getContent(HTTP_BASE_URL + testPath + "/src.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", oldContent, "out.println(data.text)");
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");

        // create dest
        params.clear();
        params.put("jcr:mixinTypes", "mix:versionable");
        params.put(":checkinNewVersionableNodes", "true");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", params);

        // assert content at new location
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");

        params.clear();
        params.put(":autoCheckout", "true");
        params.put("text@MoveFrom", testPath + "/src/text");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", params);

        // assert content at new location
        content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");

        // assert no content at old location
        oldContent = getContent(HTTP_BASE_URL + testPath + "/src.json", CONTENT_TYPE_JSON);
        assertJavascript("undefined", oldContent, "out.println(typeof(data.text))");
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");

    }

    public void testCopyingAPropertyToACheckedInNode() throws IOException {
        params.put(":checkinNewVersionableNodes", "true");
        params.put("text", "Hello");
        final String testPath = TEST_BASE_PATH + "/abs/" + System.currentTimeMillis();
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", params);

        // assert content at source location
        String oldContent = getContent(HTTP_BASE_URL + testPath + "/src.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", oldContent, "out.println(data.text)");
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");

        // create dest as empty
        params.put("jcr:mixinTypes", "mix:versionable");
        params.put(":checkinNewVersionableNodes", "true");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", params);

        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("false", content, "out.println(data['jcr:isCheckedOut'])");

        // copy text from src/text
        params.clear();
        params.put(":autoCheckout", "true");
        params.put("text@CopyFrom", testPath + "/src/text");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", params);

        // assert content at new location
        content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.text)");
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");

        // assert content at source location
        oldContent = getContent(HTTP_BASE_URL + testPath + "/src.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", oldContent, "out.println(data.text)");
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");
    }

    public void testMovingAChildNodeOfACheckedInNodeToANewVersionableNode() throws IOException {
        params.put(":checkinNewVersionableNodes", "true");
        final String testPath = TEST_BASE_PATH + "/abs/" + System.currentTimeMillis();
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", params);

        params.clear();
        params.put(":autoCheckout", "true");
        params.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src/child", params);

        // assert content at source location
        String oldContent = getContent(HTTP_BASE_URL + testPath + "/src.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");
        assertJavascript("Hello", oldContent, "out.println(data.child.text)");

        // create dest with child from src
        params.clear();
        params.put(":autoCheckout", "true");
        params.put("jcr:mixinTypes", "mix:versionable");
        params.put(":checkinNewVersionableNodes", "true");
        params.put("src@MoveFrom", testPath + "/src/child");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", params);

        // assert content at new location
        String content = getContent(HTTP_BASE_URL + testPath + "/dest.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.src.text)");
        assertJavascript("false", content, "out.println(data['jcr:isCheckedOut'])");

        // assert no content at old location
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src/child.json",
            HttpServletResponse.SC_NOT_FOUND, "Expected Not_Found for old content");
        oldContent = getContent(HTTP_BASE_URL + testPath + "/src.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");
    }

    public void testMovingAChildNodeOfACheckedInNodeToACheckedInNode() throws IOException {
        params.put(":checkinNewVersionableNodes", "true");
        final String testPath = TEST_BASE_PATH + "/abs/" + System.currentTimeMillis();
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", params);

        params.clear();
        params.put(":autoCheckout", "true");
        params.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src/child", params);

        // assert content at source location
        String oldContent = getContent(HTTP_BASE_URL + testPath + "/src.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");
        assertJavascript("Hello", oldContent, "out.println(data.child.text)");

        // create dest
        params.clear();
        params.put("jcr:mixinTypes", "mix:versionable");
        params.put(":checkinNewVersionableNodes", "true");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", params);

        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("false", content, "out.println(data['jcr:isCheckedOut'])");

        // move src child
        params.clear();
        params.put(":autoCheckout", "true");
        params.put("src@MoveFrom", testPath + "/src/child");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", params);

        // assert content at new location
        content = getContent(HTTP_BASE_URL + testPath + "/dest.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.src.text)");
        assertJavascript("false", content, "out.println(data['jcr:isCheckedOut'])");

        // assert no content at old location
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src/child.json",
            HttpServletResponse.SC_NOT_FOUND, "Expected Not_Found for old content");
        oldContent = getContent(HTTP_BASE_URL + testPath + "/src.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");
    }

    public void testCopyingANodeToACheckedInNode() throws IOException {
        params.put(":checkinNewVersionableNodes", "true");
        final String testPath = TEST_BASE_PATH + "/abs/" + System.currentTimeMillis();
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", params);

        params.clear();
        params.put(":autoCheckout", "true");
        params.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src/child", params);

        // assert content at source location
        String oldContent = getContent(HTTP_BASE_URL + testPath + "/src.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");
        assertJavascript("Hello", oldContent, "out.println(data.child.text)");

        // create dest as empty
        params.put("jcr:mixinTypes", "mix:versionable");
        params.put(":checkinNewVersionableNodes", "true");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", params);

        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");

        // copy text from src/text
        params.clear();
        params.put(":autoCheckout", "true");
        params.put("src@CopyFrom", testPath + "/src/child");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", params);

        // assert content at new location
        content = getContent(HTTP_BASE_URL + testPath + "/dest.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.src.text)");
        assertJavascript("false", content, "out.println(data['jcr:isCheckedOut'])");

        // assert content at source location
        oldContent = getContent(HTTP_BASE_URL + testPath + "/src.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", oldContent, "out.println(data.child.text)");
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");
    }

    public void testMovingAChildNodeOfACheckedInNodeToACheckedInNodeByOp() throws IOException {
        params.put(":checkinNewVersionableNodes", "true");
        final String testPath = TEST_BASE_PATH + "/abs/" + System.currentTimeMillis();
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", params);

        params.clear();
        params.put(":autoCheckout", "true");
        params.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src/child", params);

        // assert content at source location
        String oldContent = getContent(HTTP_BASE_URL + testPath + "/src.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");
        assertJavascript("Hello", oldContent, "out.println(data.child.text)");

        // create dest
        params.clear();
        params.put("jcr:mixinTypes", "mix:versionable");
        params.put(":checkinNewVersionableNodes", "true");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", params);

        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("false", content, "out.println(data['jcr:isCheckedOut'])");

        // move src child
        params.clear();
        params.put(":autoCheckout", "true");
        params.put(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_MOVE);
        params.put(":dest", testPath + "/dest/src");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src/child", params);

        // assert content at new location
        content = getContent(HTTP_BASE_URL + testPath + "/dest.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.src.text)");
        assertJavascript("false", content, "out.println(data['jcr:isCheckedOut'])");

        // assert no content at old location
        assertHttpStatus(HTTP_BASE_URL + testPath + "/src/child.json",
            HttpServletResponse.SC_NOT_FOUND, "Expected Not_Found for old content");
        oldContent = getContent(HTTP_BASE_URL + testPath + "/src.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");
    }

    public void testCopyingANodeToACheckedInNodeByOp() throws IOException {
        params.put(":checkinNewVersionableNodes", "true");
        final String testPath = TEST_BASE_PATH + "/abs/" + System.currentTimeMillis();
        testClient.createNode(HTTP_BASE_URL + testPath + "/src", params);

        params.clear();
        params.put(":autoCheckout", "true");
        params.put("text", "Hello");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src/child", params);

        // assert content at source location
        String oldContent = getContent(HTTP_BASE_URL + testPath + "/src.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");
        assertJavascript("Hello", oldContent, "out.println(data.child.text)");

        // create dest as empty
        params.put("jcr:mixinTypes", "mix:versionable");
        params.put(":checkinNewVersionableNodes", "true");
        testClient.createNode(HTTP_BASE_URL + testPath + "/dest", params);

        String content = getContent(HTTP_BASE_URL + testPath + "/dest.json", CONTENT_TYPE_JSON);
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");

        // copy child from src
        params.clear();
        params.put(":autoCheckout", "true");
        params.put(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_COPY);
        params.put(":dest", testPath + "/dest/src");
        testClient.createNode(HTTP_BASE_URL + testPath + "/src/child", params);

        // assert content at new location
        content = getContent(HTTP_BASE_URL + testPath + "/dest.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", content, "out.println(data.src.text)");
        assertJavascript("false", content, "out.println(data['jcr:isCheckedOut'])");

        // assert content at source location
        oldContent = getContent(HTTP_BASE_URL + testPath + "/src.-1.json", CONTENT_TYPE_JSON);
        assertJavascript("Hello", oldContent, "out.println(data.child.text)");
        assertJavascript("false", oldContent, "out.println(data['jcr:isCheckedOut'])");
    }


 }