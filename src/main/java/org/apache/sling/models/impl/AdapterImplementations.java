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
package org.apache.sling.models.impl;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.sling.models.spi.ImplementationPicker;

/**
 * Collects alternative adapter implementations that may be defined in a @Model.adapters attribute.
 * If multiple models implement the same adapter they are all collected and can be chose via a ImplementationPicker.
 * The implementation is thread-safe.
 */
final class AdapterImplementations {
    
    private final ConcurrentMap<String,ConcurrentNavigableMap<String,Class<?>>> adapterImplementations
            = new ConcurrentHashMap<String,ConcurrentNavigableMap<String,Class<?>>>();

    private volatile ImplementationPicker[] sortedImplementationPickers = new ImplementationPicker[0];

    public void setImplementationPickers(Collection<ImplementationPicker> implementationPickers) {
        this.sortedImplementationPickers = implementationPickers.toArray(new ImplementationPicker[implementationPickers.size()]);
    }

    public ImplementationPicker[] getImplementationPickers() {
        return this.sortedImplementationPickers;
    }

    /**
     * Add implementation mapping for the given adapter type.
     * @param adapterType Adapter type
     * @param implType Implementation type
     */
    public void add(Class<?> adapterType, Class<?> implType) {
        // although we already use a ConcurrentMap synchronize explicitly because we apply non-atomic operations on it
        synchronized (adapterImplementations) {
            String key = adapterType.getName();
            ConcurrentNavigableMap<String,Class<?>> implementations = adapterImplementations.get(key);
            if (implementations == null) {
                // to have a consistent ordering independent of bundle loading use a ConcurrentSkipListMap that sorts by class name
                implementations = new ConcurrentSkipListMap<String,Class<?>>();
                adapterImplementations.put(key, implementations);
            }
            implementations.put(implType.getName(), implType);
        }
    }
    
    /**
     * Remove implementation mapping for the given adapter type.
     * @param adapterTypeName Adapter type name
     * @param implTypeName Implementation type name
     */
    public void remove(String adapterTypeName, String implTypeName) {
        // although we already use a ConcurrentMap synchronize explicitly because we apply non-atomic operations on it
        synchronized (adapterImplementations) {
            String key = adapterTypeName;
            ConcurrentNavigableMap<String,Class<?>> implementations = adapterImplementations.get(key);
            if (implementations != null) {
                implementations.remove(implTypeName);
                if (implementations.isEmpty()) {
                    adapterImplementations.remove(key);
                }
            }
        }
    }

    /**
     * Remove all implementation mappings.
     */
    public void removeAll() {
        adapterImplementations.clear();
    }

    /**
     * Lookup the best-matching implementation for the given adapter type by enquiring the {@link ImplementationPicker} services.
     * @param adapterType Adapter type
     * @param adaptable Adaptable for reference
     * @return Implementation type or null if none detected
     */
    public Class<?> lookup(Class<?> adapterType, Object adaptable) {
        String key = adapterType.getName();

        ConcurrentNavigableMap<String,Class<?>> implementations = adapterImplementations.get(key);
        if (implementations==null || implementations.isEmpty()) {
            return null;
        }
        Collection<Class<?>> implementationsCollection = implementations.values();
        Class<?>[] implementationsArray = implementationsCollection.toArray(new Class<?>[implementationsCollection.size()]);

        for (ImplementationPicker picker : this.sortedImplementationPickers) {
            Class<?> implementation = picker.pick(adapterType, implementationsArray, adaptable);
            if (implementation != null) {
                return implementation;
            }
        }

        return null;
    }

}
