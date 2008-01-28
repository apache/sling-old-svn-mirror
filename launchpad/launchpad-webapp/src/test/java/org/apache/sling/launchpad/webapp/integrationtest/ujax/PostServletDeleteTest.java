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
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.sling.launchpad.webapp.integrationtest.HttpTestBase;
import org.apache.sling.ujax.UjaxPostServlet;

/** Test node deletion via the MicrojaxPostServlet */
public class PostServletDeleteTest extends HttpTestBase {
    public static final String TEST_BASE_PATH = "/ujax-tests";
    private String postUrl;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + getClass().getSimpleName() + "/" + System.currentTimeMillis();
        
        assertHttpStatus(postUrl, HttpServletResponse.SC_NOT_FOUND,
                "Path must not exist before test: " + postUrl);
    }

    public void testDelete() throws IOException {
        final String urlA = testClient.createNode(postUrl + UjaxPostServlet.DEFAULT_CREATE_SUFFIX, null);
        final String urlB = testClient.createNode(postUrl + UjaxPostServlet.DEFAULT_CREATE_SUFFIX, null);
        final String urlC = testClient.createNode(postUrl + UjaxPostServlet.DEFAULT_CREATE_SUFFIX, null);
        final String urlD = testClient.createNode(postUrl + "/specific-location/for-delete", null);
        
        // initially all nodes must be found
        assertHttpStatus(urlA, HttpServletResponse.SC_OK, "A must initially exist");
        assertHttpStatus(urlB, HttpServletResponse.SC_OK, "B must initially exist");
        assertHttpStatus(urlC, HttpServletResponse.SC_OK, "C must initially exist");
        assertHttpStatus(urlD, HttpServletResponse.SC_OK, "D must initially exist");
        
        // delete one and check
        final List <NameValuePair> params = new LinkedList<NameValuePair> ();
        final String deleteCmd = "ujax:delete";
        params.add(new NameValuePair(deleteCmd,urlToNodePath(urlA)));
        assertPostStatus(postUrl,HttpServletResponse.SC_MOVED_TEMPORARILY,params,"Delete must return expected status (3)");
        assertHttpStatus(urlA, HttpServletResponse.SC_NOT_FOUND, "A must be deleted (1)");
        assertHttpStatus(urlB, HttpServletResponse.SC_OK, "B must still exist");
        assertHttpStatus(urlC, HttpServletResponse.SC_OK, "C must still exist");
        assertHttpStatus(urlD, HttpServletResponse.SC_OK, "D must still exist");
        
        // delete the others with one request and check
        params.clear();
        params.add(new NameValuePair(deleteCmd,urlToNodePath(urlB)));
        params.add(new NameValuePair(deleteCmd,urlToNodePath(urlC)));
        params.add(new NameValuePair(deleteCmd,urlToNodePath(urlD)));
        assertPostStatus(postUrl,HttpServletResponse.SC_MOVED_TEMPORARILY,params,"Delete must return expected status (2)");
        assertHttpStatus(urlA, HttpServletResponse.SC_NOT_FOUND, "A must be deleted (2)");
        assertHttpStatus(urlB, HttpServletResponse.SC_NOT_FOUND, "B must be deleted (2)");
        assertHttpStatus(urlC, HttpServletResponse.SC_NOT_FOUND, "C must be deleted (2)");
        assertHttpStatus(urlD, HttpServletResponse.SC_NOT_FOUND, "D must be deleted (2)");
        
        // attempting to delete non-existing nodes is ok
        assertPostStatus(postUrl,HttpServletResponse.SC_MOVED_TEMPORARILY,params,"Delete must return expected status (2)");
    }
    
    private String urlToNodePath(String url) {
        return url.substring(HTTP_BASE_URL.length());
    }
}