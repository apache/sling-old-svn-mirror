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
package org.apache.sling.nosql.generic.resource.impl;

import java.util.Map;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.DeepReadModifiableValueMapDecorator;
import org.apache.sling.api.wrappers.DeepReadValueMapDecorator;
import org.apache.sling.nosql.generic.adapter.NoSqlData;

/**
 * Generic implementation of a NoSQL database resource.
 */
class NoSqlResource extends AbstractResource {
    
    private final NoSqlData data;
    private final ResourceResolver resourceResolver;
    private final NoSqlResourceProvider resourceProvider;
    private final ResourceMetadata metadata;
    
    public NoSqlResource(NoSqlData data, ResourceResolver resourceResolver, NoSqlResourceProvider resourceProvider) {
        this.data = data;
        this.resourceResolver = resourceResolver;
        this.resourceProvider = resourceProvider;
        this.metadata = new ResourceMetadata();
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public String getPath() {
        return data.getPath();
    }

    public String getResourceType() {
        return getValueMap().get(ResourceResolver.PROPERTY_RESOURCE_TYPE, "nt:unstructured");
    }

    public String getResourceSuperType() {
        return getValueMap().get(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_SUPER_TYPE, String.class);
    }

    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type == ValueMap.class || type == Map.class) {
            return (AdapterType)new DeepReadValueMapDecorator(this, new NoSqlValueMap(data.getProperties(), this, resourceProvider));
        }
        if (type == ModifiableValueMap.class) {
            return (AdapterType)new DeepReadModifiableValueMapDecorator(this, new NoSqlValueMap(data.getProperties(), this, resourceProvider));
        }
        return super.adaptTo(type);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
            + ", type=" + getResourceType()
            + ", superType=" + getResourceSuperType()
            + ", path=" + getPath();
    }

}
