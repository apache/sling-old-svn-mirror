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

import org.apache.sling.servlets.post.SlingPostConstants;

/** Test creating a Node and rendering it using scripts in
 *  various supported languages, using nodetype-based
 *  script resolution
 */
public class NodetypeRenderingTest extends RenderingTestBase {

    private String secondFolderOfContentPath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // set test values
        testText = "This is a test " + System.currentTimeMillis();

        // create the test node, under a path that's specific to this class to allow collisions
        secondFolderOfContentPath = "" + System.currentTimeMillis();
        final String url = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "/" + secondFolderOfContentPath + SlingPostConstants.DEFAULT_CREATE_SUFFIX;
        final Map<String,String> props = new HashMap<String,String>();
        props.put("text", testText);
        displayUrl = testClient.createNode(url, props);

        // the rendering script goes under /apps in the repository
        scriptPath = "/apps/nt/unstructured";
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
    }

    public void testWithoutScriptTxt() throws IOException {
        final String content = getContent(displayUrl + ".txt", CONTENT_TYPE_PLAIN);
        assertContains(content, "dumped by PlainTextRendererServlet");
    }

    public void testWithoutScriptHtml() throws IOException {
        final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
        assertContains(content, "dumped by HtmlRendererServlet");
    }

    public void testMiniScriptHtml() throws IOException {
        final String toDelete = uploadTestScript("no-code.esp","html.esp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertEquals("There's no code in this script, just this constant string",content);
        } finally {
            testClient.delete(toDelete);
        }
    }
    
    public void testPrint() throws IOException {
        final String toDelete = uploadTestScript("print.esp","html.esp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            final String expected = "print.esp ends";
            assertContains(content, expected);
        } finally {
            testClient.delete(toDelete);
        }
    }
    
    public void testEspHtml() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.esp","html.esp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
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

    public void TODO_FAILS_testEspHtmlWithContentBasedPath() throws IOException {

        // make sure there's no leftover rendering script
        {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertFalse("Content must not include ESP marker before test",content.contains("ESP template"));
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

    public void TODO_FAILS_testVltHtml() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.vlt","html.vlt");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertTrue("Content includes VLT marker",content.contains("Velocity template"));
            assertTrue("Content contains formatted test text",content.contains("<p><b>" + testText + "</b></p>"));
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

    public void TODO_FAILS_testFtlHtml() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.ftl","html.ftl");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertTrue("Content includes FreeMarker marker",content.contains("FreeMarker template"));
            assertTrue("Content contains formatted test text",content.contains("<p><span>" + testText + "</span></p>"));
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void TODO_FAILS_testErbHtml() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.erb","html.erb");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertTrue("Content includes Ruby marker",content.contains("Ruby template"));
            assertTrue("Content contains formatted test text",content.contains("<p><span>" + testText + "</span></p>"));
        } finally {
            testClient.delete(toDelete);
        }
    }
}
