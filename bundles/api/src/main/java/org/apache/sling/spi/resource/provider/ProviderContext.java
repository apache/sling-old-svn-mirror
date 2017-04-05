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

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.path.PathSet;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The provider context...
 *
 * @since 1.0.0 (Sling API Bundle 2.11.0)
 */
@ProviderType
public interface ProviderContext {

    /** This bit is set in {@link ResourceProvider#update(long)} if observation listeners changed. */
    long OBSERVATION_LISTENER_CHANGED = 1;

    /** This bit is set in {@link ResourceProvider#update(long)} if exclude paths changed. */
    long EXCLUDED_PATHS_CHANGED       = 2;

    /**
     * Get the observation reporter for this instance.
     * If anything related to observation configuration changes,
     * {@link ResourceProvider#update(long)} is called. From that point on
     * this method needs to be called to get the updated/new observation
     * reporter. The instance previously returned (before update was called)
     * becomes invalid and must not be used anymore.
     *
     * @return The observation reporter.
     */
    @Nonnull ObservationReporter getObservationReporter();

    /**
     * Set of paths which are "hidden" by other resource providers.
     * If anything related to observation configuration changes,
     * {@link ResourceProvider#update(long)} is called. From that point on
     * this method will return a new path set with the updated/changed
     * exclude paths.
     * @return A set of paths. The set might be empty
     */
    PathSet getExcludedPaths();
}
