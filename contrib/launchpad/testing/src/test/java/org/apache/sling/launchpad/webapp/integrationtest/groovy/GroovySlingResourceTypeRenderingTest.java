/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.launchpad.webapp.integrationtest.groovy;

import java.io.IOException;

import org.apache.sling.launchpad.webapp.integrationtest.AbstractSlingResourceTypeRenderingTest;

public class GroovySlingResourceTypeRenderingTest extends AbstractSlingResourceTypeRenderingTest {

    public void testGspJavaCode() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.gsp","html.gsp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertContains(content, "GSP template");
            assertContains(content, "TestLinkedListTest");
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testGspHtml() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.gsp","html.gsp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertContains(content, "GSP template");
            assertContains(content, "<p>" + testText + "</p>");
            assertContains(content, "<div class=\"SLING-142\" id=\"22\"/>");
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testGspHtmlInAppsFolder() throws IOException {
        // make sure there's no leftover rendering script
        {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertFalse("Content must not contain script marker before testing", content.contains("GSP template"));
        }

        // put our script under /apps/<resource type>
        final String path = "/apps/" + slingResourceType;
        testClient.mkdirs(WEBDAV_BASE_URL, path);
        final String toDelete = uploadTestScript(path,"rendering-test.gsp","html.gsp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertContains(content, "GSP template");
            assertContains(content, "<p>" + testText + "</p>");
        } finally {
            testClient.delete(toDelete);
        }
    }
}
