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
package org.apache.sling.usling.webapp.integrationtest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/** Test the various redirect options for POST, SLING-126 */
public class PostRedirectTest extends UslingHttpTestBase {

    private String postPath = "CreateNodeTest/" + System.currentTimeMillis();
    private String postUrl = HTTP_BASE_URL + "/" + postPath + "/UJAX_create";
    
    public void testForcedRedirect() throws IOException {
        final Map<String,String> params = new HashMap<String,String>();
        params.put("ujax:redirect","http://forced/");
        final Map<String,String> headers = new HashMap<String,String>();
        headers.put("Referer", "http://referer/");
        
        final String location = testClient.createNode(postUrl, params, headers, false);
        assertEquals("With forced redirect and Referer, redirect must be forced","http://forced/",location);
    }
    
    public void testDefaultRedirect() throws IOException {
        final String location = testClient.createNode(postUrl, null, null, false);
        assertTrue(
                "With no headers or parameters, redirect (" + location 
                + ") must point to created node (path=" + postPath + ")",
                location.contains(postPath));
    }
    
    public void testRefererRedirect() throws IOException {
        final Map<String,String> headers = new HashMap<String,String>();
        headers.put("Referer", "http://referer/");
        final String location = testClient.createNode(postUrl, null, headers, true);
        assertEquals("With Referer, redirect must point to referer","http://referer/",location);
    }
    
    public void testMagicStarRedirect() throws IOException {
        final Map<String,String> params = new HashMap<String,String>();
        params.put("ujax:redirect","*");
        final Map<String,String> headers = new HashMap<String,String>();
        headers.put("Referer", "http://referer/");
        final String location = testClient.createNode(postUrl, params, headers, false);
        assertTrue(
                "With magic star, redirect (" + location 
                + ") must point to created node (path=" + postPath + ")",
                location.contains(postPath));
    }
}