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
package org.apache.sling.ide.test.impl.helpers;

import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.CommandContext;
import org.apache.sling.ide.transport.FallbackNodeTypeRegistry;
import org.apache.sling.ide.transport.FileInfo;
import org.apache.sling.ide.transport.NodeTypeRegistry;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.apache.sling.ide.transport.ResourceProxy;

/**
 * The <tt>SpyRepository</tt> is a implementation of a <tt>Repository</tt> that always returns {@link SpyCommand}
 * instances from its factory methods
 *
 */
public class SpyRepository implements Repository {

    @Override
    public RepositoryInfo getRepositoryInfo() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Command<Void> newAddOrUpdateNodeCommand(CommandContext context, FileInfo fileInfo, ResourceProxy resourceProxy,
            CommandExecutionFlag... flags) {

        return new SpyCommand<Void>(resourceProxy, fileInfo, null, SpyCommand.Kind.ADD_OR_UPDATE, flags);
    }

    @Override
    public Command<Void> newReorderChildNodesCommand(ResourceProxy resourceProxy) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Command<Void> newDeleteNodeCommand(String path) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Command<ResourceProxy> newListChildrenNodeCommand(String path) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Command<ResourceProxy> newGetNodeContentCommand(String path) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Command<byte[]> newGetNodeCommand(String path) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public NodeTypeRegistry getNodeTypeRegistry() {

        return FallbackNodeTypeRegistry.createRegistryWithDefaultNodeTypes();
    }
}