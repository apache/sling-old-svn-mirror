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
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;

/** Test node deletion via the MicrojaxPostServlet */
public class PostServletDeleteTest extends HttpTestBase {
    public static final String TEST_BASE_PATH = "/sling-tests";
    private String postUrl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + getClass().getSimpleName() + "/" + System.currentTimeMillis();

        assertHttpStatus(postUrl, HttpServletResponse.SC_NOT_FOUND,
                "Path must not exist before test: " + postUrl);
    }

    public void testDelete() throws IOException {
        final String urlA = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        final String urlB = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        final String urlC = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        final String urlD = testClient.createNode(postUrl + "/specific-location/for-delete", null);
        
        // initially all nodes must be found
        assertHttpStatus(urlA + DEFAULT_EXT, HttpServletResponse.SC_OK, "A must initially exist");
        assertHttpStatus(urlB + DEFAULT_EXT, HttpServletResponse.SC_OK, "B must initially exist");
        assertHttpStatus(urlC + DEFAULT_EXT, HttpServletResponse.SC_OK, "C must initially exist");
        assertHttpStatus(urlD + DEFAULT_EXT, HttpServletResponse.SC_OK, "D must initially exist");
        
        // delete one and check
        final List <NameValuePair> params = new LinkedList<NameValuePair> ();
        params.add(new NameValuePair(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_DELETE));
        assertPostStatus(urlA,HttpServletResponse.SC_OK,params,"Delete must return expected status (3)");
        assertHttpStatus(urlA + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "A must be deleted (1)");
        assertHttpStatus(urlB + DEFAULT_EXT, HttpServletResponse.SC_OK, "B must still exist");
        assertHttpStatus(urlC + DEFAULT_EXT, HttpServletResponse.SC_OK, "C must still exist");
        assertHttpStatus(urlD + DEFAULT_EXT, HttpServletResponse.SC_OK, "D must still exist");
        
        // delete the others with successive requests
        assertPostStatus(urlB,HttpServletResponse.SC_OK,params,"Delete must return expected status (2)");
        assertPostStatus(urlC,HttpServletResponse.SC_OK,params,"Delete must return expected status (2)");
        assertPostStatus(urlD,HttpServletResponse.SC_OK,params,"Delete must return expected status (2)");
        assertHttpStatus(urlA + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "A must be deleted (2)");
        assertHttpStatus(urlB + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "B must be deleted (2)");
        assertHttpStatus(urlC + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "C must be deleted (2)");
        assertHttpStatus(urlD + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "D must be deleted (2)");
        
        // attempting to delete non-existing nodes is ok
        assertPostStatus(postUrl,HttpServletResponse.SC_OK,params,"Delete must return expected status (2)");
    }

    public void testDeleteMultiple() throws IOException {
        final String urlA = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        final String urlB = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        final String urlC = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        final String urlD = testClient.createNode(postUrl + "/specific-location/for-delete", null);

        // initially all nodes must be found
        assertHttpStatus(urlA + DEFAULT_EXT, HttpServletResponse.SC_OK, "A must initially exist");
        assertHttpStatus(urlB + DEFAULT_EXT, HttpServletResponse.SC_OK, "B must initially exist");
        assertHttpStatus(urlC + DEFAULT_EXT, HttpServletResponse.SC_OK, "C must initially exist");
        assertHttpStatus(urlD + DEFAULT_EXT, HttpServletResponse.SC_OK, "D must initially exist");

        // delete one and check
        final List <NameValuePair> params = new LinkedList<NameValuePair> ();
        params.add(new NameValuePair(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_DELETE));
        params.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, urlA.substring(HTTP_BASE_URL.length())));
        params.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, urlB.substring(HTTP_BASE_URL.length())));
        assertPostStatus(urlC,HttpServletResponse.SC_OK,params,"Delete must return expected status (3)");
        assertHttpStatus(urlA + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "A must be deleted (1)");
        assertHttpStatus(urlB + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "B must be deleted (1)");
        assertHttpStatus(urlC + DEFAULT_EXT, HttpServletResponse.SC_OK, "C must still exist");
        assertHttpStatus(urlD + DEFAULT_EXT, HttpServletResponse.SC_OK, "D must still exist");

        // delete the others with successive requests
        params.clear();
        params.add(new NameValuePair(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_DELETE));
        params.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, urlA.substring(HTTP_BASE_URL.length())));
        params.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, urlB.substring(HTTP_BASE_URL.length())));
        params.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, urlC.substring(HTTP_BASE_URL.length())));
        params.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, urlD.substring(HTTP_BASE_URL.length())));
        assertPostStatus(urlC,HttpServletResponse.SC_OK,params,"Delete must return expected status (3)");
        assertHttpStatus(urlA + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "A must be deleted (2)");
        assertHttpStatus(urlB + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "B must be deleted (2)");
        assertHttpStatus(urlC + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "C must be deleted (2)");
        assertHttpStatus(urlD + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "D must be deleted (2)");
    }

    /**
     * Test for SLING-2415 Ability to delete child nodes, without deleting the parent node
     * Using :applyTo value of "*"
     */
    public void testDeleteAllChildren() throws IOException {
        final String urlA = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        final String urlB = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        final String urlC = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        final String urlD = testClient.createNode(postUrl + "/specific-location/for-delete", null);

        // initially all nodes must be found
        assertHttpStatus(urlA + DEFAULT_EXT, HttpServletResponse.SC_OK, "A must initially exist");
        assertHttpStatus(urlB + DEFAULT_EXT, HttpServletResponse.SC_OK, "B must initially exist");
        assertHttpStatus(urlC + DEFAULT_EXT, HttpServletResponse.SC_OK, "C must initially exist");
        assertHttpStatus(urlD + DEFAULT_EXT, HttpServletResponse.SC_OK, "D must initially exist");

        // delete and check
        final List <NameValuePair> params = new LinkedList<NameValuePair> ();
        params.add(new NameValuePair(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_DELETE));
        params.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, "*"));
        assertPostStatus(postUrl,HttpServletResponse.SC_OK,params,"Delete must return expected status");
        assertHttpStatus(urlA + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "A must be deleted");
        assertHttpStatus(urlB + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "B must be deleted");
        assertHttpStatus(urlC + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "C must be deleted");
        assertHttpStatus(urlD + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "D must be deleted");
    }
    /**
     * Test for SLING-2415 Ability to delete child nodes, without deleting the parent node
     * Using :applyTo value of "/*"
     */
    public void testDeleteAllChildrenByPath() throws IOException {
        final String urlA = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        final String urlB = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        final String urlC = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        final String urlD = testClient.createNode(postUrl + "/specific-location/for-delete", null);

        // initially all nodes must be found
        assertHttpStatus(urlA + DEFAULT_EXT, HttpServletResponse.SC_OK, "A must initially exist");
        assertHttpStatus(urlB + DEFAULT_EXT, HttpServletResponse.SC_OK, "B must initially exist");
        assertHttpStatus(urlC + DEFAULT_EXT, HttpServletResponse.SC_OK, "C must initially exist");
        assertHttpStatus(urlD + DEFAULT_EXT, HttpServletResponse.SC_OK, "D must initially exist");

        // delete and check
        final List <NameValuePair> params = new LinkedList<NameValuePair> ();
        params.add(new NameValuePair(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_DELETE));
        params.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, "/*"));
        assertPostStatus(postUrl,HttpServletResponse.SC_OK,params,"Delete must return expected status");
        assertHttpStatus(urlA + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "A must be deleted");
        assertHttpStatus(urlB + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "B must be deleted");
        assertHttpStatus(urlC + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "C must be deleted");
        assertHttpStatus(urlD + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "D must be deleted");
    }
    /**
     * Test for SLING-2415 Ability to delete child nodes of a subnode, without deleting the parent node
     * Using :applyTo value of "subnode_path/*"
     */
    public void testDeleteAllChildrenOfSubNode() throws IOException {
        final String urlA = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        final String urlB = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        final String urlC = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        final String urlD = testClient.createNode(postUrl + "/specific-location/for-delete", null);

        // initially all nodes must be found
        assertHttpStatus(urlA + DEFAULT_EXT, HttpServletResponse.SC_OK, "A must initially exist");
        assertHttpStatus(urlB + DEFAULT_EXT, HttpServletResponse.SC_OK, "B must initially exist");
        assertHttpStatus(urlC + DEFAULT_EXT, HttpServletResponse.SC_OK, "C must initially exist");
        assertHttpStatus(urlD + DEFAULT_EXT, HttpServletResponse.SC_OK, "D must initially exist");

        String testBaseUrl = HTTP_BASE_URL + TEST_BASE_PATH;
        String subPath = postUrl.substring(testBaseUrl.length() + 1);
        // delete and check
        final List <NameValuePair> params = new LinkedList<NameValuePair> ();
        params.add(new NameValuePair(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_DELETE));
        params.add(new NameValuePair(SlingPostConstants.RP_APPLY_TO, String.format("%s/*", subPath)));
        assertPostStatus(testBaseUrl,HttpServletResponse.SC_OK,params,"Delete must return expected status");
        assertHttpStatus(urlA + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "A must be deleted");
        assertHttpStatus(urlB + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "B must be deleted");
        assertHttpStatus(urlC + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "C must be deleted");
        assertHttpStatus(urlD + DEFAULT_EXT, HttpServletResponse.SC_NOT_FOUND, "D must be deleted");
    }

}