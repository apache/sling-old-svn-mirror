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
 * The <code>PersistableValueMap</code> is an extension
 * of the {@link ValueMap} which allows to modify and
 * persist the properties.
 *
 * Note, that each time you call {@link Resource#adaptTo(Class)}
 * you get a new map instance which does not share modified
 * properties with other representations.
 *
 * @deprecated Use the {@link ModifiableValueMap} instead.
 */
@Deprecated
public interface PersistableValueMap extends ValueMap {

    /**
     * Persists the changes.
     * @throws PersistenceException If the changes can't be persisted.
     */
    void save() throws PersistenceException;

    /**
     * Reset the changes.
     */
    void reset();
}
