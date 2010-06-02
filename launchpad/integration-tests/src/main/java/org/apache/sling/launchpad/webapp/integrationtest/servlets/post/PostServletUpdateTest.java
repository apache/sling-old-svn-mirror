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

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;

/** Test node updates via the MicrojaxPostServlet */
public class PostServletUpdateTest extends HttpTestBase {
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

    public void testUpdateWithChanges() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("./a","123");
        props.put("./b","456");

        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        String content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertJavascript("123456", content, "out.println(data.a + data.b)");

        props.put("./a","789");
        // the testClient method is called createNode but all it does is a POST,
        // so it can be used for updates as well
        final String newLocation = testClient.createNode(location, props);
        assertEquals("Location must not changed after POST to existing node",location,newLocation);
        content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertJavascript("789456", content, "out.println(data.a + data.b)");
    }

    public void testUpdateNoChanges() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("./a","123");
        props.put("./b","456");

        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        String content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertJavascript("123456", content, "out.println(data.a + data.b)");

        props.clear();
        // the testClient method is called createNode but all it does is a POST,
        // so it can be used for updates as well
        final String newLocation = testClient.createNode(location, props);
        assertEquals("Location must not changed after POST to existing node",location,newLocation);
        content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertJavascript("123456", content, "out.println(data.a + data.b)");
    }

    public void testUpdateSomeChanges() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("./a","123");
        props.put("./b","456");
        props.put("C","not stored");

        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        String content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertJavascript("123456", content, "out.println(data.a + data.b)");

        props.clear();
        props.put("./b","457");
        props.put("C","still not stored");

        // the testClient method is called createNode but all it does is a POST,
        // so it can be used for updates as well
        final String newLocation = testClient.createNode(location, props);
        assertEquals("Location must not changed after POST to existing node",location,newLocation);
        content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertJavascript("123457", content, "out.println(data.a + data.b)");
    }

    public void testMultivalueHint() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("./f","123");
        props.put("./f@TypeHint", "String[]");
        props.put("./g","456");

        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        String content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertTrue(content.indexOf("\"f\":[\"123\"]") > 0);
        assertTrue(content.indexOf("\"g\":\"456\"") > 0);
    }

    public void testMixinTypes() throws IOException, JSONException {
        
        // create a node without mixin node types
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("jcr:primaryType","nt:unstructured");
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        
        // assert no mixins
        String content = getContent(location + ".json", CONTENT_TYPE_JSON);
        JSONObject json = new JSONObject(content);
        assertFalse("jcr:mixinTypes not expected to be set", json.has("jcr:mixinTypes"));
        
        // add mixin
        props.clear();
        props.put("jcr:mixinTypes", "mix:versionable");
        testClient.createNode(location, props);
        
        content = getContent(location + ".json", CONTENT_TYPE_JSON);
        json = new JSONObject(content);
        assertTrue("jcr:mixinTypes expected after setting them", json.has("jcr:mixinTypes"));
        
        Object mixObject = json.get("jcr:mixinTypes");
        assertTrue("jcr:mixinTypes must be an array", mixObject instanceof JSONArray);
        
        JSONArray mix = (JSONArray) mixObject;
        assertTrue("jcr:mixinTypes must have a single entry", mix.length() == 1);
        assertEquals("jcr:mixinTypes must have correct value", "mix:versionable", mix.get(0));

        // remove mixin
        props.clear();
        props.put("jcr:mixinTypes@Delete", "-");
        testClient.createNode(location, props);

        content = getContent(location + ".json", CONTENT_TYPE_JSON);
        json = new JSONObject(content);
        assertTrue("no jcr:mixinTypes expected after clearing it", !json.has("jcr:mixinTypes"));
    }
}