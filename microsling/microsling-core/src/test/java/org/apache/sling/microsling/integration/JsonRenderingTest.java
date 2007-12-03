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
package org.apache.sling.microsling.integration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Test creating Nodes and rendering them in JSON */
public class JsonRenderingTest extends MicroslingHttpTestBase {

    private String postUrl; 
    private String testText;
    private String jsonUrl;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // set test values
        testText = "This is a test " + System.currentTimeMillis();
        
        // create the test node, under a path that's specific to this class to allow collisions
        postUrl = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "_" + System.currentTimeMillis() + "/*";
        final Map<String,String> props = new HashMap<String,String>();
        props.put("text", testText);
        jsonUrl = testClient.createNode(postUrl, props) + ".json";
    }
    
    /** test our assertJavascript method with static json */ 
    public void testAssertJavascript() throws IOException {
        final String json = "{ 'a' : '123', 'b' : '456' }";
        assertJavascript("123456", json ,"out.println(data.a + data.b)");
    }
    
    public void testNonRecursive() throws IOException {
        final String json = getContent(jsonUrl, CONTENT_TYPE_JSON);
        assertJavascript(testText, json ,"out.println(data.text)");
    }
    
    public void testEscapedStrings() throws IOException {
        final Map<String,String> props = new HashMap<String,String>();
        props.put("dq", "Some text with \"double quotes\"");
        props.put("sq", "Some text with 'single quotes'");
        props.put("cb", "Some text with {curly brackets}");
        props.put("sb", "Some text with [square brackets]");
        
        final String location = testClient.createNode(postUrl, props);
        final String json = getContent(location + ".json", CONTENT_TYPE_JSON);
        
        for(String key : props.keySet()) {
            assertJavascript(props.get(key),json,"out.println(data." + key + ")");
        }
    }
    
    public void testAccentedStrings() throws IOException {
        final Map<String,String> props = new HashMap<String,String>();
        props.put("a", "Les amis en \u000C9t\u000C9 au ch\u000Eteau");
        props.put("b", "The \u000B0 degree sign and \u000F5 ntilde");
        
        final String location = testClient.createNode(postUrl, props);
        final String json = getContent(location + ".json", CONTENT_TYPE_JSON);
        
        for(String key : props.keySet()) {
            assertJavascript(props.get(key),json,"out.println(data." + key + ")");
        }
    }
 }