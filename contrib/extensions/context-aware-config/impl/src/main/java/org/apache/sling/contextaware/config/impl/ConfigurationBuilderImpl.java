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
import java.util.stream.Collectors;

import org.apache.felix.scr.impl.inject.Annotations;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.contextaware.config.ConfigurationBuilder;
import org.apache.sling.contextaware.resource.ConfigurationResourceResolver;
import org.osgi.framework.Bundle;

class ConfigurationBuilderImpl implements ConfigurationBuilder {

    private static final String NAME = "sling:configs";

    private final Resource contentResource;
    private final ConfigurationResourceResolver configurationResourceResolver;
    private final Bundle bundle;

    private String configName;

    public ConfigurationBuilderImpl(Resource resource,
            ConfigurationResourceResolver configurationResourceResolver, Bundle bundle) {
        this.contentResource = resource;
        this.configurationResourceResolver = configurationResourceResolver;
        this.bundle = bundle;
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
        final String name = getConfigurationName(clazz.getName());
        // TODO - what to return on null?
        if ( name == null ) {
            return null;
        }
        final Resource configResource = this.configurationResourceResolver.getResource(this.contentResource, name);
        final ValueMap props = configResource.getValueMap();
        if (clazz == ValueMap.class) {
            return (T)props;
        }
        return Annotations.toObject(clazz, props, bundle, true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Collection<T> asCollection(Class<T> clazz) {
        final String name = getConfigurationName(clazz.getName());
        // TODO - what to return on null?
        if ( name == null ) {
            return null;
        }
        Collection<Resource> configResources = this.configurationResourceResolver.getResourceCollection(this.contentResource, name);
        Collection<ValueMap> propsList = configResources.stream()
                .map(res -> ResourceUtil.getValueMap(res))
                .collect(Collectors.toList());
        if (clazz == ValueMap.class) {
            return (Collection<T>)propsList;
        }
        return propsList.stream()
            .map(props -> Annotations.toObject(clazz, props, bundle, true))
            .collect(Collectors.toList());
    }
}
