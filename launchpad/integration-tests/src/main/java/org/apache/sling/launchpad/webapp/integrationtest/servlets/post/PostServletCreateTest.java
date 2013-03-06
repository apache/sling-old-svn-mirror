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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;

/** Test node creation via the SlingPostServlet */
public class PostServletCreateTest extends HttpTestBase {
    public static final String TEST_BASE_PATH = "/sling-tests";
    public static final String SLASH = "/";
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
        final String location = testClient.createNode(postUrl + SLASH, null);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));
    }

    public void testCreateNodeWithExtension() throws IOException {
        final String location = testClient.createNode(postUrl + SLASH + ".html", null);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));
    }

    public void testCreateNodeWithStarAndExtension() throws IOException {
        final String location = testClient.createNode(postUrl + SLASH + "*.html", null);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have generated name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));
    }

    public void testPostWithExtensionNoStar() throws IOException {
        final String extension = ".html";
        final String location = testClient.createNode(postUrl + extension, null);
        assertEquals(
                "Expecting the original location to be modified, instead of creating a new node",
                postUrl + extension, location);
    }

    public void testPostWithExtensionSlashNoStar() throws IOException {
        final String extension = ".html";
        final String location = testClient.createNode(postUrl + SLASH + extension, null);
        assertEquals(
                "Expecting the original location to be modified, instead of creating a new node",
                postUrl + SLASH + extension, location);
    }

    public void testCreateNodeAtSpecificUrl() throws IOException {
        final String specifiedLocation = postUrl + "/specified-location";
        final String location = testClient.createNode(specifiedLocation, null);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must be created at given URL (" + specifiedLocation + ")",
                location.equals(specifiedLocation));
    }

    public void testCreateNodeAtDeepUrl() throws IOException {
        final long id = System.currentTimeMillis();
        final String specifiedLocation = postUrl + "/specified-location" + id + "/deepA/deepB/" + id;
        final String location = testClient.createNode(specifiedLocation, null);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
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
        final String createdNodeUrl = testClient.createNode(postUrl + SLASH, props);
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
        final String createdNodeUrl = testClient.createNode(postUrl + SLASH, props);
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
        final String createdNodeUrl = testClient.createNode(postUrl + SLASH, props);
        final String content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);
        assertJavascript("123456", content, "out.println(data.a + data.b)");
        assertJavascript("undefined", content, "out.println(typeof data.c)");
    }

    /** SLING-394 removed :saveParamPrefix support. We check whether this is
     * really ignored
     */
    public void testCustomSavePrefix() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("STUFF_a","123");
        props.put("STUFF_b","456");
        props.put("c","not saved");
        props.put(":saveParamPrefix","STUFF_");
        final String createdNodeUrl = testClient.createNode(postUrl + SLASH, props,null,false);
        final String content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);
        assertJavascript("undefined", content, "out.println(typeof data.a)");
        assertJavascript("undefined", content, "out.println(typeof data.b)");
        assertJavascript("123456", content, "out.println(data.STUFF_a + data.STUFF_b)");
        assertJavascript("string", content, "out.println(typeof data.c)");
    }

    /**
     * SLING-1091: test create node with an exact node name (no filtering)
     */
    public void testCreateNodeWithExactName() throws IOException {
    	Map<String,String> nodeProperties = new HashMap<String, String>();
    	nodeProperties.put(SlingPostConstants.RP_NODE_NAME, "exactNodeName");
        final String location = testClient.createNode(postUrl + SLASH, nodeProperties);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have exact name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));
        assertTrue("Node (" + location + ") must have exact name 'exactNodeName'",
        		location.endsWith("/exactNodeName"));
    }

    /**
     * SLING-1091: test error reporting when attempting to create a node with an
     * invalid exact node name.
     */
    public void testCreateNodeWithInvalidExactName() throws IOException {
		String location = postUrl + SLASH;
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(SlingPostConstants.RP_NODE_NAME, "exactNodeName*"));
		//expect a 500 status since the name is invalid
		assertPostStatus(location, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
    }

    /**
     * SLING-1091: test error reporting when attempting to create a node with an
     * already used node name.
     */
    public void testCreateNodeWithAlreadyUsedExactName() throws IOException {
        String testNodeName = "alreadyUsedExactNodeName";

    	Map<String,String> nodeProperties = new HashMap<String, String>();
    	nodeProperties.put(SlingPostConstants.RP_NODE_NAME, testNodeName);
        final String location = testClient.createNode(postUrl + SLASH, nodeProperties);
        assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have exact name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + postUrl + ")",
                location.contains(postUrl + "/"));
        assertTrue("Node (" + location + ") must have exact name '" + testNodeName + "'",
        		location.endsWith("/" + testNodeName));

        //try to create the same node again, since same name siblings are not allowed an error should be
        // thrown
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(SlingPostConstants.RP_NODE_NAME, testNodeName));
		//expect a 500 status since the name is not unique
		assertPostStatus(postUrl + SLASH, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
    }

    public void testCreatingNodeUnderFile() throws IOException {
        final String baseWebDavUrl = WEBDAV_BASE_URL + "/CreateFileTest";
        testClient.mkdir(baseWebDavUrl);

        final String testFile = "/integration-test/testfile.txt";
        final InputStream data = getClass().getResourceAsStream(testFile);

        final String webdavUrl = baseWebDavUrl + "/" + System.currentTimeMillis() + ".txt";
        try {
            assertNotNull("Local test file " + testFile + " must be found", data);

            // Upload a file via WebDAV, verify, delete and verify
            assertHttpStatus(webdavUrl, 404, "Resource " + webdavUrl + " must not exist before test");
            int status = testClient.upload(webdavUrl, data);
            assertEquals("upload must return status code 201", 201, status);
            assertHttpStatus(webdavUrl, 200, "Resource " + webdavUrl + " must exist after upload");
        } finally {
            if (data != null) {
                data.close();
            }
        }

        final String childUrl = webdavUrl + "/*";

        List<NameValuePair> list = new ArrayList<NameValuePair>();
        list.add(new NameValuePair(":nameHint", "child"));
        list.add(new NameValuePair("prop", "value"));
        list.add(new NameValuePair("jcr:primaryType", "nt:unstructured"));

        assertPostStatus(childUrl, 500, list,
                "Response to creating a child under nt:file should fail.");
        // we shouldn't check for a specific exception as that one is implementation specific (see SLING-2763)
        // a result of 500 is enough.
    }

}