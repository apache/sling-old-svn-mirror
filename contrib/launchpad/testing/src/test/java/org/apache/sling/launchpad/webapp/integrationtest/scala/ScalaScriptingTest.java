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
package org.apache.sling.launchpad.webapp.integrationtest.scala;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;

public class ScalaScriptingTest extends HttpTestBase {
    private String testRootUrl;
    private TestNode testNode;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        final String testRootPath = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "/" + System.currentTimeMillis();
        testRootUrl = testClient.createNode(testRootPath + SlingPostConstants.DEFAULT_CREATE_SUFFIX, null);

        Map<String, String> nodeProperties = new HashMap<String, String>();
        nodeProperties.put("jcr:primaryType", "nt:folder");

        // fixme: this is a workaround for the post servlet returning 200 instead of 302
        // if the path is /var/classes
        nodeProperties.put(":redirect", "*.json");

        testClient.createNode(HTTP_BASE_URL + "/var/classes", nodeProperties);
        testNode = new TestNode(testRootPath + "/test", null);
    }

    @Override
    protected void tearDown() throws Exception {
        testClient.delete(testRootUrl);
        super.tearDown();
    }

    public void testNoScript() throws Exception {
        final String content = getContent(testNode.nodeUrl + ".txt", CONTENT_TYPE_PLAIN);
        assertTrue(content.contains("PlainTextRendererServlet"));
        assertTrue("Content contains " + testNode.testText + " (" + content + ")", content.contains(testNode.testText));
    }

    public void testScala() throws Exception {
        final String toDelete = uploadTestScript(testNode.scriptPath, "scala/rendering-test.scs", "html.scala");
        try {
            checkContent(testNode);
        }
        finally {
            if(toDelete != null) {
                testClient.delete(toDelete);
            }
        }
    }

    private void checkContent(TestNode node) throws Exception {
        final String content = getContent(node.nodeUrl + ".html", CONTENT_TYPE_HTML);
        assertTrue("Scala script executed as expected (" + content + ")", content.contains("<h1>Scala rendering result</h1>"));

        final String [] expected = {
                "using resource.adaptTo:" + node.testText,
                "using currentNode:" + node.testText,
        };
        for(String exp : expected) {
            assertTrue("Content contains " + exp + "(" + content + ")", content.contains(exp));
        }
    }

}
