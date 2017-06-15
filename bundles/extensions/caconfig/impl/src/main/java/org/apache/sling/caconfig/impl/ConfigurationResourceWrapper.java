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
package org.apache.sling.caconfig.impl;

import java.util.Iterator;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

/**
 * Wrapper that returns an enhanced value map for the resource
 * providing a merged map with all inherited property values.
 * Unfortunately it's not possible to use {@link org.apache.sling.api.resource.ResourceWrapper} because when adapting
 * to a Sling Model the replace valuemap would not take effect but the original value map. 
 */
public final class ConfigurationResourceWrapper extends AbstractResource {
    
    private final Resource resource;
    private final ValueMap props;

    public ConfigurationResourceWrapper(Resource resource, ValueMap props) {
        this.resource = unwrap(resource);
        this.props = props;
    }
    
    private static Resource unwrap(Resource resource) {
        if (resource instanceof ConfigurationResourceWrapper) {
            return ((ConfigurationResourceWrapper)resource).resource;
        }
        else {
            return resource;
        }
    }
    
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type == ValueMap.class) {
            return (AdapterType)props;
        }
        return super.adaptTo(type);
    }

    public ValueMap getValueMap() {
        return props;
    }

    public String getPath() {
        return resource.getPath();
    }

    public String getName() {
        return resource.getName();
    }

    public Resource getParent() {
        return resource.getParent();
    }

    public Iterator<Resource> listChildren() {
        return resource.listChildren();
    }

    public Iterable<Resource> getChildren() {
        return resource.getChildren();
    }

    public Resource getChild(String relPath) {
        return resource.getChild(relPath);
    }

    public String getResourceType() {
        return resource.getResourceType();
    }

    public String getResourceSuperType() {
        return resource.getResourceSuperType();
    }

    public boolean hasChildren() {
        return resource.hasChildren();
    }

    public boolean isResourceType(String resourceType) {
        return resource.isResourceType(resourceType);
    }

    public ResourceMetadata getResourceMetadata() {
        return resource.getResourceMetadata();
    }

    public ResourceResolver getResourceResolver() {
        return resource.getResourceResolver();
    }
    
    /**
     * @return Returns a string representation of this wrapper consisting of the class'
     * simple name, the {@link #getResourceType() resource type} and
     * {@link #getPath() path} as well as the string representation of the wrapped resource}.
     */
    @Override
    public String toString() {
        final String className;
        if (getClass().getSimpleName().length() == 0) {
            className = getClass().getName();
        } else {
            className = getClass().getSimpleName();
        }
        return className + ", type=" + getResourceType()
            + ", path=" + getPath() + ", resource=[" + resource + "]";
    }

}
