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
package org.apache.sling.caconfig.spi;

import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Data for persisting configuration properties.
 */
@ProviderType
public final class ConfigurationPersistData {

    private final Map<String,Object> properties;
    private String collectionItemName;
    
    /**
     * @param properties Property values
     */
    public ConfigurationPersistData(@Nonnull Map<String, Object> properties) {
        this.properties = properties;
    }

    /**
     * @return Property values
     */
    public @Nonnull Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * @return Resource collection item name. To be set only for resource collection items.
     */
    public @CheckForNull String getCollectionItemName() {
        return collectionItemName;
    }

    /**
     * @param value Resource collection item name.  To be set only for resource collection items.
     * @return this
     */
    public ConfigurationPersistData collectionItemName(String value) {
        this.collectionItemName = value;
        return this;
    }

}
