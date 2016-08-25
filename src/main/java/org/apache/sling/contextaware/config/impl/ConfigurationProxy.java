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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.contextaware.config.ConfigurationResolveException;

/**
 * Maps the property of a resource to a dynamic proxy object implementing
 * the annotation class defining the configuration parameters.
 * Nested configurations with annotation classes referencing other annotation classes are also supported.
 */
final class ConfigurationProxy {
    
    private static final Pattern METHOD_NAME_MAPPING = Pattern.compile("(\\$\\$)|(\\$)|(__)|(_)");
    
    private ConfigurationProxy() {
        // static methods only
    }
    
    /**
     * Get dynamic proxy for given resources's properties mapped to given annotation class.
     * @param resource Resource
     * @param clazz Annotation class
     * @return Dynamic proxy object
     */
    @SuppressWarnings("unchecked")
    public @Nonnull static <T> T get(@Nullable Resource resource, @Nonnull Class<T> clazz) {
        
        // only annotation interface classes are supported
        if (!(clazz.isInterface() && clazz.isAnnotation())) {
            throw new ConfigurationResolveException("Annotation interface class expected: " + clazz.getName());
        }

        // create dynamic proxy for annotation class accessing underlying resource properties
        // wrap in caching invocation handler so client code can call all methods multiple times
        // without having to worry about performance
        return (T)Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz },
                new CachingInvocationHandler(new DynamicProxyInvocationHandler(resource)));
    }

    /**
     * Maps resource properties to annotation class proxy, and support nested configurations.
     */
    static class DynamicProxyInvocationHandler implements InvocationHandler {
        
        private final Resource resource;
        
        private DynamicProxyInvocationHandler(Resource resource) {
            this.resource = resource;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String propName = getPropertyName(method.getName());

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
                    Iterable<Resource> listItemResources = childResource != null ? childResource.getChildren() : Collections.emptyList();
                    List<Object> listItems = new ArrayList<Object>();
                    for (Resource listItemResource : listItemResources) {
                        listItems.add(get(listItemResource, componentType));
                    }
                    return listItems.toArray((Object[])Array.newInstance(componentType, listItems.size()));
                }
                else {
                    return get(childResource, componentType);
                }
            }
            
            // validate type
            if (!isValidType(componentType)) {
                throw new ConfigurationResolveException("Unsupported type " + componentType.getName()
                  + " in " + method.getDeclaringClass() + "#" + method.getName());
            }
            
            // detect default value
            Object defaultValue = method.getDefaultValue();
            if (defaultValue == null && targetType.isPrimitive()) {
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
        
    }
    
    /**
     * Implements the method name mapping as defined in OSGi R6 Compendium specification,
     * Chapter 112. Declarative Services Specification, Chapter 112.8.2.1. Component Property Mapping. 
     * @param Method
     * @return Mapped property name
     */
    static String getPropertyName(String methodName) {
        Matcher matcher = METHOD_NAME_MAPPING.matcher(methodName);
        StringBuffer mappedName = new StringBuffer();
        while (matcher.find()) {
            String replacement = "";
            if (matcher.group(1) != null) {
                replacement = "\\$";
            }
            if (matcher.group(2) != null) { 
                replacement = "";
            }
            if (matcher.group(3) != null) {
                replacement = "_";
            }
            if (matcher.group(4) != null) {
                replacement = ".";
            }
            matcher.appendReplacement(mappedName, replacement);
        }
        matcher.appendTail(mappedName);
        return mappedName.toString();
    }
    
    /**
     * Ensures the given type is support for reading configuration parameters.
     * @param type Type
     * @return true if type is supported
     */
    static boolean isValidType(Class<?> type) {
        return type == String.class
                || type == int.class
                || type == long.class
                || type == double.class
                || type == boolean.class;
    }
    
    /**
     * Invocation handler that caches all results for each method name, and returns
     * the result from cache on next invocation.
     */
    static class CachingInvocationHandler implements InvocationHandler {
        
        private final InvocationHandler delegate;
        private final Map<String, Object> results = new HashMap<>();
        private static final Object NULL_OBJECT = new Object();
        
        public CachingInvocationHandler(InvocationHandler delegate) {
            this.delegate = delegate;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String key = method.getName();
            Object result = results.get(key);
            if (result == null) {
                result = delegate.invoke(proxy, method, args);
                if (result == null) {
                    result = NULL_OBJECT;
                }
                results.put(key,  result);
            }
            if (result == NULL_OBJECT) {
                return null;
            }
            else {
                return result;
            }
        }
        
    }
    
}
