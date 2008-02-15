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
package org.apache.sling.api.resource;

/**
 * Simple helper class representing nonexisting resources.
 */
public final class NonExistingResource implements Resource {

    private final ResourceResolver resourceResolver;

    private final String resourceURI;

    private final ResourceMetadata resourceMetadata;

    public NonExistingResource(ResourceResolver resourceResolver,
            String resourceURI) {
        this.resourceResolver = resourceResolver;
        this.resourceURI = resourceURI;

        resourceMetadata = new ResourceMetadata();
        resourceMetadata.setResolutionPath(resourceURI);
    }

    public String getPath() {
        return resourceURI;
    }

    public String getResourceType() {
        return RESOURCE_TYPE_NON_EXISTING;
    }

    public ResourceMetadata getResourceMetadata() {
        return resourceMetadata;
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    /**
     * Returns <code>null</code> because a non-existing resource cannot adapt
     * to anything.
     */
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return null;
    }

    public String toString() {
        return getClass().getSimpleName() + ", path=" + getPath();
    }
}
