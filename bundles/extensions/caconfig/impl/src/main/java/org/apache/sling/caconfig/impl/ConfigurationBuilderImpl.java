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
package org.apache.sling.caconfig.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.caconfig.ConfigurationResolveException;
import org.apache.sling.caconfig.ConfigurationResolver;
import org.apache.sling.caconfig.impl.ConfigurationProxy.ChildResolver;
import org.apache.sling.caconfig.impl.metadata.AnnotationClassParser;
import org.apache.sling.caconfig.impl.override.ConfigurationOverrideManager;
import org.apache.sling.caconfig.resource.impl.util.ConfigNameUtil;
import org.apache.sling.caconfig.resource.impl.util.MapUtil;
import org.apache.sling.caconfig.resource.spi.ConfigurationResourceResolvingStrategy;
import org.apache.sling.caconfig.spi.ConfigurationInheritanceStrategy;
import org.apache.sling.caconfig.spi.ConfigurationMetadataProvider;
import org.apache.sling.caconfig.spi.ConfigurationPersistenceStrategy;
import org.apache.sling.caconfig.spi.metadata.ConfigurationMetadata;
import org.apache.sling.caconfig.spi.metadata.PropertyMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConfigurationBuilderImpl implements ConfigurationBuilder {

    private final Resource contentResource;
    private final ConfigurationResolver configurationResolver;
    private final ConfigurationResourceResolvingStrategy configurationResourceResolvingStrategy;
    private final ConfigurationPersistenceStrategy configurationPersistenceStrategy;
    private final ConfigurationInheritanceStrategy configurationInheritanceStrategy;
    private final ConfigurationOverrideManager configurationOverrideManager;
    private final ConfigurationMetadataProvider configurationMetadataProvider;
    private final Collection<String> configBucketNames;

    private String configName;

    private static final Logger log = LoggerFactory.getLogger(ConfigurationBuilderImpl.class);
    
    public ConfigurationBuilderImpl(final Resource resource,
            final ConfigurationResolver configurationResolver,
            final ConfigurationResourceResolvingStrategy configurationResourceResolvingStrategy,
            final ConfigurationPersistenceStrategy configurationPersistenceStrategy,
            final ConfigurationInheritanceStrategy configurationInheritanceStrategy,
            final ConfigurationOverrideManager configurationOverrideManager,
            final ConfigurationMetadataProvider configurationMetadataProvider,
            final Collection<String> configBucketNames) {
        this.contentResource = resource;
        this.configurationResolver = configurationResolver;
        this.configurationResourceResolvingStrategy = configurationResourceResolvingStrategy;
        this.configurationPersistenceStrategy = configurationPersistenceStrategy;
        this.configurationInheritanceStrategy = configurationInheritanceStrategy;
        this.configurationOverrideManager = configurationOverrideManager;
        this.configurationMetadataProvider = configurationMetadataProvider;
        this.configBucketNames = configBucketNames;
    }

    @Override
    public ConfigurationBuilder name(String configName) {
        ConfigNameUtil.ensureValidConfigName(configName);
        this.configName = configName;
        return this;
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
        T convert(Resource resource, Class<T> clazz, String configName);
    }

    /**
     * Get singleton configuration resource and convert it to the desired target class.
     * @param configName Configuration name
     * @param clazz Target class
     * @param converter Conversion method
     * @return Converted singleton configuration
     */
    private <T> T getConfigResource(String configName, Class<T> clazz, Converter<T> converter) {
        Iterator<Resource> resourceInheritanceChain = null;
        if (this.contentResource != null) {
            validateConfigurationName(configName);
            resourceInheritanceChain = this.configurationResourceResolvingStrategy
                    .getResourceInheritanceChain(this.contentResource, configBucketNames, configName);
        }
        return convert(resourceInheritanceChain, clazz, converter, configName, false);
    }

    /**
     * Get configuration resource collection and convert it to the desired target class.
     * @param configName Configuration name
     * @param clazz Target class
     * @param converter Conversion method
     * @return Converted configuration collection
     */
    private <T> Collection<T> getConfigResourceCollection(String configName, Class<T> clazz, Converter<T> converter) {
        if (this.contentResource != null) {
           validateConfigurationName(configName);
           final Collection<T> result = new ArrayList<>();
           Collection<Iterator<Resource>> resourceInheritanceChains = this.configurationResourceResolvingStrategy
                   .getResourceCollectionInheritanceChain(this.contentResource, configBucketNames, configName);;
           if (resourceInheritanceChains != null) {
               for (final Iterator<Resource> resourceInheritanceChain : resourceInheritanceChains) {
                   final T obj = convert(resourceInheritanceChain, clazz, converter, configName, true);
                   if (obj != null) {
                       result.add(obj);
                   }
               }
           }
           return result;
        }
        else {
            return Collections.emptyList();
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> T convert(Iterator<Resource> resourceInhertianceChain, Class<T> clazz, Converter<T> converter,
            String name, boolean appendResourceName) {
        Resource configResource = null;
        String conversionName = name;
        if (resourceInhertianceChain != null) {
            // apply persistence transformation
            Iterator<Resource> transformedResources = IteratorUtils.transformedIterator(resourceInhertianceChain,
                    new Transformer() {
                        @Override
                        public Object transform(Object input) {
                            return configurationPersistenceStrategy.getResource((Resource)input);
                        }
                    });
            // apply resource inheritance
            configResource = configurationInheritanceStrategy.getResource(transformedResources);
            // apply overrides
            configResource = configurationOverrideManager.overrideProperties(contentResource.getPath(), name, configResource);
            // build name
            if (configResource != null && appendResourceName) {
                conversionName = conversionName + "/" + configResource.getName();
            }
        }
        if (log.isTraceEnabled() && configResource != null) {
            log.trace("+ Found config resource for context path " + contentResource.getPath() + ": " + configResource.getPath() + " "
                    + MapUtil.traceOutput(configResource.getValueMap()));
        }
        return converter.convert(configResource, clazz, conversionName);
    }
    
    /**
     * Apply default values from configuration metadata (where no real data is present).
     * @param resource Resource
     * @param configName Configuration name
     * @return null if no default values found, or a wrapped resource with added default properties.
     */
    private Resource applyDefaultValues(Resource resource, String configName) {
        if (resource == null) {
            return null;
        }
        Map<String,Object> updatedMap = applyDefaultValues(resource.getValueMap(), configName);
        if (updatedMap == null) {
            return resource;
        }
        return new ConfigurationResourceWrapper(resource, new ValueMapDecorator(updatedMap));
    }
    
    /**
     * Apply default values from configuration metadata (where no real data is present).
     * @param props Properties
     * @param configName Configuration name
     * @return null if no default values found, or a new map with added default properties.
     */
    private Map<String,Object> applyDefaultValues(Map<String,Object> props, String configName) {
        ConfigurationMetadata metadata = configurationMetadataProvider.getConfigurationMetadata(configName);
        if (metadata == null) {
            // probably a configuration list - remove item name from end
            if (StringUtils.contains(configName, "/")) {
                String partialConfigName = StringUtils.substringBeforeLast(configName, "/");
                metadata = configurationMetadataProvider.getConfigurationMetadata(partialConfigName);
            }
            if (metadata == null) {
                return null;
            }
        }
        Map<String,Object> updatedMap = new HashMap<>();
        for (PropertyMetadata<?> propertyMetadata : metadata.getPropertyMetadata().values()) {
            if (propertyMetadata.getDefaultValue() != null) {
                updatedMap.put(propertyMetadata.getName(), propertyMetadata.getDefaultValue());
            }
        }
        if (updatedMap.isEmpty()) {
            return null;
        }
        updatedMap.putAll(props);
        return updatedMap;
    }

    // --- Annotation class support ---

    @Override
    public <T> T as(final Class<T> clazz) {
        final String name = getConfigurationNameForAnnotationClass(clazz);
        if (log.isDebugEnabled()) {
            log.debug("Get configuration for context path {}, name '{}', class {}", contentResource.getPath(), name, clazz.getName());
        }
        return getConfigResource(name, clazz, new AnnotationConverter<T>());
    }

    @Override
    public <T> Collection<T> asCollection(Class<T> clazz) {
        final String name = getConfigurationNameForAnnotationClass(clazz);
        if (log.isDebugEnabled()) {
            log.debug("Get configuration collection for context path {}, name '{}', class {}", contentResource.getPath(), name, clazz.getName());
        }
        return getConfigResourceCollection(name, clazz, new AnnotationConverter<T>());
    }

    private String getConfigurationNameForAnnotationClass(Class<?> clazz) {
        if (this.configName != null) {
            return this.configName;
        }
        else {
            // derive configuration name from annotation class if no name specified
            return AnnotationClassParser.getConfigurationName(clazz);
        }
    }

    private class AnnotationConverter<T> implements Converter<T> {
        @Override
        public T convert(final Resource resource, final Class<T> clazz, final String configName) {
            return ConfigurationProxy.get(resource, clazz, new ChildResolver() {
                private ConfigurationBuilder getConfiguration(String nestedConfigName) {
                    String childName = configurationPersistenceStrategy.getResourcePath(configName) + "/" + nestedConfigName;
                    return configurationResolver.get(contentResource).name(childName);
                }
                @Override
                public <C> C getChild(String configName, Class<C> clazz) {
                    return getConfiguration(configName).as(clazz);
                }
                @Override
                public <C> Collection<C> getChildren(String configName, Class<C> clazz) {
                    return getConfiguration(configName).asCollection(clazz);
                }
            });
        }
    }
    
    // --- ValueMap support ---

    @Override
    public ValueMap asValueMap() {
        if (log.isDebugEnabled()) {
            log.debug("Get ValueMap for context path {}, name '{}'", contentResource.getPath(), this.configName);
        }
        return getConfigResource(this.configName, ValueMap.class, new ValueMapConverter());
    }

    @Override
    public Collection<ValueMap> asValueMapCollection() {
        if (log.isDebugEnabled()) {
            log.debug("Get ValueMap collection for context path {}, name '{}'", contentResource.getPath(), this.configName);
        }
        return getConfigResourceCollection(this.configName, ValueMap.class, new ValueMapConverter());
    }

    private class ValueMapConverter implements Converter<ValueMap> {
        @Override
        public ValueMap convert(Resource resource, Class<ValueMap> clazz, String configName) {
            ValueMap props = ResourceUtil.getValueMap(resource);
            Map<String,Object> updatedMap = applyDefaultValues(props, configName);
            if (updatedMap != null) {
                return new ValueMapDecorator(updatedMap);
            }
            else {
                return props;
            }
        }
    }
    
    // --- Adaptable support ---

    @Override
    public <T> T asAdaptable(Class<T> clazz) {
        if (log.isDebugEnabled()) {
            log.debug("Get adaptable for context path {}, name '{}', class {}", contentResource.getPath(), this.configName, clazz);
        }
        return getConfigResource(this.configName, clazz, new AdaptableConverter<T>());
    }

    @Override
    public <T> Collection<T> asAdaptableCollection(Class<T> clazz) {
        if (log.isDebugEnabled()) {
            log.debug("Get adaptable collection for context path {}, name '{}', class {}", contentResource.getPath(), this.configName, clazz);
        }
        return getConfigResourceCollection(this.configName, clazz, new AdaptableConverter<T>());
    }

    private class AdaptableConverter<T> implements Converter<T> {
        @Override
        public T convert(Resource resource, Class<T> clazz, String configName) {
            if (resource == null || clazz == ConfigurationBuilder.class) {
                return null;
            }
            return applyDefaultValues(resource, configName).adaptTo(clazz);
        }
    }

}
