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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class MockSessionTest {
    
    @Test
    public void testEmptySession() throws RepositoryException {
        Session session = MockJcr.newSession();
        
        Node rootNode = session.getRootNode();
        assertNotNull(rootNode);
        assertFalse(rootNode.getProperties().hasNext());
        assertFalse(rootNode.getNodes().hasNext());
    }

    @Test
    public void testRootGetNodes() throws RepositoryException {
        Session s = MockJcr.newSession();
        Node root = s.getRootNode();
        root.addNode("node1");
        root.addNode("node2");

        int countChildren = 0;
        NodeIterator iter = s.getRootNode().getNodes();
        while (iter.hasNext()) {
            iter.next();
            countChildren++;
        }
        assertEquals(2, countChildren);
    }
	
    @Test
    public void testNodePropertyCreateRead() throws RepositoryException {
        Session session = MockJcr.newSession();
        
        Node rootNode = session.getNode("/");
        assertEquals(rootNode, session.getRootNode());

        Node node1 = rootNode.addNode("node1");
        node1.setProperty("prop1a", "value1a");
        node1.setProperty("prop1b", "value1b");

        Node node2 = rootNode.addNode("node2");
        node2.setProperty("prop2", "value2");

        assertEquals(node1, rootNode.getNode("node1"));
        assertEquals(node1, session.getNode("/node1"));
        assertEquals(node1, session.getItem("/node1"));
        assertEquals(node1, session.getNodeByIdentifier(node1.getIdentifier()));
        assertTrue(session.nodeExists("/node1"));
        assertTrue(session.itemExists("/node1"));
        assertEquals(node2, rootNode.getNode("node2"));
        assertEquals(node2, session.getNode("/node2"));
        assertEquals(node2, session.getItem("/node2"));
        assertEquals(node2, session.getNodeByIdentifier(node2.getIdentifier()));
        assertTrue(session.nodeExists("/node2"));
        assertTrue(session.itemExists("/node2"));

        Property prop1a = node1.getProperty("prop1a");
        Property prop1b = node1.getProperty("prop1b");
        Property prop2 = node2.getProperty("prop2");

        assertEquals(prop1a, session.getProperty("/node1/prop1a"));
        assertEquals(prop1a, session.getItem("/node1/prop1a"));
        assertTrue(session.propertyExists("/node1/prop1a"));
        assertTrue(session.itemExists("/node1/prop1a"));
        assertEquals(prop1b, session.getProperty("/node1/prop1b"));
        assertEquals(prop1b, session.getItem("/node1/prop1b"));
        assertTrue(session.propertyExists("/node1/prop1b"));
        assertTrue(session.itemExists("/node1/prop1b"));
        assertEquals(prop2, session.getProperty("/node2/prop2"));
        assertEquals(prop2, session.getItem("/node2/prop2"));
        assertTrue(session.propertyExists("/node2/prop2"));
        assertTrue(session.itemExists("/node2/prop2"));

        assertEquals("value1a", prop1a.getString());
        assertEquals("value1b", prop1b.getString());
        assertEquals("value2", prop2.getString());

        assertFalse(session.propertyExists("/node1"));
        assertFalse(session.nodeExists("/node1/prop1a"));

        assertEquals(JcrConstants.NT_UNSTRUCTURED, node1.getPrimaryNodeType().getName());
        assertTrue(node1.isNodeType(JcrConstants.NT_UNSTRUCTURED));
        assertTrue(node1.getPrimaryNodeType().isNodeType(JcrConstants.NT_UNSTRUCTURED));

    }

    @Test
    public void testNodeRemove() throws RepositoryException {
        Session session = MockJcr.newSession();

        Node rootNode = session.getRootNode();
        Node node1 = rootNode.addNode("node1");
        assertTrue(session.itemExists("/node1"));
        node1.remove();
        assertFalse(session.itemExists("/node1"));
        assertFalse(rootNode.getNodes().hasNext());
    }

    @Test
    public void testNodesWithSpecialNames() throws RepositoryException {
        Session session = MockJcr.newSession();

        Node rootNode = session.getRootNode();

        Node node1 = rootNode.addNode("node1.ext");
        Node node11 = node1.addNode("Node Name With Spaces");
        node11.setProperty("prop11", "value11");
        Node node12 = node1.addNode("node12_ext");
        node12.setProperty("prop12", "value12");

        assertTrue(session.itemExists("/node1.ext"));
        assertTrue(session.itemExists("/node1.ext/Node Name With Spaces"));
        assertTrue(session.itemExists("/node1.ext/node12_ext"));

        assertEquals("value11", node11.getProperty("prop11").getString());
        assertEquals("value12", node12.getProperty("prop12").getString());

        NodeIterator nodes = node1.getNodes();
        assertEquals(2, nodes.getSize());
    }

    @Test
    public void testItemsExists() throws RepositoryException {
        Session session = MockJcr.newSession();

        assertFalse(session.nodeExists("/node1"));
        assertFalse(session.itemExists("/node2"));
        assertFalse(session.propertyExists("/node1/prop1"));
    }

    @Test(expected = PathNotFoundException.class)
    public void testNodeNotFoundException() throws RepositoryException {
        Session session = MockJcr.newSession();

        session.getNode("/node1");
    }

    @Test(expected = PathNotFoundException.class)
    public void testPropertyNotFoundException() throws RepositoryException {
        Session session = MockJcr.newSession();

        session.getProperty("/node1/prop1");
    }

    @Test(expected = PathNotFoundException.class)
    public void testItemNotFoundException() throws RepositoryException {
        Session session = MockJcr.newSession();

        session.getItem("/node2");
    }

    @Test(expected = ItemNotFoundException.class)
    public void testIdentifierFoundException() throws RepositoryException {
        Session session = MockJcr.newSession();

        session.getNodeByIdentifier("unknown");
    }

    @Test
    public void testNamespaces() throws RepositoryException {
        Session session = MockJcr.newSession();

        // test initial namespaces
        assertArrayEquals(new String[] { "jcr" }, session.getNamespacePrefixes());
        assertEquals("http://www.jcp.org/jcr/1.0", session.getNamespaceURI("jcr"));
        assertEquals("jcr", session.getNamespacePrefix("http://www.jcp.org/jcr/1.0"));

        // add dummy namespace
        session.setNamespacePrefix("dummy", "http://mydummy");

        assertEquals(ImmutableSet.of("jcr", "dummy"), ImmutableSet.copyOf(session.getNamespacePrefixes()));
        assertEquals("http://mydummy", session.getNamespaceURI("dummy"));
        assertEquals("dummy", session.getNamespacePrefix("http://mydummy"));

        // test via namespace registry
        NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();

        assertEquals(ImmutableSet.of("jcr", "dummy"), ImmutableSet.copyOf(namespaceRegistry.getPrefixes()));
        assertEquals(ImmutableSet.of("http://www.jcp.org/jcr/1.0", "http://mydummy"),
                ImmutableSet.copyOf(namespaceRegistry.getURIs()));
        assertEquals("http://mydummy", namespaceRegistry.getURI("dummy"));
        assertEquals("dummy", namespaceRegistry.getPrefix("http://mydummy"));

        // remove dummy namespace
        namespaceRegistry.unregisterNamespace("dummy");

        assertEquals(ImmutableSet.of("jcr"), ImmutableSet.copyOf(session.getNamespacePrefixes()));
        assertEquals("http://www.jcp.org/jcr/1.0", session.getNamespaceURI("jcr"));
        assertEquals("jcr", session.getNamespacePrefix("http://www.jcp.org/jcr/1.0"));
    }

    @Test
    public void testUserId() {
        Session session = MockJcr.newSession();

        assertEquals(MockJcr.DEFAULT_USER_ID, session.getUserID());
    }

    @Test
    public void testWithCustomUserWorkspace() {
        Session mySession = MockJcr.newSession("myUser", "myWorkspace");
        assertEquals("myUser", mySession.getUserID());
        assertEquals("myWorkspace", mySession.getWorkspace().getName());
    }

    @Test
    public void testSaveRefresh() throws RepositoryException {
        Session session = MockJcr.newSession();

        // methods can be called without any effect
        assertFalse(session.hasPendingChanges());
        session.save();
        session.refresh(true);
        session.refresh(false);
    }

    @Test
    public void testHasPendingChanges() throws RepositoryException {
        Session session = MockJcr.newSession();

        Node foo = session.getRootNode().addNode("foo");
        assertTrue(session.hasPendingChanges());
        session.save();
        assertFalse(session.hasPendingChanges());
        foo.setProperty("bar1", "foobar");
        assertTrue(session.hasPendingChanges());
        session.save();
        assertFalse(session.hasPendingChanges());
        foo.getProperty("bar1").remove();
        assertTrue(session.hasPendingChanges());
        session.save();
        assertFalse(session.hasPendingChanges());
        foo.remove();
        assertTrue(session.hasPendingChanges());
        session.save();
        assertFalse(session.hasPendingChanges());
    }

    @Test
    public void testGetRepository() {
        Session session = MockJcr.newSession();

        assertNotNull(session.getRepository());
    }

    @Test
    public void testCheckPermission() throws RepositoryException {
        Session session = MockJcr.newSession();

        session.checkPermission("/any/path", "anyActions");
    }

    @Test
    public void testPathsAreNormalized() throws RepositoryException {
        Session session = MockJcr.newSession();
        // 3.4.6 Passing Paths
        // When a JCR path is passed as an argument to a JCR method it may be normalized
        // or non-normalized and in standard or non-standard form.

        session.getRootNode().addNode("foo");
        assertTrue("Requesting node /foo/ should succeed", session.nodeExists("/foo/"));
        assertTrue("Requesting item /foo/ should succeed", session.itemExists("/foo/"));

        session.getRootNode().addNode("bar/");
        assertTrue("Creating /bar/ should succeed", session.nodeExists("/bar"));

        session.removeItem("/foo/");
        assertFalse("Removing /foo/ should succeed", session.nodeExists("/foo"));
    }
    
    @Test
    public void testNewState() throws RepositoryException {
        Session session = MockJcr.newSession();

        Node node = session.getRootNode().addNode("foo");
        Property property = node.setProperty("testProp", "value123");
        assertTrue(node.isNew());
        assertTrue(property.isNew());
        
        session.save();
        assertFalse(node.isNew());
        assertFalse(property.isNew());
    }

    @Test
    public void testLogout() throws Exception {
        Session session = MockJcr.newSession();

        assertTrue(session.isLive());
        session.logout();
        assertFalse(session.isLive());
    }
    
}
