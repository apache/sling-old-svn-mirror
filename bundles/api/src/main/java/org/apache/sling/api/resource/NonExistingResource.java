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

import javax.annotation.Nonnull;

/**
 * Simple helper class representing nonexisting resources.
 *
 * This is an utility class to <em>create</em> non existing resources.
 * It is not meant to be used to check if a resource is a non existing
 * resource. Use the {@link ResourceUtil#isNonExistingResource(Resource)}
 * method instead (or check the resource type yourself). The reason
 * for this is that this resource might be wrapped by other resource
 * implementations like resource decorators etc.
 */
public final class NonExistingResource extends SyntheticResource {

    /**
     * Create a new non existing resource.
     * @param resourceResolver The resource resolver.
     * @param resourceURI The path of the resource.
     */
    public NonExistingResource(final ResourceResolver resourceResolver,
            final String resourceURI) {
        super(resourceResolver, resourceURI, RESOURCE_TYPE_NON_EXISTING);
    }

    /**
     * @see org.apache.sling.api.resource.SyntheticResource#getResourceType()
     */
    public final @Nonnull String getResourceType() {
        // overwrite to prevent overwriting of this method in extensions of
        // this class because the specific resource type is the marker of a
        // NonExistingResource
        return RESOURCE_TYPE_NON_EXISTING;
    }

    public String toString() {
        // overwrite to only list the class name and path, type is irrelevant
        return getClass().getSimpleName() + ", path=" + getPath();
    }

    /**
     * 
     * @return the parent resource (might be a {@link NonExistingResource} in case the parent does not exist either).
     */
    public Resource getParent() {
        Resource parent = super.getParent();
        if (parent == null) {
            return new NonExistingResource(getResourceResolver(), ResourceUtil.getParent(getPath()));
        } else {
            return parent;
        }
    }

}
