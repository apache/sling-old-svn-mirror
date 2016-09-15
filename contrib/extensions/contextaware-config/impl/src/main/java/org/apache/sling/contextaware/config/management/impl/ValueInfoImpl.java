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
package org.apache.sling.contextaware.config.management.impl;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.sling.contextaware.config.management.ValueInfo;
import org.apache.sling.contextaware.config.spi.metadata.PropertyMetadata;

final class ValueInfoImpl<T> implements ValueInfo<T> {
    
    private final T value;
    private final T defaultValue;
    private final String configSourcePath;
    private final PropertyMetadata<T> propertyMetadata;
    
    public ValueInfoImpl(T value, String configSourcePath, PropertyMetadata<T> propertyMetadata) {
        this.value = value;
        this.defaultValue = propertyMetadata != null ? propertyMetadata.getDefaultValue() : null;
        this.configSourcePath = configSourcePath;
        this.propertyMetadata = propertyMetadata;
    }

    @Override
    public PropertyMetadata<T> getPropertyMetadata() {
        return propertyMetadata;
    }

    @Override
    public T getValue() {
        return ObjectUtils.defaultIfNull(value, defaultValue);
    }

    @Override
    public T getEffectiveValue() {
        // TODO: this may return different values when property inheritance is enabled
        return getValue();
    }

    @Override
    public String getConfigSourcePath() {
        return configSourcePath;
    }

    @Override
    public boolean isInherited() {
        // TODO: implement check
        return false;
    }

    @Override
    public boolean isOverridden() {
        // TODO: implement check
        return false;
    }

    @Override
    public boolean isDefault() {
        return value == null && defaultValue != null;
    }

    @Override
    public boolean isLocked() {
        // TODO: implement check
        return false;
    }

}
