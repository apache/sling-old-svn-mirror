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
package org.apache.sling.testing.mock.jcr;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.jackrabbit.JcrConstants;
import org.junit.Before;
import org.junit.Test;

import javax.jcr.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MockNodeTest {

    private Session session;
    private Node rootNode;
    private Node node1;
    private Property prop1;
    private Node node11;

    @Before
    public void setUp() throws RepositoryException {
        this.session = MockJcr.newSession();
        this.rootNode = this.session.getRootNode();
        this.node1 = this.rootNode.addNode("node1");
        this.prop1 = this.node1.setProperty("prop1", "value1");
        this.node11 = this.node1.addNode("node11");
    }

    @Test
    public void testGetNodes() throws RepositoryException {
        final Node node111 = this.node11.addNode("node111");

        NodeIterator nodes = this.node1.getNodes();
        assertEquals(1, nodes.getSize());
        assertEquals(this.node11, nodes.nextNode());

        assertTrue(this.node1.hasNodes());
        assertTrue(this.node11.hasNodes());
        assertFalse(node111.hasNodes());

        nodes = this.node1.getNodes("^node.*$");
        assertEquals(1, nodes.getSize());
        assertEquals(this.node11, nodes.nextNode());

        nodes = this.node1.getNodes("unknown?");
        assertEquals(0, nodes.getSize());
    }

    @Test
    public void testGetProperties() throws RepositoryException {
        PropertyIterator properties = this.node1.getProperties();
        Map<String, Property> props = propertiesToMap(properties);
        assertEquals(2, properties.getSize());
        assertEquals(this.prop1, props.get("prop1"));

        assertTrue(this.node1.hasProperties());
        assertTrue(this.node11.hasProperties());

        properties = this.node1.getProperties("^prop.*$");
        assertEquals(1, properties.getSize());
        assertEquals(this.prop1, properties.next());

        properties = this.node1.getProperties("unknown?");
        assertEquals(0, properties.getSize());
    }

    private Map<String, Property> propertiesToMap(PropertyIterator properties) throws RepositoryException {
        final HashMap<String, Property> props = new HashMap<String, Property>();
        while (properties.hasNext()) {
            final Property property = properties.nextProperty();
            props.put(property.getName(), property);
        }
        return props;
    }

    @Test
    public void testPrimaryType() throws RepositoryException {
        assertEquals("nt:unstructured", this.node1.getPrimaryNodeType().getName());
        assertEquals("nt:unstructured", this.node1.getProperty("jcr:primaryType").getString());
        final PropertyIterator properties = this.node1.getProperties();
        while (properties.hasNext()) {
            final Property property = properties.nextProperty();
            if (JcrConstants.JCR_PRIMARYTYPE.equals(property.getName())) {
                return;
            }
        }
        fail("Properties did not include jcr:primaryType");
    }

    @Test
    public void testIsNode() {
        assertTrue(this.node1.isNode());
        assertFalse(this.prop1.isNode());
    }

    @Test
    public void testHasNode() throws RepositoryException {
        assertTrue(this.node1.hasNode("node11"));
        assertFalse(this.node1.hasNode("node25"));
    }

    @Test
    public void testHasProperty() throws RepositoryException {
        assertTrue(this.node1.hasProperty("prop1"));
        assertFalse(this.node1.hasProperty("prop25"));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetUUID() throws RepositoryException {
        assertEquals(this.node1.getIdentifier(), this.node1.getUUID());
    }

    @Test
    public void testGetPrimaryItem() throws RepositoryException {
        Node dataParent = this.node1.addNode("dataParent");
        Property dataProperty = dataParent.setProperty(JcrConstants.JCR_DATA, "data");
        assertEquals(dataProperty, dataParent.getPrimaryItem());

        Node contentParent = this.node1.addNode("contentParent");
        Node contentNode = contentParent.addNode(JcrConstants.JCR_CONTENT);
        assertEquals(contentNode, contentParent.getPrimaryItem());
    }

    @Test(expected = ItemNotFoundException.class)
    public void testGetPrimaryItemNotFound() throws RepositoryException {
        this.node1.getPrimaryItem();
    }

    @Test
    public void testNtFileNode() throws RepositoryException {
        Node ntFile = rootNode.addNode("testFile", JcrConstants.NT_FILE);
        assertNotNull(ntFile.getProperty(JcrConstants.JCR_CREATED).getDate());
        assertNotNull(ntFile.getProperty("jcr:createdBy").getString());
    }

    @Test
    public void testAddMixinAndGetMixinTypes() throws RepositoryException {
        String mixinName = "mixinName";
        Node nodeWithMixin = rootNode.addNode("nodeWithMixin");
        assertTrue(ArrayUtils.isEmpty(nodeWithMixin.getMixinNodeTypes()));
        nodeWithMixin.addMixin(mixinName);
        assertTrue(nodeWithMixin.getMixinNodeTypes().length == 1);
        assertEquals(mixinName, nodeWithMixin.getMixinNodeTypes()[0].getName());
    }

    @Test
    public void testGetIndex() throws RepositoryException {
        Node parentNode = rootNode.addNode("parent");
        Node node1 = parentNode.addNode("node1");
        Node node2 = parentNode.addNode("node2");
        Node node3 = parentNode.addNode("node3");
        assertEquals(0, node1.getIndex());
        assertEquals(1, node2.getIndex());
        assertEquals(2, node3.getIndex());
    }

    @Test
    public void testNodeRepositioning() throws  RepositoryException {
        Node parent = rootNode.addNode("parent");
        String node1Name = "a";
        String node2Name = "b";
        String node3Name = "c";
        String node4Name = "d";
        Node node1 = parent.addNode(node1Name);
        Node node2 = parent.addNode(node2Name);
        Node node3 = parent.addNode(node3Name);
        Node node4 = parent.addNode(node4Name);
        assertEquals(0, node1.getIndex());
        assertEquals(1, node2.getIndex());
        assertEquals(2, node3.getIndex());
        assertEquals(3, node4.getIndex());

        parent.orderBefore(node3Name, node2Name);
        assertEquals(0, node1.getIndex());
        assertEquals(1, node3.getIndex());
        assertEquals(2, node2.getIndex());
        assertEquals(3, node4.getIndex());

        parent.orderBefore(node1Name, node4Name);
        assertEquals(0, node3.getIndex());
        assertEquals(1, node2.getIndex());
        assertEquals(2, node1.getIndex());
        assertEquals(3, node4.getIndex());
    }

    @Test
    public void testRemoveMixin() throws RepositoryException {
        String mixinName = "mixinName";
        Node nodeWithMixin = rootNode.addNode("nodeWithMixin");
        nodeWithMixin.addMixin(mixinName);
        assertFalse(ArrayUtils.isEmpty(nodeWithMixin.getMixinNodeTypes()));
        nodeWithMixin.removeMixin(mixinName);
        assertTrue(ArrayUtils.isEmpty(nodeWithMixin.getMixinNodeTypes()));
    }
}
