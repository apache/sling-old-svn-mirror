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

package org.apache.sling.resource.collection;

import java.util.Map;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * The <code>ResourceCollectionManager<code> defines the API to get, create and delete
 * resource collections {@link org.apache.sling.resource.collection.ResourceCollection}.
 *
 * The ResourceCollectionManager service can be retrieved by looking it up from the
 * service registry or by adapting a {@link ResourceResolver}.
 */
public interface ResourceCollectionManager {

    /**
     * This method returns a resource collection for the given <code>resource</code>
     * that represents a {@link ResourceCollection}.
     *
     * It returns null if given resource is not a collection
     *
     * @param resource resource that represents a collection
     * @return The {@link org.apache.sling.resource.collection.ResourceCollection} representing the collection.
     *
     */
    ResourceCollection getCollection(Resource resource);


    /**
     * This method creates a resource collection with a given name under the <code>parentResource</code>.
     * The changes are transient and have to be saved by resourceResolver.commit()
     *
     * @param parentResource parent resource where collection needs to be created.
     * @param name The name for collection.
     *
     * @return The {@link org.apache.sling.resource.collection.ResourceCollection} representing the created collection.
     *
     * @throws {@link PersistenceException} if the operation fails
     */
    ResourceCollection createCollection(Resource parentResource, String name) throws PersistenceException;

    /**
     * This method creates a resource collection with a given name under the <code>parentResource</code>.
     * The changes are transient and have to be saved by resourceResolver.commit()
     *
     * @param parentResource parent resource where collection needs to be created.
     * @param name The name for collection.
     * @param properties The additional data for resource collection
     *
     * @return The {@link org.apache.sling.resource.collection.ResourceCollection} representing the created collection.
     *
     * @throws {@link PersistenceException} if the operation fails
     */
    ResourceCollection createCollection(Resource parentResource, String name, Map<String,Object> properties) throws PersistenceException;

    /**
     * Removes the {@link org.apache.sling.resource.collection.ResourceCollection} corresponding to the collection represented by
     * <code>resource</code>.
     * The changes are transient and have to be saved by resourceResolver.commit()
     *
     * @param resource resource representing a collection to be deleted.
     * @return <code>true</code> if the collection was successfully removed.
     *
     * @throws {@link PersistenceException} if the operation fails
     */
    boolean deleteCollection(Resource resource) throws PersistenceException;
}