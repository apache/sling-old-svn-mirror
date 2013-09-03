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
package org.apache.sling.api.wrappers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.ValueMap;

/**
 * An implementation of the {@link ValueMap} based on two {@link ValueMap}s:
 * - One containing the properties
 * - Another one containing the defaults to use in case the properties map
 *   does not contain the values.
 * In case you would like to avoid duplicating properties on multiple resources,
 * you can use a <code>CompositeValueMap</code> to get a concatenated map of
 * properties.
 * @since 2.3
 */
public class CompositeValueMap implements ValueMap {

    /**
     * Current properties
     */
    private final ValueMap properties;

    /**
     * Default properties
     */
    private final ValueMap defaults;

    /**
     * Merge mode
     */
    private final boolean merge;

    /**
     * Constructor
     * @param properties The {@link ValueMap} to read from
     * @param defaults The default {@link ValueMap} to use as fallback
     */
    public CompositeValueMap(final ValueMap properties, final ValueMap defaults) {
        this(properties, defaults, true);
    }

    /**
     * Constructor
     * @param properties The {@link ValueMap} to read from
     * @param defaults The default {@link ValueMap} to use as fallback
     * @param merge Merge flag
     *              - If <code>true</code>, getting a key would return the
     *              current property map's value (if available), even if the
     *              corresponding default does not exist.
     *              - If <code>false</code>, getting a key would return
     *              <code>null</code> if the corresponding default does not
     *              exist
     */
    public CompositeValueMap(final ValueMap properties, final ValueMap defaults, boolean merge) {
        if (properties == null) {
            throw new IllegalArgumentException("Properties need to be provided");
        }
        this.properties = properties;
        this.defaults = defaults != null ? defaults : ValueMap.EMPTY;
        this.merge = merge;
    }

    // ---- ValueMap

    /**
     * {@inheritDoc}
     */
    public <T> T get(final String key, final Class<T> type) {
        if (merge || defaults.containsKey(key)) {
            // Check if property has been provided, if not use defaults
            if (properties.containsKey(key)) {
                return properties.get(key, type);
            } else {
                return defaults.get(key, type);
            }
        }

        // Override mode and no default value provided for this key
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final String key, final T defaultValue) {
        if (defaultValue == null) {
            return (T) get(key);
        }

        T value = get(key, (Class<T>) defaultValue.getClass());
        if (value != null) {
            return value;
        }

        return defaultValue;
    }


    // ---- Map

    /**
     * {@inheritDoc}
     */
    public int size() {
        return keySet().size();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        if ( defaults.size() > 0 || (merge && properties.size() > 0) ) {
            return false;
        }
        return size() == 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(final Object key) {
        return keySet().contains(key.toString());
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(final Object value) {
        return values().contains(value);
    }

    /**
     * {@inheritDoc}
     */
    public Object get(final Object key) {
        if (merge || defaults.containsKey(key)) {
            // Check if property has been provided, if not use defaults
            if (properties.containsKey(key)) {
                return properties.get(key);
            } else {
                return defaults.get(key);
            }
        }

        // Override mode and no default value provided for this key
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Object put(final String aKey, final Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Object remove(final Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(final Map<? extends String, ?> properties) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> keySet() {
        return buildAggregatedMap().keySet();
    }

    /**
     * {@inheritDoc}
     */
    public Collection<Object> values() {
        return buildAggregatedMap().values();
    }

    /**
     * {@inheritDoc}
     */
    public Set<Entry<String, Object>> entrySet() {
        return buildAggregatedMap().entrySet();
    }

    /**
     * Build the aggregated map containing all values.
     */
    private Map<String, Object> buildAggregatedMap() {
        final Map<String, Object> entries = new HashMap<String, Object>();

        // Add properties in merge mode or if defaults exists
        for (final Entry<String, Object> entry : properties.entrySet()) {
            if (merge || defaults.containsKey(entry.getKey())) {
                entries.put(entry.getKey(), entry.getValue());
            }
        }

        // Add missing defaults
        for (final Entry<String, Object> entry : defaults.entrySet()) {
            if ( ! entries.containsKey(entry.getKey()) ) {
                entries.put(entry.getKey(), entry.getValue());
            }
        }

        return entries;
    }
}
