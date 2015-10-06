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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.nodetype.NodeType;

import org.apache.sling.ide.transport.NodeTypeRegistry;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryException;

/**
 * The <tt>SerializationKindManager</tt> is a helper class which implements common logic dealing with how to serialize
 * repository contents on disk
 * 
 */
public class SerializationKindManager {

    private final Set<String> fullMetadataNodeTypes = new HashSet<>();
    private final Set<String> fileNodeTypes = new HashSet<>();
    private final Set<String> folderNodeTypes = new HashSet<>();

    public void init(Repository repository) throws RepositoryException {

        // first pass, init the mappings
        final NodeTypeRegistry nodeTypeRegistry = repository.getNodeTypeRegistry();
        if (nodeTypeRegistry==null) {
            throw new IllegalStateException("nodeTypeRegistry must not be null here");
        }
        final List<NodeType> nodeTypes = nodeTypeRegistry.getNodeTypes();

        // detect node types which have an nt:file or nt:folder parent in the hierarchy
        for (Iterator<NodeType> it = nodeTypes.iterator(); it.hasNext();) {
            final NodeType nt = it.next();
            final String nodeType = nt.getName();
            SerializationKind serializationKind = getSerializationKind(nodeType, nodeTypeRegistry);
            if (serializationKind == null) {
                // don't care
                continue;
            }
            switch (serializationKind) {
                case FILE:
                    fileNodeTypes.add(nodeType);
                    break;
                case FOLDER:
                    folderNodeTypes.add(nodeType);
                    break;
                case METADATA_FULL:
                    fullMetadataNodeTypes.add(nodeType);
                default:
                    // don't care
                    break;
            }

        }
    }

    private SerializationKind getSerializationKind(String nodeType, NodeTypeRegistry nodeTypeRegistry) {

        if (Repository.NT_FILE.equals(nodeType)) {
            return SerializationKind.FILE;
        }
        
        if (Repository.NT_RESOURCE.equals(nodeType)) {
        	return SerializationKind.FILE;
        }

        if (Repository.NT_FOLDER.equals(nodeType)) {
            return SerializationKind.FOLDER;
        }

        if ("rep:accessControl".equals(nodeType) || "rep:Policy".equals(nodeType) || "cq:Widget".equals(nodeType)
                || "cq:EditConfig".equals(nodeType) || "cq:WorkflowModel".equals(nodeType)
                || "vlt:FullCoverage".equals(nodeType) || "mix:language".equals(nodeType)
                || "sling:OsgiConfig".equals(nodeType)) {
            return SerializationKind.METADATA_FULL;
        }
        String[] parents = nodeTypeRegistry.getNodeType(nodeType).getDeclaredSupertypeNames();
        if (parents == null)
            return null;

        for (String parent : parents) {
            SerializationKind parentSerializationKind = getSerializationKind(parent, nodeTypeRegistry);
            if (parentSerializationKind != null) {
                return parentSerializationKind;
            }
        }

        return null;
    }

    public SerializationKind getSerializationKind(String nodeTypeName, List<String> mixinNodeTypeNames) {

        SerializationKind kind = null;

        // 1. check mixins
        for (String mixinNodeType : mixinNodeTypeNames) {
            kind = getSerializationKind0(mixinNodeType);
            if (kind != null) {
                return kind;
            }
        }

        // 2. check node type
        kind = getSerializationKind0(nodeTypeName);
        if (kind != null) {
            return kind;
        }

        // 3. default to partial
        return SerializationKind.METADATA_PARTIAL;
    }

    private SerializationKind getSerializationKind0(String nodeTypeName) {

        if (fullMetadataNodeTypes.contains(nodeTypeName)) {
            return SerializationKind.METADATA_FULL;
        }

        if (fileNodeTypes.contains(nodeTypeName)) {
            return SerializationKind.FILE;
        }

        if (folderNodeTypes.contains(nodeTypeName)) {
            return SerializationKind.FOLDER;
        }

        return null;
    }
}
