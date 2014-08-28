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

import aQute.bnd.annotation.ConsumerType;

/**
 * A dynamic resource provider is an extension of a resource provider which
 * is only supported if the resource provider has been created through
 * a {@link ResourceProviderFactory}.
 *
 * A dynamic resource provider supports access to systems where the
 * connection to the system is dynamic and might go away (due to network
 * changes, updates etc.).
 *
 * The {@link #isLive()} method can be called to check whether the
 * provider is still active.
 * The {@link #close()} method should be called to free any resources
 * held by this resource provider.
 *
 * @see ResourceProviderFactory#getResourceProvider(java.util.Map)
 * @see ResourceProviderFactory#getAdministrativeResourceProvider(java.util.Map)
 *
 * @since 2.2
 */
@ConsumerType
public interface DynamicResourceProvider extends ResourceProvider {

    /**
     * Returns <code>true</code> if this resource provider has not been closed
     * yet and can still be used.
     * <p>
     * This method will never throw an exception
     * even after the resource provider has been closed
     *
     * @return <code>true</code> if the resource provider has not been closed
     *         yet and is still active.. Once the resource provider has been closed
     *         or is not active anymore, this method returns <code>false</code>.
     */
    boolean isLive();

    /**
     * Close the resource provider.
     * Once the resource provider is not used anymore, it should be closed with
     * this method.
     */
    void close();
}
