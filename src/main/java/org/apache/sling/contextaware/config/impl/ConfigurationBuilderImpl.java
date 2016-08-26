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
package org.apache.sling.contextaware.config.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.contextaware.config.ConfigurationBuilder;
import org.apache.sling.contextaware.config.ConfigurationResolveException;
import org.apache.sling.contextaware.config.resource.ConfigurationResourceResolver;

class ConfigurationBuilderImpl implements ConfigurationBuilder {

    private static final String CONFIGS_PARENT_NAME = "sling:configs";

    private final Resource contentResource;
    private final ConfigurationResourceResolver configurationResourceResolver;

    private String configName;

    public ConfigurationBuilderImpl(final Resource resource,
            final ConfigurationResourceResolver configurationResourceResolver) {
        this.contentResource = resource;
        this.configurationResourceResolver = configurationResourceResolver;
    }

    @Override
    public ConfigurationBuilder name(String configName) {
        if (!isNameValid(configName)) {
            throw new IllegalArgumentException("Invalid configuration name: " + configName);
        }
        this.configName = configName;
        return this;
    }

    /**
     * Check the name.
     * A name must not be null and relative.
     * @param name The name
     * @return {@code true} if it is valid
     */
    private boolean isNameValid(final String name) {
        return !StringUtils.isBlank(name) 
                && !StringUtils.startsWith(name, "/") 
                && !StringUtils.contains(name, "../");
    }

    private String getConfigurationName(Class<?> clazz) {
        if (clazz == ValueMap.class && configName == null) {
            throw new ConfigurationResolveException("Configuration name is required when getting configuration properties as ValueMap.");
        }
        String name = configName;
        
        // derive configuration name from annotation class if no name specified
        if (name == null) {
            name = clazz.getName();
        }
        
        return CONFIGS_PARENT_NAME + '/' + name;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T as(final Class<T> clazz) {
        Resource configResource = null;
        if (this.contentResource != null) {
            final String name = getConfigurationName(clazz);
            configResource = this.configurationResourceResolver.getResource(this.contentResource, name);
        }
        if (clazz == ValueMap.class) {
            return (T)ResourceUtil.getValueMap(configResource);
        }
        return ConfigurationProxy.get(configResource, clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Collection<T> asCollection(Class<T> clazz) {
        if (this.contentResource != null) {
            final String name = getConfigurationName(clazz);
            Collection<Resource> configResources = this.configurationResourceResolver.getResourceCollection(this.contentResource, name);
            if (clazz == ValueMap.class) {
                return (Collection<T>)configResources.stream()
                        .map(res -> ResourceUtil.getValueMap(res))
                        .collect(Collectors.toList());
            }
            return configResources.stream()
                .map(resource -> ConfigurationProxy.get(resource, clazz))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
    
}
