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
package org.apache.sling.commons.testing.jcr;

import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

public class MockNodeIterator implements NodeIterator {

    private Node[] nodes;
    private int idx;
    private static final Node [] EMPTY_NODE_ARRAY = {};

    public MockNodeIterator() {
        this(EMPTY_NODE_ARRAY);
    }
    
    public MockNodeIterator(Node[] nodes) {
        this.nodes = (nodes != null) ? nodes : new Node[0];
        this.idx = 0;
    }

    public Node nextNode() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        return nodes[idx++];
    }

    public long getPosition() {
        return idx-1;
    }

    public long getSize() {
        return nodes.length;
    }

    public void skip(long skipNum) {
        idx += skipNum;
    }

    public boolean hasNext() {
        return idx < nodes.length;
    }

    public Object next() {
        return nextNode();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}
