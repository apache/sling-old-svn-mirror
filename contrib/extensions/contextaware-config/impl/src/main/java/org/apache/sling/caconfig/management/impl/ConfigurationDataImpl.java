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
package org.apache.sling.caconfig.management.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.impl.override.ConfigurationOverrideManager;
import org.apache.sling.caconfig.management.ConfigurationData;
import org.apache.sling.caconfig.management.ValueInfo;
import org.apache.sling.caconfig.spi.metadata.ConfigurationMetadata;
import org.apache.sling.caconfig.spi.metadata.PropertyMetadata;

final class ConfigurationDataImpl implements ConfigurationData {
    
    private final ConfigurationMetadata configMetadata;
    private final Resource resolvedConfigurationResource;
    private final Resource writebackConfigurationResource;
    private final List<Resource> configurationResourceInheritanceChain;
    private final Resource contextResource;
    private final String configName;
    private final ConfigurationOverrideManager configurationOverrideManager;
    private final boolean configResourceCollection;
    
    private Set<String> propertyNamesCache;
    private ValueMap valuesCache;
    private ValueMap effectiveValuesCache;
    
    @SuppressWarnings("unchecked")
    public ConfigurationDataImpl(ConfigurationMetadata configMetadata,
            Resource resolvedConfigurationResource, Resource writebackConfigurationResource,
            Iterator<Resource> configurationResourceInheritanceChain,
            Resource contextResource, String configName, ConfigurationOverrideManager configurationOverrideManager,
            boolean configResourceCollection) {
        this.configMetadata = configMetadata;
        this.resolvedConfigurationResource = resolvedConfigurationResource;
        this.writebackConfigurationResource = writebackConfigurationResource;
        this.configurationResourceInheritanceChain = configurationResourceInheritanceChain != null
                ? IteratorUtils.toList(configurationResourceInheritanceChain) : null;
        this.contextResource = contextResource;
        this.configName = configName;
        this.configurationOverrideManager = configurationOverrideManager;
        this.configResourceCollection = configResourceCollection;
    }

    public ConfigurationDataImpl(ConfigurationMetadata configMetadata,
            Resource contextResource, String configName, ConfigurationOverrideManager configurationOverrideManager,
            boolean configResourceCollection) {
        this(configMetadata, null, null, null,
                contextResource, configName, configurationOverrideManager,
                configResourceCollection);
    }
    
    @Override
    public String getConfigName() {
        return configName;
    }

    @Override
    public String getCollectionItemName() {
        return (configResourceCollection && resolvedConfigurationResource != null)
                ? resolvedConfigurationResource.getName() : null;
    }

    @Override
    public String getResourcePath() {
        if (writebackConfigurationResource != null) {
            return writebackConfigurationResource.getPath();
        }
        return null;
    }

    @Override
    public Set<String> getPropertyNames() {
        if (propertyNamesCache == null) {
            propertyNamesCache = new HashSet<>();
            if (configMetadata != null) {
                propertyNamesCache.addAll(configMetadata.getPropertyMetadata().keySet());
            }
            if (resolvedConfigurationResource != null) {
                propertyNamesCache.addAll(ResourceUtil.getValueMap(resolvedConfigurationResource).keySet());
            }
        }
        return propertyNamesCache;
    }

    @Override
    public ValueMap getValues() {
        if (valuesCache == null) {
            if (writebackConfigurationResource != null) {
                valuesCache = ResourceUtil.getValueMap(writebackConfigurationResource);
            }
            else {
                valuesCache = ValueMap.EMPTY;
            }
        }
        return valuesCache;
    }

    @Override
    public ValueMap getEffectiveValues() {
        if (effectiveValuesCache == null) {
            Map<String,Object> props = new HashMap<>();
            if (configMetadata != null) {
                for (PropertyMetadata<?> propertyMetadata : configMetadata.getPropertyMetadata().values()) {
                    if (propertyMetadata.getDefaultValue() != null) {
                        props.put(propertyMetadata.getName(), propertyMetadata.getDefaultValue());
                    }
                }
            }
            if (resolvedConfigurationResource != null) {
                props.putAll(ResourceUtil.getValueMap(resolvedConfigurationResource));
            }
            effectiveValuesCache = new ValueMapDecorator(props);
        }
        return effectiveValuesCache;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ValueInfo<?> getValueInfo(String propertyName) {
        PropertyMetadata propertyMetadata = configMetadata != null ? configMetadata.getPropertyMetadata().get(propertyName) : null;
        Object value;
        Object effectiveValue;
        if (propertyMetadata != null) {
            value = getValues().get(propertyName, propertyMetadata.getType());
            effectiveValue = getEffectiveValues().get(propertyName, ClassUtils.primitiveToWrapper(propertyMetadata.getType()));
        }
        else {
            value = getValues().get(propertyName);
            effectiveValue = getEffectiveValues().get(propertyName);
        }
        return new ValueInfoImpl(propertyName, value, effectiveValue, propertyMetadata,
                resolvedConfigurationResource,
                writebackConfigurationResource,
                configurationResourceInheritanceChain,
                contextResource,
                configName,
                configurationOverrideManager);
    }

}
