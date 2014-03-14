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

import java.util.Map;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.DeepReadValueMapDecorator;

import com.mongodb.DBObject;

public class MongoDBResource extends AbstractResource {

    /** The complete resource path. */
    private final String resourcePath;

    /** The collection */
    private final String collection;

    /** The resource resolver. */
    private final ResourceResolver resourceResolver;

    /** Metadata. */
    protected ResourceMetadata metadata = new ResourceMetadata();

    /** The db object. */
    private DBObject dbObject;

    /** The MongoDB resource provider. */
    private final MongoDBResourceProvider provider;

    public MongoDBResource(final ResourceResolver resolver,
                    final String resourcePath,
                    final String collection,
                    final DBObject dbObject,
                    final MongoDBResourceProvider provider) {
        this.resourceResolver = resolver;
        this.resourcePath = resourcePath;
        this.collection = collection;
        this.dbObject = dbObject;
        this.provider = provider;
    }

    public MongoDBResource(final MongoDBResource source) {
        this.resourceResolver = source.resourceResolver;
        this.resourcePath = source.resourcePath;
        this.collection = source.collection;
        this.dbObject = source.dbObject;
        this.provider = source.provider;
    }
    /**
     * @see org.apache.sling.api.resource.Resource#getPath()
     */
    public String getPath() {
        return this.resourcePath;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceType()
     */
    public String getResourceType() {
        // get resource type from data
        final Object rt = this.dbObject.get("sling:resourceType");
        if ( rt != null ) {
            return rt.toString();
        }
        return "nt:unstructured";
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceSuperType()
     */
    public String getResourceSuperType() {
        // get resource type from data
        final Object rt = this.dbObject.get("sling:resourceSuperType");
        if ( rt != null ) {
            return rt.toString();
        }
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
        if ( type == ValueMap.class || type == Map.class ) {
            this.dbObject = this.provider.getUpdatedDBObject(this.resourcePath, this.dbObject);
            return (AdapterType) new DeepReadValueMapDecorator(this, new ReadableValueMap(this.dbObject));
        } else if ( type == ModifiableValueMap.class ) {
            this.dbObject = this.provider.getUpdatedDBObject(this.resourcePath, this.dbObject);
            return (AdapterType) new DeepReadValueMapDecorator(this, new ChangeableValueMap(this));
        }

        return super.adaptTo(type);
    }

    /**
     * Return the collection.
     */
    public String getCollection() {
        return this.collection;
    }

    /**
     * Get the current properties.
     */
    public DBObject getProperties() {
        return this.dbObject;
    }

    @Override
    public String toString() {
        return "MongoDBResource [resourcePath=" + resourcePath + ", dbPath=" + this.dbObject.get(provider.getPROP_PATH()) + ", collection=" + collection
                        + ", resourceResolver=" + resourceResolver + "]";
    }

    public void changed() {
        this.provider.changed(this);
    }
}
