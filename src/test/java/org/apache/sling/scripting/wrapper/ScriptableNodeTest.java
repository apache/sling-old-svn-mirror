/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.scripting.wrapper;

import javax.jcr.Node;

import org.apache.sling.scripting.RepositoryScriptingTestBase;
import org.apache.sling.scripting.ScriptEngineHelper;

/** Test the ScriptableNode class "live", by retrieving
 *  Nodes from a Repository and executing javascript code
 *  using them.
 */
public class ScriptableNodeTest extends RepositoryScriptingTestBase {

    private Node node;
    private String testText;
    private ScriptEngineHelper.Data data;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        node = getNewNode();
        testText = "Test-" + System.currentTimeMillis();
        node.setProperty("text", testText);
        
        data = new ScriptEngineHelper.Data();
        data.put("node", node);
        data.put("property", node.getProperty("text"));
    }

    /** TODO reactivate this once SLING-154 is fixed */
    public void TODO_FAILS_testPrimaryNodeType() throws Exception {
        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        data.put("node", getTestRootNode());
        assertEquals(
                "nt:unstructured",
                script.evalToString("out.print(node.getPrimaryNodeType())", data)
        );
    }

    public void testViaPropertyNoWrappers() throws Exception {
        assertEquals(
                testText,
                script.evalToString("out.print(property.value.string)", data)
        );
    }
    
    public void testViaPropertyWithWrappers() throws Exception {
        assertEquals(
                testText,
                script.evalToString("out.print(property)", data)
        );
    }
    
    /** TODO reactivate this once SLING-154 is fixed */
    public void TODO_FAILStestViaNodeNoWrappers() throws Exception {
        assertEquals(
                testText,
                script.evalToString("out.print(node.properties.text.value.string)", data)
        );
    }
    
    public void testViaNodeDirectPropertyAccess() throws Exception {
        assertEquals(
                testText,
                script.evalToString("out.print(node.text)", data)
        );
    }
}
