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
package org.apache.sling.contextaware.config.spi;

import java.util.SortedSet;

import javax.annotation.Nonnull;

import org.apache.sling.contextaware.config.spi.metadata.ConfigurationMetadata;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Allows application to provide the necessary metadata for configurations.
 */
@ConsumerType
public interface ConfigurationMetadataProvider {

    /**
     * Get all configuration names.
     * @return Configuration names
     */
    @Nonnull SortedSet<String> getConfigurationNames();

    /**
     * Get configuration metadata.
     * @param configName Configuration name
     * @return Configuration metadata or null if none exists for the given name.
     */
    ConfigurationMetadata getConfigurationMetadata(String configName);

}
