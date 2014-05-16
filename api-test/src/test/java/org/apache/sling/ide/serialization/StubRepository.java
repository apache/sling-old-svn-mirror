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

import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.FileInfo;
import org.apache.sling.ide.transport.NodeTypeRegistry;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.transport.Result;

public class StubRepository implements Repository {

    @Override
    public Command<ResourceProxy> newListChildrenNodeCommand(final String path) {

        if ("/jcr:system/jcr:nodeTypes".equals(path)) {
            return new Command<ResourceProxy>() {

                @Override
                public Result<ResourceProxy> execute() {

                    final ResourceProxy root = new ResourceProxy(path);
                    root.addProperty("jcr:primaryType", "rep:nodeTypes");

                    // nt:file
                    ResourceProxy ntFile = NodeTypeResourceBuilder.newBuilder(root, "nt:file")
                            .setSupertypes(new String[] { "nt:hierarchyNode" }).build();
                    
                    // nt:folder
                    ResourceProxy ntFolder = NodeTypeResourceBuilder.newBuilder(root, "nt:folder")
                            .setSupertypes(new String[] { "nt:hierarchyNode" }).build();

                    // nt:hierarchyNode
                    ResourceProxy ntHierarchyNode = NodeTypeResourceBuilder.newBuilder(root, "nt:hierarchyNode")
                            .setSupertypes(new String[] { "mix:created", "nt:base" }).build();

                    // nt:unstructured
                    ResourceProxy ntUnstructured = NodeTypeResourceBuilder.newBuilder(root, "nt:unstructured")
                            .setSupertypes(new String[] {"nt:base" }).build();

                    // nt:base
                    ResourceProxy ntBase = NodeTypeResourceBuilder.newBuilder(root, "nt:base")
                            .setSupertypes(new String[] { }).build();
                    
                    // sling:OsgiConfig
                    ResourceProxy slingOsgiConfig = NodeTypeResourceBuilder.newBuilder(root, "sling:OsgiConfig")
                            .setSupertypes(new String[] {"nt:hierarchyNode", "nt:unstructured" }).build();
                    
                    // sling:Folder
                    ResourceProxy slingFolder = NodeTypeResourceBuilder.newBuilder(root, "sling:Folder")
                            .setSupertypes(new String[] {"nt:folder" }).build();
                    
                    // vlt:FullCoverage
                    ResourceProxy vltFullCoverage = NodeTypeResourceBuilder.newBuilder(root, "vlt:FullCoverage")
                            .setIsMixin(true).setSupertypes(new String[] {}).build();

                    root.addChild(ntFile);
                    root.addChild(ntFolder);
                    root.addChild(ntHierarchyNode);
                    root.addChild(ntUnstructured);
                    root.addChild(ntBase);
                    root.addChild(slingOsgiConfig);
                    root.addChild(slingFolder);
                    root.addChild(vltFullCoverage);

                    return new Result<ResourceProxy>() {
                        public ResourceProxy get() throws RepositoryException {
                            return root;
                        }

                        @Override
                        public boolean isSuccess() {
                            return true;
                        };
                    };
                }

                @Override
                public String getPath() {
                    return path;
                }
            };
        }

        return null;
    }

    @Override
    public Command<ResourceProxy> newGetNodeContentCommand(String path) {
        return null;
    }

    @Override
    public Command<byte[]> newGetNodeCommand(String path) {
        return null;
    }

    @Override
    public Command<Void> newDeleteNodeCommand(ResourceProxy resource) {
        return null;
    }

    @Override
    public Command<Void> newAddOrUpdateNodeCommand(FileInfo fileInfo, ResourceProxy resourceInfo) {
        return null;
    }

    @Override
    public RepositoryInfo getRepositoryInfo() {
        return null;
    }
    
    @Override
    public NodeTypeRegistry getNodeTypeRegistry() {
        return null;
    }
}