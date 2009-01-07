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
package org.apache.sling.configresource.impl;

import org.apache.sling.adapter.SlingAdaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;

public abstract class ConfigResource
    extends SlingAdaptable
    implements Resource {

    /** The resource path. */
    private final String path;

    /** The resource resolver. */
    private final ResourceResolver resolver;

    /** Metadata */
    private final ResourceMetadata metaData;

    public ConfigResource(final String path, final ResourceResolver resolver) {
        this.path = path;
        this.resolver = resolver;
        this.metaData = new ResourceMetadata();
        metaData.put(ResourceMetadata.RESOLUTION_PATH, path);
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getPath()
     */
    public String getPath() {
        return this.path;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceMetadata()
     */
    public ResourceMetadata getResourceMetadata() {
        return this.metaData;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceResolver()
     */
    public ResourceResolver getResourceResolver() {
        return this.resolver;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceSuperType()
     */
    public String getResourceSuperType() {
        return null;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceType()
     */
    public String getResourceType() {
        return ConfigAdminProviderConstants.RESOURCE_TYPE_ROOT;
    }

    /**
     * @see org.apache.sling.adapter.SlingAdaptable#adaptTo(java.lang.Class)
     */
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return super.adaptTo(type);
    }

}
