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
package org.apache.sling.caconfig.spi.metadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.ClassUtils;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Defines a configuration property.
 * @param <T> Property value type
 */
@ProviderType
public final class PropertyMetadata<T> extends AbstractMetadata<PropertyMetadata<T>> {

    // these are all types supported for fields of annotation classes (plus class which indicates nested configurations)
    private static final Class<?>[] SUPPORTED_TYPES_ARRAY = {
        String.class,
        int.class,
        long.class,
        double.class,
        boolean.class
    };
    
    /**
     * Set with all types support for property metadata (not including nested configurations).
     */
    public static final Set<Class<?>> SUPPORTED_TYPES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(SUPPORTED_TYPES_ARRAY)));
    
    private final Class<T> type;
    private T defaultValue;
    private ConfigurationMetadata configurationMetadata;
    private int order;

    /**
     * @param name Property name
     * @param type Property type
     */
    @SuppressWarnings("unchecked")
    public PropertyMetadata(@Nonnull String name, @Nonnull Class<T> type) {
        super(name);
        Class<T> convertedType = (Class<T>)typeToPrimitive(type);
        if (!isSupportedType(convertedType)) {
            throw new IllegalArgumentException("Invalid type for property '" + name + "': " + type);
        }
        this.type = convertedType;
    }

    /**
     * @param name Property name
     * @param defaultValue Default value (also defines property type)
     */
    @SuppressWarnings("unchecked")
    public PropertyMetadata(@Nonnull String name, @Nonnull T defaultValue) {
        this(name, (Class<T>)defaultValue.getClass());
        this.defaultValue = defaultValue;
    }
    
    private static Class<?> typeToPrimitive(Class<?> clazz) {
        if (clazz.isArray()) {
            if (ClassUtils.isPrimitiveWrapper(clazz.getComponentType())) {
                if (clazz == Integer[].class) {
                    return int[].class;
                }
                if (clazz == Long[].class) {
                    return long[].class;
                }
                if (clazz == Double[].class) {
                    return double[].class;
                }
                if (clazz == Boolean[].class) {
                    return boolean[].class;
                }
            }
        }
        else if (ClassUtils.isPrimitiveWrapper(clazz)) {
            if (clazz == Integer.class) {
                return int.class;
            }
            if (clazz == Long.class) {
                return long.class;
            }
            if (clazz == Double.class) {
                return double.class;
            }
            if (clazz == Boolean.class) {
                return boolean.class;
            }
        }
        return clazz;
    }

    private static boolean isSupportedType(Class<?> paramType) {
        if (paramType.isArray()) {
            return isSupportedType(paramType.getComponentType());
        }
        for (Class<?> type : SUPPORTED_TYPES) {
            if (type.equals(paramType)) {
                return true;
            }
        }
        if (paramType == ConfigurationMetadata.class) {
            return true;
        }
        return false;
    }

    /**
     * @return Parameter type
     */
    public @Nonnull Class<T> getType() {
        return this.type;
    }

    /**
     * @return Default value if parameter is not set for configuration
     */
    public T getDefaultValue() {
        return this.defaultValue;
    }
    
    /**
     * @param value Default value if parameter is not set for configuration
     * @return this;
     */
    public PropertyMetadata<T> defaultValue(T value) {
        this.defaultValue = value;
        return this;
    }
    
    /**
     * @return Number to control property order in configuration editor.
     */
    public int getOrder() {
        return order;
    }

    /**
     * @param value Number to control property order in configuration editor.
     * @return this
     */
    public PropertyMetadata<T> order(int value) {
        this.order = value;
        return this;
    }

    /**
     * @return Metadata for nested configuration
     */
    public ConfigurationMetadata getConfigurationMetadata() {
        return configurationMetadata;
    }

    /**
     * @param configurationMetadata Metadata for nested configuration
     * @return this;
     */
    public PropertyMetadata<T> configurationMetadata(ConfigurationMetadata configurationMetadata) {
        this.configurationMetadata = configurationMetadata;
        return this;
    }
    
    /**
     * @return true if this property describes a nested configuration.
     *   In this case it is ensured configuration metadata is present, and the type is ConfigurationMetadata or ConfigurationMetadata[].
     */
    public boolean isNestedConfiguration() {
        return configurationMetadata != null
                && (this.type.equals(ConfigurationMetadata.class) || this.type.equals(ConfigurationMetadata[].class));
    }
    
    @Override
    public String toString() {
        return getName() + "[" + this.type.getSimpleName() + "]";
    }

}
