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

import org.apache.sling.api.adapter.AdapterManager;

/**
 * The <code>SyntheticResource</code> class is a simple implementation of the
 * <code>Resource</code> interface which may be used to provide a resource
 * object which has no actual resource data.
 */
public class SyntheticResource implements Resource {

    /** The adapter manager used for adapting the synthetic resource. */
    private static volatile AdapterManager ADAPTER_MANAGER;

    /**
     * Set the adapter manager to be used by a synthetic resource.
     * A bundle implementing the adapter manager can set the manager through this method.
     * The set adapter manager will be used in the {@link #adaptTo(Class)} method
     * of a synthetic resource.
     * @param adapterMgr The adapter manager.
     */
    public static void setAdapterManager(final AdapterManager adapterMgr) {
        ADAPTER_MANAGER = adapterMgr;
    }

    /**
     * Unset an adapter manager previously set with {@link #setAdapterManager(AdapterManager)}
     * @param adapterMgr The adapter manager
     */
    public static void unsetAdapterManager(final AdapterManager adapterMgr) {
        if ( ADAPTER_MANAGER == adapterMgr ) {
            ADAPTER_MANAGER = null;
        }
    }

    /** The resoure resolver to which this resource is related */
    private final ResourceResolver resourceResolver;

    /** The path of the synthetic resource */
    private final String path;

    /** The type this synthetic resource assumes.
     * TODO as soon as we remove the {@link #setResourceType(String)} methode we can make this final. */
    private String resourceType;

    /** The metadata of this resource just containig the resource path */
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
     * Helper method for sub classes to set the resource type
     * @param resourceType The resource type
     * @deprecated The resource type should be set through the constructor.
     */
    @Deprecated
    protected void setResourceType(String resourceType) {
        if (this.resourceType != null) {
            throw new IllegalArgumentException("Resource type already set ("
                    + this.resourceType + "), cannot change it");
        }
        this.resourceType = resourceType;
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

    /**
     * If a adapter manager has been set through {@link #setAdapterManager(AdapterManager)}
     * this adapter manager is used to adapt the resource to the given class.
     * Otherwise this method returns <code>null</code>.
     */
    public <Type> Type adaptTo(Class<Type> type) {
        final AdapterManager adapterMgr = ADAPTER_MANAGER;
        if ( adapterMgr != null ) {
            return adapterMgr.getAdapter(this, type);
        }
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ", type=" + getResourceType()
            + ", path=" + getPath();
    }

}
