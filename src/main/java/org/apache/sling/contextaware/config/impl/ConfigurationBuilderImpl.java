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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

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

    /**
     * Validate the configuration name.
     * @param name Configuration name or relative path
     */
    private void validateConfigurationName(String name) {
        if (name == null) {
            throw new ConfigurationResolveException("Configuration name is required.");
        }
    }

    /**
     * Converts configuration resource into given class.
     * @param <T> Target class
     */
    private interface Converter<T> {
        T convert(Resource resource, Class<T> clazz);
    }

    /**
     * Get singleton configuration resource and convert it to the desired target class.
     * @param name Configuration name
     * @param clazz Target class
     * @param converter Conversion method
     * @return Converted singleton configuration
     */
    private <T> T getConfigResource(String name, Class<T> clazz, Converter<T> converter) {
        Resource configResource = null;
        if (this.contentResource != null) {
            validateConfigurationName(name);
            configResource = this.configurationResourceResolver.getResource(this.contentResource, CONFIGS_PARENT_NAME, name);
        }
        return converter.convert(configResource, clazz);
    }

    /**
     * Get configuration resource collection and convert it to the desired target class.
     * @param name Configuration name
     * @param clazz Target class
     * @param converter Conversion method
     * @return Converted configuration collection
     */
    private <T> Collection<T> getConfigResourceCollection(String name, Class<T> clazz, Converter<T> converter) {
        if (this.contentResource != null) {
           validateConfigurationName(name);
           final Collection<T> result = new ArrayList<>();
           for(final Resource rsrc : this.configurationResourceResolver.getResourceCollection(this.contentResource, CONFIGS_PARENT_NAME, name)) {
               final T obj = converter.convert(rsrc, clazz);
               if ( obj != null ) {
                   result.add(obj);
               }
           }
           return result;
        } else {
            return Collections.emptyList();
        }
    }

    // --- Annotation class support ---

    @Override
    public <T> T as(final Class<T> clazz) {
        final String name = getConfigurationNameForAnnotationClass(clazz);
        return getConfigResource(name, clazz, new AnnotationConverter<T>());
    }

    @Override
    public <T> Collection<T> asCollection(Class<T> clazz) {
        final String name = getConfigurationNameForAnnotationClass(clazz);
        return getConfigResourceCollection(name, clazz, new AnnotationConverter<T>());
    }

    private String getConfigurationNameForAnnotationClass(Class<?> clazz) {
        if (this.configName != null) {
            return this.configName;
        }
        else {
            // derive configuration name from annotation class if no name specified
            return clazz.getName();
        }
    }

    private static class AnnotationConverter<T> implements Converter<T> {
        @Override
        public T convert(Resource resource, Class<T> clazz) {
            return ConfigurationProxy.get(resource, clazz);
        }
    }
    
    // --- ValueMap support ---

    @Override
    public ValueMap asValueMap() {
        return getConfigResource(this.configName, ValueMap.class, new ValueMapConverter());
    }

    @Override
    public Collection<ValueMap> asValueMapCollection() {
        return getConfigResourceCollection(this.configName, ValueMap.class, new ValueMapConverter());
    }

    private static class ValueMapConverter implements Converter<ValueMap> {
        @Override
        public ValueMap convert(Resource resource, Class<ValueMap> clazz) {
            return ResourceUtil.getValueMap(resource);
        }
    }
    
    // --- Adaptable support ---

    @Override
    public <T> T asAdaptable(Class<T> clazz) {
        return getConfigResource(this.configName, clazz, new AdaptableConverter<T>());
    }

    @Override
    public <T> Collection<T> asAdaptableCollection(Class<T> clazz) {
        return getConfigResourceCollection(this.configName, clazz, new AdaptableConverter<T>());
    }

    private static class AdaptableConverter<T> implements Converter<T> {
        @Override
        public T convert(Resource resource, Class<T> clazz) {
            if (resource == null || clazz == ConfigurationBuilder.class) {
                return null;
            }
            return resource.adaptTo(clazz);
        }
    }

}
