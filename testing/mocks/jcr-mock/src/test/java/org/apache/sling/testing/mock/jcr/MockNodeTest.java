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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
import org.junit.Before;
import org.junit.Test;

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
        Node ntFile = this.session.getRootNode().addNode("testFile", JcrConstants.NT_FILE);
        assertNotNull(ntFile.getProperty(JcrConstants.JCR_CREATED).getDate());
        assertNotNull(ntFile.getProperty("jcr:createdBy").getString());
    }

    @Test
    public void testGetMixinNodeTypes() throws Exception {
        assertEquals(0, this.node1.getMixinNodeTypes().length);
    }
    
}
