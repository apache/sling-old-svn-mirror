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
package org.apache.sling.resourcemerger.impl;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;

/**
 * Simple helper class representing nonexisting resources.
 * 
 * This is similar to NonExistingResource, with the exception that it 
 * always returns a valid resource for the parent resource unless
 * this is a top level (e.g. /foo) path.
 */
public final class StubResource extends SyntheticResource {

    /**
     * Create a new stub resource.
     * @param resourceResolver The resource resolver.
     * @param resourceURI The path of the resource.
     */
    public StubResource(final ResourceResolver resourceResolver,
            final String resourceURI) {
        super(resourceResolver, resourceURI, RESOURCE_TYPE_NON_EXISTING);
    }

    /**
     * @see org.apache.sling.api.resource.SyntheticResource#getResourceType()
     */
    public final String getResourceType() {
        // overwrite to prevent overwriting of this method in extensions of
        // this class because the specific resource type is the marker of a
        // NonExistingResource
        return RESOURCE_TYPE_NON_EXISTING;
    }

    public String toString() {
        // overwrite to only list the class name and path, type is irrelevant
        return getClass().getSimpleName() + ", path=" + getPath();
    }

    @Override
    public Resource getParent() {
        final Resource parent = super.getParent();
        if (parent != null) {
            return parent;
        } else {
            final String absPath = getPath();
            final int lastIdx = absPath.lastIndexOf('/');
            if (lastIdx <= 0) {
                return null;
            } else {
                final String parentPath = absPath.substring(0, lastIdx);
                return new StubResource(getResourceResolver(), parentPath);
            }
        }
    }
}
