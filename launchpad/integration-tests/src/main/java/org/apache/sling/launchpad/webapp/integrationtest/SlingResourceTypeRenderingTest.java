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

/** Test creating a Node and rendering it using scripts in
 *  various supported languages, using slingResourceType-based
 *  script resolution
 */
public class SlingResourceTypeRenderingTest extends AbstractSlingResourceTypeRenderingTest {

    public void testWithoutScriptTxt() throws IOException {
        final String content = getContent(displayUrl + ".txt", CONTENT_TYPE_PLAIN);
        assertContains(content, "dumped by PlainTextRendererServlet");
    }

    public void testWithoutScriptHtml() throws IOException {
        final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
        assertContains(content, "dumped by HtmlRendererServlet");
    }

    public void testEspHtml() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.esp","html.esp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertContains(content, "ESP template");
            assertContains(content, "<p>" + testText + "</p>");
            assertContains(content, "<div class=\"SLING-142\" id=\"22\"/>");
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testEspJavaCode() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.esp","html.esp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertContains(content, "ESP template");
            assertContains(content, "TestLinkedListTest");
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testEspHtmlInAppsFolder() throws IOException {
        // make sure there's no leftover rendering script
        {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertFalse("Content must not contain script marker before testing", content.contains("ESP template"));
        }

        // put our script under /apps/<resource type>
        final String path = "/apps/" + slingResourceType;
        testClient.mkdirs(WEBDAV_BASE_URL, path);
        final String toDelete = uploadTestScript(path,"rendering-test.esp","html.esp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertContains(content, "ESP template");
            assertContains(content, "<p>" + testText + "</p>");
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void TODO_FAILS_testEspHtmlWithContentBasedPath() throws IOException {

        // make sure there's no leftover rendering script
        {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertContains(content, "ESP template");
        }

        // put our script in the /apps/<second folder level of content> (SLING-125)
        final String path = "/apps/" + secondFolderOfContentPath;
        testClient.mkdirs(WEBDAV_BASE_URL, path);
        final String toDelete = uploadTestScript(path,"rendering-test.esp","html.esp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertContains(content, "ESP template");
            assertContains(content, "<p>" + testText + "</p>");
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testEspHtmlWithSelectors() throws IOException {
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath + "/a4");
        final String toDeleteA = uploadTestScript("rendering-test.esp","html.esp");
        final String toDeleteB = uploadTestScript("rendering-test-2.esp","a4.esp");
        final String toDeleteC = uploadTestScript("rendering-test-3.esp","a4/print.esp");

        try {
            String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertTrue("Without selectors, content includes standard marker",content.contains("ESP template"));
            assertTrue("Without selectors, content contains formatted test text",content.contains("<p>" + testText + "</p>"));

            content = getContent(displayUrl + ".a4.print.html", CONTENT_TYPE_HTML);
            assertTrue("With a4.print selectors, content includes marker 3",content.contains("Template #3 for ESP tests"));
            assertTrue("With a4.print selectors, content contains italic text",content.contains("<em>" + testText + "</em>"));

            content = getContent(displayUrl + ".a4.html", CONTENT_TYPE_HTML);
            assertTrue("With a4 selector, content includes marker 2",content.contains("Template #2 for ESP tests"));
            assertTrue("With a4 selector, content contains bold text",content.contains("<b>" + testText + "</b>"));

            content = getContent(displayUrl + ".different.html", CONTENT_TYPE_HTML);
            assertTrue("With different selector only, content includes standard marker",content.contains("ESP template"));
            assertTrue("With different selector only, content contains formatted test text",content.contains("<p>" + testText + "</p>"));
        } finally {
            testClient.delete(toDeleteA);
            testClient.delete(toDeleteB);
            testClient.delete(toDeleteC);
        }
    }

    public void TODO_FAILS_testEspHtmlUppercase() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.esp","html.esp");
        try {
            final String content = getContent(displayUrl + ".HTML", CONTENT_TYPE_HTML);
            assertTrue("Content includes ESP marker",content.contains("ESP template"));
            assertTrue("Content contains formatted test text",content.contains("<p>" + testText + "</p>"));
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void TODO_FAILS_testEspNoExtension() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.esp","GET.esp");
        try {
            final String content = getContent(displayUrl, CONTENT_TYPE_PLAIN);
            assertTrue("Content includes ESP marker",content.contains("ESP template"));
            assertTrue("Content contains formatted test text",content.contains("<p>" + testText + "</p>"));
        } finally {
            testClient.delete(toDelete);
        }
    }

    /** SLING-107, verify that extension is used instead of Content-Type for script name */
    public void testEspJs() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.esp","js.esp");
        try {
            final String content = getContent(displayUrl + ".js", CONTENT_TYPE_JS);
            // template makes no JS sense, that's not a problem for this test
            assertContains(content, "ESP template");
            assertContains(content, "<p>" + testText + "</p>");
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testEspXml() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.esp","xml.esp");
        try {
            final String content = getContent(displayUrl + ".xml", CONTENT_TYPE_XML);
            assertContains(content, "ESP template");
            assertContains(content, "<p>" + testText + "</p>");
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testEspPlain() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.esp","txt.esp");
        try {
            final String content = getContent(displayUrl + ".txt", CONTENT_TYPE_PLAIN);
            assertContains(content, "ESP template");
            assertContains(content, "<p>" + testText + "</p>");
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void TODO_FAILS_testJsHtml() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.ecma","html.ecma");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertTrue("Content includes JS marker",content.contains("Raw javascript template"));
            assertTrue("Content contains formatted test text",content.contains("<p><em>" + testText + "</em></p>"));
        } finally {
            testClient.delete(toDelete);
        }
    }
}