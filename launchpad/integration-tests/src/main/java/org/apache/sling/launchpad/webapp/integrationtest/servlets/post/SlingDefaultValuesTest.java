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

import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.commons.testing.integration.NameValuePairList;
import org.apache.sling.servlets.post.SlingPostConstants;

/** {#link SlingPropertyValueSetter} sets the value of some properties
 *  with default values if they are empty. This is tested here with various cases.
 */

public class SlingDefaultValuesTest extends HttpTestBase {

    public static final String TEST_BASE_PATH = "/sling-tests";
    private String postUrl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
    }

    public void testDefaultBehaviour() throws IOException {
        final Map<String, String> props = new HashMap<String, String>();
        props.put("a","");

        final String createdNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        String content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);

        // default behaviour writes empty string
        assertJavascript("undefined", content, "out.println(\"\" + data.a)");

        // overwrite with "123"
        props.put("a", "123");
        testClient.createNode(createdNodeUrl, props);
        content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);

        assertJavascript("123", content, "out.println(data.a)");

        // and clear again
        props.put("a", "");
        testClient.createNode(createdNodeUrl, props);
        content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);

        assertJavascript("undefined", content, "out.println(\"\" + data.a)");

        // check array
        NameValuePairList params = new NameValuePairList();
        params.add("x", "1");
        params.add("x", "2");
        params.add("x", "3");

        testClient.createNode(createdNodeUrl, params, null, false);
        content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);

        assertJavascript("123", content, "out.println(data.x)");

        // check array with empty value
        params = new NameValuePairList();
        params.add("x", "1");
        params.add("x", "");
        params.add("x", "3");

        testClient.createNode(createdNodeUrl, params, null, false);
        content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);

        assertJavascript("3.0", content, "out.println(data.x.length)");
        assertJavascript("1", content, "out.println(data.x[0])");
        assertJavascript("", content, "out.println(data.x[1])");
        assertJavascript("3", content, "out.println(data.x[2])");

        // check array with empty value and ignore blanks
        params = new NameValuePairList();
        params.add("x", "1");
        params.add("x", "");
        params.add("x", "3");
        params.add("x@IgnoreBlanks", "true");

        testClient.createNode(createdNodeUrl, params, null, false);
        content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);

        assertJavascript("2.0", content, "out.println(data.x.length)");
        assertJavascript("1", content, "out.println(data.x[0])");
        assertJavascript("3", content, "out.println(data.x[1])");
    }

    public void testWithSpecificDefault() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("a","");
        props.put("a@DefaultValue","123");

        final String createdNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        final String content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);

        assertJavascript("123", content, "out.println(data.a)");
    }

    public void testWithSpecificDefaultAndNoValueField() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("a@DefaultValue","123");
        props.put("a@UseDefaultWhenMissing","yes");

        final String createdNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        final String content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);

        assertJavascript("123", content, "out.println(data.a)");
    }

    public void testWithIgnore() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("a","");
        props.put("a@DefaultValue",":ignore");

        final String createdNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        final String content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);

        assertJavascript("undefined", content, "out.println(typeof(data.a))");
    }

    public void testWithNull() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("a","123");

        final String createdNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        String content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);

        assertJavascript("123", content, "out.println(data.a)");

        // now try to delete prop by sending empty string
        props.put("a","");
        props.put("a@DefaultValue",":null");
        testClient.createNode(createdNodeUrl, props);
        content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);

        assertJavascript("undefined", content, "out.println(typeof(data.a))");
    }
}