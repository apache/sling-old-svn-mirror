/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.launchpad.webapp.integrationtest.groovy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.launchpad.webapp.integrationtest.RenderingTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSONGroovyBuilderTest extends RenderingTestBase {

    /** Logger instance */
    private static final Logger log =
            LoggerFactory.getLogger(JSONGroovyBuilderTest.class);

    private String slingResourceType;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // set test values
        slingResourceType = "integration-test/srt." + System.currentTimeMillis();
        testText = "This is a test " + System.currentTimeMillis();

        // create the test node, under a path that's specific to this class to allow collisions
        final String url = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "/" + System.currentTimeMillis() + SlingPostConstants.DEFAULT_CREATE_SUFFIX;
        final Map<String,String> props = new HashMap<String,String>();
        props.put("sling:resourceType", slingResourceType);
        props.put("text", testText);
        displayUrl = testClient.createNode(url, props);

        // the rendering script goes under /apps in the repository
        scriptPath = "/apps/" + slingResourceType;
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
    }

    public void testObject() throws IOException, JSONException {
        final String toDelete = uploadTestScript("builder_object.groovy","json.groovy");
        try {
            final String content = getContent(displayUrl + ".json", CONTENT_TYPE_JSON);
            JSONObject jo = new JSONObject(content);
            assertEquals("Content contained wrong number of items", 1, jo.length());
            assertEquals("Content contained wrong key", "text", jo.keys().next());
            assertEquals("Content contained wrong data", testText, jo.get("text"));
        } finally {
            testClient.delete(toDelete);
        }
    }
    
    public void testRichObject() throws IOException, JSONException {
        final String toDelete = uploadTestScript("builder_rich_object.groovy","json.groovy");
        try {
            final String content = getContent(displayUrl + ".json", CONTENT_TYPE_JSON);
            log.debug("{} content: {}", displayUrl, content);
            JSONObject jo = new JSONObject(content);
            assertEquals("Content contained wrong number of items", 2, jo.length());
            assertEquals("Content contained wrong data", testText, jo.get("text"));
            assertEquals("Content contained wrong data", "bar", ((JSONObject) jo.get("obj")).get("foo"));
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testArray() throws IOException, JSONException {
        final String toDelete = uploadTestScript("builder_array.groovy","json.groovy");
        try {
            final String content = getContent(displayUrl + ".json", CONTENT_TYPE_JSON);
            JSONArray jo = new JSONArray(content);
            assertEquals("Content contained wrong number of items", 1, jo.length());
            assertEquals("Content contained wrong data", testText, jo.get(0));
        } finally {
            testClient.delete(toDelete);
        }
    }
}
