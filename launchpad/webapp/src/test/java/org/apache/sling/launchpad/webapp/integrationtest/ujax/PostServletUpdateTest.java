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
package org.apache.sling.launchpad.webapp.integrationtest.ujax;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.launchpad.webapp.integrationtest.HttpTestBase;
import org.apache.sling.servlets.post.impl.SlingPostServlet;

/** Test node updates via the MicrojaxPostServlet */
public class PostServletUpdateTest extends HttpTestBase {
    public static final String TEST_BASE_PATH = "/ujax-tests";
    private String postUrl;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
    }
    
   public void testPostPathIsUnique() throws IOException {
        assertHttpStatus(postUrl, HttpServletResponse.SC_NOT_FOUND,
                "Path must not exist before test: " + postUrl);
    }

    public void testUpdateWithChanges() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("./a","123");
        props.put("./b","456");
        
        final String location = testClient.createNode(postUrl + SlingPostServlet.DEFAULT_CREATE_SUFFIX, props);
        String content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertJavascript("123456", content, "out.println(data.a + data.b)");
        
        props.put("./a","789");
        // the testClient method is called createNode but all it does is a POST,
        // so it can be used for updates as well
        final String newLocation = testClient.createNode(location, props);
        assertEquals("Location must not changed after POST to existing node",location,newLocation);
        content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertJavascript("789456", content, "out.println(data.a + data.b)");
    }
    
    public void testUpdateNoChanges() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("./a","123");
        props.put("./b","456");
        
        final String location = testClient.createNode(postUrl + SlingPostServlet.DEFAULT_CREATE_SUFFIX, props);
        String content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertJavascript("123456", content, "out.println(data.a + data.b)");
        
        props.clear();
        // the testClient method is called createNode but all it does is a POST,
        // so it can be used for updates as well
        final String newLocation = testClient.createNode(location, props);
        assertEquals("Location must not changed after POST to existing node",location,newLocation);
        content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertJavascript("123456", content, "out.println(data.a + data.b)");
    }
    
    public void testUpdateSomeChanges() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("./a","123");
        props.put("./b","456");
        props.put("C","not stored");
        
        final String location = testClient.createNode(postUrl + SlingPostServlet.DEFAULT_CREATE_SUFFIX, props);
        String content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertJavascript("123456", content, "out.println(data.a + data.b)");
        
        props.clear();
        props.put("./b","457");
        props.put("C","still not stored");
        
        // the testClient method is called createNode but all it does is a POST,
        // so it can be used for updates as well
        final String newLocation = testClient.createNode(location, props);
        assertEquals("Location must not changed after POST to existing node",location,newLocation);
        content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertJavascript("123457", content, "out.println(data.a + data.b)");
    }
    
 }