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

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Defines how configuration data is stored in the configuration resource.
 * This SPI allows application to define their own content structure and node types to be used for configuration data storage.
 */
@ConsumerType
public interface ConfigurationPersistenceStrategy {

    /**
     * Allows the strategy to transform the given configuration resource according to it's persistent strategies,
     * e.g. fetching the data from a child resource instead of the given resource. 
     * @param resource Configuration resource
     * @return Transformed configuration resource. If null is returned this strategy does not support the given configuration resource.
     */
    Resource getResource(@Nonnull Resource resource);
    
    /**
     * Stores configuration data for a singleton configuration resource.
     * The changes are written using the given resource resolver. They are not committed, this is left to the caller.
     * @param resourceResolver Resource resolver
     * @param configResourcePath Path to store configuration data to. The resource (and it's parents) may not exist and may have to be created. 
     * @param properties Configuration properties
     * @return true if the data was persisted. false if persisting the data was not accepted by this persistence strategy (but in case of error throw an exception).
     */
    boolean persist(@Nonnull ResourceResolver resourceResolver,
            @Nonnull String configResourcePath, @Nonnull Map<String,Object> properties);
    
    /**
     * Stores configuration data for a configuration resource collection.
     * The changes are written using the given resource resolver. They are not committed, this is left to the caller.
     * @param resourceResolver Resource resolver
     * @param configResourceCollectionParentPath Parent path to store configuration collection data to. The resource (and it's parents) may not exist and may have to be created. 
     * @param propertiesCollection Configuration properties
     * @return true if the data was persisted. false if persisting the data was not accepted by this persistence strategy (but in case of error throw an exception).
     */
    boolean persistCollection(@Nonnull ResourceResolver resourceResolver,
            @Nonnull String configResourceCollectionParentPath, @Nonnull Collection<Map<String,Object>> propertiesCollection);
    
}
