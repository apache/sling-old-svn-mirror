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

import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;

import org.apache.sling.commons.json.jcr.JsonItemWriter;
import org.apache.sling.scripting.RepositoryScriptingTestBase;
import org.apache.sling.scripting.javascript.internal.ScriptEngineHelper;

/** Test the ScriptableNode class "live", by retrieving
 *  Nodes from a Repository and executing javascript code
 *  using them.
 */
public class ScriptableNodeTest extends RepositoryScriptingTestBase {

    private Node node;
    private Property textProperty;
    private String testText;
    private Property numProperty;
    private double testNum;
    private Property calProperty;
    private Calendar testCal;
    private ScriptEngineHelper.Data data;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        node = getNewNode();
        testText = "Test-" + System.currentTimeMillis();
        node.setProperty("text", testText);
        node.setProperty("otherProperty", node.getPath());

        testNum = System.currentTimeMillis();
        node.setProperty("num", testNum);

        testCal = Calendar.getInstance();
        node.setProperty("cal", testCal);

        data = new ScriptEngineHelper.Data();
        data.put("node", node);
        textProperty = node.getProperty("text");
        data.put("property", textProperty);
        numProperty = node.getProperty("num");
        data.put("numProperty", numProperty);
        calProperty = node.getProperty("cal");
        data.put("calProperty", calProperty);
    }

    public void testDefaultValue() throws Exception {
        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        data.put("node", getTestRootNode());
        assertEquals(
                getTestRootNode().getPath(),
                script.evalToString("out.print(node)", data)
        );
    }

    public void testPrimaryNodeType() throws Exception {
        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        data.put("node", getTestRootNode());
        assertEquals(
                "nt:unstructured",
                script.evalToString("out.print(node.getPrimaryNodeType().getName())", data)
        );
    }

    public void testPrimaryNodeTypeProperty() throws Exception {
        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        data.put("node", getTestRootNode());
        assertEquals(
                "nt:unstructured",
                script.evalToString("out.print(node['jcr:primaryType'])", data)
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
            textProperty.getString(),
            script.evalToString("out.print(property)", data)
        );
    }

    public void testViaNodeDirectPropertyAccess() throws Exception {
        assertEquals(
            testText,
            script.evalToString("out.print(node.text)", data)
        );
    }

    public void testViaPropertyNoWrappersNum() throws Exception {
        assertEquals(
            testNum,
            script.eval("numProperty.value.getDouble()", data)
        );
    }

    public void testViaPropertyWithWrappersNum() throws Exception {
        assertEquals(
            testNum,
            script.eval("0+numProperty", data)
        );
    }

    public void testViaNodeDirectPropertyAccessNum() throws Exception {
        assertEquals(
            testNum,
            script.eval("node.num", data)
        );
    }

    public void testViaPropertyNoWrappersCal() throws Exception {
        assertEquals(
                testCal,
                script.eval("calProperty.value.getDate()", data)
        );
    }

    public void testViaNodeDirectPropertyAccessCal() throws Exception {
    	final SimpleDateFormat f = new SimpleDateFormat(JsonItemWriter.ECMA_DATE_FORMAT, JsonItemWriter.DATE_FORMAT_LOCALE);
    	final String expected = f.format(testCal.getTime());
        assertEquals(
                expected,
                script.evalToString("out.print(node.cal)", data)
        );
    }

    public void testCalDateClass() throws Exception {
        assertEquals(
                "number",
                script.evalToString("out.print(typeof node.cal.date.time)", data)
        );
    }

    public void testPropertyParent() throws Exception {
        // need to use node.getProperty('num') to have a ScriptableProperty,
        // node.num only returns a wrapped value
        assertEquals(
                "nt:unstructured",
                script.eval("node.getProperty('num').parent['jcr:primaryType']", data)
        );
    }

    public void testPropertyAncestor() throws Exception {
        // call getAncestor which is not explicitly defined in ScriptableProperty,
        // to verify that all Property methods are available and that we get a
        // correctly wrapped result (SLING-397)
        assertEquals(
                "rep:root",
                script.eval("node.getProperty('num').getAncestor(0)['jcr:primaryType']", data)
        );
    }

    public void testPropertiesIterationNoWrapper() throws Exception {
        final String code =
            "var props = node.getProperties();"
            + " for(i in props) { out.print(props[i].name); out.print(' '); }"
        ;
        final String result = script.evalToString(code, data);
        final String [] names = { "text", "otherProperty" };
        for(String name : names) {
            assertTrue("result (" + result + ") contains '" + name + "'", result.contains(name));
        }
    }

    public void testAddNodeDefaultType() throws Exception {
        final String path = "subdt_" + System.currentTimeMillis();
        final String code =
            "var n = node.addNode('" + path + "');\n"
            + "out.print(n['jcr:primaryType']);\n"
        ;
        assertEquals("nt:unstructured", script.evalToString(code, data));
    }

    public void testAddNodeSpecificType() throws Exception {
        final String path = "subst_" + System.currentTimeMillis();
        final String code =
            "var n = node.addNode('" + path + "', 'nt:folder');\n"
            + "out.print(n['jcr:primaryType']);\n"
        ;
        assertEquals("nt:folder", script.evalToString(code, data));
    }

    public void testGetNode() throws Exception {
        final String path = "subgn_" + System.currentTimeMillis();
        final String code =
            "node.addNode('" + path + "', 'nt:resource');\n"
            + "var n=node.getNode('" + path + "');\n"
            + "out.print(n['jcr:primaryType']);\n"
        ;
        assertEquals("nt:resource", script.evalToString(code, data));
    }

    public void testGetProperty() throws Exception {
        final String code = "out.print(node.getProperty('text'));";
        assertEquals(testText, script.evalToString(code, data));
    }

    public void testGetNodesNoPattern() throws Exception {
        final String path = "subgnnp_" + System.currentTimeMillis();
        final String code =
            "node.addNode('" + path + "_A');\n"
            + "node.addNode('" + path + "_B');\n"
            + "var nodes = node.getNodes();\n"
            + "for (i in nodes) { out.print(nodes[i].getName() + ' '); }\n"
        ;
        assertEquals(path + "_A " + path + "_B ", script.evalToString(code, data));
    }

    public void testGetNodesWithPattern() throws Exception {
        final String path = "subgnnp_" + System.currentTimeMillis();
        final String code =
            "node.addNode('1_" + path + "_A');\n"
            + "node.addNode('1_" + path + "_B');\n"
            + "node.addNode('2_" + path + "_C');\n"
            + "var nodes = node.getNodes('1_*');\n"
            + "for (i in nodes) { out.print(nodes[i].getName() + ' '); }\n"
        ;
        assertEquals("1_" + path + "_A 1_" + path + "_B ", script.evalToString(code, data));
    }

    public void testRemoveNode() throws Exception {
        final String code =
            "node.addNode('toremove');\n"
            + "out.print(node.hasNode('toremove'))\n"
            + "out.print(' ')\n"
            + "node.getNode('toremove').remove()\n"
            + "out.print(node.hasNode('toremove'))\n"
        ;
        assertEquals("true false", script.evalToString(code, data));
    }

    /** Test SLING-389 */
    public void testForCurrentNode() throws Exception {
        final String code = "for (var a in node) {}; out.print('ok')";
        assertEquals("ok", script.evalToString(code, data));
    }

    public void testChildNodeAccess() throws Exception {
        final String path = "subtcna_" + System.currentTimeMillis();
        final String code =
            "node.addNode('" + path + "');\n"
            + "var n=node.getNode('" + path + "');\n"
            + "out.print(n['jcr:primaryType']);\n"
            + "out.print(' ');\n"
            + "var n2=node['" + path + "'];\n"
            + "out.print(n2['jcr:primaryType']);\n"
        ;
        assertEquals("nt:unstructured nt:unstructured", script.evalToString(code, data));
    }

    /** Verify that the getAncestor() method (which is not explicitely defined in ScriptableNode)
     *  is available, to check SLING-397.
     */
    public void testGetAncestor() throws Exception {
        {
            final String code = "out.print(node.getAncestor(0).getPath());";
            assertEquals("/", script.evalToString(code, data));
        }

        {
            final String code = "out.print(node.getAncestor(0)['jcr:primaryType']);";
            assertEquals("rep:root", script.evalToString(code, data));
        }
    }

    public void testIsNodeType() throws Exception {
        final String code =
            "out.print(node.isNodeType('nt:unstructured'));\n"
            + "out.print(' ');\n"
            + "out.print(node.isNodeType('nt:file'));"
        ;
        assertEquals("true false", script.evalToString(code, data));
    }

    public void testGetSession() throws Exception {
        assertEquals(
                "Root node found via node.session",
                "/",
                script.eval("node.session.getRootNode().getPath()", data)
        );
        assertEquals(
                "Root node found via node.getSession()",
                "/",
                script.eval("node.getSession().getRootNode().getPath()", data)
        );
    }

    /**
     * Test for regressing this issue:
     * https://issues.apache.org/jira/browse/SLING-534
     */
    public void testMultiValReferencePropLookup() throws Exception {
        Node refNode1 = getNewNode();
        refNode1.addMixin("mix:referenceable");
        Node refNode2 = getNewNode();
        refNode2.addMixin("mix:referenceable");

        node.setProperty("singleRef", refNode1);
        node.setProperty("multiRef", new Value[] {
            session.getValueFactory().createValue(refNode1),
            session.getValueFactory().createValue(refNode2) });

        String code = "node['singleRef']";
        assertTrue(script.eval(code, data) instanceof Node);
        code = "node.singleRef instanceof Array";
        assertEquals(false, script.eval(code, data));

        code = "node.multiRef instanceof Array";
        assertEquals(true, script.eval(code, data));

        code = "node.multiRef[0]";
        assertTrue(script.eval(code, data) instanceof Node);
    }
}
