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

import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;

/** Test the @ValueFrom field name suffix, SLING-130 */
public class ValueFromTest extends HttpTestBase {

    private String postUrl;
    private String testText;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // set test values
        testText = "This is a test " + System.currentTimeMillis();
        
        // create the test node, under a path that's specific to this class to allow collisions
        postUrl = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "/" + System.currentTimeMillis() + SlingPostConstants.DEFAULT_CREATE_SUFFIX;
        final Map<String,String> props = new HashMap<String,String>();
        props.put("text", testText);
    }
    
    public void testWithoutValueFrom() throws IOException {
        final Map<String,String> props = new HashMap<String,String>();
        props.put("./text", testText);
        final String jsonUrl = testClient.createNode(postUrl, props) + ".json";
        final String json = getContent(jsonUrl, CONTENT_TYPE_JSON);
        assertJavascript(testText, json, "out.println(data.text)"); 
        
    }
    
    public void testWithValueFrom() throws IOException {
        final Map<String,String> props = new HashMap<String,String>();
        props.put("./text@ValueFrom", "fulltext");
        props.put("fulltext", testText);
        final String jsonUrl = testClient.createNode(postUrl, props) + ".json";
        final String json = getContent(jsonUrl, CONTENT_TYPE_JSON);
        assertJavascript("string", json, "out.println(typeof data.text)"); 
        assertJavascript(testText, json, "out.println(data.text)"); 
        assertJavascript("undefined", json, "out.println(typeof data.fulltext)"); 
    }
    
    public void testWithValueFromAndMissingField() throws IOException {
        final Map<String,String> props = new HashMap<String,String>();
        props.put("./jcr:created", "");
        props.put("./text@ValueFrom", "fulltext");

        // no fulltext field on purpose, field must be ignored
        
        final String jsonUrl = testClient.createNode(postUrl, props) + ".json";
        final String json = getContent(jsonUrl, CONTENT_TYPE_JSON);
        assertJavascript("undefined", json, "out.println(typeof data.text)"); 
        assertJavascript("undefined", json, "out.println(typeof data['text@ValueFrom'])"); 
    }
    
    
    
 }