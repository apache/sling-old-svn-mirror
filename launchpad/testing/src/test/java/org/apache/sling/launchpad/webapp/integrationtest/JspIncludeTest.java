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
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;


/** Test the {link ScriptHelper#include) functionality */
 public class JspIncludeTest extends HttpTestBase {

    private String nodeUrlA;
    private String testTextA;
    private String nodeUrlB;
    private String testTextB;
    private String nodeUrlC;
    private String nodeUrlD;
    private String nodeUrlE;
    private String scriptPath;
    private String forcedResourceType;
    private Set<String> toDelete = new HashSet<String>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Create the test nodes under a path that's specific to this class to
        // allow collisions
        final String url = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "/" + System.currentTimeMillis() + SlingPostConstants.DEFAULT_CREATE_SUFFIX;
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

        // Node E is like B but with an extension on the include path
        props.put("pathToInclude", new URL(nodeUrlA).getPath() + ".html");
        nodeUrlE = testClient.createNode(url, props);

        // Node C is used for the infinite loop detection test
        props.remove("pathToInclude");
        props.put("testInfiniteLoop","true");
        nodeUrlC = testClient.createNode(url, props);

        // Node D is used for the "force resource type" test
        forcedResourceType = getClass().getSimpleName() + "/" + System.currentTimeMillis();
        props.remove("testInfiniteLoop");
        props.put("forceResourceType", forcedResourceType);
        props.put("pathToInclude", new URL(nodeUrlA).getPath());
        nodeUrlD = testClient.createNode(url, props);

        // Script for forced resource type
        scriptPath = "/apps/" + forcedResourceType;
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
        toDelete.add(uploadTestScript(scriptPath,"include-forced.jsp","html.jsp"));

        // The main rendering script goes under /apps in the repository
        scriptPath = "/apps/nt/unstructured";
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
        toDelete.add(uploadTestScript(scriptPath,"include-test.jsp","html.jsp"));
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        for(String script : toDelete) {
            testClient.delete(script);
        }
    }

    public void testWithoutInclude() throws IOException {
        final String content = getContent(nodeUrlA + ".html", CONTENT_TYPE_HTML);
        assertTrue("Content includes JSP marker",content.contains("JSP template"));
        assertTrue("Content contains formatted test text",content.contains("<p class=\"main\">" + testTextA + "</p>"));
        assertFalse("Nothing has been included",content.contains("<p>Including"));
    }

    public void testWithInclude() throws IOException {
        final String content = getContent(nodeUrlB + ".html", CONTENT_TYPE_HTML);
        assertTrue("Content includes JSP marker",content.contains("JSP template"));
        assertTrue("Content contains formatted test text",content.contains("<p class=\"main\">" + testTextB + "</p>"));
        assertTrue("Include has been used",content.contains("<p>Including"));
        assertTrue("Text of node A is included (" + content + ")",content.contains(testTextA));
    }

    public void testWithIncludeAndExtension() throws IOException {
        final String content = getContent(nodeUrlE + ".html", CONTENT_TYPE_HTML);
        assertTrue("Content includes JSP marker",content.contains("JSP template"));
        assertTrue("Content contains formatted test text",content.contains("<p class=\"main\">" + testTextB + "</p>"));
        assertTrue("Include has been used",content.contains("<p>Including"));
        assertTrue("Text of node A is included (" + content + ")",content.contains(testTextA));
    }

    public void testInfiniteLoopDetection() throws IOException {
        // Node C has a property that causes an infinite include loop,
        // Sling must indicate the problem in its response
        final GetMethod get = new GetMethod(nodeUrlC + ".html");
        httpClient.executeMethod(get);
        final String content = get.getResponseBodyAsString();
        assertTrue("Response contains infinite loop error message",
                content.contains("InfiniteIncludeLoopException"));

        // TODO: SLING-515, status is 500 when running the tests as part of the maven build
        // but 200 if running tests against a separate instance started with mvn jetty:run
        // final int status = get.getStatusCode();
        // assertEquals("Status is 500 for infinite loop",HttpServletResponse.SC_INTERNAL_SERVER_ERROR, status);
    }

    public void testForcedResourceType() throws IOException {
        final String content = getContent(nodeUrlD + ".html", CONTENT_TYPE_HTML);
        assertTrue("Content includes JSP marker",content.contains("JSP template"));
        assertTrue("Content contains formatted test text",content.contains("<p class=\"main\">" + testTextB + "</p>"));
        assertTrue("Include has been used",content.contains("<p>Including"));
        assertTrue("Text of node A is included (" + content + ")",content.contains(testTextA));
        assertTrue("Resource type has been forced (" + content + ")",content.contains("Forced resource type:" + forcedResourceType));
    }
}
