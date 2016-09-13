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
package org.apache.sling.contextaware.config.resource.spi;

import java.util.Collection;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Defines how and where the configuration resources are looked up.
 * This SPI allows application to define their own configuration storage and inheritance strategies.
 */
@ConsumerType
public interface ConfigurationResourceResolvingStrategy {

    /**
     * Get a context-aware singleton configuration resource defined by the given configuration name.
     *
     * @param resource Context resource to fetch configuration for
     * @param bucketName Configuration "bucket" name. Each high-level configuration resolver should store 
     *     it's configuration data grouped in a child resource of the configuration resource. This is what
     *     we call a "bucket", and the resource name is specified with this parameter.
     * @param configName Configuration name or relative path.
     * @return Configuration resource or {@code null}.
     */
    @CheckForNull Resource getResource(@Nonnull Resource resource, @Nonnull String bucketName, @Nonnull String configName);

    /**
     * Get a collection of context-aware configuration resources defined by the given configuration name.
     * @param resource Context resource to fetch configuration for
     * @param bucketName Configuration "bucket" name. Each high-level configuration resolver should store 
     *     it's configuration data grouped in a child resource of the configuration resource. This is what
     *     we call a "bucket", and the resource name is specified with this parameter.
     * @param configName Configuration name or relative path.
     * @return Collection of configuration resources, the collection might be empty.
     */
    @Nonnull Collection<Resource> getResourceCollection(@Nonnull Resource resource, @Nonnull String bucketName, @Nonnull String configName);
    
}
