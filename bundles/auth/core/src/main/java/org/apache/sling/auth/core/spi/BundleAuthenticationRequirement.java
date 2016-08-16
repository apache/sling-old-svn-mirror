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
package org.apache.sling.auth.core.spi;

import java.util.Map;

import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Interface to define and manage external authentication requirements defined
 * by individual bundles. A bundle is considered a client, requirements from
 * different bundles are managed separately. If a bundle needs to handle
 * different sets of authentication requirements, it needs to handle this
 * internally.
 *
 * If the client bundle is stopped, all requirements are cleared.
 *
 * In contrast to the service listener present with {@link org.apache.sling.auth.core.impl.SlingAuthenticator}
 * which always retrieves and processes the complete list of authentication
 * requirement mappings stored in the {@link org.apache.sling.auth.core.AuthConstants#AUTH_REQUIREMENTS}
 * property, this interface allows for subsequent updates to append or remove
 * more requirements as they get detected by a given client.
 *
 * @since 1.3.0
 */
@ProviderType
public interface BundleAuthenticationRequirement {

    /**
     * Set the given {@code requirements} for the client bundle.
     * If no collection for the client bundle exists it will be created,
     * otherwise it will be replaced.
     *
     * @param requirements The requirements to be set (including replacing any existing entries).
     * @see {@link #appendRequirements(String, Map)} for a call that doesn't replace existing entries.
     * @throws NullPointerException If requirements is {@code null}
     */
    void setRequirements(@Nonnull Map<String, Boolean> requirements);

    /**
     * Append the given {@code requirements} to the collection of requirements defined
     * for the client bundle. A new collection will be created if it doesn't exist yet.
     * If the given {@code requirements} contains keys that have been set before,
     * those entries will not cause any change.
     *
     * @param requirements The requirements to be appended.
     * @see {@link #setRequirements(String, Map)} for a call that replaces all existing entries.
     * @throws NullPointerException If requirements is {@code null}
     */
    void appendRequirements(@Nonnull Map<String, Boolean> requirements);

    /**
     * Remove the specified {@code requirements} as defined by the client bundle.
     * @param requirements The requirements to be removed.
     * @throws NullPointerException If requirements is {@code null}
     */
    void removeRequirements(@Nonnull Map<String, Boolean> requirements);

    /**
     * Clear all authentication requirements registered by the client bundle.
     *
     * @throws NullPointerException If requirements is {@code null}
     */
    void clearRequirements();
}
