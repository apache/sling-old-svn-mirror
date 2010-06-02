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
package org.apache.sling.launchpad.webapp.integrationtest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Test creating a Node using the MicroslingIntegrationTestClient */
public class CreateNodeTest extends HttpTestBase {

    public void testCreateNode() throws IOException {
        final String url = HTTP_BASE_URL + "/CreateNodeTest_1_" + System.currentTimeMillis();

        // add some properties to the node
        final Map<String,String> props = new HashMap<String,String>();
        props.put("name1","value1");
        props.put("name2","value2");

        // POST and get URL of created node
        String urlOfNewNode = null;
        try {
            urlOfNewNode = testClient.createNode(url, props);
        } catch(IOException ioe) {
            fail("createNode failed: " + ioe);
        }

        // get and check URL of created node
        final GetMethod get = new GetMethod(urlOfNewNode + DEFAULT_EXT);
        final int status = httpClient.executeMethod(get);
        assertEquals(urlOfNewNode + " must be accessible after createNode",200,status);
        final String responseBodyStr = get.getResponseBodyAsString();
        assertTrue(responseBodyStr.contains("value1"));
        assertTrue(responseBodyStr.contains("value2"));

        // test default txt and html renderings
        getContent(urlOfNewNode + DEFAULT_EXT, CONTENT_TYPE_PLAIN);
        getContent(urlOfNewNode + ".txt", CONTENT_TYPE_PLAIN);
        getContent(urlOfNewNode + ".html", CONTENT_TYPE_HTML);
        getContent(urlOfNewNode + ".json", CONTENT_TYPE_JSON);
        getContent(urlOfNewNode + ".xml", CONTENT_TYPE_XML);

        // And extensions for which we have no renderer fail
        assertHttpStatus(urlOfNewNode + ".pdf", 404);
        assertHttpStatus(urlOfNewNode + ".someWeirdExtension", 404);
    }

    public void testCreateNodeMultipart() throws IOException {
        final String url = HTTP_BASE_URL + "/CreateNodeTest_2_" + System.currentTimeMillis();

        // add some properties to the node
        final Map<String,String> props = new HashMap<String,String>();
        props.put("name1","value1B");
        props.put("name2","value2B");

        // POST and get URL of created node
        String urlOfNewNode = null;
        try {
            urlOfNewNode = testClient.createNode(url, props, null, true);
        } catch(IOException ioe) {
            fail("createNode failed: " + ioe);
        }

        // check node contents (not all renderings - those are tested above)
        final GetMethod get = new GetMethod(urlOfNewNode + DEFAULT_EXT);
        final int status = httpClient.executeMethod(get);
        assertEquals(urlOfNewNode + " must be accessible after createNode",200,status);
        final String responseBodyStr = get.getResponseBodyAsString();
        assertTrue(responseBodyStr.contains("value1B"));
        assertTrue(responseBodyStr.contains("value2B"));
   }

    public void testCreateNodeWithNodeType() throws IOException {
        final String url = HTTP_BASE_URL + "/CreateNodeTest_3_" + System.currentTimeMillis();

        // add node type param
        final Map<String,String> props = new HashMap<String,String>();
        props.put("jcr:primaryType","nt:folder");

        // POST and get URL of created node
        String urlOfNewNode = null;
        try {
            urlOfNewNode = testClient.createNode(url, props);
        } catch(IOException ioe) {
            fail("createNode failed: " + ioe);
        }

        String content = getContent(urlOfNewNode + ".json", CONTENT_TYPE_JSON);
        assertJavascript("nt:folder", content, "out.println(data['jcr:primaryType'])");
    }

    public void testCreateNewNodeWithNodeType() throws IOException {
        final String url = HTTP_BASE_URL + "/CreateNodeTest_4_" + System.currentTimeMillis() + "/*";

        // add node type param
        final Map<String,String> props = new HashMap<String,String>();
        props.put("jcr:primaryType","nt:folder");

        // POST and get URL of created node
        String urlOfNewNode = null;
        try {
            urlOfNewNode = testClient.createNode(url, props);
        } catch(IOException ioe) {
            fail("createNode failed: " + ioe);
        }

        String content = getContent(urlOfNewNode + ".json", CONTENT_TYPE_JSON);
        assertJavascript("nt:folder", content, "out.println(data['jcr:primaryType'])");
    }

    public void testDeepCreateNodeWithNodeType() throws IOException {
        final String url = HTTP_BASE_URL + "/CreateNodeTest_5_" + System.currentTimeMillis();

        // add node type param
        final Map<String,String> props = new HashMap<String,String>();
        props.put("jcr:primaryType","nt:folder");
        props.put("foo/jcr:primaryType","nt:folder");
        props.put("foo/bar/jcr:primaryType","nt:folder");

        // POST and get URL of created node
        String urlOfNewNode = null;
        try {
            urlOfNewNode = testClient.createNode(url, props);
        } catch(IOException ioe) {
            fail("createNode failed: " + ioe);
        }

        String content = getContent(urlOfNewNode + ".3.json", CONTENT_TYPE_JSON);
        assertJavascript("nt:folder", content, "out.println(data['jcr:primaryType'])");
        assertJavascript("nt:folder", content, "out.println(data.foo['jcr:primaryType'])");
        assertJavascript("nt:folder", content, "out.println(data.foo.bar['jcr:primaryType'])");
    }

    public void testCreateEmptyNode() throws IOException {
        final String url = HTTP_BASE_URL + "/CreateNodeTest_6_" + System.currentTimeMillis();

        // add node type param
        final Map<String,String> props = new HashMap<String,String>();

        // POST and get URL of created node
        String urlOfNewNode = null;
        try {
            urlOfNewNode = testClient.createNode(url, props);
        } catch(IOException ioe) {
            fail("createNode failed: " + ioe);
        }

        // get and check URL of created node
        final GetMethod get = new GetMethod(urlOfNewNode + DEFAULT_EXT);
        final int status = httpClient.executeMethod(get);
        assertEquals(urlOfNewNode + " must be accessible after createNode",200,status);
    }
}