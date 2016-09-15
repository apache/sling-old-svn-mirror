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
package org.apache.sling.contextaware.config.management.impl;

import java.util.HashSet;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.contextaware.config.management.ConfigurationData;
import org.apache.sling.contextaware.config.management.ValueInfo;
import org.apache.sling.contextaware.config.spi.metadata.ConfigurationMetadata;
import org.apache.sling.contextaware.config.spi.metadata.PropertyMetadata;

final class ConfigurationDataImpl implements ConfigurationData {
    
    private final ValueMap properties;
    private final String configSourcePath;
    private final ConfigurationMetadata configMetadata;
    
    public ConfigurationDataImpl(Resource configResource, ConfigurationMetadata configMetadata) {
        this(configResource.getValueMap(), configResource.getPath(), configMetadata);
    }

    public ConfigurationDataImpl(ConfigurationMetadata configMetadata) {
        this(ValueMap.EMPTY, null, configMetadata);
    }

    private ConfigurationDataImpl(ValueMap propertes, String configSourcePath, ConfigurationMetadata configMetadata) {
        this.properties = propertes;
        this.configSourcePath = configSourcePath;
        this.configMetadata = configMetadata;
    }

    @Override
    public Set<String> getPropertyNames() {
        Set<String> propertyNames = new HashSet<>();
        if (configMetadata != null) {
            propertyNames.addAll(configMetadata.getPropertyMetadata().keySet());
        }
        propertyNames.addAll(properties.keySet());
        return propertyNames;
    }

    @Override
    public ValueMap getValues() {
        // TODO: merge with default values
        return properties;
    }

    @Override
    public ValueMap getEffectiveValues() {
        // TODO: this may return different values when property inheritance is enabled
        return getValues();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ValueInfo<?> getValueInfo(String propertyName) {
        PropertyMetadata propertyMetadata = configMetadata != null ? configMetadata.getPropertyMetadata().get(propertyName) : null;
        Object value;
        if (propertyMetadata != null) {
            value = properties.get(propertyName, propertyMetadata.getType());
        }
        else {
            value = properties.get(propertyName);
        }
        return new ValueInfoImpl(value, configSourcePath, propertyMetadata);
    }

}
