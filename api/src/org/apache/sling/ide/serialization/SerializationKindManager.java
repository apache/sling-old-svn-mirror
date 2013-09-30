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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.transport.Result;
import org.apache.sling.ide.util.PathUtil;

/**
 * The <tt>SerializationKindManager</tt> is a helper class which implements common logic dealing with how to serialize
 * repository contents on disk
 * 
 */
public class SerializationKindManager {

    private final Set<String> fullMetadataNodeTypes = new HashSet<String>();
    private final Set<String> fileNodeTypes = new HashSet<String>();
    private final Set<String> folderNodeTypes = new HashSet<String>();

    public void init(Repository repository) throws RepositoryException {

        // first pass, init the mappings
        Map<String, String[]> nodeTypesToParentNodeTypes = new HashMap<String, String[]>();

        Result<ResourceProxy> jcrSystem = repository.newListChildrenNodeCommand("/jcr:system/jcr:nodeTypes").execute();
        for (ResourceProxy child : jcrSystem.get().getChildren()) {
            String nodeType = PathUtil.getName(child.getPath());
            String[] superTypes = (String[]) child.getProperties().get("jcr:supertypes");

            nodeTypesToParentNodeTypes.put(nodeType, superTypes);
        }

        // detect node types which have an nt:file or nt:folder parent in the hierarchy
        for (String nodeType : nodeTypesToParentNodeTypes.keySet()) {
            SerializationKind serializationKind = getSerializationKind(nodeType, nodeTypesToParentNodeTypes);
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

    private SerializationKind getSerializationKind(String nodeType, Map<String, String[]> nodeTypesToParentNodeTypes) {

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
        String[] parents = nodeTypesToParentNodeTypes.get(nodeType);
        if (parents == null)
            return null;

        for (String parent : parents) {
            SerializationKind parentSerializationKind = getSerializationKind(parent, nodeTypesToParentNodeTypes);
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
