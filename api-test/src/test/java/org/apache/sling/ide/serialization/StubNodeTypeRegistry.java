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
package org.apache.sling.ide.serialization;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;

import org.apache.sling.ide.transport.NodeTypeRegistry;
import org.apache.sling.ide.transport.RepositoryException;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

public class StubNodeTypeRegistry implements NodeTypeRegistry {

    List<NodeType> nodeTypes = new LinkedList<NodeType>();
    
    @Override
    public boolean isAllowedPrimaryChildNodeType(String parentNodeType,
            String childNodeType) throws RepositoryException {
        throw new IllegalStateException("not yet implemented");
    }

    @Override
    public Collection<String> getAllowedPrimaryChildNodeTypes(
            String parentNodeType) throws RepositoryException {
        throw new IllegalStateException("not yet implemented");
    }
    
    void addNodeType(String name, String[] superTypeNames) {
        nodeTypes.add(getNodeTypeMock(name, superTypeNames));
    }

    private NodeType getNodeTypeMock(String name, String[] superTypeNames) {
        NodeType nodeType = Mockito.mock(NodeType.class);
        when(nodeType.getName()).thenReturn(name);
        if (superTypeNames!=null) {
            NodeType[] superTypes = new NodeType[superTypeNames.length];
            for (int i = 0; i < superTypeNames.length; i++) {
                String aSuperTypeName = superTypeNames[i];
                NodeType aSuperType = getNodeTypeMock(aSuperTypeName, null);
                superTypes[i] = aSuperType;
            }
            when(nodeType.getSupertypes()).thenReturn(superTypes);
            when(nodeType.getDeclaredSupertypeNames()).thenReturn(superTypeNames);
        }
        return nodeType;
    }

    @Override
    public List<NodeType> getNodeTypes() {
        return new LinkedList<NodeType>(nodeTypes);
    }

    @Override
    public NodeType getNodeType(String name) {
        for (Iterator<NodeType> it = nodeTypes.iterator(); it.hasNext();) {
            NodeType nt = it.next();
            if (nt.getName().equals(name)) {
                return nt;
            }
        }
        return getNodeTypeMock(name, null);
    }

}
