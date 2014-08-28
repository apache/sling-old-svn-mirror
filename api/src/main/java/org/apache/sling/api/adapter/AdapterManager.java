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
package org.apache.sling.api.adapter;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>AdapterManager</code> defines the service interface for a manager
 * for object adaption. The adapter manager coordinates the registered
 * {@link AdapterFactory} services on behalf of clients wishing to adapt objects
 * to other types. One such client is the {@link SlingAdaptable} class, which
 * uses the implementation of this bundle to adapt "itself".
 * <p>
 * Clients may either extend from the {@link SlingAdaptable} class or access the
 * <code>AdapterManager</code> service from the OSGi service registry to adapt
 * objects to other types.
 * <p>
 * This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface AdapterManager {

    /**
     * The name under which this service is registered with the OSGi service
     * registry.
     */
    String SERVICE_NAME = "org.apache.sling.api.adapter.AdapterManager";

    /**
     * Returns an adapter object of the requested <code>AdapterType</code> for
     * the given <code>adaptable</code> object.
     * <p>
     * The <code>adaptable</code> object may be any non-<code>null</code> object
     * and is not required to implement the <code>Adaptable</code> interface.
     *
     * @param <AdapterType> The generic type of the adapter (target) type.
     * @param adaptable The object to adapt to the adapter type.
     * @param type The type to which the object is to be adapted.
     * @return The adapted object or <code>null</code> if no factory exists to
     *         adapt the <code>adaptable</code> to the <code>AdapterType</code>
     *         or if the <code>adaptable</code> cannot be adapted for any other
     *         reason.
     */
    <AdapterType> AdapterType getAdapter(Object adaptable,
            Class<AdapterType> type);

}