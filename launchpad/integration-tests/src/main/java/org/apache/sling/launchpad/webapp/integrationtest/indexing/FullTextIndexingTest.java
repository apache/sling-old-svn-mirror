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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.HttpTest;
import org.apache.sling.testing.tools.retry.RetryLoop;
import org.apache.sling.testing.tools.retry.RetryLoop.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
/**
 * The <tt>FullTextIndexingTest</tt> verifies that a PDF file which is uploaded will have its contents indexed and
 * available for full-text searches, in several different paths.
 * 
 */
@RunWith(Parameterized.class)
public class FullTextIndexingTest {

    private final HttpTest H = new HttpTest();

    private final String uploadPath;
    private final String fileName;
    private final String expectedText;

    @Parameters(name="{index} - {0}")
    public static Collection<Object[]> data() {
        final List<Object []> result = new ArrayList<Object []>();
        result.add(new Object[] { "lorem-ipsum.pdf", "Excepteur", "/tmp/test-" });
        result.add(new Object[] { "another.pdf", "some text that we will search for", "/var/test-" });
        result.add(new Object[] { "french.pdf", "un autre PDF pour le test fulltext", "/libs/test-" });
        return result;
    }
    
    public FullTextIndexingTest(String filename, String expectedText, String uploadPathPrefix) {
        this.fileName = filename;
        this.expectedText = expectedText;
        this.uploadPath = uploadPathPrefix + getClass().getSimpleName();
    }

    @Test
    public void testUploadedPdfIsIndexed() throws Exception {

        final String queryUrl = HttpTest.WEBDAV_BASE_URL + "/content.query.json?queryType=xpath&statement="
                + URLEncoder.encode("/jcr:root" + uploadPath 
                + "//*[jcr:contains(.,'" + expectedText + "')]", "UTF-8");

        final Condition c = new Condition() {

            public boolean isTrue() throws Exception {
                String result = H.getContent(queryUrl, HttpTest.CONTENT_TYPE_JSON);

                JSONArray results = new JSONArray(result);

                if (results.length() == 0) {
                    return false;
                }

                String expectedPath = HttpTest.SERVLET_CONTEXT + uploadPath + "/" + fileName + "/jcr:content";

                for (int i = 0; i < results.length(); i++) {
                    JSONObject object = results.getJSONObject(i);
                    if (expectedPath.equals(object.getString("jcr:path"))) {
                        return true;
                    }
                }

                return false;
            }

            public String getDescription() {
                return "A document containing '" + expectedText + "' is found under " + uploadPath;
            }
        };
        
        assertFalse("Expecting search to return nothing before upload", c.isTrue());
        
        String localPath = "/integration-test/indexing/" + fileName;
        InputStream resourceToUpload = getClass().getResourceAsStream(localPath);
        if (resourceToUpload == null)
            throw new IllegalArgumentException("No resource to upload found at " + localPath);

        H.getTestClient().mkdirs(HttpTest.WEBDAV_BASE_URL, uploadPath);
        final int status = H.getTestClient().upload(HttpTest.WEBDAV_BASE_URL + uploadPath + "/" + fileName, resourceToUpload);
        assertEquals("Upload status code", 201, status);

        // Increased the timeout to 45 seconds to avoid failures with Oak - indexes not ready??
        new RetryLoop(c, 45, 50);
    }

    @Before
    public void setUp() throws Exception {
        H.setUp();
        H.getTestClient().delete(HttpTest.WEBDAV_BASE_URL + uploadPath);
    }

    @After
    public void tearDown() throws Exception {
        H.getTestClient().delete(HttpTest.WEBDAV_BASE_URL + uploadPath);
        H.tearDown();
    }
}