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
package org.apache.sling.usling.webapp.integrationtest;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/** Test the {link ScriptHelper#include) functionality */
 public class IncludeTest extends UslingHttpTestBase {

    private String nodeUrlA;
    private String testTextA;
    private String nodeUrlB;
    private String testTextB;
    private String nodeUrlC;
    private String scriptPath;
    private String toDelete;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // Create the test nodes under a path that's specific to this class to
        // allow collisions
        final String url = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "/" + System.currentTimeMillis() + "/UJAX_create";
        final Map<String,String> props = new HashMap<String,String>();
        
        // Create two test nodes and store their paths
        testTextA = "Text A " + System.currentTimeMillis();
        props.put("text", testTextA);
        nodeUrlA = testClient.createNode(url, props);
        
        // Node B stores the path of A, so that the test script can
        // include A when rendering B
        testTextB = "Text B " + System.currentTimeMillis();
        props.put("text", testTextB);
        props.put("pathToInclude", new URL(nodeUrlA).getPath());
        nodeUrlB = testClient.createNode(url, props);
        
        // Node C is used for the infinite loop detection test
        props.remove("pathToInclude");
        props.put("testInfiniteLoop","true");
        nodeUrlC = testClient.createNode(url, props);

        // the rendering script goes under /apps in the repository
        scriptPath = "/apps/nt/unstructured";
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
        toDelete = uploadTestScript(scriptPath,"include-test.esp","html.esp");
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        testClient.delete(toDelete);
    }

    public void testWithoutInclude() throws IOException {
        final String content = getContent(nodeUrlA + ".html", CONTENT_TYPE_HTML);
        assertTrue("Content includes ESP marker",content.contains("ESP template"));
        assertTrue("Content contains formatted test text",content.contains("<p class=\"main\">" + testTextA + "</p>"));
        assertFalse("Nothing has been included",content.contains("<p>Including"));
    }

    public void testWithInclude() throws IOException {
        final String content = getContent(nodeUrlB + ".html", CONTENT_TYPE_HTML);
        assertTrue("Content includes ESP marker",content.contains("ESP template"));
        assertTrue("Content contains formatted test text",content.contains("<p class=\"main\">" + testTextB + "</p>"));
        assertTrue("Include has been used",content.contains("<p>Including"));
    }
    
    public void TODO_FAILS_testInfiniteLoopDetection() throws IOException {
        // Node C has a property that causes an infinite include loop,
        // microsling must return an error 500 in this case
        assertHttpStatus(nodeUrlC + ".html", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

}
