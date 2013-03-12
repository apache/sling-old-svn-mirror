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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Test rendering resources based on their JCR node type */
public class NodeTypeBasedRenderingTest extends RenderingTestBase{
    public static final String TEST_PATH = "/testing/" + NodeTypeBasedRenderingTest.class.getSimpleName();
    private String testNodeUrl;
    private List<String> toDelete = new ArrayList<String>();
    private static AtomicInteger counter = new AtomicInteger();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testClient.delete(HTTP_BASE_URL + TEST_PATH);
        testNodeUrl = null;
        toDelete.clear();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        for(String url : toDelete) {
            testClient.delete(url);
        }
    }

    private void testSpecificNodeType(String nodeType, String ... moreProperties) throws IOException {
        // Create test node of specified type
        testClient.mkdirs(HTTP_BASE_URL, TEST_PATH);
        final Map<String, String> props = new HashMap<String, String>();
        props.put("jcr:primaryType", nodeType);
        
        if(moreProperties != null) {
            for(int i=0; i < moreProperties.length; i+=2) {
                props.put(moreProperties[i], moreProperties[i+1]);
            }
        }
        
        final String testNodePath = TEST_PATH + "/test_" + counter.incrementAndGet();
        testNodeUrl = testClient.createNode(HTTP_BASE_URL + testNodePath, props);
        toDelete.add(testNodeUrl);
        
        {
            // No script -> default rendering
            final String content = getContent(testNodeUrl + ".html", CONTENT_TYPE_HTML);
            assertContains(content, "dumped by HtmlRendererServlet");
        }
        
        {
            // With script -> custom rendering based on mapping of JCR
            // node type to Sling resource type
            final String scriptPath = "/apps/" + nodeType.replaceAll(":", "/");
            testClient.mkdirs(HTTP_BASE_URL, scriptPath);
            toDelete.add(uploadTestScript(scriptPath, "nodetype-and-path.esp", "html.esp"));
            final String content = getContent(testNodeUrl + ".html", CONTENT_TYPE_HTML);
            assertContains(content, "RENDERED BY nodetype-and-path.esp");
            assertContains(content, "TYPE " + nodeType);
            assertContains(content, "PATH " + testNodePath);
        }
    }
    
    public void testNtFolder() throws IOException {
        testSpecificNodeType("nt:folder");
    }
    
    public void testNtUnstructured() throws IOException {
        testSpecificNodeType("nt:unstructured");
    }
    
    public void testNtUnstructuredWithResourceType() throws IOException {
        testSpecificNodeType("nt:unstructured", "sling:resourceType", "nt:unstructured");
    }
    
    public void testSlingFolder() throws IOException {
        testSpecificNodeType("sling:Folder");
    }
    
    public void testSlingFolderWithResourceType() throws IOException {
        testSpecificNodeType("sling:Folder", "sling:resourceType", "sling:Folder");
    }
    
    public void testSlingOrderedFolder() throws IOException {
        testSpecificNodeType("sling:OrderedFolder");
    }
    
    public void testSlingOrderedFolderWithResourceType() throws IOException {
        testSpecificNodeType("sling:OrderedFolder", "sling:resourceType", "sling:OrderedFolder");
    }
    
}