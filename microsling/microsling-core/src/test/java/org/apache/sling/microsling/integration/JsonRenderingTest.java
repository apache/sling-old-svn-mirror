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

    private String testText;
    private String jsonUrl;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // set test values
        testText = "This is a test " + System.currentTimeMillis();
        
        // create the test node, under a path that's specific to this class to allow collisions
        final String url = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "." + System.currentTimeMillis();
        final Map<String,String> props = new HashMap<String,String>();
        props.put("text", testText);
        jsonUrl = testClient.createNode(url, props) + ".json";
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
    
    public void TODOneedToTestMoreJsonStuff() {
        // TODO - test recursive JSON retrieval, property filters, etc...
    }
 }