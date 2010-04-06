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
 * The <code>ResourceWrapper</code> is a wrapper for any <code>Resource</code>
 * delegating all method calls to the wrapped resource by default. Extensions of
 * this class may overwrite any method to return different values as
 * appropriate.
 */
public class ResourceWrapper implements Resource {

    /** the wrapped resource */
    private final Resource resource;

    /**
     * Creates a new wrapper instance delegating all method calls to the given
     * <code>resource</code>.
     */
    public ResourceWrapper(Resource resource) {
        this.resource = resource;
    }

    /**
     * Returns the <code>Resource</code> wrapped by this instance.
     * This method can be overwritten by subclasses if required. All
     * other methods use this method to get the resource object.
     */
    public Resource getResource() {
        return resource;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getPath()
     */
    public String getPath() {
        return getResource().getPath();
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceMetadata()
     */
    public ResourceMetadata getResourceMetadata() {
        return getResource().getResourceMetadata();
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceResolver()
     */
    public ResourceResolver getResourceResolver() {
        return getResource().getResourceResolver();
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceType()
     */
    public String getResourceType() {
        return getResource().getResourceType();
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceSuperType()
     */
    public String getResourceSuperType() {
        return getResource().getResourceSuperType();
    }

    /**
     * @see org.apache.sling.api.adapter.Adaptable#adaptTo(java.lang.Class)
     */
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return getResource().adaptTo(type);
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getClass().getSimpleName() + ", type=" + getResourceType()
            + ", path=" + getPath() + ", resource=[" + getResource() + "]";
    }

}
