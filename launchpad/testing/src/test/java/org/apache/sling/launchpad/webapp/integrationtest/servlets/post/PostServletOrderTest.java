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

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Test the order option for node creation via the MicrojaxPostServlet */
public class PostServletOrderTest extends HttpTestBase {

    public static final String TEST_BASE_PATH = "/sling-tests-order";

    private static final String[] DEFAULT_ORDER = new String[]{"a","b","c","d"};

    /*
        does not work (yet) since rhino does not preserve order of
        object elements.

    private static final String TEST_SCRIPT =
            "var s=''; " +
            "for (var a in data) {" +
            "   var n = data[a];" +
            "   if (typeof(n) == 'object') s += a + ',';" +
            "}" +
            "out.println(s);";
     */

    /**
     * Create nodes and check if they are in default order
     */
    public void testStandardOrder() throws IOException {
        final String postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
        createNodes(postUrl, DEFAULT_ORDER);
        verifyOrder(postUrl, DEFAULT_ORDER);
    }

    /**
     * Create nodes and check if they are in correct order after a
     * :order="first" request
     */
    public void testOrderFirst() throws IOException {
        final String postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
        createNodes(postUrl, DEFAULT_ORDER);

        final Map <String, String> props = new HashMap <String, String> ();
        props.put(":order","first");
        testClient.createNode(postUrl + "/c", props);
        verifyOrder(postUrl, new String[]{"c", "a", "b", "d"});
    }

    /**
     * Create nodes and check if they are in correct order after a
     * :order="last" request
     */
    public void testOrderLast() throws IOException {
        final String postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
        createNodes(postUrl, DEFAULT_ORDER);

        final Map <String, String> props = new HashMap <String, String> ();
        props.put(":order","last");
        testClient.createNode(postUrl + "/c", props);
        verifyOrder(postUrl, new String[]{"a", "b", "d", "c"});
    }

    /**
     * Create nodes and check if they are in correct order after a
     * :order="before" request
     */
    public void testOrderBefore() throws IOException {
        final String postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
        createNodes(postUrl, DEFAULT_ORDER);

        final Map <String, String> props = new HashMap <String, String> ();
        props.put(":order","before b");
        testClient.createNode(postUrl + "/c", props);
        verifyOrder(postUrl, new String[]{"a", "c", "b", "d"});
    }

    /**
     * Create nodes and check if they are in correct order after a
     * :order="after" request
     */
    public void testOrderAfter() throws IOException {
        final String postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
        createNodes(postUrl, DEFAULT_ORDER);

        final Map <String, String> props = new HashMap <String, String> ();
        props.put(":order","after c");
        testClient.createNode(postUrl + "/b", props);
        verifyOrder(postUrl, new String[]{"a", "c", "b", "d"});
    }

    /**
     * Create nodes and check if they are in correct order after a
     * :order="N" request, where new position is greater than old one.
     */
    public void testOrderIntToBack() throws IOException {
        final String postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
        createNodes(postUrl, DEFAULT_ORDER);

        final Map <String, String> props = new HashMap <String, String> ();
        props.put(":order","2");
        testClient.createNode(postUrl + "/a", props);
        verifyOrder(postUrl, new String[]{"b", "c", "a", "d"});
    }

    /**
     * Create nodes and check if they are in correct order after a
     * :order="N" request, where new position is less than old one.
     */
    public void testOrderIntToFront() throws IOException {
        final String postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
        createNodes(postUrl, DEFAULT_ORDER);

        final Map <String, String> props = new HashMap <String, String> ();
        props.put(":order","1");
        testClient.createNode(postUrl + "/d", props);
        verifyOrder(postUrl, new String[]{"a", "d", "b", "c"});
    }

    /**
     * Create nodes and check if they are in correct order after a
     * :order="0" request
     */
    public void testOrderIntZero() throws IOException {
        final String postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
        createNodes(postUrl, DEFAULT_ORDER);

        final Map <String, String> props = new HashMap <String, String> ();
        props.put(":order","0");
        testClient.createNode(postUrl + "/d", props);
        verifyOrder(postUrl, new String[]{"d", "a", "b", "c"});
    }

    /**
     * Create nodes and check if they are in correct order after a
     * :order="N" request, where new position is out of bounds
     */
    public void testOrderIntOOB() throws IOException {
        final String postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
        createNodes(postUrl, DEFAULT_ORDER);

        final Map <String, String> props = new HashMap <String, String> ();
        props.put(":order","100");
        testClient.createNode(postUrl + "/a", props);
        verifyOrder(postUrl, new String[]{"b", "c", "d", "a"});
    }

    /**
     * Create test nodes
     */
    private String[] createNodes(String parentUrl, String[] names)
            throws IOException {
        String[] urls = new String[names.length];
        for (int i=0; i<names.length; i++) {
            urls[i] = testClient.createNode(parentUrl + "/" + names[i], null);
        }
        return urls;
    }

    /**
     * Verify node order
     */
    private void verifyOrder(String parentUrl, String[] names)
            throws IOException {
        // check that nodes appear in creation order in their parent's list of children
        final String content = getContent(parentUrl + ".1.json", CONTENT_TYPE_JSON);
        String expected = "";
        for (String n: names) {
            expected +=n + ",";
        }
        //assertJavascript(expected, content, TEST_SCRIPT);
        try {
            String actual = "";
            JSONObject obj = new JSONObject(content);
            JSONArray n = obj.names();
            for (int i=0; i<n.length(); i++) {
                String name = n.getString(i);
                Object o = obj.get(name);
                if (o instanceof JSONObject) {
                    actual += name + ",";
                }
            }
            assertEquals(expected, actual);
        } catch (JSONException e) {
            throw new IOException(e.toString());
        }
    }
 }