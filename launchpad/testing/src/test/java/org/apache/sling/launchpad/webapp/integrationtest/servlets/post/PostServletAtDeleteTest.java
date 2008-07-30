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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;

/** Test support of @Delete suffix of SLING-458 */
public class PostServletAtDeleteTest extends HttpTestBase {
    public static final String TEST_BASE_PATH = "/sling-tests";
    
    private static final String PROP_NAME = "text";
    private static final String PROP_NAME2 = "title";
    
    private static final String PROP_VALUE = "some value";
    private static final String PROP_VALUE2 = "title value";
    
    private String postUrl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + getClass().getSimpleName() + "/" + System.currentTimeMillis();

        assertHttpStatus(postUrl, HttpServletResponse.SC_NOT_FOUND,
                "Path must not exist before test: " + postUrl);
    }

    public void testDeleteOnly() throws Exception {
        final Map<String, String> props = new HashMap<String, String>();
        props.put(PROP_NAME, PROP_VALUE);
        final String contentURL = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        
        try {
            // assert property is set correctly
            final String propURL = contentURL + "/" + PROP_NAME;
            final String data = getContent(propURL + ".json", CONTENT_TYPE_JSON);
            final JSONObject json = new JSONObject(data);
            assertEquals(PROP_VALUE, json.get(PROP_NAME));
            
            final List <NameValuePair> params = new LinkedList<NameValuePair> ();
            params.add(new NameValuePair(PROP_NAME + SlingPostConstants.SUFFIX_DELETE, "don't care"));
            
            assertPostStatus(contentURL, HttpServletResponse.SC_OK, params, PROP_NAME + " must be deleted");
            assertHttpStatus(propURL, HttpServletResponse.SC_NOT_FOUND, PROP_NAME + " must be deleted");
        } finally {
            deleteNode(contentURL);
        }
    }
    
    public void testDeleteWithModify() throws Exception {
        final Map<String, String> props = new HashMap<String, String>();
        props.put(PROP_NAME, PROP_VALUE);
        final String contentURL = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);

        try {
            // assert property is set correctly
            final String propURL = contentURL + "/" + PROP_NAME;
            final String data = getContent(propURL + ".json", CONTENT_TYPE_JSON);
            final JSONObject json = new JSONObject(data);
            assertEquals(PROP_VALUE, json.get(PROP_NAME));
            
            final List <NameValuePair> params = new LinkedList<NameValuePair> ();
            params.add(new NameValuePair(PROP_NAME + SlingPostConstants.SUFFIX_DELETE, "don't care"));
            params.add(new NameValuePair(PROP_NAME2, PROP_VALUE2));
    
            assertPostStatus(contentURL, HttpServletResponse.SC_OK, params, PROP_NAME + " must be deleted");
            assertHttpStatus(propURL, HttpServletResponse.SC_NOT_FOUND, PROP_NAME + " must be deleted");
            assertHttpStatus(contentURL + "/" + PROP_NAME2, HttpServletResponse.SC_OK, PROP_NAME2 + " must exist");

            final String data2 = getContent(contentURL + ".json", CONTENT_TYPE_JSON);
            final JSONObject json2 = new JSONObject(data2);
            assertFalse(json2.has(PROP_VALUE));
            assertEquals(PROP_VALUE2, json2.get(PROP_NAME2));

        } finally {
            deleteNode(contentURL);
        }
    }
    
    protected void deleteNode(String nodeURL) throws IOException {
        // delete one and check
        final List <NameValuePair> params = new LinkedList<NameValuePair> ();
        params.add(new NameValuePair(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_DELETE));
        assertPostStatus(nodeURL, HttpServletResponse.SC_OK, params, nodeURL + " must be deleted");
    }
}