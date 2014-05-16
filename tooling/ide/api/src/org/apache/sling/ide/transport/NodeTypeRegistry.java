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
package org.apache.sling.ide.transport;

import java.util.Collection;
import java.util.List;

import javax.jcr.nodetype.NodeType;

/**
 * Keeps the list of all node type definitions, provides them
 * individually as well as with some utility methods such as
 * calculating all allowed child node types of a particular 
 * node type.
 */
public interface NodeTypeRegistry {
    
    public List<NodeType> getNodeTypes();
    
    public NodeType getNodeType(String name);

    public boolean isAllowedPrimaryChildNodeType(String parentNodeType, String childNodeType) throws RepositoryException;

    public Collection<String> getAllowedPrimaryChildNodeTypes(String parentNodeType) throws RepositoryException;

}
