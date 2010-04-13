/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.jcr.resource.internal;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Decorated resource which prepends the workspace name to
 * a delegate resource's path.
 */
class WorkspaceDecoratedResource implements Resource {

    private final Resource delegate;
    private final String workspaceName;

    WorkspaceDecoratedResource(Resource resource, String workspaceName) {
        this.delegate = resource;
        this.workspaceName = workspaceName;
    }

    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return delegate.adaptTo(type);
    }

    public String getPath() {
        if (workspaceName != null) {
            return workspaceName + ":" + delegate.getPath();
        } else {
            return delegate.getPath();
        }
    }

    public ResourceMetadata getResourceMetadata() {
        return delegate.getResourceMetadata();
    }

    public ResourceResolver getResourceResolver() {
        return delegate.getResourceResolver();
    }

    public String getResourceSuperType() {
        return delegate.getResourceSuperType();
    }

    public String getResourceType() {
        return delegate.getResourceType();
    }

    public String toString() {
        return delegate.toString();
    }

}
