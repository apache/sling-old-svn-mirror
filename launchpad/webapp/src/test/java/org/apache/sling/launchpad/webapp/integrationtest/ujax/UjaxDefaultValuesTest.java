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
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.launchpad.webapp.integrationtest.HttpTestBase;
import org.apache.sling.servlets.post.impl.SlingPostServlet;

/** {#link UjaxPropertyValueSetter} sets the value of some properties
 *  with default values if they are empty. This is tested here with various cases.
 */

public class UjaxDefaultValuesTest extends HttpTestBase {

    public static final String TEST_BASE_PATH = "/ujax-tests";
    private String postUrl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
    }

    public void testDefaultBehaviour() throws IOException {
        final Map<String, String> props = new HashMap<String, String>();
        props.put("a","");

        final String createdNodeUrl = testClient.createNode(postUrl + SlingPostServlet.DEFAULT_CREATE_SUFFIX, props);
        String content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);

        // default behaviour writes empty string
        assertJavascript("", content, "out.println(data.a)");

        // overwrite with "123"
        props.put("a", "123");
        testClient.createNode(createdNodeUrl, props);
        content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);

        assertJavascript("123", content, "out.println(data.a)");

        // and clear again
        props.put("a", "");
        testClient.createNode(createdNodeUrl, props);
        content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);

        assertJavascript("", content, "out.println(data.a)");
    }

    public void testWithSpecificDefault() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("a","");
        props.put("a@DefaultValue","123");

        final String createdNodeUrl = testClient.createNode(postUrl + SlingPostServlet.DEFAULT_CREATE_SUFFIX, props);
        final String content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);

        assertJavascript("123", content, "out.println(data.a)");
    }

    public void testWithIgnore() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("a","");
        props.put("a@DefaultValue","ujax:ignore");

        final String createdNodeUrl = testClient.createNode(postUrl + SlingPostServlet.DEFAULT_CREATE_SUFFIX, props);
        final String content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);

        assertJavascript("undefined", content, "out.println(typeof(data.a))");
    }

    public void testWithNull() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("a","123");

        final String createdNodeUrl = testClient.createNode(postUrl + SlingPostServlet.DEFAULT_CREATE_SUFFIX, props);
        String content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);

        assertJavascript("123", content, "out.println(data.a)");

        // now try to delete prop by sending empty string
        props.put("a","");
        props.put("a@DefaultValue","ujax:null");
        testClient.createNode(createdNodeUrl, props);
        content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);

        assertJavascript("undefined", content, "out.println(typeof(data.a))");
    }
}