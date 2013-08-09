/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.mongodb.impl;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.ValueMap;

import com.mongodb.DBObject;

public class ReadableValueMap implements ValueMap {

    protected Map<String, Object> valueMap;

    public ReadableValueMap(final DBObject dbObject) {
        this.createValueMap(dbObject);
    }

    protected void createValueMap(final DBObject dbObject) {
        final Map<String, Object> map = new HashMap<String, Object>();
        if (dbObject == null) {
            this.valueMap = Collections.<String, Object> emptyMap();
        }
        for(final String key : dbObject.keySet()) {
            final String name = MongoDBResourceProvider.keyToPropName(key);
            if ( name != null ) {
                map.put(key, dbObject.get(name));
            }
        }
        this.valueMap = Collections.unmodifiableMap(map);
    }

    /**
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(final Object key) {
        return this.valueMap.containsKey(key);
    }

    /**
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(final Object value) {
        return this.valueMap.containsValue(value);
    }

    /**
     * @see java.util.Map#entrySet()
     */
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return this.valueMap.entrySet();
    }

    /**
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(final Object key) {
        return this.valueMap.get(key);
    }

    /**
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        return this.valueMap.isEmpty();
    }

    /**
     * @see java.util.Map#keySet()
     */
    public Set<String> keySet() {
        return this.valueMap.keySet();
    }

    /**
     * @see java.util.Map#size()
     */
    public int size() {
        return this.valueMap.size();
    }

    /**
     * @see java.util.Map#values()
     */
    public Collection<Object> values() {
        return this.valueMap.values();
    }

    /**
     * @see org.apache.sling.api.resource.ValueMap#get(java.lang.String, java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final String key, final Class<T> type) {
        if (type == null) {
            return (T) get(key);
        }

        final Object val = this.get(key);
        if ( val == null ) {
            return null;
        }
        return convertToType(val, type);
    }

    /**
     * @see org.apache.sling.api.resource.ValueMap#get(java.lang.String, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final String key,final T defaultValue) {
        if (defaultValue == null) {
            return (T) get(key);
        }

        T value = get(key, (Class<T>) defaultValue.getClass());
        if (value == null) {
            value = defaultValue;
        }

        return value;
    }

    /**
     * @see java.util.Map#clear()
     */
    public void clear() {
        throw new UnsupportedOperationException("clear");
    }

    /**
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException("put");
    }

    /**
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map<? extends String, ? extends Object> m) {
        throw new UnsupportedOperationException("putAll");
    }

    /**
     * @see java.util.Map#remove(java.lang.Object)
     */
    public Object remove(Object key) {
        throw new UnsupportedOperationException("remove");
    }

    /**
     * Converts the object to the given type.
     * @param obj object
     * @param type type
     * @return the converted object
     */
    @SuppressWarnings("unchecked")
    private <T> T convertToType(final Object obj, final Class<T> type) {
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
        } catch (final NumberFormatException e) {
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
                values.add(convertToType(o, type));
            }
        } else {
            values.add(convertToType(obj, type));
        }
        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(type, values.size());
        return values.toArray(result);
    }
}
