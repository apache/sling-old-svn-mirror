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
package org.apache.sling.contextaware.config.management;

import java.util.Collection;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Manages reading and writing configurations e.g. for Editor GUIs.
 * It manages only configurations handled by {@link org.apache.sling.contextaware.config.ConfigurationResolver},
 * no low-level configuration resources managed by {@link org.apache.sling.contextaware.config.resource.ConfigurationResourceResolver}.
 */
@ProviderType
public interface ConfigurationManager {

    /**
     * Get configuration data for the given context resource and configuration name.
     * @param resource Context resource
     * @param configName Configuration name
     * @return Configuration data. Is null when no configuration resource found and no configuration metadata exists.
     */
    @CheckForNull ConfigurationData getConfigurationData(@Nonnull Resource resource, @Nonnull String configName);

    /**
     * Get configuration data collection for the given context resource and configuration name.
     * @param resource Context resource
     * @param configName Configuration name
     * @return Configuration data collection. Is empty when no configuration resources found.
     */
    @Nonnull Collection<ConfigurationData> getConfigurationDataCollection(@Nonnull Resource resource, @Nonnull String configName);
    
    /**
     * Write configuration to repository data using the inner-most context path as reference.
     * @param resource Context resource
     * @param configName Configuration name
     * @param values Values to be stored. All existing properties are erased and replaced with the new ones.
     */
    void writeConfigurationData(@Nonnull Resource resource, @Nonnull String configName, @Nonnull ValueMap values);

    /**
     * Write configuration data collection using the inner-most context path as reference.
     * @param resource Context resource
     * @param configName Configuration name
     * @param values Value collection to be stored. All existing collection entries on this context path level are erased and replaced with the new ones.
     */
    void writeConfigurationDataCollection(@Nonnull Resource resource, @Nonnull String configName, @Nonnull Collection<ValueMap> values);
    
    /**
     * Creates a new empty configuration data item for a configuration data collection for the given configuration name.
     * @param configName Configuration name
     * @return Configuration data. Is null when no configuration metadata exists.
     */
    @CheckForNull ConfigurationData newConfigurationDataItem(@Nonnull String configName);
    
}
