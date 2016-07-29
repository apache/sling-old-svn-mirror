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
package org.apache.sling.config;

import java.util.Collection;

import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Defines how the configuration should be mapped and returned.
 */
@ProviderType
public interface ConfigurationBuilder {
    
    /**
     * Define configuration name. Optional when using annotation class, mandatory when getting configuration as value map.
     * @param configName Relative path
     * @return Configuration builder
     */
    @Nonnull ConfigurationBuilder name(@Nonnull String configName);
    
    /**
     * Get configuration as singleton and its properties mapped to the given annotation class.
     * @param clazz Annotation class or {@link org.apache.sling.api.resource.ValueMap}
     * @return Configuration object
     */
    @Nonnull <T> T as(@Nonnull Class<T> clazz);

    /**
     * Get list of configuration instances with its properties mapped to the given annotation class.
     * @param clazz Annotation class or {@link org.apache.sling.api.resource.ValueMap}
     * @return List of configuration objects
     */
    @Nonnull <T> Collection<T> asCollection(@Nonnull Class<T> clazz);

}
