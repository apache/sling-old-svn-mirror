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

import java.util.Arrays;
import java.util.Collection;
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
import org.apache.sling.caconfig.management.ConfigurationManager;
import org.apache.sling.caconfig.management.ValueInfo;
import org.apache.sling.caconfig.spi.ConfigurationPersistenceStrategy;
import org.apache.sling.caconfig.spi.metadata.ConfigurationMetadata;
import org.apache.sling.caconfig.spi.metadata.PropertyMetadata;

final class ConfigurationDataImpl implements ConfigurationData {
    
    private final ConfigurationMetadata configMetadata;
    private final Resource resolvedConfigurationResource;
    private final Resource writebackConfigurationResource;
    private final List<Resource> configurationResourceInheritanceChain;
    private final Resource contextResource;
    private final String configName;
    private final ConfigurationManager configurationManager;
    private final ConfigurationOverrideManager configurationOverrideManager;
    private final ConfigurationPersistenceStrategy configurationPersistenceStrategy;
    private final boolean configResourceCollection;
    private final String collectionItemName;
    
    private Set<String> propertyNamesCache;
    private ValueMap valuesCache;
    private ValueMap effectiveValuesCache;
    
    private static final Set<String> PROPERTIES_TO_IGNORE = new HashSet<>(Arrays.asList(
            "jcr:primaryType"));
    
    @SuppressWarnings("unchecked")
    public ConfigurationDataImpl(ConfigurationMetadata configMetadata,
            Resource resolvedConfigurationResource, Resource writebackConfigurationResource,
            Iterator<Resource> configurationResourceInheritanceChain,
            Resource contextResource, String configName,
            ConfigurationManager configurationManager, ConfigurationOverrideManager configurationOverrideManager,
            ConfigurationPersistenceStrategy configurationPersistenceStrategy,
            boolean configResourceCollection, String collectionItemName) {
        this.configMetadata = configMetadata;
        this.resolvedConfigurationResource = resolvedConfigurationResource;
        this.writebackConfigurationResource = writebackConfigurationResource;
        this.configurationResourceInheritanceChain = configurationResourceInheritanceChain != null
                ? IteratorUtils.toList(configurationResourceInheritanceChain) : null;
        this.contextResource = contextResource;
        this.configName = configName;
        this.configurationManager = configurationManager;
        this.configurationOverrideManager = configurationOverrideManager;
        this.configurationPersistenceStrategy = configurationPersistenceStrategy;
        this.configResourceCollection = configResourceCollection;
        this.collectionItemName = collectionItemName;
    }

    public ConfigurationDataImpl(ConfigurationMetadata configMetadata,
            Resource contextResource, String configName,
            ConfigurationManager configurationManager, ConfigurationOverrideManager configurationOverrideManager,
            ConfigurationPersistenceStrategy configurationPersistenceStrategy,
            boolean configResourceCollection) {
        this(configMetadata, null, null, null,
                contextResource, configName,
                configurationManager, configurationOverrideManager,
                configurationPersistenceStrategy,
                configResourceCollection, null);
    }
    
    @Override
    public String getConfigName() {
        return configName;
    }

    @Override
    public String getCollectionItemName() {
        return collectionItemName;
    }

    @Override
    public String getResourcePath() {
        if (writebackConfigurationResource != null) {
            return writebackConfigurationResource.getPath();
        }
        if (resolvedConfigurationResource != null) {
            return resolvedConfigurationResource.getPath();
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
            removeIgnoredProperties(propertyNamesCache);
        }
        return propertyNamesCache;
    }

    @Override
    public ValueMap getValues() {
        if (valuesCache == null) {
            Map<String,Object> props = new HashMap<>();
            if (writebackConfigurationResource != null) {
                props.putAll( ResourceUtil.getValueMap(writebackConfigurationResource));
            }
            removeIgnoredProperties(props);
            resolveNestedConfigs(props);
            valuesCache = new ValueMapDecorator(props);
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
            removeIgnoredProperties(props);
            resolveNestedConfigs(props);
            effectiveValuesCache = new ValueMapDecorator(props);
        }
        return effectiveValuesCache;
    }
    
    private void removeIgnoredProperties(Set<String> propertyNames) {
        propertyNames.removeAll(PROPERTIES_TO_IGNORE);
    }

    private void removeIgnoredProperties(Map<String,Object> props) {
        for (String propertyName : PROPERTIES_TO_IGNORE) {
            props.remove(propertyName);
        }
    }
    
    private void resolveNestedConfigs(Map<String,Object> props) {
        if (configMetadata == null) {
            return;
        }
        for (PropertyMetadata<?> propertyMetadata : configMetadata.getPropertyMetadata().values()) {
            if (propertyMetadata.isNestedConfiguration()) {
                ConfigurationMetadata nestedConfigMetadata = propertyMetadata.getConfigurationMetadata();
                String nestedConfigName;
                if (configResourceCollection) {
                    nestedConfigName = configurationPersistenceStrategy.getResourcePath(configName + "/" + getCollectionItemName()) + "/" + nestedConfigMetadata.getName();
                }
                else {
                    nestedConfigName = configurationPersistenceStrategy.getResourcePath(configName) + "/" + nestedConfigMetadata.getName();
                }
                if (propertyMetadata.getType().equals(ConfigurationMetadata.class)) {
                    ConfigurationData configData = configurationManager.getConfiguration(contextResource, nestedConfigName);
                    props.put(propertyMetadata.getName(), configData);
                }
                else if (propertyMetadata.getType().equals(ConfigurationMetadata[].class)) {
                    Collection<ConfigurationData> configDatas = configurationManager.getConfigurationCollection(contextResource, nestedConfigName).getItems();
                    props.put(propertyMetadata.getName(), configDatas.toArray(new ConfigurationData[configDatas.size()]));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public ValueInfo<?> getValueInfo(String propertyName) {
        PropertyMetadata<?> propertyMetadata = getPropertyMetadata(propertyName);
        Object value;
        Object effectiveValue;
        if (propertyMetadata != null) {
            Class<?> type = ClassUtils.primitiveToWrapper(propertyMetadata.getType());
            if (type == ConfigurationMetadata.class) {
                type = ConfigurationData.class;
            }
            else if (type == ConfigurationMetadata[].class) {
                type = ConfigurationData[].class;
            }
            value = getValues().get(propertyName, type);
            effectiveValue = getEffectiveValues().get(propertyName, type);
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
    
    private PropertyMetadata<?> getPropertyMetadata(String propertyName) {
        if (configMetadata == null) {
            return null;
        }
        else {
            return configMetadata.getPropertyMetadata().get(propertyName);
        }
    }
    
}
