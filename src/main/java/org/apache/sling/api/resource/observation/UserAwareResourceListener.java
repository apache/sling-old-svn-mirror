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
package org.apache.sling.api.resource.observation;

import java.util.List;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.ResourceResolver;

import aQute.bnd.annotation.ConsumerType;

/**
 * A resource listener which allows to act on the changes in the context of the user
 * who initiated the changes. If that information is not known, a resolver using the
 * access rights of a configured service user is used.
 * <p>
 * By default a resource listener gets only local events which means events
 * caused by changes persisted on the same instance as this listener is registered.
 * If the resource listener is interested in external events, the implementation
 * should implement the {@link ExternalResourceListener} interface.
 */
@ConsumerType
public interface UserAwareResourceListener {

    /**
     * Array of paths - required.
     */
    String PATHS = "resource.paths";

    /**
     * Array of change types - optional.
     * If this property is missing, added, removed and changed events are reported.
     */
    String CHANGES = "resource.change.types";

    /**
     * Required property containing the service user name to use for the ResourceResolver
     * passed to onChange, if the change event does not provide the actual user.
     * The value of this property must be of type string.
     */
    String SERVICE_USER = "resource.serviceuser";

    /**
     * Report a resource change based on the filter properties of this listener.
     * @param resolver A resolver using the access rights of the user who initiated
     *        the change or the configured service user
     * @param changes The changes.
     * @param usesInitiatingUser {@code true} if the resolver uses the user initiating
     *                           the changes.
     */
    void onChange(@Nonnull ResourceResolver resolver,
                  @Nonnull List<ResourceChange> changes,
                  boolean usesInitiatingUser);
}
