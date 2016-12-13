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
package org.apache.sling.caconfig.spi;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Defines how configuration data is stored in the configuration resource.
 * This SPI allows application to define their own content structure and node types to be used for configuration data storage.
 */
@ConsumerType
public interface ConfigurationPersistenceStrategy {

    /**
     * Allows the strategy to transform the given configuration resource according to it's persistent strategies,
     * e.g. fetching the data from a child resource instead of the given resource. 
     * @param resource Configuration resource
     * @return Transformed configuration resource. If null is returned this strategy does not support the given configuration resource.
     */
    @CheckForNull Resource getResource(@Nonnull Resource resource);
    
    /**
     * Allows the strategy to transform the given configuration resource path according to it's persistent strategies,
     * e.g. fetching the data from a child resource instead of the given resource. 
     * @param resourcePath Configuration resource path or part of it (e.g. config name)
     * @return Transformed configuration resource path. If null is returned this strategy does not support the given configuration resource path.
     */
    @CheckForNull String getResourcePath(@Nonnull String resourcePath);
    
    /**
     * Stores configuration data for a singleton configuration resource.
     * The changes are written using the given resource resolver. They are not committed, this is left to the caller.
     * @param resourceResolver Resource resolver
     * @param configResourcePath Path to store configuration data to. The resource (and it's parents) may not exist and may have to be created. 
     * @param data Configuration data to be stored. All existing properties are erased and replaced with the new ones.
     * @return true if the data was persisted. false if persisting the data was not accepted by this persistence strategy
     *      (in case of error throw an exception).
     */
    boolean persistConfiguration(@Nonnull ResourceResolver resourceResolver,
            @Nonnull String configResourcePath, @Nonnull ConfigurationPersistData data);
    
    /**
     * Stores configuration data for a configuration resource collection.
     * The changes are written using the given resource resolver. They are not committed, this is left to the caller.
     * @param resourceResolver Resource resolver
     * @param configResourceCollectionParentPath Parent path to store configuration collection data to.
     *      The resource (and it's parents) may not exist and may have to be created. 
     * @param data Configuration collection data. All existing collection entries on this context path level are erased and replaced with the new ones.
     * @return true if the data was persisted. false if persisting the data was not accepted by this persistence strategy
     *      (in case of error throw an exception).
     */
    boolean persistConfigurationCollection(@Nonnull ResourceResolver resourceResolver,
            @Nonnull String configResourceCollectionParentPath, @Nonnull ConfigurationCollectionPersistData data);
 
    /**
     * Delete configuration or configuration collection data from repository using the inner-most context path as reference.
     * @param resourceResolver Resource resolver
     * @param configResourcePath Path to store configuration data to. The resource (and it's parents) may not exist and may have to be created. 
     * @return true if the data was delete. false if deleting the data was not accepted by this persistence strategy
     *      (in case of error throw an exception).
     */
    boolean deleteConfiguration(@Nonnull ResourceResolver resourceResolver, @Nonnull String configResourcePath);
    
}
