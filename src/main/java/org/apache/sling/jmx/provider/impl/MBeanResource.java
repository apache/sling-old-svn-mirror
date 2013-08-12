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
package org.apache.sling.jmx.provider.impl;

import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanInfo;
import javax.management.ObjectName;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

public class MBeanResource extends AbstractResource {

    private final String path;

    private final ResourceResolver resourceResolver;

    private final ResourceMetadata metadata = new ResourceMetadata();

    private final MBeanInfo info;

    private final ObjectName objectName;

    private final String resourceType;

    public MBeanResource(final ResourceResolver resolver,
            final String resourceType,
            final String path,
            final MBeanInfo info,
            final ObjectName objectName) {
        this.resourceResolver = resolver;
        this.path = path;
        this.info = info;
        this.objectName = objectName;
        final int pos = resourceType.lastIndexOf(':');
        if ( pos == -1 ) {
            this.resourceType = resourceType;
        } else {
            this.resourceType = resourceType.substring(0, pos);
        }
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getPath()
     */
    public String getPath() {
        return this.path;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceType()
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceSuperType()
     */
    public String getResourceSuperType() {
        return "sling:mbean";
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceMetadata()
     */
    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceResolver()
     */
    public ResourceResolver getResourceResolver() {
        return this.resourceResolver;
    }

    @Override
    public <AdapterType> AdapterType adaptTo(final Class<AdapterType> type) {
        if ( type == ValueMap.class || type == Map.class ) {
            final Map<String, Object> propMap = this.getPropertiesMap();
            return (AdapterType) new ValueMapDecorator(propMap);
        }
        return super.adaptTo(type);
    }

    private Map<String, Object> getPropertiesMap() {
        final Map<String, Object> result = new HashMap<String, Object>();
        result.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, this.getResourceType());
        result.put("sling:resourceSuperType", this.getResourceSuperType());

        if ( this.info.getDescription() != null ) {
            result.put("mbean:description", this.info.getDescription());
        }
        result.put("mbean:className", this.info.getClassName());
        result.put("mbean:objectName", this.objectName.getCanonicalName());

        return result;
    }
}
