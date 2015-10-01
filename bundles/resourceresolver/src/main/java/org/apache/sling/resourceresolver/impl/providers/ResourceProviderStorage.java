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
package org.apache.sling.resourceresolver.impl.providers;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.runtime.dto.AuthType;
import org.apache.sling.resourceresolver.impl.providers.tree.PathTree;

public class ResourceProviderStorage {

    private final List<ResourceProviderHandler> allHandlers;

    private final List<ResourceProviderHandler> authRequiredHandlers;

    private final PathTree<ResourceProviderHandler> handlersTree;

    public ResourceProviderStorage(List<ResourceProviderHandler> handlers) {
        this.allHandlers = handlers;
        this.authRequiredHandlers = new ArrayList<ResourceProviderHandler>();
        for (ResourceProviderHandler h : allHandlers) {
            if (h.getInfo().getAuthType() == AuthType.required) {
                this.authRequiredHandlers.add(h);
            }
        }
        this.handlersTree = new PathTree<ResourceProviderHandler>(handlers);
    }

    public List<ResourceProviderHandler> getAll() {
        return allHandlers;
    }

    public List<ResourceProviderHandler> getAuthRequired() {
        return authRequiredHandlers;
    }

    public PathTree<ResourceProviderHandler> getTree() {
        return handlersTree;
    }
}
