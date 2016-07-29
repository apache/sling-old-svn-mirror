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

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.impl.inject.Annotations;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.contextaware.config.ConfigurationBuilder;
import org.apache.sling.contextaware.resource.ConfigurationResourceResolver;
import org.osgi.framework.Bundle;

class ConfigurationBuilderImpl implements ConfigurationBuilder {

    private final Resource resource;
    private final ConfigurationResourceResolver configurationResourceResolver;
    private final Bundle bundle;

    private String configName;

    public ConfigurationBuilderImpl(Resource resource,
            ConfigurationResourceResolver configurationResourceResolver, Bundle bundle) {
        this.resource = resource;
        this.configurationResourceResolver = configurationResourceResolver;
        this.bundle = bundle;
    }

    @Override
    public ConfigurationBuilder name(String configName) {
        this.configName = configName;
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T as(Class<T> clazz) {
        // TODO: this is only a dummy implementation
        String name = StringUtils.defaultString(configName, clazz.getName());
        ValueMap props = getValueMap(configurationResourceResolver, resource, name);
        if (clazz == ValueMap.class) {
            return (T)props;
        }
        return Annotations.toObject(clazz, props, bundle, true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Collection<T> asCollection(Class<T> clazz) {
        // TODO: this is only a dummy implementation
        String name = StringUtils.defaultString(configName, clazz.getName());
        Collection<ValueMap> propsList = getValueMapCollection(configurationResourceResolver, resource, name);
        if (clazz == ValueMap.class) {
            return (Collection<T>)propsList;
        }
        return propsList.stream()
            .map(props -> Annotations.toObject(clazz, props, bundle, true))
            .collect(Collectors.toList());
    }

    private static ValueMap getValueMap(ConfigurationResourceResolver resolver, Resource resource, String configName) {
        // TODO: this is only a dummy implementation
        Resource configResource = resolver.getResource(resource, configName);
        return ResourceUtil.getValueMap(configResource);
    }

    private static Collection<ValueMap> getValueMapCollection(ConfigurationResourceResolver resolver, Resource resource, String configName) {
        // TODO: this is only a dummy implementation
        Collection<Resource> configResources = resolver.getResourceList(resource, configName);
        return configResources.stream()
                .map(res -> ResourceUtil.getValueMap(res))
                .collect(Collectors.toList());
    }

}
