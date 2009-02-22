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

/** A GET to *.html and *.json must work even if there is no Node
 *  at the specified path (SLING-344)
 */
public class GetStarTest extends RenderingTestBase {
    private final String random = getClass().getSimpleName() + String.valueOf(System.currentTimeMillis());


    public void testGetStarWithScript() throws IOException {
        final String scriptPath = "/apps/" + random;
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
        urlsToDelete.add(WEBDAV_BASE_URL + scriptPath);
        final String fakeNodePath = HTTP_BASE_URL + "/sling-test-pbrt/" + random + "/something/*.html";

        {
            final String content = getContent(fakeNodePath, CONTENT_TYPE_HTML);
            assertContains(content, "dumped by HtmlRendererServlet");
        }

        final String urlToDelete = uploadTestScript(scriptPath, "rendering-test.esp", "html.esp");
        try {
            final String content = getContent(fakeNodePath, CONTENT_TYPE_HTML);
            assertContains(content, "ESP template");
        } finally {
            testClient.delete(urlToDelete);
        }

    }

}
