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
import javax.jcr.PropertyType;

/**
 * Test references to nodes both as singular properties and list properties.
 */
public class NodeReferenceTest extends FreemarkerTestBase {

    private Node node1;
    private Node node2;
    private Node node3;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        node1 = rootNode.addNode("nodefrom", "nt:unstructured");

        node2 = rootNode.addNode("reference1");
        node2.addMixin("mix:referenceable");

        node3 = rootNode.addNode("reference2");
        node3.addMixin("mix:referenceable");

        node1.setProperty("singlereference", node2.getUUID(), PropertyType.REFERENCE);
        node1.setProperty("multireference", new String[]{ node2.getUUID(), node3.getUUID()}, PropertyType.REFERENCE);
    }

    public void testReferenceAsProperty() throws Exception {
        assertEquals(node2.getUUID(), freemarker.evalToString("${node.nodefrom.@singlereference}"));
    }

    public void testReferenceAsNode() throws Exception {
        assertEquals(node2.getPath(), freemarker.evalToString("${node.nodefrom.singlereference}"));
    }

    public void testMultiReferenceAsProperty() throws Exception {
        String expect = node2.getUUID() + node3.getUUID();
        assertEquals(expect, freemarker.evalToString("<#list node.nodefrom.@multireference as ref>${ref}</#list>"));
    }

    public void testMultiReferenceAsNode() throws Exception {
        String expect = node2.getPath() + node3.getPath();
        assertEquals(expect, freemarker.evalToString("<#list node.nodefrom.multireference as ref>${ref}</#list>"));
    }

}
