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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.contextaware.config.ConfigurationBuilder;
import org.apache.sling.contextaware.config.ConfigurationResolveException;
import org.apache.sling.contextaware.config.resource.ConfigurationResourceResolver;

class ConfigurationBuilderImpl implements ConfigurationBuilder {

    private static final String NAME = "sling:configs";

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
    
    @SuppressWarnings("unchecked")
    private <T> T convertPropsToClass(Resource resource, Class<T> clazz) {
        
        // only annotation interface classes are supported
        if (!(clazz.isInterface() && clazz.isAnnotation())) {
            throw new ConfigurationResolveException("Annotation interface class expected: " + clazz.getName());
        }

        // create dynamic proxy for annotation class accessing underlying resource properties
        return (T)Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz },
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    String propName = getInterfacePropertyName(method);
                    if (propName == null) {
                        return null;
                    }

                    // check for nested configuration classes
                    Class<?> targetType = method.getReturnType();
                    Class<?> componentType = method.getReturnType();
                    boolean isArray = targetType.isArray();
                    if (isArray) {
                        componentType = targetType.getComponentType();
                    }
                    if (componentType.isInterface() && componentType.isAnnotation()) {
                        Resource childResource = resource != null ? resource.getChild(propName) : null;
                        if (isArray) {
                            Iterable<Resource> listItemResources = childResource != null ? childResource.getChildren() : new ArrayList<>();
                            List<Object> listItems = new ArrayList<Object>();
                            for (Resource listItemResource : listItemResources) {
                                listItems.add(convertPropsToClass(listItemResource, componentType));
                            }
                            return listItems.toArray((Object[])Array.newInstance(componentType, listItems.size()));
                        }
                        else {
                            return convertPropsToClass(childResource, componentType);
                        }
                    }
                    
                    // detect default value
                    Object defaultValue = method.getDefaultValue();
                    if (defaultValue == null && targetType.isPrimitive() && !targetType.isArray()) {
                        // get default value for primitive data type (use hack via array)
                        defaultValue = Array.get(Array.newInstance(targetType, 1), 0);
                    }
                    
                    // get value from valuemap with given type/default value
                    ValueMap props = ResourceUtil.getValueMap(resource);
                    Object value;
                    if (defaultValue != null) {
                        value = props.get(propName, defaultValue);
                    }
                    else {
                        value = props.get(propName, targetType);
                    }
                    return value;
                    
                }
            });
    }

    private static String getInterfacePropertyName(Method md) {
        // TODO: support all the escaping mechanisms.
        return md.getName().replace('_', '.');
    }
    
}
