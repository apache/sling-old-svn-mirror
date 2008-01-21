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

/** Test the order option for node creation via the MicrojaxPostServlet */
public class PostServletOrderTest extends HttpTestBase {
    public static final String TEST_BASE_PATH = "/ujax-tests";
    private String postUrl;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
    }
    
   public void TODO_FAILS_testPostPathIsUnique() throws IOException {
        assertHttpStatus(postUrl, HttpServletResponse.SC_NOT_FOUND,
                "Path must not exist before test: " + postUrl);
    }

    /** Create several nodes without the order option, and check ordering */
    public void testStandardOrder() throws IOException {
        final String [] nodeUrl = new String[4];
        nodeUrl[0] = testClient.createNode(postUrl + "/UJAX_create", null);
        nodeUrl[1] = testClient.createNode(postUrl + "/UJAX_create", null);
        nodeUrl[2] = testClient.createNode(postUrl + "/UJAX_create", null);
        nodeUrl[3] = testClient.createNode(postUrl + "/UJAX_create", null);
        
        final String [] nodeName = new String[nodeUrl.length];
        for(int i = 0;  i < nodeUrl.length; i++) {
            nodeName[i] = nodeUrl[i].substring(nodeUrl[i].lastIndexOf('/') + 1);
        }

        // check that nodes appear in creation order in their parent's list of children
        final String json = getContent(postUrl + ".1.json", CONTENT_TYPE_JSON);
        for(int i = 0;  i < nodeUrl.length - 1; i++) {
            final int posA = json.indexOf(nodeName[i]);
            final int posB = json.indexOf(nodeName[i + 1]);
            if(posB <= posA) {
                fail("Expected '" + nodeName[i] + " to come before " + nodeName[i + 1] + " in JSON data '" + json + "'");
            }
        }
    }
    
    /** Create several nodes with the order option, and check ordering */
    public void testZeroOrder() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("ujax:order","0");
        
        final String [] nodeUrl = new String[4];
        nodeUrl[0] = testClient.createNode(postUrl + "/UJAX_create", props);
        nodeUrl[1] = testClient.createNode(postUrl + "/UJAX_create", props);
        nodeUrl[2] = testClient.createNode(postUrl + "/UJAX_create", props);
        nodeUrl[3] = testClient.createNode(postUrl + "/UJAX_create", props);
        
        final String [] nodeName = new String[nodeUrl.length];
        for(int i = 0;  i < nodeUrl.length; i++) {
            nodeName[i] = nodeUrl[i].substring(nodeUrl[i].lastIndexOf('/') + 1);
        }

        // check that nodes appear in reverse creation order in their parent's list of children
        final String json = getContent(postUrl + ".1.json", CONTENT_TYPE_JSON);
        for(int i = 0;  i < nodeUrl.length - 1; i++) {
            final int posA = json.indexOf(nodeName[i]);
            final int posB = json.indexOf(nodeName[i + 1]);
            if(posA <= posB) {
                fail("Expected '" + nodeName[i] + " to come after " + nodeName[i + 1] + " in JSON data '" + json + "'");
            }
        }
    }
 }