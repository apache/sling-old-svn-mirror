/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.launchpad.webapp.integrationtest.indexing;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.HttpTest;
import org.apache.sling.testing.tools.retry.RetryLoop;
import org.apache.sling.testing.tools.retry.RetryLoop.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The <tt>FullTextIndexingTest</tt> verifies that a PDF file which is uploaded will have its contents indexed and
 * available for full-text searches, in several different paths.
 * 
 */
public class FullTextIndexingTest {

    private final HttpTest H = new HttpTest();

    private String folderName;
    private String fileName = "lorem-ipsum.pdf";

    @Test
    public void testUploadedPdfIsIndexed() throws IOException, JSONException {

        String localPath = "/integration-test/indexing/" + fileName;
        InputStream resourceToUpload = getClass().getResourceAsStream(localPath);
        if (resourceToUpload == null)
            throw new IllegalArgumentException("No resource to upload found at " + localPath);

        H.getTestClient().mkdir(HttpTest.WEBDAV_BASE_URL + "/" + folderName);
        H.getTestClient().upload(HttpTest.WEBDAV_BASE_URL + "/" + folderName + "/" + fileName, resourceToUpload);

        final String fullTextSearchParameter = "Excepteur";
        final String queryUrl = HttpTest.WEBDAV_BASE_URL + "/content.query.json?queryType=xpath&statement="
                + URLEncoder.encode("/jcr:root/" + folderName 
                + "//*[jcr:contains(.,'" + fullTextSearchParameter+ "')]", "UTF-8");

        new RetryLoop(new Condition() {

            public boolean isTrue() throws Exception {
                String result = H.getContent(queryUrl, HttpTest.CONTENT_TYPE_JSON);

                JSONArray results = new JSONArray(result);

                if (results.length() == 0) {
                    return false;
                }

                String expectedPath = HttpTest.SERVLET_CONTEXT + "/" + folderName + "/" + fileName + "/jcr:content";

                for (int i = 0; i < results.length(); i++) {
                    JSONObject object = results.getJSONObject(i);
                    if (expectedPath.equals(object.getString("jcr:path"))) {
                        return true;
                    }
                }

                return false;
            }

            public String getDescription() {
                return "A document containing '" + fullTextSearchParameter + "' is found under /" + folderName;
            }
        }, 10, 50);
    }

    @Before
    public void setUp() throws Exception {
        H.setUp();
        folderName = getClass().getSimpleName();
        H.getTestClient().delete(HttpTest.WEBDAV_BASE_URL + "/" + folderName);
    }

    @After
    public void tearDown() throws Exception {
        H.getTestClient().delete(HttpTest.WEBDAV_BASE_URL + "/" + folderName);
        H.tearDown();
    }
}