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

    // the wrapped resource
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
     */
    public Resource getResource() {
        return resource;
    }

    public String getPath() {
        return resource.getPath();
    }

    public ResourceMetadata getResourceMetadata() {
        return resource.getResourceMetadata();
    }

    public ResourceResolver getResourceResolver() {
        return resource.getResourceResolver();
    }

    public String getResourceType() {
        return resource.getResourceType();
    }

    public String getResourceSuperType() {
        return resource.getResourceSuperType();
    }
    
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return resource.adaptTo(type);
    }

    public String toString() {
        return getClass().getSimpleName() + ", type=" + getResourceType()
            + ", path=" + getPath() + ", resource=[" + getResource() + "]";
    }

}
