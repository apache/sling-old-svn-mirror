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

import static org.junit.Assert.assertTrue;

import org.apache.sling.commons.testing.integration.HttpTest;
import org.apache.sling.commons.testing.integration.HttpTestNode;
import org.apache.sling.commons.testing.junit.Retry;
import org.apache.sling.commons.testing.junit.RetryRule;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.testing.tools.retry.RetryLoop;
import org.apache.sling.testing.tools.retry.RetryLoop.Condition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Test JSP scripting
 *  TODO this class can be generalized to be used for any scripting language,
 *  that would help in testing all scripting engines.
 */
public class JspScriptingTest {

    private String testRootUrl;
    private HttpTestNode rtNode;
    private HttpTestNode unstructuredNode;
    
    public static final int CHECK_CONTENT_TIMEOUT_SECONDS = 5;
    public static final int CHECK_CONTENT_INTERVAL_MSEC = 500;

    /** HTTP tests helper */
    private final HttpTest H = new HttpTest();
    
    @Rule
    public RetryRule retryRule = new RetryRule();

    @Before
    public void setUp() throws Exception {
        H.setUp();

        final String testRootPath = HttpTest.HTTP_BASE_URL + "/" + getClass().getSimpleName() + "/" + System.currentTimeMillis();
        testRootUrl = H.getTestClient().createNode(testRootPath + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        rtNode = new HttpTestNode(H.getTestClient(), testRootPath + "/rt", null);
        unstructuredNode = new HttpTestNode(H.getTestClient(), testRootPath + "/unstructured", null);
    }

    @Before
    public void tearDown() throws Exception {
        H.getTestClient().delete(testRootUrl);
        H.tearDown();
    }

    @Test
    @Retry
    public void testRtNoScript() throws Exception {
        final String content = H.getContent(rtNode.nodeUrl + ".txt", HttpTest.CONTENT_TYPE_PLAIN);
        assertTrue(content.contains("PlainTextRendererServlet"));
        assertTrue("Content contains " + rtNode.testText + " (" + content + ")", content.contains(rtNode.testText));
    }

    @Test
    @Retry
    public void testUnstructuredNoScript() throws Exception {
        final String content = H.getContent(unstructuredNode.nodeUrl + ".txt", HttpTest.CONTENT_TYPE_PLAIN);
        assertTrue(content.contains("PlainTextRendererServlet"));
        assertTrue("Content contains " + unstructuredNode.testText + " (" + content + ")", content.contains(unstructuredNode.testText));
    }

    @Test
    @Retry
    public void testRtJsp() throws Exception {
        final String toDelete = H.uploadTestScript(rtNode.scriptPath, "rendering-test.jsp", "html.jsp");
        try {
            checkContent(rtNode);
        } finally {
            if(toDelete != null) {
                H.getTestClient().delete(toDelete);
            }
        }
    }

    @Test
    @Retry
    public void testUnstructuredJsp() throws Exception {
        final String toDelete = H.uploadTestScript(unstructuredNode.scriptPath, "rendering-test.jsp", "html.jsp");
        try {
            checkContent(unstructuredNode);
        } finally {
            if(toDelete != null) {
                H.getTestClient().delete(toDelete);
            }
        }
    }

    @Test
    @Retry
    public void testTagFile() throws Exception {
        String tagFile = null;
        String tagUsingScript = null;
        try {
            H.getTestClient().mkdirs(HttpTest.WEBDAV_BASE_URL, "/apps/testing/tags");
            tagFile = H.uploadTestScript("/apps/testing/tags", "example.tag", "example.tag");
            tagUsingScript = H.uploadTestScript(unstructuredNode.scriptPath, "withtag.jsp", "html.jsp");

            final String content = H.getContent(unstructuredNode.nodeUrl + ".html", HttpTest.CONTENT_TYPE_HTML);
            assertTrue("JSP script executed as expected (" + content + ")", content.contains("<h1>Hello, Sling User</h1>"));

        } finally {
            if (tagFile != null) {
                H.getTestClient().delete(tagFile);
            }
            if (tagUsingScript != null) {
                H.getTestClient().delete(tagUsingScript);
            }
        }
    }

    /* Verify that overwriting a JSP script changes the output within a reasonable time 
     * (might not be immediate as there's some observation and caching involved) */
    @Test
    public void testChangingJsp() throws Exception {
        String toDelete = null;

        try {
            final String [] scripts = { "jsp1.jsp", "jsp2.jsp" };
            for(String script : scripts) {
                toDelete = H.uploadTestScript(unstructuredNode.scriptPath, script, "html.jsp");
                final String expected = "text from " + script + ":" + unstructuredNode.testText;
                
                final Condition c = new Condition() {

                    public String getDescription() {
                        return "Expecting " + expected;
                    }

                    public boolean isTrue() throws Exception {
                        final String content = H.getContent(unstructuredNode.nodeUrl + ".html", HttpTest.CONTENT_TYPE_HTML);
                        return content.contains(expected);
                    }
                };
                
                new RetryLoop(c, CHECK_CONTENT_TIMEOUT_SECONDS, CHECK_CONTENT_INTERVAL_MSEC);
            }
        } finally {
            if(toDelete != null) {
                H.getTestClient().delete(toDelete);
            }
        }
    }
    
    @Test
    @Retry
    public void testEnum() throws Exception {
        String toDelete = null;
        try {
            toDelete = H.uploadTestScript(unstructuredNode.scriptPath, "enum-test.jsp", "txt.jsp");
            final String content = H.getContent(unstructuredNode.nodeUrl + ".txt", HttpTest.CONTENT_TYPE_PLAIN);
            for(String expected : new String[] { "FOO=FOO", "BAR=BAR"}) {
                assertTrue("Content contains '" + expected + "'(" + content + ")", content.contains(expected));
            }
        } finally {
            if(toDelete != null) {
                H.getTestClient().delete(toDelete);
            }
        }
    }

    private void checkContent(final HttpTestNode tn) throws Exception {
        final String content = H.getContent(tn.nodeUrl + ".html", HttpTest.CONTENT_TYPE_HTML);
        assertTrue("JSP script executed as expected (" + content + ")", content.contains("<h1>JSP rendering result</h1>"));
        
        final String [] expected = {
                "using resource.adaptTo:" + tn.testText,
                "using currentNode:" + tn.testText,
        };
        for(String exp : expected) {
            assertTrue("Content contains " + exp + "(" + content + ")", content.contains(exp));
        }
    }
}