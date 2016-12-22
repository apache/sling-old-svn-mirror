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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.impl.override.ConfigurationOverrideManager;
import org.apache.sling.caconfig.management.ValueInfo;
import org.apache.sling.caconfig.spi.metadata.PropertyMetadata;

final class ValueInfoImpl<T> implements ValueInfo<T> {
    
    private final String name;
    private final T value;
    private final T effectiveValue;
    private final T defaultValue;
    private final PropertyMetadata<T> propertyMetadata;
    private final Resource resolvedConfigurationResource;
    private final Resource writebackConfigurationResource;
    private final List<Resource> configurationResourceInheritanceChain;
    private final Resource contextResource;
    private final String configName;
    private final ConfigurationOverrideManager configurationOverrideManager;
    private final boolean isAllOverridden;
    
    public ValueInfoImpl(String name, T value, T effectiveValue, PropertyMetadata<T> propertyMetadata,
            Resource resolvedConfigurationResource, Resource writebackConfigurationResource,
            List<Resource> configurationResourceInheritanceChain,
            Resource contextResource, String configName, ConfigurationOverrideManager configurationOverrideManager,
            boolean isAllOverridden) {
        this.name = name;
        this.value = value;
        this.effectiveValue = effectiveValue;
        this.defaultValue = propertyMetadata != null ? propertyMetadata.getDefaultValue() : null;
        this.propertyMetadata = propertyMetadata;
        this.resolvedConfigurationResource = resolvedConfigurationResource;
        this.writebackConfigurationResource = writebackConfigurationResource;
        this.configurationResourceInheritanceChain = configurationResourceInheritanceChain;
        this.contextResource = contextResource;
        this.configName = configName;
        this.configurationOverrideManager = configurationOverrideManager;
        this.isAllOverridden = isAllOverridden;
    }
    
    @Override
    public String getName() {
        return name;
    }
 
    @Override
    public PropertyMetadata<T> getPropertyMetadata() {
        return propertyMetadata;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public T getEffectiveValue() {
        return effectiveValue;
    }

    @Override
    public String getConfigSourcePath() {
        if (effectiveValue != null && resolvedConfigurationResource != null) {
            Resource resource = getResourceFromInheritanceChain();
            if (resource != null) {
                return resource.getPath();
            }
        }
        return null;
    }

    @Override
    public boolean isDefault() {
        if (defaultValue == null) {
            return false;
        }
        if (resolvedConfigurationResource == null) {
            return true;
        }
        else {
            return !resolvedConfigurationResource.getValueMap().containsKey(name);
        }
    }

    @Override
    public boolean isInherited() {
        if (isDefault() || effectiveValue == null) {
            return false;
        }
        else if (resolvedConfigurationResource == null) {
            return false;
        }
        else if (writebackConfigurationResource == null) {
            return true;
        }
        else if (!StringUtils.equals(resolvedConfigurationResource.getPath(), writebackConfigurationResource.getPath())) {
            return true;
        }
        else {
            Resource inheritanceSource = getResourceFromInheritanceChain();
            if (inheritanceSource != null) {
                return !StringUtils.equals(resolvedConfigurationResource.getPath(), inheritanceSource.getPath());
            }
            else {
                return false;
            }
        }
    }
    
    private Resource getResourceFromInheritanceChain() {
        if (configurationResourceInheritanceChain == null) {
            return null;
        }
        return getResourceFromInheritanceChain(configurationResourceInheritanceChain.iterator());
    }

    private Resource getResourceFromInheritanceChain(Iterator<Resource> inheritanceChain) {
        if (!inheritanceChain.hasNext()) {
            return null;
        }
        Resource resource = inheritanceChain.next();
        Object valueFromResource = resource.getValueMap().get(name, effectiveValue.getClass());
        if (valueFromResource != null) {
            return resource;
        }
        return getResourceFromInheritanceChain(inheritanceChain);
    }

    @Override
    public boolean isOverridden() {
        if (contextResource == null) {
            return false;
        }
        if (isAllOverridden) {
            return true;
        }
        Map<String,Object> overrideProperties = configurationOverrideManager.overrideProperties(
                    contextResource.getPath(), configName, Collections.<String,Object>emptyMap());
        if (overrideProperties != null) {
            return overrideProperties.containsKey(name)
                    || (getValue() != null && effectiveValue == null);
        }
        else {
            return false;
        }
    }

}
