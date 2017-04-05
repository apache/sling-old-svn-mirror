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

import java.util.Collection;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Provides access to the configuration collection data and metadata for a given context path.
 */
@ProviderType
public interface ConfigurationCollectionData {

    /**
     * Get configuration name.
     * @return Configuration name
     */
    @Nonnull String getConfigName();

    /**
     * @return Configuration collection items
     */
    @Nonnull Collection<ConfigurationData> getItems();
    
    /**
     * @return Path of the configuration collection resource parent path or null if it cannot be determined.
     */
    @CheckForNull String getResourcePath();
    
    /**
     * @return Properties for the configuration collection itself. Does not contain configuration data, but control data e.g. for enabling collection inheritance.
     */
    @Nonnull Map<String, Object> getProperties();
    
}
