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

    public void TODO_FAILS_testJstHtml() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.jst","html.jst");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertContains(content, "JST template");
            assertContains(content, "\"text\":\"" + testText + "\"");
            assertContains(content, "div id=\"JstDefaultRendering");
            assertContains(content, "out.write( currentNode.text )");
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void TODO_FAILS_testJstScriptTagA() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.jst","html.jst");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertContains(content, "JST template");
            assertContains(content, "something scripted");
            assertContains(content, "<script>something");
            assertContains(content, "scripted</script>");
        } finally {
            testClient.delete(toDelete);
        }
    }

    /** TODO this test currently fails, see SLING-114 */
    public void TODO_FAILS_testJstHtmlScriptTagB() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.jst","html.jst");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertContains(content, "JST template");
            assertContains(content, "more scripting");
            assertContains(content, "<script>more");
            assertContains(content, "scripting</script>");
        } finally {
            testClient.delete(toDelete);
        }
    }
    
    public void testPythonHtml() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.py","html.py");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertContains(content, "Python");
            assertContains(content, "<p>" + testText + "</p>");
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testPythonJavaCode() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.py","html.py");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertContains(content, "Python");
            assertContains(content, "TestLinkedListTest");
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testPythonHtmlInAppsFolder() throws IOException {
        // make sure there's no leftover rendering script
        {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertFalse("Content must not contain script marker before testing", content.contains("Python"));
        }

        // put our script under /apps/<resource type>
        final String path = "/apps/" + slingResourceType;
        testClient.mkdirs(WEBDAV_BASE_URL, path);
        final String toDelete = uploadTestScript(path,"rendering-test.py","html.py");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertContains(content, "Python");
            assertContains(content, "<p>" + testText + "</p>");
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testPythonXml() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.py","xml.py");
        try {
            final String content = getContent(displayUrl + ".xml", CONTENT_TYPE_XML);
            assertContains(content, "Python");
            assertContains(content, "<p>" + testText + "</p>");
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testPythonPlain() throws IOException {
    	final String toDelete = uploadTestScript("rendering-test.py","txt.py");
    	try {
    		final String content = getContent(displayUrl + ".txt", CONTENT_TYPE_PLAIN);
    		assertContains(content, "Python");
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