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

/**
 * The <code>ModifiableValueMap</code> is an extension
 * of the {@link ValueMap} which allows to modify and
 * persist properties. All changes to this map are transient
 * and only available through this map instance.
 *
 * Changes can be pushed into the transient persistence layer
 * with a call to {@link #update()}. This causes the changes
 * to be stored in the persistence layer for committing. Once
 * {@link ResourceResolver#commit()} is called, the
 * changes are finally persisted.
 *
 * Note, that each time you call {@link Resource#adaptTo(Class)}
 * you get a new map instance which does not share modified
 * properties with other representations until {@link #update()}
 * is called. In general, to avoid confusion, it's better to use
 * one modifiable value map for a resource per resource resolver.
 *
 * @since 2.2
 */
public interface ModifiableValueMap extends ValueMap {

    /**
     * Persists the changes in the transient persistence layer.
     * Once update is called this map has no temporary changes
     * any more.
     * A call to {@link ResourceResolver#commit()} is required
     * to permanently persist the changes.
     * @throws PersistenceException If the changes can't be persisted.
     */
    void update() throws PersistenceException;

    /**
     * Revert temporary changes.
     */
    void revert();

    /**
     * Are there any temporary changes?
     */
    boolean hasChanges();
}
