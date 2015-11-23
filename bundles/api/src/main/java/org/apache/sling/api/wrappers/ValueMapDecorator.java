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
import java.util.ArrayList;
import java.util.Collection;
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
                return (T) String.valueOf(getSingleValue(obj));
            } else if (type == Integer.class) {
                return (T) (Integer) Integer.parseInt(getSingleValue(obj));
            } else if (type == Long.class) {
                return (T) (Long) Long.parseLong(getSingleValue(obj));
            } else if (type == Double.class) {
                return (T) (Double) Double.parseDouble(getSingleValue(obj));
            } else if (type == Boolean.class) {
                return (T) (Boolean) Boolean.parseBoolean(getSingleValue(obj));
            } else {
                return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Gets a single value of String from the object. If the object is an array it returns it's first element.
     * @param obj object or object array.
     * @return result of <code>toString()</code> on object or first element of an object array. If @param obj is null
     * or it's an array with first element that is null, then null is returned.
     */
    private String getSingleValue(Object obj) {
        final String result;
        if (obj == null) {
            result = null;
        } else if (obj.getClass().isArray()) {
            final Object[] values = (Object[]) obj;
            result = values[0] != null ? values[0].toString() : null;
        } else {
            result = obj.toString();
        }
        return result;
    }

    /**
     * Converts the object to an array of the given type
     * @param obj the object or object array
     * @param type the component type of the array
     * @return and array of type T
     */
    private <T> T[] convertToArray(Object obj, Class<T> type) {
        if (obj.getClass().isArray()) {
            final Object[] array = (Object[]) obj;
            List<Object> resultList = new ArrayList<Object>();
            for (int i = 0; i < array.length; i++) {
                T singleValueResult = convert(array[i], type);
                if (singleValueResult != null) {
                    resultList.add(singleValueResult);
                }
            }
            if (resultList.isEmpty()) {
                return null;
            }
            return resultList.toArray((T[]) Array.newInstance(type, resultList.size()));
        } else {
            @SuppressWarnings("unchecked")
            final T singleValueResult = convert(obj, type);
            // return null for type conversion errors instead of single element array with value null
            if (singleValueResult == null) {
                return null;
            }
            final T[] arrayResult = (T[]) Array.newInstance(type, 1);
            arrayResult[0] = singleValueResult;
            return arrayResult;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name, T defaultValue) {
        if ( defaultValue == null ) {
            return (T)get(name);
        }
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

    @Override
    public String toString() {
        return super.toString() + " : " + this.base.toString();
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return base.hashCode();
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        return base.equals(obj);
    }


}