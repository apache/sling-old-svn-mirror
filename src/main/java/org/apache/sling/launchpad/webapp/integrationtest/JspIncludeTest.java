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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.sling.commons.testing.integration.HttpTest;
import org.apache.sling.commons.testing.junit.Retry;
import org.apache.sling.commons.testing.junit.RetryRule;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Test the {link ScriptHelper#include) functionality */
 public class JspIncludeTest {
	 
    private String nodeUrlA;
    private String testTextA;
    private String nodeUrlB;
    private String testTextB;
    private String nodeUrlC;
    private String nodeUrlD;
    private String nodeUrlE;
    private String nodeUrlF;
    private String nodeUrlG;
    private String nodeUrlH;
	private String nodeUrlI;
    private String forcedResourceType;
    private Set<String> toDelete = new HashSet<String>();

    /** HTTP tests helper */
    private final HttpTest H = new HttpTest();

    @Rule
    public RetryRule retryRule = new RetryRule();

    @Before
    public void setUp() throws Exception {
        H.setUp();

        // Create the test nodes under a path that's specific to this class to
        // allow collisions
        final String url = HttpTest.HTTP_BASE_URL + "/" + getClass().getSimpleName() + "/" + System.currentTimeMillis() + SlingPostConstants.DEFAULT_CREATE_SUFFIX;
        final Map<String,String> props = new HashMap<String,String>();

        // Create two test nodes and store their paths
        testTextA = "Text A " + System.currentTimeMillis();
        props.put("text", testTextA);
        nodeUrlA = H.getTestClient().createNode(url, props);
        String pathToInclude = nodeUrlA.substring(HttpTest.HTTP_BASE_URL.length());

        // Node B stores the path of A, so that the test script can
        // include A when rendering B
        testTextB = "Text B " + System.currentTimeMillis();
        props.put("text", testTextB);
        props.put("pathToInclude", pathToInclude);
        nodeUrlB = H.getTestClient().createNode(url, props);

        // Node E is like B but with an extension on the include path
        props.put("pathToInclude", pathToInclude + ".html");
        nodeUrlE = H.getTestClient().createNode(url, props);

        // Node F is used for the max calls detection test
        props.put("testMaxCalls","true");
        nodeUrlF = H.getTestClient().createNode(url, props);

        // Node C is used for the infinite loop detection test
        props.remove("pathToInclude");
        props.remove("testMaxCalls");
        props.put("testInfiniteLoop","true");
        nodeUrlC = H.getTestClient().createNode(url, props);

        // Node D is used for the "force resource type" test
        forcedResourceType = getClass().getSimpleName() + "/" + System.currentTimeMillis();
        props.remove("testInfiniteLoop");
        props.put("forceResourceType", forcedResourceType);
        props.put("pathToInclude", pathToInclude);
        nodeUrlD = H.getTestClient().createNode(url, props);

        // Node G is used for the basic "call" test
        props.remove("forceResourceType");
        props.remove("pathToInclude");
        props.put("testCallScript", "called-test.jsp");
        nodeUrlG = H.getTestClient().createNode(url, props);
        
        // Node I is used for the "var" test
        props.remove("forceResourceType");
        props.remove("testCallScript");
        props.put("testVarInclude","true"); 
        props.put("pathToInclude", pathToInclude);
        props.put("VAR_INCLUDE", "VALUE");
        nodeUrlI = H.getTestClient().createNode(url, props);

        // Script for forced resource type
        H.setScriptPath("/apps/" + forcedResourceType);
        H.getTestClient().mkdirs(HttpTest.WEBDAV_BASE_URL, H.getScriptPath());
        toDelete.add(H.uploadTestScript(H.getScriptPath(),"include-forced.jsp","html.jsp"));

        // The main rendering script goes under /apps in the repository
        H.setScriptPath("/apps/nt/unstructured");
        H.getTestClient().mkdirs(HttpTest.WEBDAV_BASE_URL, H.getScriptPath());
        toDelete.add(H.uploadTestScript(H.getScriptPath(),"include-test.jsp","html.jsp"));
        toDelete.add(H.uploadTestScript(H.getScriptPath(),"called-test.jsp","called-test.jsp"));

        // Node H is used for "call" test where the called script is inherited from the supertype
        String nodeHResourceType = getClass().getSimpleName() + "/" + System.currentTimeMillis();
        String nodeHSuperResourceType = getClass().getSimpleName() + "/super" + System.currentTimeMillis();
        props.put("sling:resourceType", nodeHResourceType);
        props.put("sling:resourceSuperType", nodeHSuperResourceType);
        nodeUrlH = H.getTestClient().createNode(url, props);
        H.setScriptPath("/apps/" + nodeHResourceType);
        H.getTestClient().mkdirs(HttpTest.WEBDAV_BASE_URL, H.getScriptPath());
        toDelete.add(H.uploadTestScript(H.getScriptPath(),"calling-test.jsp","html.jsp"));
        H.setScriptPath("/apps/" + nodeHSuperResourceType);
        H.getTestClient().mkdirs(HttpTest.WEBDAV_BASE_URL, H.getScriptPath());
        toDelete.add(H.uploadTestScript(H.getScriptPath(),"called-test.jsp","called-test.jsp"));
    }

    @After
    public void tearDown() throws Exception {
        H.tearDown();
        for(String script : toDelete) {
           H.getTestClient().delete(script);
        }
    }

    @Test 
    @Retry
    public void testWithoutInclude() throws IOException {
        final String content = H.getContent(nodeUrlA + ".html", HttpTest.CONTENT_TYPE_HTML);
        assertTrue("Content includes JSP marker",content.contains("JSP template"));
        assertTrue("Content contains formatted test text",content.contains("<p class=\"main\">" + testTextA + "</p>"));
        assertFalse("Nothing has been included",content.contains("<p>Including"));
    }

    @Test 
    @Retry
    public void testWithInclude() throws IOException {
        final String content = H.getContent(nodeUrlB + ".html", HttpTest.CONTENT_TYPE_HTML);
        assertTrue("Content includes JSP marker",content.contains("JSP template"));
        assertTrue("Content contains formatted test text",content.contains("<p class=\"main\">" + testTextB + "</p>"));
        assertTrue("Include has been used",content.contains("<p>Including"));
        assertTrue("Text of node A is included (" + content + ")",content.contains(testTextA));
    }

    @Test 
    @Retry
    public void testWithIncludeAndExtension() throws IOException {
        final String content = H.getContent(nodeUrlE + ".html", HttpTest.CONTENT_TYPE_HTML);
        assertTrue("Content includes JSP marker",content.contains("JSP template"));
        assertTrue("Content contains formatted test text",content.contains("<p class=\"main\">" + testTextB + "</p>"));
        assertTrue("Include has been used",content.contains("<p>Including"));
        assertTrue("Text of node A is included (" + content + ")",content.contains(testTextA));
    }

    @Test 
    @Retry
    public void testInfiniteLoopDetection() throws IOException {
        // Node C has a property that causes an infinite include loop,
        // Sling must indicate the problem in its response
        final GetMethod get = new GetMethod(nodeUrlC + ".html");
        H.getHttpClient().executeMethod(get);
        final String content = get.getResponseBodyAsString();
        assertTrue(
            "Response contains infinite loop error message",
            content.contains("org.apache.sling.api.request.RecursionTooDeepException"));

        // TODO: SLING-515, status is 500 when running the tests as part of the maven build
        // but 200 if running tests against a separate instance started with mvn jetty:run
        // final int status = get.getStatusCode();
        // assertEquals("Status is 500 for infinite loop",HttpServletResponse.SC_INTERNAL_SERVER_ERROR, status);
    }

    @Test 
    @Retry
    public void testMaxCallsDetection() throws IOException {
        // Node F has a property that causes over 1000 includes
        // Sling must indicate the problem in its response
        final GetMethod get = new GetMethod(nodeUrlF + ".html");
        H.getHttpClient().executeMethod(get);
        final String content = get.getResponseBodyAsString();
        assertTrue(
            "Response contains infinite loop error message",
            content.contains("org.apache.sling.api.request.TooManyCallsException"));

        // TODO: SLING-515, status is 500 when running the tests as part of the maven build
        // but 200 if running tests against a separate instance started with mvn jetty:run
        // final int status = get.getStatusCode();
        // assertEquals("Status is 500 for infinite loop",HttpServletResponse.SC_INTERNAL_SERVER_ERROR, status);
    }

    @Test 
    @Retry
    public void testForcedResourceType() throws IOException {
        final String content = H.getContent(nodeUrlD + ".html", HttpTest.CONTENT_TYPE_HTML);
        assertTrue("Content includes JSP marker",content.contains("JSP template"));
        assertTrue("Content contains formatted test text",content.contains("<p class=\"main\">" + testTextB + "</p>"));
        assertTrue("Include has been used",content.contains("<p>Including"));
        assertTrue("Text of node A is included (" + content + ")",content.contains(testTextA));
        assertTrue("Resource type has been forced (" + content + ")",content.contains("Forced resource type:" + forcedResourceType));
    }

    @Test 
    @Retry
    public void testCall() throws IOException {
        final String content = H.getContent(nodeUrlG + ".html", HttpTest.CONTENT_TYPE_HTML);
        assertTrue("Content includes JSP marker",content.contains("JSP template"));
        assertTrue("Content contains formatted test text",content.contains("<p class=\"main\">" + testTextB + "</p>"));
        assertTrue("Call has been used",content.contains("<p>Calling"));
        assertTrue("Call has been made",content.contains("called"));
    }

    @Test 
    @Retry
    public void testCallToSupertype() throws IOException {
        final String content = H.getContent(nodeUrlH + ".html", HttpTest.CONTENT_TYPE_HTML);
        assertTrue("Content includes JSP marker",content.contains("JSP template"));
        assertTrue("Call has been made",content.contains("called"));
    }
    
    @Test 
    @Retry
    public void testVarInclude() throws IOException{
        final String content = H.getContent(nodeUrlI + ".html", HttpTest.CONTENT_TYPE_HTML);
        assertTrue("Content includes Loaded test content:",content.contains("Loaded test content:"));
    }
}