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

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Getting context-aware configurations for a given resource context.
 * Context-specific configuration may be different for different parts of the resource
 * hierarchy, and configuration parameter inheritance may take place.
 *
 * This service builds on top of the {@link org.apache.sling.contextaware.config.resource.ConfigurationResourceResolver}
 * and uses that service to resolve configuration resources. These resources
 * can then be converted into application specific configuration objects
 * using the {@link ConfigurationBuilder}.
 */
@ProviderType
public interface ConfigurationResolver {

    /**
     * Get configuration for given resource.
     * @param resource Context resource
     * @return Configuration builder
     */
    @Nonnull ConfigurationBuilder get(@Nonnull Resource resource);

}
