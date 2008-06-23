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
package org.apache.sling.scripting.freemarker.wrapper;

import org.apache.sling.scripting.freemarker.FreemarkerTestBase;

import javax.jcr.Node;

/**
 * Test freemarker node model.
 */
public class NodeModelTest extends FreemarkerTestBase {
    private Node node1;
    private Node node2;
    private Node node3;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        node1 = rootNode.addNode("child1");
        node1.setProperty("text", "Test-" + System.currentTimeMillis());
        node2 = rootNode.addNode("child2", "nt:unstructured");
        node3 = node1.addNode("grandchild1");
    }

    public void testDefaultValue() throws Exception {
        assertEquals(rootNode.getPath(), freemarker.evalToString("${node}"));
    }

    public void testParentNode() throws Exception {
        assertEquals(node1.getPath(), freemarker.evalToString("${node.child1.grandchild1?parent}"));
    }

    public void testRootNode() throws Exception {
        assertEquals("/", freemarker.evalToString("${node.child1.grandchild1?root}"));
    }

    public void testChildrenSequence() throws Exception {
        assertEquals("2", freemarker.evalToString("${node?children?size}"));
    }

    public void testNodeName() throws Exception {
        assertEquals(rootNode.getName(), freemarker.evalToString("${node?node_name}"));
    }

    public void testNodeType() throws Exception {
        assertEquals(node2.getPrimaryNodeType().getName(), freemarker.evalToString("${node.child2?node_type}"));
    }

    public void testChildNodeByName() throws Exception {
        assertEquals(node2.getPath(), freemarker.evalToString("${node.child2}"));
    }

    public void testChildPropertyByName() throws Exception {
        assertEquals(node1.getProperty("text").getString(), freemarker.evalToString("${node.child1.@text}"));
    }

    public void testIsEmpty() throws Exception {
        assertEquals("true", freemarker.evalToString("<#if node.child1?has_content>true<#else>false</#if>"));
        assertEquals("false", freemarker.evalToString("<#if node.child2?has_content>true<#else>false</#if>"));
    }

}
