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

import org.apache.sling.servlets.post.SlingPostConstants;

/** Test the rendering of JCR Properties, directly addressed by URLs.
 *  See SLING-133
 */
public class PropertyRenderingTest extends RenderingTestBase {

    private String slingResourceType;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // set test values
        testText = "This is a test " + System.currentTimeMillis();
        slingResourceType = getClass().getName();

        // create the test node, under a path that's specific to this class to allow collisions
        final String url = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "/" + System.currentTimeMillis() + SlingPostConstants.DEFAULT_CREATE_SUFFIX;
        final Map<String,String> props = new HashMap<String,String>();
        props.put("sling:resourceType", slingResourceType);
        props.put("text", testText);
        displayUrl = testClient.createNode(url, props);
    }

    public void testNodeAccess() throws IOException {
        final String json = getContent(displayUrl + ".json", CONTENT_TYPE_JSON);
        assertJavascript(testText, json, "out.println(data.text)");
    }
    
    public void testTextJson() throws IOException {
        final String json = getContent(displayUrl + "/text.json", CONTENT_TYPE_JSON);
        assertEquals("{\"text\":\"" + testText + "\"}",json);
    }

    public void testTextHtml() throws IOException {
        final String data = getContent(displayUrl + "/text.html", CONTENT_TYPE_HTML);
        assertEquals(testText, data);
    }
    
    public void testTextTxt() throws IOException {
        final String data = getContent(displayUrl + "/text.txt", CONTENT_TYPE_PLAIN);
        assertEquals(testText, data);
    }
    
    public void testTextNoExt() throws IOException {
        final String data = getContent(displayUrl + "/text", null);
        assertEquals(testText, data);
    }
    
    public void testResourceTypeNoExt() throws IOException {
        final String data = getContent(displayUrl + "/sling:resourceType", null);
        assertEquals(slingResourceType, data);
    }
}