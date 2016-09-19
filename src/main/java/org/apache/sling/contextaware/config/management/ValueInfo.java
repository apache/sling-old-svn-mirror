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

import javax.annotation.CheckForNull;

import org.apache.sling.contextaware.config.spi.metadata.PropertyMetadata;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Provides detailed information about a given configuration value.
 * @param <T> Property type
 */
@ProviderType
public interface ValueInfo<T> {

    /**
     * Property metadata.
     * @return Property metadata. Null if no metadata exists.
     */
    @CheckForNull PropertyMetadata<T> getPropertyMetadata();
    
    /**
     * Get value stored for the current context path. No inherited value. No default value.
     * @return Value
     */
    @CheckForNull T getValue();
    
    /**
     * Get value storedf or the current context path, or inherited from upper levels, or the default value.
     * @return Value
     */
    @CheckForNull T getEffectiveValue();
    
    /**
     * Get the path of the configuration resource the value is stored in.
     * @return Resource path or null if no resource associated. 
     */
    @CheckForNull String getConfigSourcePath();

    /**
     * @return true if no value is defined but a default value is returned.
     */
    boolean isDefault();
    
    /**
     * @return true if the value is not defined for the current context path but inherited from upper levels.
     */
    // for future use
    //boolean isInherited();
    
    /**
     * @return true if the value is overridden by an configuration override provider.
     */
    // for future use
    //boolean isOverridden();
    
    /**
     * @return true if this value is locked on a higher level and is not allowed to be overridden.
     */
    // for future use
    //boolean isLocked();
    
}
