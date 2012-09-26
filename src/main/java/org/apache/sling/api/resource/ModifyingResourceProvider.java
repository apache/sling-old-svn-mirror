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
package org.apache.sling.api.resource;

import java.util.Map;

/**
 * A modifying resource provider is an extension of a resource provider which
 * is only supported if the resource provider has been created through
 * a {@link ResourceProviderFactory}.
 *
 * A modifying resource provider allows to create, update, and delete
 * resources. Update is handled through {@link ModifiableValueMap}.
 *
 * All changes should be kept in a transient store until {@link #commit(ResourceResolver)}
 * is called. {@link #revert(ResourceResolver)} discards all transient changes.
 *
 * If the modifying resource provider needs to clean up resources when it
 * is discarded like removing objects from the transient state which are
 * not committed etc., it should also implement the {@link DynamicResourceProvider}
 * interface.
 *
 * @see ResourceProviderFactory#getResourceProvider(java.util.Map)
 * @see ResourceProviderFactory#getAdministrativeResourceProvider(java.util.Map)
 *
 * @since 2.2.0
 */
public interface ModifyingResourceProvider extends ResourceProvider {

    /**
     * Create a new resource at the given path.
     * The new resource is put into the transient space of this provider
     * until {@link #commit(ResourceResolver)} is called.
     *
     * @param resolver The current resource resolver.
     * @param path The resource path.
     * @param properties Optional properties
     * @return The new resource.
     *
     * @throws PersistenceException If anything fails
     */
    Resource create(ResourceResolver resolver, String path, Map<String, Object> properties)
    throws PersistenceException;

    /**
     * Delete the resource at the given path.
     * This change is kept in the transient space of this provider
     * until {@link #commit(ResourceResolver)} is called.
     *
     * @param resolver The current resource resolver.
     * @param path The resource path.
     *
     * @throws PersistenceException If anything fails
     */
    void delete(ResourceResolver resolver, String path)
    throws PersistenceException;

    /**
     * Revert all transient changes: create, delete and updates.
     *
     * @param resolver The current resource resolver.
     */
    void revert(ResourceResolver resolver);

    /**
     * Commit all transient changes: create, delete and updates
     *
     * @param resolver The current resource resolver.
     *
     * @throws PersistenceException If anything fails
     */
    void commit(ResourceResolver resolver)
    throws PersistenceException;

    /**
     * Are there any transient changes?
     *
     * @param resolver The current resource resolver.
     */
    boolean hasChanges(ResourceResolver resolver);
}
