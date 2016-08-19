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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.contextaware.config.ConfigurationBuilder;
import org.apache.sling.contextaware.config.resource.ConfigurationResourceResolver;
import org.osgi.service.converter.ConversionException;
import org.osgi.service.converter.Converter;

class ConfigurationBuilderImpl implements ConfigurationBuilder {

    private static final String NAME = "sling:configs";

    private final Resource contentResource;
    private final ConfigurationResourceResolver configurationResourceResolver;

    private final Converter converter;

    private String configName;

    public ConfigurationBuilderImpl(final Resource resource,
            final ConfigurationResourceResolver configurationResourceResolver,
            final Converter converter) {
        this.contentResource = resource;
        this.configurationResourceResolver = configurationResourceResolver;
        this.converter = converter;
    }

    @Override
    public ConfigurationBuilder name(String configName) {
        this.configName = configName;
        return this;
    }

    /**
     * Check the name.
     * A name must not be null and relative.
     * @param name The name
     * @return {@code true} if it is valid
     */
    private boolean checkName(final String name) {
        if (name == null || name.isEmpty() || name.startsWith("/") || name.contains("../") ) {
            return false;
        }
        return true;
    }

    private String getConfigurationName(final String defaultName) {
        String name;
        if ( this.configName != null ) {
            name = this.configName;
        } else {
            name = defaultName;
        }
        if ( checkName(name) ) {
            return NAME + '/' + name;
        }
        return null;
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> T as(final Class<T> clazz) {
        Resource configResource = null;
        final String name = getConfigurationName(clazz.getName());
        if ( name != null && this.contentResource != null ) {
            configResource = this.configurationResourceResolver.getResource(this.contentResource, name);
        }
        if (clazz == ValueMap.class) {
            return (T)ResourceUtil.getValueMap(configResource);
        }
        return convertPropsToClass(configResource, clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Collection<T> asCollection(Class<T> clazz) {
        final String name = getConfigurationName(clazz.getName());
        if ( name != null && this.contentResource != null ) {
            Collection<Resource> configResources = this.configurationResourceResolver.getResourceCollection(this.contentResource, name);
            if (clazz == ValueMap.class) {
                return (Collection<T>)configResources.stream()
                        .map(res -> ResourceUtil.getValueMap(res))
                        .collect(Collectors.toList());
            }
            return configResources.stream()
                .map(resource -> convertPropsToClass(resource, clazz))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
    
    private <T> T convertPropsToClass(Resource resource, Class<T> clazz) {
        /*
         * do not add special handling for ConversionException (it's already a runtime exception).
         * when using annotation classes it's unlikely that conversion exceptions are thrown when converting the class,
         * they will be thrown when accessing one of this properties. so it's not possible possible to
         * protected the upstream code completely from ConverstionExceptions, no need to do it here then. 
         */
        ValueMap props = ResourceUtil.getValueMap(resource);
        T result = converter.convert(props).to(clazz);
        if (result == null) {
            String path = resource != null ? resource.getPath() : "<unknown>";
            throw new ConversionException("Unable to convert config properties from " + path + " to " + clazz.getName() + " - result is null.");
        }
        return result;
    }
    
}
