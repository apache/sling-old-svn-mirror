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
package org.apache.sling.ide.eclipse.ui.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.transport.Result;
import org.apache.sling.ide.util.PathUtil;

// TODO this should be made API after merging the vlt branch back to trunk
public class SerializationKindManager {

    // TODO this list should be picked up from the config.xml file, not hardcoded
    private final Set<String> fullMetadataNodeTypes = new HashSet<String>();
    {
        fullMetadataNodeTypes.add("rep:AccessControl");
        fullMetadataNodeTypes.add("rep:Policy");
        fullMetadataNodeTypes.add("cq:Widget");
        fullMetadataNodeTypes.add("cq:EditConfig");
        fullMetadataNodeTypes.add("cq:WorkflowModel");
        fullMetadataNodeTypes.add("vlt:FullCoverage");
        fullMetadataNodeTypes.add("mix:language");
        fullMetadataNodeTypes.add("sling:OsgiConfig");
    }
    private final Set<String> fileNodeTypes = new HashSet<String>();
    private final Set<String> folderNodeTypes = new HashSet<String>();

    public void init(Repository repository) throws RepositoryException {

        // first pass, init the mappings
        Map<String, String[]> nodeTypesToParentNodeTypes = new HashMap<String, String[]>();

        Result<ResourceProxy> jcrSystem = repository.newListChildrenNodeCommand("/jcr:system/jcr:nodeTypes").execute();
        for (ResourceProxy child : jcrSystem.get().getChildren()) {
            String nodeType = PathUtil.getName(child.getPath());
            String[] superTypes = (String[]) child.getProperties().get("jcr:supertypes");

            if (superTypes.length == 0) {
                continue;
            }

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

        if (Repository.NT_FOLDER.equals(nodeType)) {
            return SerializationKind.FOLDER;
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

    public SerializationKind getSerializationKind(String nodeTypeName) {

        if (fileNodeTypes.contains(nodeTypeName)) {
            return SerializationKind.FILE;
        }

        if (folderNodeTypes.contains(nodeTypeName)) {
            return SerializationKind.FOLDER;
        }

        if (fullMetadataNodeTypes.contains(nodeTypeName)) {
            return SerializationKind.METADATA_FULL;
        }

        return SerializationKind.METADATA_PARTIAL;
    }

    enum SerializationKind {
        FILE, FOLDER, METADATA_PARTIAL, METADATA_FULL;
    }
}
