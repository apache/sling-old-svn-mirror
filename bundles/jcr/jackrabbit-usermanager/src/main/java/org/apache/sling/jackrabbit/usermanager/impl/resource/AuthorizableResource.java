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
package org.apache.sling.jackrabbit.usermanager.impl.resource;

import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.adapter.annotations.Adaptable;
import org.apache.sling.adapter.annotations.Adapter;
import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

/**
 * Resource implementation for Authorizable
 */
@Adaptable(adaptableClass = Resource.class, adapters = {
    @Adapter({Map.class, ValueMap.class, Authorizable.class}),
    @Adapter(condition="If the resource is an AuthorizableResource and represents a JCR User", value = User.class),
    @Adapter(condition="If the resource is an AuthorizableResource and represents a JCR Group", value = Group.class)
})
public class AuthorizableResource extends AbstractResource {
    private Authorizable authorizable = null;

    private ResourceResolver resourceResolver = null;

    private final String path;

    private final String resourceType;

    private final ResourceMetadata metadata;

    public AuthorizableResource(Authorizable authorizable,
            ResourceResolver resourceResolver, String path) {
        super();

        this.resourceResolver = resourceResolver;
        this.authorizable = authorizable;
        this.path = path;
        if (authorizable.isGroup()) {
            this.resourceType = "sling/group";
        } else {
            this.resourceType = "sling/user";
        }

        this.metadata = new ResourceMetadata();
        metadata.setResolutionPath(path);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.sling.api.resource.Resource#getPath()
     */
    public String getPath() {
        return path;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.sling.api.resource.Resource#getResourceMetadata()
     */
    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.sling.api.resource.Resource#getResourceResolver()
     */
    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.sling.api.resource.Resource#getResourceSuperType()
     */
    public String getResourceSuperType() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.sling.api.resource.Resource#getResourceType()
     */
    public String getResourceType() {
        return resourceType;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.sling.api.adapter.Adaptable#adaptTo(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type == Map.class || type == ValueMap.class) {
            return (AdapterType) new AuthorizableValueMap(authorizable); // unchecked
                                                                         // cast
        } else if (type == Authorizable.class
            || (type == User.class && !authorizable.isGroup())
            || (type == Group.class && authorizable.isGroup())) {
            return (AdapterType) authorizable;
        }

        return super.adaptTo(type);
    }

    public String toString() {
        String id = null;
        if (authorizable != null) {
            try {
                id = authorizable.getID();
            } catch (RepositoryException e) {
                // ignore it.
            }
        }
        return getClass().getSimpleName() + ", id=" + id + ", path="
            + getPath();
    }
}
