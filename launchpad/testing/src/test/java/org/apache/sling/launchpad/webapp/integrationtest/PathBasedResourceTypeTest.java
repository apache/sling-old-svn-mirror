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

/** Test the SLING-340 way of deriving the resource type from
 *  the content path.
 */

public class PathBasedResourceTypeTest extends RenderingTestBase {

    public static final String testPath = "sling-integration-test-" + System.currentTimeMillis();
    public static final String contentPath = "/sling-test-pbrt/" + testPath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        urlsToDelete.add(WEBDAV_BASE_URL + contentPath);
    }

    public void testDefaultResourceType() throws IOException {
        final TestNode tn = new TestNode(HTTP_BASE_URL + contentPath, null);

        // without script -> default rendering
        String content = getContent(tn.nodeUrl + ".html", CONTENT_TYPE_HTML);
        assertContains(content, "dumped by HtmlRendererServlet");

        // check default resource type
        final String scriptPath = "/apps/" + testPath;
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
        urlsToDelete.add(WEBDAV_BASE_URL + scriptPath);
        final String urlsToDelete = uploadTestScript(scriptPath, "rendering-test.esp", "html.esp");
        try {
            content = getContent(tn.nodeUrl + ".html", CONTENT_TYPE_HTML);
            assertContains(content, "ESP template");
        } finally {
            testClient.delete(urlsToDelete);
        }
    }

    public void testExplicitResourceType() throws IOException {

        final String resourceType = getClass().getSimpleName();
        final Map<String, String> properties = new HashMap<String, String>();
        properties.put(SLING_RESOURCE_TYPE, resourceType);
        final TestNode tn = new TestNode(HTTP_BASE_URL + contentPath, properties);

        urlsToDelete.add(uploadTestScript(tn.scriptPath, "rendering-test-2.esp", "html.esp"));
        final String content = getContent(tn.nodeUrl + ".html", CONTENT_TYPE_HTML);
        assertContains(content, "Template #2 for ESP tests");
    }
}
