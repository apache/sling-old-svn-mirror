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
package org.apache.sling.microsling.integration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Test creating a Node and rendering it using scripts in 
 *  various supported languages, using nodetype-based
 *  script resolution
 */
public class NodetypeRenderingTest extends RenderingTestBase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // set test values
        testText = "This is a test " + System.currentTimeMillis();
        
        // create the test node
        final String url = HTTP_BASE_URL + TEST_PATH + ".sling";
        final Map<String,String> props = new HashMap<String,String>();
        props.put("text", testText);
        displayUrl = testClient.createNode(url, props);
        
        // the rendering script goes under /sling/scripts in the repository
        scriptPath = "/sling/scripts/NODETYPES/nt/unstructured";
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
    }
    
    public void testWithoutScript() throws IOException {
        final String content = getContent(displayUrl + ".html", CONTENT_TYPE_PLAIN);
        assertTrue("Content includes default servlet marker",content.contains("dumped by DefaultSlingServlet"));
    }
    
    public void testEspHtml() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.esp","html.esp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
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
        final String toDelete = uploadTestScript("rendering-test.esp","plain.esp");
        try {
            final String content = getContent(displayUrl + ".txt", CONTENT_TYPE_PLAIN);
            assertTrue("Content includes ESP marker",content.contains("ESP template"));
            assertTrue("Content contains formatted test text",content.contains("<p>" + testText + "</p>"));
        } finally {
            testClient.delete(toDelete);
        }
    }
    
    public void testVltHtml() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.vlt","html.vlt");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertTrue("Content includes VLT marker",content.contains("Velocity template"));
            assertTrue("Content contains formatted test text",content.contains("<p><b>" + testText + "</b></p>"));
        } finally {
            testClient.delete(toDelete);
        }
    }
    
    public void testJsHtml() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.js","html.js");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertTrue("Content includes JS marker",content.contains("Raw javascript template"));
            assertTrue("Content contains formatted test text",content.contains("<p><em>" + testText + "</em></p>"));
        } finally {
            testClient.delete(toDelete);
        }
    }
    
    public void testFtlHtml() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.ftl","html.ftl");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertTrue("Content includes FreeMarker marker",content.contains("FreeMarker template"));
            assertTrue("Content contains formatted test text",content.contains("<p><span>" + testText + "</span></p>"));
        } finally {
            testClient.delete(toDelete);
        }
    }
}