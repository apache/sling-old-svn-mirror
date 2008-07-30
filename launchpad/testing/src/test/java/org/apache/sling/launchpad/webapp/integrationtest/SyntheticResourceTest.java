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
package org.apache.sling.launchpad.webapp.integrationtest;

import java.io.IOException;

import org.apache.sling.commons.testing.integration.HttpTestBase;


/** Test the SLING-129 {@link org.apache.sling.api.resource.SyntheticResource}, by requesting
 *  non-existent Nodes..
 */
public class SyntheticResourceTest extends HttpTestBase {

    /** recurse random URLs under basePath, going nLevels deep,
     *  and check that we get the specified status for every URL
     * @throws IOException
     */
    private void assertDeepGetStatus(String basePath, int nLevels, int expectedStatus, String urlSuffix) throws IOException {
        String path = basePath;
        for(int level=1; level <= nLevels; level++) {
            final String url = HTTP_BASE_URL + path + urlSuffix;
            assertHttpStatus(url, expectedStatus,"Unexpected status at URL=" + url);
            basePath += "/level_" + level + "_" + (int)(Math.random() * Integer.MAX_VALUE);
        }
    }

    public void TODO_FAILS_testSearchSyntheticResource() throws IOException {
        // build a very random deep URL under /search and
        // verify that we get a 200 every time
        assertDeepGetStatus("/search",10,200,"");
    }

    public void TODO_FAILS_testSearchSyntheticResourceHtml() throws IOException {
        // build a very random deep URL under /search and
        // verify that we get a 200 every time
        assertDeepGetStatus("/search",10,200,".html");
        assertDeepGetStatus("/search",10,200,".a4.print.html");
    }

    public void TODO_FAILS_testSearchSyntheticResourceJson() throws IOException {
        // build a very random deep URL under /search and
        // verify that we get a 200 every time
        assertDeepGetStatus("/search",10,200,".json");
        assertDeepGetStatus("/search",10,200,".a4.print.json");
    }

    public void TODO_FAILS_testSearchSyntheticResourceTxt() throws IOException {
        // build a very random deep URL under /search and
        // verify that we get a 200 every time
        assertDeepGetStatus("/search",10,200,".txt");
        assertDeepGetStatus("/search",10,200,".a4.print.txt");
    }

    public void testNoSyntheticResourceTest() throws IOException {
        // walk down a random path, verify that we
        // get 404s all the time
        final String basePath = "/" + getClass().getSimpleName() + "_" + System.currentTimeMillis() + "/" + (int)(Math.random() * Integer.MAX_VALUE);
        assertDeepGetStatus(basePath,10,404,"");
    }

    public void TODO_FAILS_testSyntheticResourceWithJstTemplate() throws IOException {
        final String synthResourceUrl = HTTP_BASE_URL + "/search/something.SyntheticResourceTest.html";
        {
            final String content = getContent(synthResourceUrl, CONTENT_TYPE_HTML);
            assertFalse("Content must not include JST marker before test",content.contains("JST template"));
        }

        //final String scriptPath = "/apps/" + SyntheticResource.DEFAULT_RESOURCE_TYPE + "/SyntheticResourceTest";
        final String scriptPath = "TODO";
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
        final String toDelete = uploadTestScript(scriptPath,"rendering-test.jst","html.jst");
        try {
            final String content = getContent(synthResourceUrl, CONTENT_TYPE_HTML);
            assertTrue("Content includes JST marker: " + content,content.contains("JST template"));
        } finally {
            testClient.delete(toDelete);
        }
    }


}
