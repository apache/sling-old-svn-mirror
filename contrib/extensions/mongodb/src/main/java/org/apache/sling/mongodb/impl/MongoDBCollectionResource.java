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
package org.apache.sling.mongodb.impl;

import java.util.Collections;
import java.util.Map;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;

public class MongoDBCollectionResource extends AbstractResource {

    private final String path;

    private final ResourceResolver resourceResolver;

    private final ResourceMetadata metadata = new ResourceMetadata();

    public MongoDBCollectionResource(final ResourceResolver resolver, final String path) {
        this.resourceResolver = resolver;
        this.path = path;
        this.metadata.setResolutionPath(this.path);
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
        return "mongodb:collection";
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceSuperType()
     */
    public String getResourceSuperType() {
        return null;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceMetadata()
     */
    public ResourceMetadata getResourceMetadata() {
        return this.metadata;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceResolver()
     */
    public ResourceResolver getResourceResolver() {
        return this.resourceResolver;
    }

    /**
     * @see org.apache.sling.api.adapter.SlingAdaptable#adaptTo(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType adaptTo(final Class<AdapterType> type) {
        if ( type == Map.class ) {
            return (AdapterType) Collections.EMPTY_MAP;
        }
        return super.adaptTo(type);
    }
}
