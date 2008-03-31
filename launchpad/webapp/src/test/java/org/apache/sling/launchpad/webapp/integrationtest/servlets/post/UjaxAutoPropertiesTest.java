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
package org.apache.sling.launchpad.webapp.integrationtest.servlets.post;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.launchpad.webapp.integrationtest.HttpTestBase;
import org.apache.sling.servlets.post.impl.SlingPostServlet;

/** {#link MicrojaxPropertyValueSetter} sets the value of some properties
 *  automatically if they are empty. This is tested here with various cases.
 */  

public class UjaxAutoPropertiesTest extends HttpTestBase {

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

    public void testCreatedAndModified() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("a","123");
        
        props.put("created","");
        props.put("createdBy","");
        props.put("lastModified","");
        props.put("lastModifiedBy","");
        
        final String createdNodeUrl = testClient.createNode(postUrl + SlingPostServlet.DEFAULT_CREATE_SUFFIX, props);
        String content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);
        
        assertJavascript("123", content, "out.println(data.a)");
        assertJavascript("admin", content, "out.println(data.createdBy)");
        assertJavascript("admin", content, "out.println(data.lastModifiedBy)");
        assertJavascript("true", content, "out.println(data.created.length > 0)");
        assertJavascript("true", content, "out.println(data.lastModified.length > 0)");
        assertJavascript("true", content, "out.println(data.lastModified == data.created)");

        // update node and check that "last modified" has changed
        try {
            Thread.sleep(1000L);
        } catch(InterruptedException ignored) {
            // ignore
        }
        
        testClient.createNode(createdNodeUrl, props);
        content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);
        
        assertJavascript("123", content, "out.println(data.a)");
        assertJavascript("admin", content, "out.println(data.createdBy)");
        assertJavascript("admin", content, "out.println(data.lastModifiedBy)");
        assertJavascript("true", content, "out.println(data.created.length > 0)");
        assertJavascript("true", content, "out.println(data.lastModified.length > 0)");
        assertJavascript("true", content, "out.println(data.lastModified > data.created)");
    }
    
    public void testWithSpecificValues() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("a","123");
        
        props.put("created","a");
        props.put("createdBy","b");
        props.put("lastModified","c");
        props.put("lastModifiedBy","d");
        
        final String createdNodeUrl = testClient.createNode(postUrl + SlingPostServlet.DEFAULT_CREATE_SUFFIX, props);
        final String content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);
        
        assertJavascript("123", content, "out.println(data.a)");
        assertJavascript("a", content, "out.println(data.created)");
        assertJavascript("b", content, "out.println(data.createdBy)");
        assertJavascript("c", content, "out.println(data.lastModified)");
        assertJavascript("d", content, "out.println(data.lastModifiedBy)");
    }
}
