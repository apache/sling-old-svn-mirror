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
package org.apache.sling.auth.core;

import java.util.Map;
import javax.annotation.Nonnull;

import org.osgi.framework.ServiceReference;

/**
 * Interface to define and manage external authentication requirements defined
 * by individual service references.
 *
 * In contrast to the service listener present with {@link org.apache.sling.auth.core.impl.SlingAuthenticator}
 * which always retrieves and processes the complete list of authentication
 * requirement mappings stored in the {@link AuthConstants#AUTH_REQUIREMENTS}
 * property, this interface allows for subsequent updates to append or remove
 * more requirements as they get detected by a given service.
 */
public interface AuthenticationRequirement {
    /**
     * Set the given {@code requirements} for the specified {@code serviceReference}.
     * If no collection exists it will be created otherwise it will be replaced.
     *
     * @param serviceReference The service reference, which registered the given requirements.
     * @param requirements The requirements to be set (including replacing any existing entries).
     * @see {@link #appendRequirements(ServiceReference, Map)} for a call that doesn't replace existing entries.
     */
    void setRequirements(@Nonnull ServiceReference serviceReference, @Nonnull Map<String, Boolean> requirements);

    /**
     * Append the given {@code requirements} to the collection of requirements defined
     * for the specified {@code serviceReference}. A new collection will be created
     * if it doesn't exist yet.
     * If the given {@code requirements} contain keys that have been set before,
     * those entries will be ignored.
     *
     * @param serviceReference The service reference, which registered the given requirements.
     * @param requirements The requirements to be appended.
     * @see {@link #setRequirements(ServiceReference, Map)} for a call that replaces all existing entries.
     */
    void appendRequirements(@Nonnull ServiceReference serviceReference, @Nonnull Map<String, Boolean> requirements);

    /**
     * Remove the specified {@code requirements} as defined by given {@code serviceReference}.
     *
     * @param serviceReference The service reference, which registered the given requirements.
     * @param requirements The requirements to be removed.
     */
    void removeRequirements(@Nonnull ServiceReference serviceReference, @Nonnull Map<String, Boolean> requirements);

    /**
     * Clear all authentication requirements registered by the given {@code serviceReference}.
     *
     * @param serviceReference
     */
    void clearRequirements(@Nonnull ServiceReference serviceReference);
}