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
package org.apache.sling.ide.impl.vlt;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.nodetype.NodeType;

import org.apache.sling.ide.transport.NodeTypeRegistry;
import org.apache.sling.ide.transport.RepositoryException;

public class VltNodeTypeRegistry implements NodeTypeRegistry {
    
    private Map<String,VltNodeType> nodeTypes = new HashMap<>();

    public VltNodeTypeRegistry(VltRepository repo) throws RepositoryException {
        VltNodeTypeFactory factory = new VltNodeTypeFactory();
        factory.init(repo);
        nodeTypes = factory.getNodeTypes();
    }
    
    @Override
    public boolean isAllowedPrimaryChildNodeType(String parentNodeType, String childNodeType) throws RepositoryException {
        Set<String> allowedChildren = getNodeType(parentNodeType).getAllowedPrimaryChildNodeTypes();
        return (allowedChildren.contains(childNodeType));
    }

    @Override
    public Collection<String> getAllowedPrimaryChildNodeTypes(String parentNodeType) throws RepositoryException {
        Set<String> allowedChildren = getNodeType(parentNodeType).getAllowedPrimaryChildNodeTypes();
        return Collections.unmodifiableCollection(allowedChildren);
    }
    
    @Override
    public List<NodeType> getNodeTypes() {
        List<NodeType> result = new LinkedList<>();
        for (Iterator<VltNodeType> it = nodeTypes.values().iterator(); it.hasNext();) {
            VltNodeType nt = it.next();
            result.add(nt);
        }
        return result;
    }

    @Override
    public VltNodeType getNodeType(String name) {
        return nodeTypes.get(name);
    }
    
}
