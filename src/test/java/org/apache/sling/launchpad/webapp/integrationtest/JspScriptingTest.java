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

import org.apache.sling.servlets.post.SlingPostConstants;

/** Test JSP scripting
 *  TODO this class can be generalized to be used for any scripting language,
 *  that would help in testing all scripting engines.
 */
public class JspScriptingTest extends HttpTestBase {
 
    private String testRootUrl;
    private TestNode rtNode;
    private TestNode unstructuredNode;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        final String testRootPath = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "/" + System.currentTimeMillis();
        testRootUrl = testClient.createNode(testRootPath + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);
        rtNode = new TestNode(testRootPath + "/rt", null);
        unstructuredNode = new TestNode(testRootPath + "/unstructured", null);
    }

    @Override
    protected void tearDown() throws Exception {
        testClient.delete(testRootUrl);
        super.tearDown();
    }
    
    public void testRtNoScript() throws Exception {
        final String content = getContent(rtNode.nodeUrl + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue(content.contains("PlainTextRendererServlet"));
        assertTrue("Content contains " + rtNode.testText + " (" + content + ")", content.contains(rtNode.testText));
    }

    public void testUnstructuredNoScript() throws Exception {
        final String content = getContent(unstructuredNode.nodeUrl + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue(content.contains("PlainTextRendererServlet"));
        assertTrue("Content contains " + unstructuredNode.testText + " (" + content + ")", content.contains(unstructuredNode.testText));
    }
    
    public void testRtJsp() throws Exception {
        final String toDelete = uploadTestScript(rtNode.scriptPath, "rendering-test.jsp", "html.jsp");
        try {
            checkContent(rtNode);
        } finally {
            if(toDelete != null) {
                testClient.delete(toDelete);
            }
        }
    }

    public void testUnstructuredJsp() throws Exception {
        final String toDelete = uploadTestScript(unstructuredNode.scriptPath, "rendering-test.jsp", "html.jsp");
        try {
            checkContent(unstructuredNode);
        } finally {
            if(toDelete != null) {
                testClient.delete(toDelete);
            }
        }
    }
    
    private void checkContent(TestNode tn) throws Exception {
        final String content = getContent(tn.nodeUrl + ".html", CONTENT_TYPE_HTML);
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
