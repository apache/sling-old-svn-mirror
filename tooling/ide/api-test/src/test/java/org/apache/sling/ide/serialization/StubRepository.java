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
        final StubNodeTypeRegistry stubNodeTypeRegistry = new StubNodeTypeRegistry();
        
        stubNodeTypeRegistry.addNodeType("nt:file", new String[] {"nt:hierarchyNode"});
        stubNodeTypeRegistry.addNodeType("nt:folder", new String[] {"nt:hierarchyNode"});
        stubNodeTypeRegistry.addNodeType("nt:hierarchyNode", new String[] {"mix:created", "nt:base"});
        stubNodeTypeRegistry.addNodeType("nt:unstructured", new String[] {"nt:base"});
        stubNodeTypeRegistry.addNodeType("nt:base", new String[] {});
        stubNodeTypeRegistry.addNodeType("sling:OsgiConfig", new String[] {"nt:hierarchyNode", "nt:unstructured"});
        stubNodeTypeRegistry.addNodeType("sling:Folder", new String[] {"nt:folder"});
        stubNodeTypeRegistry.addNodeType("vlt:FullCoverage", new String[] {});

        return stubNodeTypeRegistry;
    }
}