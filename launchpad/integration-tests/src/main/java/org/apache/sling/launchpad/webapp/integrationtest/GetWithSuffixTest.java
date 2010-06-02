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
package org.apache.sling.launchpad.webapp.integrationtest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/** GET requests with a suffix should fail with a 404, otherwise
 *  we get a lot of extra possible URLs which point to the same
 *  content.
 */
public class GetWithSuffixTest extends RenderingTestBase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // set test values
        testText = "This is a test " + System.currentTimeMillis();

        // create the test node, under a path that's specific to this class to allow collisions
        final String url = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "_" + System.currentTimeMillis();
        final Map<String,String> props = new HashMap<String,String>();
        props.put("text", testText);
        displayUrl = testClient.createNode(url, props);

        // the rendering script goes under /apps in the repository
        scriptPath = "/apps/nt/unstructured";
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
    }

    public void testWithExactUrl() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.esp","html.esp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertTrue("Content includes ESP marker",content.contains("ESP template"));
            assertTrue("Content contains formatted test text",content.contains("<p>" + testText + "</p>"));
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testGETScript() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.esp","GET.esp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertTrue("Content includes ESP marker",content.contains("ESP template"));
            assertTrue("Content contains formatted test text",content.contains("<p>" + testText + "</p>"));

            final String content2 = getContent(displayUrl + ".txt", CONTENT_TYPE_PLAIN);
            assertTrue("Content includes ESP marker",content2.contains("ESP template"));
            assertTrue("Content contains formatted test text",content2.contains("<p>" + testText + "</p>"));
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testWithExtraPathA() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.esp","txt.esp");
        try {
            // expected to be found as resource with ext .txt and suffix
            final String content = getContent(displayUrl + ".txt/extra.html", CONTENT_TYPE_PLAIN);
            assertTrue("Content includes ESP marker",content.contains("ESP template"));
            assertTrue("Content contains formatted test text",content.contains("<p>" + testText + "</p>"));
        } finally {
            testClient.delete(toDelete);
        }
    }

    /** behavior seems slightly different if using GET.esp vs. html.esp for the
     *  script name, verify that both give a 404
     */
    public void testWithExtraPathB() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.esp","GET.esp");
        try {
            // expected to not be found as dispalyUrl has no dots
            assertHttpStatus(displayUrl + "/extra/more.a4.html", HttpServletResponse.SC_NOT_FOUND);
        } finally {
            testClient.delete(toDelete);
        }
    }
}
