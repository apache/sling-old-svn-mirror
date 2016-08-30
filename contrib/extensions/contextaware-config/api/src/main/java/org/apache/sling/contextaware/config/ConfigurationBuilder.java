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
package org.apache.sling.contextaware.config;

import java.util.Collection;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.ValueMap;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Defines how the configuration should be mapped and returned.
 */
@ProviderType
public interface ConfigurationBuilder {

    /**
     * Define configuration name.
     * Optional for the {@link #as(Class)} and {@link #asCollection(Class)} methods, mandatory for the others. 
     * @param configName Relative path
     * @return Configuration builder
     */
    @Nonnull ConfigurationBuilder name(@Nonnull String configName);

    /**
     * Get configuration as singleton resource and its properties mapped to the given annotation class.
     * Configuration name is optional - if not given via {@link #name(String)} method it is derived
     * from the annotation interface class name.
     * @param clazz Annotation interface class
     * @return Configuration object. Contains only the default values if content resource or configuration cannot be found.
     */
    @Nonnull <T> T as(@Nonnull Class<T> clazz);

    /**
     * Get collection of configuration resources with their properties mapped to the given annotation class.
     * Configuration name is optional - if not given via {@link #name(String)} method it is derived
     * from the annotation interface class name.
     * @param clazz Annotation interface class
     * @return Collection of configuration objects. Is empty if content resource or configuration cannot be found.
     */
    @Nonnull <T> Collection<T> asCollection(@Nonnull Class<T> clazz);

    /**
     * Get configuration as singleton resource and return its properties as value map.
     * @return Value map. Map is empty if content resource or configuration cannot be found.
     */
    @Nonnull ValueMap asValueMap();

    /**
     * Get collection of configuration resources with their properties mapped to the given annotation class.
     * @param clazz Annotation interface class
     * @return Collection of value map. Is empty if content resource or configuration cannot be found.
     */
    @Nonnull Collection<ValueMap> asValueMapCollection();

    /**
     * Get configuration as singleton configuration resource and adapt it to the given class.
     * @param clazz Class that can be adapted from a {@link Resource}
     * @return Object instance or null if content resource or configuration cannot be found or if the adaption was not possible.
     */
    <T> T asAdaptable(@Nonnull Class<T> clazz);

    /**
     * Get collection of configuration resources and adapt them to the given class.
     * @param clazz Class that can be adapted from a {@link Resource}
     * @return Collection of object instances. Is empty if content resource or configuration cannot be found or if the adaption was not possible.
     */
    @Nonnull <T> Collection<T> asAdaptableCollection(@Nonnull Class<T> clazz);

}
