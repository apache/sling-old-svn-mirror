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

import javax.servlet.http.HttpServletResponse;

/** Test creating Nodes and rendering them in JSON */
public class JsonRenderingTest extends HttpTestBase {

    private String postUrl; 
    private String testText;
    private String jsonUrl;
    private String createdNodeUrl;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // set test values
        testText = "This is a test " + System.currentTimeMillis();
        
        // create the test node, under a path that's specific to this class to allow collisions
        postUrl = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "_" + System.currentTimeMillis() + "/UJAX_create";
        final Map<String,String> props = new HashMap<String,String>();
        props.put("text", testText);
        createdNodeUrl = testClient.createNode(postUrl, props);
        jsonUrl = createdNodeUrl + ".json";
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
    
    /**  Create a node with children, verify that we get them back in JSON format */ 
    public void testRecursiveOneLevel() throws IOException {
        final Map<String,String> props = new HashMap<String,String>();
        props.put("text", testText);
        
        final String parentNodeUrl = testClient.createNode(postUrl, props);
        final String [] children = { "A", "B", "C" };
        for(String child : children) {
            props.put("child", child);
            testClient.createNode(parentNodeUrl + "/" + child, props);
        }
        
        final String json = getContent(parentNodeUrl + ".1.json", CONTENT_TYPE_JSON);
        assertJavascript(testText, json, "out.print(data.text)");
        for(String child : children) {
            assertJavascript(child, json, "out.print(data['" + child + "'].child)");
            assertJavascript(testText, json, "out.print(data['" + child + "'].text)");
        }
    }
    
    /**  Create a node with children, verify that we do not get them back in 
     *   JSON format if using recursion level=0 */ 
    public void testRecursiveZeroLevels() throws IOException {
        final Map<String,String> props = new HashMap<String,String>();
        props.put("text", testText);
        
        final String parentNodeUrl = testClient.createNode(postUrl, props);
        final String [] children = { "A", "B", "C" };
        for(String child : children) {
            props.put("child", child);
            testClient.createNode(parentNodeUrl + "/" + child, props);
        }
        
        // .json and .0.json must both return 0 levels
        final String [] extensions = { ".json", ".0.json" };
        for(String extension : extensions) {
            final String json = getContent(parentNodeUrl + extension, CONTENT_TYPE_JSON);
            assertJavascript(testText, json, "out.print(data.text)");
            for(String child : children) {
                final String testInfo = "extension: " + extension;
                assertJavascript("undefined", json, "out.print(typeof data['" + child + "'])", testInfo);
            }
        }
    }
    
    public void testInvalidLevel() throws IOException {
        assertHttpStatus(createdNodeUrl + ".notAnIntegerOnPurpose.json", HttpServletResponse.SC_BAD_REQUEST);
    }
    
    public void testEscapedStrings() throws IOException {
        final Map<String,String> props = new HashMap<String,String>();
        props.put("dq", "Some text with \"double quotes\"");
        props.put("sq", "Some text with 'single quotes'");
        props.put("cb", "Some text with {curly brackets}");
        props.put("sb", "Some text with [square brackets]");
        props.put("eol", "Some text with end\nof\nlines\nand\ttabs");
        
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