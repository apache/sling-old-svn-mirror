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

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Defines how configuration data is stored in the configuration resource.
 * This SPI allows application to define their own content structure and node types to be used for configuration data storage.
 */
@ConsumerType
public interface ConfigurationPersistenceStrategy {

    /**
     * Allows the strategy to transform the given configuration resource according to it's persistent strategies.
     * @param resource Configuration resource
     * @return Transformed configuration resource. If null is returned this strategy does not support the given configuration resource.
     */
    Resource getResource(@Nonnull Resource resource);
    
}
