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
package org.apache.sling.caconfig.management.multiplexer;

import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Detects all {@link org.apache.sling.caconfig.spi.ConfigurationOverrideProvider} implementations in the container
 * and consolidates their result based on service ranking.
 */
@ProviderType
public interface ConfigurationOverrideMultiplexer {

    /**
     * Checks if the whole configuration for the given context path and name is overridden.
     * @param contextPath Context path
     * @param configName Config name
     * @return true if the whole configuration is overridden.
     */
    boolean isAllOverridden(@Nonnull String contextPath, @Nonnull String configName);

    /**
     * Override properties for given context path and configuration name.
     * @param contextPath Path of context resource for which configuration was resolved
     * @param configName Configuration name
     * @param properties Resolved configuration properties
     * @return Overwritten or replaced properties - or null if no override took place
     */
    @CheckForNull Map<String,Object> overrideProperties(@Nonnull String contextPath, @Nonnull String configName, @Nonnull Map<String,Object> properties);
    
    /**
     * Override properties in given configuration resource (if any overrides are defined).
     * @param contextPath Context path
     * @param configName Configuration name
     * @param configResource Resolved configuration resource
     * @return Resource with overwritten configuration properties - or original configuration resource if no override took place
     */
    @CheckForNull Resource overrideProperties(@Nonnull String contextPath, @Nonnull String configName, @CheckForNull Resource configResource);

}
