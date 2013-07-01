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
 * The <code>SyntheticResource</code> class is a simple implementation of the
 * <code>Resource</code> interface which may be used to provide a resource
 * object which has no actual resource data.
 */
public class SyntheticResource extends AbstractResource {

    /** The resource resolver to which this resource is related */
    private final ResourceResolver resourceResolver;

    /** The path of the synthetic resource */
    private final String path;

    /** The type this synthetic resource assumes */
    private final String resourceType;

    /** The metadata of this resource just containing the resource path */
    private final ResourceMetadata resourceMetadata;

    /**
     * Creates a synthetic resource with the given <code>path</code> and
     * <code>resourceType</code>.
     */
    public SyntheticResource(ResourceResolver resourceResolver, String path,
            String resourceType) {
        this.resourceResolver = resourceResolver;
        this.path = path;
        this.resourceType = resourceType;
        this.resourceMetadata = new ResourceMetadata();
        this.resourceMetadata.setResolutionPath(path);
    }

    /**
     * Creates a synthetic resource with the given <code>ResourceMetadata</code>
     * and <code>resourceType</code>.
     */
    public SyntheticResource(ResourceResolver resourceResolver, ResourceMetadata rm,
    		String resourceType) {
        this.resourceResolver = resourceResolver;
        this.path = rm.getResolutionPath();
        this.resourceType = resourceType;
        this.resourceMetadata = rm;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getPath()
     */
    public String getPath() {
        return path;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceType()
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Synthetic resources by default do not have a resource super type.
     */
    public String getResourceSuperType() {
        return null;
    }

    /**
     * Returns a resource metadata object containing just the path of this
     * resource as the {@link ResourceMetadata#RESOLUTION_PATH} property.
     */
    public ResourceMetadata getResourceMetadata() {
        return resourceMetadata;
    }

    /**
     * Returns the {@link ResourceResolver} with which this synthetic resource
     * is related or <code>null</code> if none.
     */
    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ", type=" + getResourceType()
            + ", path=" + getPath();
    }

}
