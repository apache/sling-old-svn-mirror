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

package org.apache.sling.resource.collection.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.resource.collection.ResourceCollection;
import org.apache.sling.resource.collection.ResourceCollectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Implements <code>ResourceCollectionManger</code> interface. And provides
 * create, delete, get apis for ResourceCollection.
 *
 * A ResourceCollectionManager instance can be retrieved by adapting ResourceResolver.
 */
public class ResourceCollectionManagerImpl implements ResourceCollectionManager {

    private static final Logger log = LoggerFactory.getLogger(ResourceCollectionManager.class);

    private final ResourceResolver resolver;

    public ResourceCollectionManagerImpl(ResourceResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * {@inheritDoc}
     */
    public ResourceCollection getCollection(Resource resource) {
    	if (resource != null) {
    		if (resource.isResourceType(ResourceCollection.RESOURCE_TYPE)) {
                return new ResourceCollectionImpl(resource);
            }
    	} else {
    		throw new IllegalArgumentException("resource can not be null");
    	}

    	return null;
    }

    /**
     * {@inheritDoc}
     */
    public ResourceCollection createCollection(Resource parentResource, String name)
            throws PersistenceException {
        return createCollection(parentResource, name, null);
    }

    /**
     * {@inheritDoc}
     */
    public ResourceCollection createCollection(Resource parentResource, String name,
            Map<String, Object> properties) throws PersistenceException {

        if (parentResource != null) {
        	String fullPath = parentResource.getPath() + name;

            if (resolver.getResource(fullPath) != null) {
                throw new IllegalArgumentException("invalid path, " + fullPath
                    + "resource already exists");
            }

            if (properties == null) {
                properties = new HashMap<String, Object>();
            }

            // make sure correct resource type is used
            properties.put(
                    ResourceResolver.PROPERTY_RESOURCE_TYPE,
                    ResourceCollection.RESOURCE_TYPE);

            Resource collectionRes = resolver.create(parentResource, name, properties);
            resolver.create(collectionRes, "members", null);
            log.debug("collection  {} created", fullPath);

            return new ResourceCollectionImpl(
                resolver.getResource(collectionRes.getPath()));
        } else {
            log.error("parent resource can not be null");
            throw new IllegalArgumentException("parent resource can not be null ");
        }

    }

    /**
     * {@inheritDoc}
     *
     * @throws PersistenceException
     */
    public boolean deleteCollection(Resource resource)
            throws PersistenceException {
    	if (resource != null) {
	        log.debug("collection  {} deleted", resource.getPath());
	        resolver.delete(resource);
	        return true;
    	} else {
    		throw new IllegalArgumentException("resource can not be null");
    	}
    }

    /**
     * {@inheritDoc}
     */
	public ResourceResolver getResourceResolver() {
		return resolver;
	}

}