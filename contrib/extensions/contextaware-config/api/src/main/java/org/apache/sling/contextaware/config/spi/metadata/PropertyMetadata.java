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
package org.apache.sling.contextaware.config.spi.metadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Defines a configuration property.
 * @param <T> Property value type
 */
@ProviderType
public final class PropertyMetadata<T> extends AbstractMetadata {

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

    public PropertyMetadata(@Nonnull String name, @Nonnull Class<T> type) {
        super(name);
        if (type == null || !isSupportedType(type)) {
            throw new IllegalArgumentException("Parameter '" + name + "': Invalid type " + type);
        }
        this.type = type;
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
     */
    public void setDefaultValue(T value) {
        this.defaultValue = value;
    }

    @Override
    public String toString() {
        return getName() + "[" + this.type.getSimpleName() + "]";
    }

}
