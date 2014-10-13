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

import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;

/**
 * Mock {@link NodeTypeManager} implementation.
 */
class MockNodeTypeManager implements NodeTypeManager {

    @Override
    public NodeType getNodeType(String nodeTypeName) {
        // accept all node types and return a mock
        return new MockNodeType(nodeTypeName);
    }

    @Override
    public boolean hasNodeType(String name) {
        // accept all node types
        return true;
    }

    // --- unsupported operations ---

    @Override
    public NodeTypeIterator getAllNodeTypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeTypeIterator getPrimaryNodeTypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeTypeIterator getMixinNodeTypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeTypeTemplate createNodeTypeTemplate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeTypeTemplate createNodeTypeTemplate(NodeTypeDefinition ntd) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeDefinitionTemplate createNodeDefinitionTemplate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyDefinitionTemplate createPropertyDefinitionTemplate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeType registerNodeType(NodeTypeDefinition ntd, boolean allowUpdate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeTypeIterator registerNodeTypes(NodeTypeDefinition[] ntds, boolean allowUpdate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unregisterNodeType(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unregisterNodeTypes(String[] names) {
        throw new UnsupportedOperationException();
    }

}
