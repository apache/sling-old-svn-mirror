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
package org.apache.sling.caconfig.spi.metadata;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Defines a configuration.
 */
@ProviderType
public final class ConfigurationMetadata extends AbstractMetadata<ConfigurationMetadata> {

    private final Map<String,PropertyMetadata<?>> propertyMetadata;
    private final boolean collection;

    /**
     * @param name Configuration name
     * @param propertyMetadata Property metadata
     * @param collection Collection
     */
    public ConfigurationMetadata(@Nonnull String name,
            Collection<PropertyMetadata<?>> propertyMetadata,
            boolean collection) {
        super(name);
        this.propertyMetadata = toMap(propertyMetadata);
        this.collection = collection;
    }
    
    private static Map<String,PropertyMetadata<?>> toMap(Collection<PropertyMetadata<?>> propertyMetadata) {
        Map<String,PropertyMetadata<?>> map = new LinkedHashMap<>();
        for (PropertyMetadata<?> item : propertyMetadata) {
            if (map.containsKey(item.getName())) {
                throw new IllegalArgumentException("Duplicate property name: " + item.getName());
            }
            map.put(item.getName(), item);
        }
        return map;
    }
    
    /**
     * @return true if configuration is singleton
     */
    public boolean isSingleton() {
        return !collection;
    }
    
    /**
     * @return true if configuration is collection
     */
    public boolean isCollection() {
        return collection;
    }

    /**
     * @return Configuration properties
     */
    public Map<String,PropertyMetadata<?>> getPropertyMetadata() {
        return this.propertyMetadata;
    }

}
