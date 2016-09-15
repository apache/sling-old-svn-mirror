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

import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.ValueMap;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Provides access to the configuration data and metadata for a given context path.
 */
@ProviderType
public interface ConfigurationData {

    /**
     * List of property names defined in configuration metadata or values are defined for.
     * @return Property names
     */
    @Nonnull Set<String> getPropertyNames();

    /**
     * Configuration values stored for the given context path. No inherited values. No default values.
     * @return Values
     */
    @Nonnull ValueMap getValues();

    /**
     * Configuration values stored for the given context path merged with inherited values and default values.
     * @return Values
     */
    @Nonnull ValueMap getEffectiveValues();

    /**
     * Get detailed metadata information about the property value.
     * @param propertyName Property name
     * @return Value information. Null if neither property metadata nor an existing value exists.
     */
    @CheckForNull ValueInfo<?> getValueInfo(String propertyName);
    
}
