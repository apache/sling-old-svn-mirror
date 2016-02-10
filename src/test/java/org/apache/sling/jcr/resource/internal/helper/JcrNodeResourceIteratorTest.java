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
package org.apache.sling.jcr.resource.internal.helper;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockNodeIterator;
import org.apache.sling.jcr.resource.internal.HelperData;
import org.apache.sling.jcr.resource.internal.PathMapperImpl;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrNodeResourceIterator;

import junit.framework.TestCase;

public class JcrNodeResourceIteratorTest extends TestCase {

    private HelperData getHelperData() {
        return new HelperData(new AtomicReference<DynamicClassLoaderManager>(), new PathMapperImpl());
    }

    public void testEmpty() {
        NodeIterator ni = new MockNodeIterator(null);
        JcrNodeResourceIterator ri = new JcrNodeResourceIterator(null, null, null, ni, getHelperData(), null);

        assertFalse(ri.hasNext());

        try {
            ri.next();
            fail("Expected no element in the iterator");
        } catch (NoSuchElementException nsee) {
            // expected
        }
    }

    public void testSingle() throws RepositoryException {
        String path = "/parent/path/node";
        Node node = new MockNode(path);
        NodeIterator ni = new MockNodeIterator(new Node[] { node });
        JcrNodeResourceIterator ri = new JcrNodeResourceIterator(null, null, null, ni, getHelperData(), null);

        assertTrue(ri.hasNext());
        Resource res = ri.next();
        assertEquals(path, res.getPath());
        assertEquals(node.getPrimaryNodeType().getName(), res.getResourceType());

        assertFalse(ri.hasNext());

        try {
            ri.next();
            fail("Expected no element in the iterator");
        } catch (NoSuchElementException nsee) {
            // expected
        }
    }

    public void testMulti() throws RepositoryException {
        int numNodes = 10;
        String pathBase = "/parent/path/node/";
        Node[] nodes = new Node[numNodes];
        for (int i=0; i < nodes.length; i++) {
            nodes[i] = new MockNode(pathBase + i, "some:type" + i);
        }
        NodeIterator ni = new MockNodeIterator(nodes);
        JcrNodeResourceIterator ri = new JcrNodeResourceIterator(null, null, null, ni, getHelperData(), null);

        for (int i=0; i < nodes.length; i++) {
            assertTrue(ri.hasNext());
            Resource res = ri.next();
            assertEquals(pathBase + i, res.getPath());
            assertEquals(nodes[i].getPrimaryNodeType().getName(), res.getResourceType());
        }

        assertFalse(ri.hasNext());

        try {
            ri.next();
            fail("Expected no element in the iterator");
        } catch (NoSuchElementException nsee) {
            // expected
        }
    }

    public void testRoot() throws RepositoryException {
        String path = "/child";
        Node node = new MockNode(path);
        NodeIterator ni = new MockNodeIterator(new Node[] { node });
        JcrNodeResourceIterator ri = new JcrNodeResourceIterator(null, "/", null, ni, getHelperData(), null);

        assertTrue(ri.hasNext());
        Resource res = ri.next();
        assertEquals(path, res.getPath());
        assertEquals(node.getPrimaryNodeType().getName(), res.getResourceType());

        assertFalse(ri.hasNext());

        try {
            ri.next();
            fail("Expected no element in the iterator");
        } catch (NoSuchElementException nsee) {
            // expected
        }
    }
}
