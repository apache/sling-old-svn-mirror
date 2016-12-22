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
package org.apache.sling.caconfig.management;

import java.util.SortedSet;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.spi.ConfigurationCollectionPersistData;
import org.apache.sling.caconfig.spi.ConfigurationPersistData;
import org.apache.sling.caconfig.spi.metadata.ConfigurationMetadata;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Manages reading and writing configurations e.g. for Editor GUIs.
 * It manages only configurations handled by {@link org.apache.sling.caconfig.ConfigurationResolver},
 * no low-level configuration resources managed by {@link org.apache.sling.caconfig.resource.ConfigurationResourceResolver}.
 */
@ProviderType
public interface ConfigurationManager {

    /**
     * Get configuration data for the given context resource and configuration name.
     * @param resource Context resource
     * @param configName Configuration name
     * @return Configuration data. Is null when no configuration resource found and no configuration metadata exists.
     */
    @CheckForNull ConfigurationData getConfiguration(@Nonnull Resource resource, @Nonnull String configName);

    /**
     * Get configuration data collection for the given context resource and configuration name.
     * @param resource Context resource
     * @param configName Configuration name
     * @return Configuration data collection. Is empty when no configuration resources found.
     */
    @Nonnull ConfigurationCollectionData getConfigurationCollection(@Nonnull Resource resource, @Nonnull String configName);
    
    /**
     * Write configuration data to repository using the inner-most context path as reference.
     * @param resource Context resource
     * @param configName Configuration name
     * @param data Configuration data to be stored. All existing properties are erased and replaced with the new ones.
     */
    void persistConfiguration(@Nonnull Resource resource, @Nonnull String configName,
            @Nonnull ConfigurationPersistData data);

    /**
     * Write configuration data collection using the inner-most context path as reference.
     * @param resource Context resource
     * @param configName Configuration name
     * @param data Configuration collection data to be stored. All existing collection entries on this context path level are erased and replaced with the new ones.
     */
    void persistConfigurationCollection(@Nonnull Resource resource, @Nonnull String configName,
            @Nonnull ConfigurationCollectionPersistData data);
    
    /**
     * Creates a new empty configuration data item for a configuration data collection for the given configuration name.
     * @param resource Context resource
     * @param configName Configuration name
     * @return Configuration data. Is null when no configuration metadata exists.
     */
    @CheckForNull ConfigurationData newCollectionItem(@Nonnull Resource resource, @Nonnull String configName);

    /**
     * Delete configuration or configuration collection data from repository using the inner-most context path as reference.
     * @param resource Context resource
     * @param configName Configuration name
     */
    void deleteConfiguration(@Nonnull Resource resource, @Nonnull String configName);
    
    /**
     * Get all configuration names.
     * The results of all configuration metadata provider implementations are merged.
     * @return Configuration names
     */
    @Nonnull SortedSet<String> getConfigurationNames();

    /**
     * Get configuration metadata from any configuration metadata provider.
     * @param configName Configuration name
     * @return Configuration metadata or null if none exists for the given name.
     */
    @CheckForNull ConfigurationMetadata getConfigurationMetadata(@Nonnull String configName);
    
    /**
     * Rewrite given resource path or configuration name according to current persistence strategies.
     * @param configResourcePath Resource path or config name
     * @return Rewritten resoure path or config name
     */
    @CheckForNull String getPersistenceResourcePath(@Nonnull String configResourcePath);
    
}
