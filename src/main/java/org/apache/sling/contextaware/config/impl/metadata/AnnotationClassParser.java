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
package org.apache.sling.contextaware.config.impl.metadata;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.contextaware.config.annotation.Configuration;
import org.apache.sling.contextaware.config.annotation.Property;
import org.apache.sling.contextaware.config.spi.metadata.ConfigurationMetadata;
import org.apache.sling.contextaware.config.spi.metadata.PropertyMetadata;

/**
 * Helper methods for parsing metadata from configuration annotation classes.
 */
public final class AnnotationClassParser {
    
    private static final Pattern METHOD_NAME_MAPPING = Pattern.compile("(\\$\\$)|(\\$)|(__)|(_)");
    
    private AnnotationClassParser() {
        // static methods only
    }

    /**
     * Checks if the given class is suitable to be mapped with context-aware configuration.
     * The given class has to be an annotation class, and the {@link Configuration} annotation has to be present.
     * @param clazz Given class
     * @return True if class is suitable for context-aware configuration
     */
    public static boolean isContextAwareConfig(Class<?> clazz) {
        return clazz.isAnnotation() && clazz.isAnnotationPresent(Configuration.class);
    }
    
    /**
     * Get configuration name for given configuration annotation class.
     * @param clazz Annotation class
     * @return Configuration name
     */
    public static String getConfigurationName(Class<?> clazz) {
        Configuration configAnnotation = clazz.getAnnotation(Configuration.class);
        if (configAnnotation == null) {
            return null;
        }
        return getConfigurationName(clazz, configAnnotation);
    }

    /**
     * Get configuration name for given configuration annotation class.
     * @param clazz Annotation class
     * @param configAnnotation Configuration metadata
     * @return Configuration name
     */
    private static String getConfigurationName(Class<?> clazz, Configuration configAnnotation) {
        String configName = configAnnotation.name();
        if (StringUtils.isBlank(configName)) {
            configName = clazz.getName();
        }
        return configName;
    }

    /**
     * Implements the method name mapping as defined in OSGi R6 Compendium specification,
     * Chapter 112. Declarative Services Specification, Chapter 112.8.2.1. Component Property Mapping.
     * @param Method
     * @return Mapped property name
     */
    public static String getPropertyName(String methodName) {
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
     * Build configuration metadata by parsing the given annotation interface class and it's configuration annotations.
     * @param clazz Configuration annotation class
     * @return Configuration metadata
     */
    public static ConfigurationMetadata buildConfigurationMetadata(Class<?> clazz) {
        Configuration configAnnotation = clazz.getAnnotation(Configuration.class);
        if (configAnnotation == null) {
            throw new IllegalArgumentException("Class has not @Configuration annotation: " + clazz.getName());
        }
        
        // configuration metadata
        String configName = getConfigurationName(clazz, configAnnotation);
        ConfigurationMetadata configMetadata = new ConfigurationMetadata(configName);
        configMetadata.setLabel(emptyToNull(configAnnotation.label()));
        configMetadata.setDescription(emptyToNull(configAnnotation.description()));
        configMetadata.setProperties(propsArrayToMap(configAnnotation.property()));

        // property metadata
        List<PropertyMetadata<?>> propertyMetadataList = new ArrayList<>();
        Method[] propertyMethods = clazz.getDeclaredMethods();
        for (Method propertyMethod : propertyMethods) {
            propertyMetadataList.add(buildPropertyMetadata(propertyMethod, propertyMethod.getReturnType()));
        }
        configMetadata.setPropertyMetadata(propertyMetadataList);
        
        return configMetadata;
    }
    
    @SuppressWarnings("unchecked")
    private static <T> PropertyMetadata<T> buildPropertyMetadata(Method propertyMethod, Class<T> type) {
        String propertyName = getPropertyName(propertyMethod.getName());
        PropertyMetadata<T> propertyMetadata = new PropertyMetadata<>(propertyName, type);
        propertyMetadata.setDefaultValue((T)propertyMethod.getDefaultValue());
        
        Property propertyAnnotation = propertyMethod.getAnnotation(Property.class);
        if (propertyAnnotation != null) {            
            propertyMetadata.setLabel(emptyToNull(propertyAnnotation.label()));
            propertyMetadata.setDescription(emptyToNull(propertyAnnotation.description()));
            propertyMetadata.setProperties(propsArrayToMap(propertyAnnotation.property()));
        }
        else {
            Map<String,String> emptyMap = Collections.emptyMap();
            propertyMetadata.setProperties(emptyMap);
        }
        
        return propertyMetadata;
    }
    
    private static String emptyToNull(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        else {
            return value;
        }
    }
    
    private static Map<String,String> propsArrayToMap(String[] properties) {
        Map<String,String> props = new HashMap<>();
        for (String property : properties) {
            int index = StringUtils.indexOf(property,  "=");
            if (index >= 0) {
                String key = property.substring(0, index);
                String value = property.substring(index + 1);
                props.put(key, value);
            }
        }
        return props;
    }
    
}
