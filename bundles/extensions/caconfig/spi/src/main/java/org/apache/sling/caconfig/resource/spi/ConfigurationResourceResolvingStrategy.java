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
package org.apache.sling.caconfig.resource.spi;

import java.util.Collection;
import java.util.Iterator;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Defines how and where the configuration resources are looked up.
 * This SPI allows application to define their own configuration storage and inheritance strategies.
 *
 * If this strategy supports inheritance for collections, it should use the optional
 * {@link CollectionInheritanceDecider} SPI interface. If such services are available,
 * they should be called in order of there service ranking, starting with the highest
 * ranking. The first decider service providing a non null return value is used
 * for the decision.
 */
@ConsumerType
public interface ConfigurationResourceResolvingStrategy {

    /**
     * Get a context-aware singleton configuration resource defined by the given configuration name.
     * @param resource Context resource to fetch configuration for
     * @param bucketNames Configuration "bucket" names. For each inheritance level all bucket names are tried, and the first
     *   one that has a result is included.
     * @param configName Configuration name or relative path.
     * @return Configuration resource or {@code null} if this strategy did not found matching resources.
     */
    @CheckForNull Resource getResource(@Nonnull Resource resource, @Nonnull Collection<String> bucketNames, @Nonnull String configName);

    /**
     * Get a collection of context-aware configuration resources defined by the given configuration name.
     * @param resource Context resource to fetch configuration for
     * @param bucketNames Configuration "bucket" names. For each inheritance level all bucket names are tried, and the first
     *   one that has a result is included.
     * @param configName Configuration name or relative path.
     * @return Collection of configuration resources or {@code null} if this strategy did not found matching resources.
     */
    @CheckForNull Collection<Resource> getResourceCollection(@Nonnull Resource resource, @Nonnull Collection<String> bucketNames, @Nonnull String configName);

    /**
     * Get a context-aware singleton configuration resource inheritance chain defined by the given configuration name.
     * The first item of the inheritance chain it the same resource returned by {@link #getResource(Resource, Collection, String)}.
     * @param resource Context resource to fetch configuration for
     * @param bucketNames Configuration "bucket" names. For each inheritance level all bucket names are tried, and the first
     *   one that has a result is included.
     * @param configName Configuration name or relative path.
     * @return Configuration resource inheritance chain or {@code null} if this strategy did not found matching resources.
     */
    @CheckForNull Iterator<Resource> getResourceInheritanceChain(@Nonnull Resource resource, @Nonnull Collection<String> bucketNames, @Nonnull String configName);

    /**
     * Get a collection of context-aware configuration resource inheritance chains defined by the given configuration name.
     * The first item of each inheritance chain is the same item returned by {@link #getResourceCollection(Resource, Collection, String)}.
     * @param resource Context resource to fetch configuration for
     * @param bucketNames Configuration "bucket" names. For each inheritance level all bucket names are tried, and the first
     *   one that has a result is included.
     * @param configName Configuration name or relative path.
     * @return Collection of configuration resource inheritance chains or {@code null} if this strategy did not found matching resources.
     */
    @CheckForNull Collection<Iterator<Resource>> getResourceCollectionInheritanceChain(@Nonnull Resource resource, @Nonnull Collection<String> bucketNames, @Nonnull String configName);

    /**
     * Get the configuration resource path for storing configuration data for the given context resource and configuration name.
     * This path is used when no configuration resource exists yet, but new configuration data should be stored.
     * So usually the returned path does not yet exist (and perhaps not even it's parents).
     * @param resource Context resource to fetch configuration for
     * @param bucketName Configuration "bucket" name. Each high-level configuration resolver should store
     *     it's configuration data grouped in a child resource of the configuration resource. This is what
     *     we call a "bucket", and the resource name is specified with this parameter.
     * @param configName Configuration name or relative path.
     * @return Resource path, or null if no matching configuration resource path can be determined
     */
    @CheckForNull String getResourcePath(@Nonnull Resource resource, @Nonnull String bucketName, @Nonnull String configName);

    /**
     * Get the configuration resource collection parent path for storing configuration data for the given context resource and configuration name.
     * This path is used when no configuration resource collection exists yet, but new configuration data should be stored.
     * So usually the returned path does not yet exist (and perhaps not even it's parents).
     * @param resource Context resource to fetch configuration for
     * @param bucketName Configuration "bucket" name. Each high-level configuration resolver should store
     *     it's configuration data grouped in a child resource of the configuration resource. This is what
     *     we call a "bucket", and the resource name is specified with this parameter.
     * @param configName Configuration name or relative path.
     * @return Resource path, or null if no matching configuration resource path can be determined
     */
    @CheckForNull String getResourceCollectionParentPath(@Nonnull Resource resource, @Nonnull String bucketName, @Nonnull String configName);

}
