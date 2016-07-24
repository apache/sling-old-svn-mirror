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
package org.apache.sling.spi.resource.provider;

import java.util.List;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.observation.ResourceChange;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A {@code ResourceProvider} must use an observation reporter
 * to report changes to resources. The resource provider gets
 * an instance of this reporter through the {@link ProviderContext}.
 *
 * @since 1.0.0 (Sling API Bundle 2.11.0)
 */
@ProviderType
public interface ObservationReporter {

    /**
     * Get the list of observer configurations affecting the provider this
     * reporter is bound to.
     * @return A list of observer configurations, the list might be empty.
     */
    @Nonnull List<ObserverConfiguration> getObserverConfigurations();

    /**
     * A resource provider can inform about a list of changes.
     * If the resource provider is not able to report external events on other instances,
     * it should set the distribute flag. In this case the resource resolver implementation
     * will distribute the events to all other instances.
     *
     * Due to performance reasons, the observation reporter might not verify if the
     * reported change matches the observer configurations.
     *
     * @param changes The list of changes.
     * @param distribute Whether the changes should be distributed to other instances.
     */
    void reportChanges(@Nonnull Iterable<ResourceChange> changes, boolean distribute);
}
