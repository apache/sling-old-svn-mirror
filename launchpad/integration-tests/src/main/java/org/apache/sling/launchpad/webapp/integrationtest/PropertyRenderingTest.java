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

import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.NameValuePairList;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Test the rendering of JCR Properties, directly addressed by URLs.
 *  See SLING-133
 */
public class PropertyRenderingTest extends RenderingTestBase {

    /** Logger instance */
    private static final Logger log =
            LoggerFactory.getLogger(PropertyRenderingTest.class);

    private String slingResourceType;

    private String testMultiText1;
    private String testMultiText2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // set test values
        testText = "This is a test " + System.currentTimeMillis();
        testMultiText1 = "This is a multivalued test " + System.currentTimeMillis();
        testMultiText2 = "This is another multivalued test " + System.currentTimeMillis();

        slingResourceType = getClass().getName();

        // create the test node, under a path that's specific to this class to allow collisions
        final String url = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "/" + System.currentTimeMillis() + SlingPostConstants.DEFAULT_CREATE_SUFFIX;

        NameValuePairList list = new NameValuePairList();
        list.add("sling:resourceType", slingResourceType);
        list.add("text", testText);
        list.add("multiText", testMultiText1);
        list.add("multiText", testMultiText2);
        displayUrl = testClient.createNode(url, list, null, true);
    }

    public void testNodeAccess() throws IOException {
        final String json = getContent(displayUrl + ".json", CONTENT_TYPE_JSON);
        assertJavascript(testText, json, "out.println(data.text)");
        assertJavascript(testMultiText1, json, "out.println(data.multiText[0])");
        assertJavascript(testMultiText2, json, "out.println(data.multiText[1])");
    }

    public void testTextJson() throws Exception {
        final String json = getContent(displayUrl + "/text.json", CONTENT_TYPE_JSON);
        final JSONObject obj = new JSONObject(json);
        assertEquals(testText, obj.get("text"));
    }

    public void testTextHtml() throws IOException {
        final String data = getContent(displayUrl + "/text.html", CONTENT_TYPE_HTML);
        assertTrue(data.contains(testText));
    }

    public void testTextTxt() throws IOException {
        final String data = getContent(displayUrl + "/text.txt", CONTENT_TYPE_PLAIN);
        assertEquals(testText, data);
    }

    public void testTextNoExt() throws IOException {
        final String data = getContent(displayUrl + "/text", CONTENT_TYPE_PLAIN);
        assertEquals(testText, data);
    }

    public void testMultiValuedTextJson() throws Exception {
        final String json = getContent(displayUrl + "/multiText.json", CONTENT_TYPE_JSON);
        final JSONObject obj = new JSONObject(json);
        assertEquals("[\"" + testMultiText1 + "\",\""+ testMultiText2 + "\"]", obj.get("multiText").toString());
    }

    public void testMultiValuedTextHtml() throws IOException {
        final String data = getContent(displayUrl + "/multiText.html", CONTENT_TYPE_HTML);
        log.debug("multiText.html content: {}", data);
        assertTrue(data.contains(testMultiText1));
        assertTrue(data.contains(testMultiText2));
    }

    public void testMultiValuedTextTxt() throws IOException {
        final String data = getContent(displayUrl + "/multiText.txt", CONTENT_TYPE_PLAIN);
        assertEquals("[" + testMultiText1 + ", " + testMultiText2 + "]", data);
    }

    public void testMultiValuedTextNoExt() throws IOException {
        // multi-valued properties can't be adapted to a stream, so this returns an error
        assertHttpStatus(displayUrl + "/multiText", 403);
    }

    public void testResourceTypeNoExt() throws IOException {
        final String data = getContent(displayUrl + "/sling:resourceType", CONTENT_TYPE_PLAIN);
        assertEquals(slingResourceType, data);
    }
}