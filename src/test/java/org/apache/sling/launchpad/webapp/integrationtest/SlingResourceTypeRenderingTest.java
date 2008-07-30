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
 *  various supported languages, using slingResourceType-based
 *  script resolution
 */
public class SlingResourceTypeRenderingTest extends RenderingTestBase {

    private String slingResourceType;
    private String secondFolderOfContentPath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // set test values
        slingResourceType = "integration-test/srt." + System.currentTimeMillis();
        testText = "This is a test " + System.currentTimeMillis();

        // create the test node, under a path that's specific to this class to allow collisions
        secondFolderOfContentPath = "" + System.currentTimeMillis();
        final String url = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "/" + secondFolderOfContentPath + SlingPostConstants.DEFAULT_CREATE_SUFFIX;
        final Map<String,String> props = new HashMap<String,String>();
        props.put("sling:resourceType", slingResourceType);
        props.put("text", testText);
        displayUrl = testClient.createNode(url, props);

        // the rendering script goes under /apps in the repository
        scriptPath = "/apps/" + slingResourceType;
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
    }

    public void testWithoutScriptTxt() throws IOException {
        final String content = getContent(displayUrl + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue("Content includes PlainTextRendererServlet marker",content.contains("dumped by PlainTextRendererServlet"));
    }

    public void testWithoutScriptHtml() throws IOException {
        final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
        assertTrue("Content contains default rendering",content.contains("Node dumped by HtmlRendererServlet"));
    }

    public void testEspHtml() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.esp","html.esp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertTrue("Content includes ESP marker",content.contains("ESP template"));
            assertTrue("Content contains formatted test text",content.contains("<p>" + testText + "</p>"));
            assertTrue("Content (" + content + ") contains attribute generated with simplified syntax",
                    content.contains("<div class=\"SLING-142\" id=\"22\"/>"));
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testEspJavaCode() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.esp","html.esp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertTrue("Content includes ESP marker",content.contains("ESP template"));
            assertTrue("Content contains java code output",content.contains("TestLinkedListTest"));
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testEspHtmlInAppsFolder() throws IOException {
        // make sure there's no leftover rendering script
        {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertFalse("Content must not include ESP marker before test",content.contains("ESP template"));
        }

        // put our script under /apps/<resource type>
        final String path = "/apps/" + slingResourceType;
        testClient.mkdirs(WEBDAV_BASE_URL, path);
        final String toDelete = uploadTestScript(path,"rendering-test.esp","html.esp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertTrue("Content includes ESP marker",content.contains("ESP template"));
            assertTrue("Content contains formatted test text",content.contains("<p>" + testText + "</p>"));
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
            assertTrue("Content includes ESP marker",content.contains("ESP template"));
            assertTrue("Content contains formatted test text",content.contains("<p>" + testText + "</p>"));
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

    public void TODO_FAILS_testJstHtml() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.jst","html.jst");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertTrue("Content includes JST marker",content.contains("JST template"));
            assertTrue("Content contains JSON data",content.contains("\"text\":\"" + testText + "\""));
            assertTrue("Content contains default rendering",content.contains("div id=\"JstDefaultRendering"));
            assertTrue("Content contains javascript rendering code",content.contains("out.write( currentNode.text )"));
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void TODO_FAILS_testJstScriptTagA() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.jst","html.jst");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertTrue("Content includes JST marker",content.contains("JST template"));
            assertTrue("Content contains scripted stuff (" + content + ")",
                    content.contains("something scripted"));
            assertFalse("Script opening tag must be broken in two in content (" + content + ")",
                    content.contains("<script>something")); 
            assertFalse("Script closing tag must be broken in two in content (" + content + ")",
                    content.contains("scripted</script>")); 
        } finally {
            testClient.delete(toDelete);
        }
    }

    /** TODO this test currently fails, see SLING-114 */
    public void TODO_FAILS_testJstHtmlScriptTagB() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.jst","html.jst");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertTrue("Content includes JST marker",content.contains("JST template"));
            assertTrue("Content contains scripted stuff (" + content + ")",
                    content.contains("more scripting"));
            assertFalse("Script opening tag must be broken in two in content (" + content + ")",
                    content.contains("<script>more")); 
            assertFalse("Script closing tag must be broken in two in content (" + content + ")",
                    content.contains("scripting</script>")); 
        } finally {
            testClient.delete(toDelete);
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
            assertTrue("Content includes ESP marker",content.contains("ESP template"));
            assertTrue("Content contains formatted test text",content.contains("<p>" + testText + "</p>"));
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testEspXml() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.esp","xml.esp");
        try {
            final String content = getContent(displayUrl + ".xml", CONTENT_TYPE_XML);
            assertTrue("Content includes ESP marker",content.contains("ESP template"));
            assertTrue("Content contains formatted test text",content.contains("<p>" + testText + "</p>"));
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testEspPlain() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.esp","txt.esp");
        try {
            final String content = getContent(displayUrl + ".txt", CONTENT_TYPE_PLAIN);
            assertTrue("Content includes ESP marker",content.contains("ESP template"));
            assertTrue("Content contains formatted test text",content.contains("<p>" + testText + "</p>"));
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