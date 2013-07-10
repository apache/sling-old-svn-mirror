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
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.testing.tools.retry.RetryLoop;
import org.apache.sling.testing.tools.retry.RetryLoop.Condition;

/**
 * The <tt>FullTextIndexingTest</tt> verifies that a PDF file which is uploaded will have its contents indexed and
 * available for full-text searches
 * 
 */
public class FullTextIndexingTest extends HttpTestBase {

    private String folderName;
    private String fileName = "lorem-ipsum.pdf";

    public void testUploadedPdfIsIndexed() throws IOException, JSONException {

        String localPath = "/integration-test/indexing/" + fileName;
        InputStream resourceToUpload = getClass().getResourceAsStream(localPath);
        if (resourceToUpload == null)
            throw new IllegalArgumentException("No resource to upload found at " + localPath);

        testClient.mkdir(WEBDAV_BASE_URL + "/" + folderName);
        testClient.upload(WEBDAV_BASE_URL + "/" + folderName + "/" + fileName, resourceToUpload);

        final String fullTextSearchParameter = "Excepteur";
        final String queryUrl = WEBDAV_BASE_URL + "/content.query.json?queryType=xpath&statement="
                + URLEncoder.encode("/jcr:root/" + folderName 
                + "//*[jcr:contains(.,'" + fullTextSearchParameter+ "')]", "UTF-8");

        new RetryLoop(new Condition() {

            public boolean isTrue() throws Exception {
                String result = getContent(queryUrl, CONTENT_TYPE_JSON);

                JSONArray results = new JSONArray(result);

                if (results.length() == 0) {
                    return false;
                }

                String expectedPath = SERVLET_CONTEXT + "/" + folderName + "/" + fileName + "/jcr:content";

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

    protected void setUp() throws Exception {
        super.setUp();

        folderName = getClass().getSimpleName();
        testClient.delete(WEBDAV_BASE_URL + "/" + folderName);
    }

    protected void tearDown() throws Exception {

        testClient.delete(WEBDAV_BASE_URL + "/" + folderName);
        super.tearDown();
    }
}
