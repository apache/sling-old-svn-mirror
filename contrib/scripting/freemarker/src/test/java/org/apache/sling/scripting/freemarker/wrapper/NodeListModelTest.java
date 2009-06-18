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
import javax.jcr.NodeIterator;

/**
 * Test freemarker node list model.
 */
public class NodeListModelTest extends FreemarkerTestBase {
    private Node node1;
    private Node node2;
    private Node node3;


    @Override
    protected void setUp() throws Exception {
        super.setUp();

        node1 = rootNode.addNode("Test-1-" + System.currentTimeMillis());
        node2 = rootNode.addNode("Test-2-" + System.currentTimeMillis());
        node3 = rootNode.addNode("Test-3-" + System.currentTimeMillis());
    }

    public void testGetByIndex() throws Exception {
        assertEquals(node1.getPath(), freemarker.evalToString("${node[0]}"));
        assertEquals(node2.getPath(), freemarker.evalToString("${node[1]}"));
        assertEquals(node3.getPath(), freemarker.evalToString("${node[2]}"));
    }

    public void testGetSize() throws Exception {
        assertEquals("3", freemarker.evalToString("${node?size}"));
    }

    public void testIteration() throws Exception {
        String expect = "";
        NodeIterator iter = rootNode.getNodes();
        while (iter.hasNext()) {
            expect += iter.nextNode().getPath();
        }
        assertEquals(expect, freemarker.evalToString("<#list node as child>${child}</#list>"));
    }

}
