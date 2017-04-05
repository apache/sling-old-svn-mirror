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

import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.path.PathSet;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A observer configuration describes active configurations from registered listeners.
 *
 * All implementations of {@code ObserverConfiguration} are comparable using the
 * {@link Object#equals(Object)} method. Two configurations are equal, if they
 * have exactly the same parameters.
 *
 * A resource provider implementation observation must support sending remove
 * events as explained in {@link org.apache.sling.api.resource.observation.ResourceChangeListener}.
 * If an observer configuration contains a pattern or is limited to a sub tree,
 * removal events not matching the pattern or removing events of parents of
 * that sub tree must be provided to the listener nevertheless - if the
 * listener is interested in resource remove events.
 *
 * @since 1.0.0 (Sling API Bundle 2.11.0)
 */
@ProviderType
public interface ObserverConfiguration {

    /**
     * {@code true} if a listener is interested in external events.
     * @return {@code true} if a listener is interested in external events.
     */
    boolean includeExternal();

    /**
     * The set of paths this listener is interested in. Each entry is absolute.
     * @return Non empty set of paths
     */
    @Nonnull PathSet getPaths();

    /**
     * The set of excluded paths.
     * All the paths are sub paths from one entry of {@link #getPaths()}
     * @return A set of excluded paths, might be empty.
     */
    @Nonnull PathSet getExcludedPaths();

    /**
     * The set of types listeners are interested in.
     * @return Non empty set of types
     */
    @Nonnull Set<ResourceChange.ChangeType> getChangeTypes();

    /**
     * Set containing the set of property names which
     * serves as an optional hint for the underlying to
     * only report property changes enlisted, ie the
     * underlying might ignore this.
     * @return Set containing the set of property names or {@code null}
     */
    @Nonnull Set<String> getPropertyNamesHint();

    /**
     * Checks whether a path matches one of the paths of this configuration
     * but is not in the excluded paths set.
     * @param path The path to check
     * @return {@code true} if the path matches the configuration.
     */
    boolean matches(String path);
}
