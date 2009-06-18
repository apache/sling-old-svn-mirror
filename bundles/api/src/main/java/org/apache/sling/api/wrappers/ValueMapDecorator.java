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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.ValueMap;

/**
 * <code>ValueMapDecorator</code> decorates another {@link Map}
 * to provide a basic implementation for the additional methods
 * of a {@link ValueMap}.
 */
public class ValueMapDecorator implements ValueMap {

    /**
     * underlying map
     */
    private final Map<String, Object> base;

    /**
     * Creates a new wrapper around a given map.
     * @param base wrapped object
     */
    public ValueMapDecorator(Map<String, Object> base) {
        this.base = base;
    }

    /**
     * {@inheritDoc}
     */
    public <T> T get(String name, Class<T> type) {
        return convert(get(name), type);
    }

    /**
     * Converts the object to the given type.
     * @param obj object
     * @param type type
     * @return the converted object
     */
    @SuppressWarnings("unchecked")
    private <T> T convert(Object obj, Class<T> type) {
        // todo: do smarter checks
        try {
            if (obj == null) {
                return null;
            } else if (type.isAssignableFrom(obj.getClass())) {
                return (T) obj;
            } else if (type.isArray()) {
                return (T) convertToArray(obj, type.getComponentType());
            } else if (type == String.class) {
                return (T) String.valueOf(obj);
            } else if (type == Integer.class) {
                return (T) (Integer) Integer.parseInt(obj.toString());
            } else if (type == Long.class) {
                return (T) (Long) Long.parseLong(obj.toString());
            } else if (type == Double.class) {
                return (T) (Double) Double.parseDouble(obj.toString());
            } else if (type == Boolean.class) {
                return (T) (Boolean) Boolean.parseBoolean(obj.toString());
            } else {
                return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Converts the object to an array of the given type
     * @param obj tje object or object array
     * @param type the component type of the array
     * @return and array of type T
     */
    private <T> T[] convertToArray(Object obj, Class<T> type) {
        List<T> values = new LinkedList<T>();
        if (obj.getClass().isArray()) {
            for (Object o: (Object[]) obj) {
                values.add(convert(o, type));
            }
        } else {
            values.add(convert(obj, type));
        }
        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(type, values.size());
        return values.toArray(result);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name, T defaultValue) {
        T value = get(name, (Class<T>) defaultValue.getClass());
        return value == null ? defaultValue : value;
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return base.size();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return base.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        return base.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(Object value) {
        return base.containsValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Object get(Object key) {
        return base.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public Object put(String key, Object value) {
        return base.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    public Object remove(Object key) {
        return base.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(Map<? extends String, ?> t) {
        base.putAll(t);
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        base.clear();
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> keySet() {
        return base.keySet();
    }

    /**
     * {@inheritDoc}
     */
    public Collection<Object> values() {
        return base.values();
    }

    /**
     * {@inheritDoc}
     */
    public Set<Entry<String, Object>> entrySet() {
        return base.entrySet();
    }
}