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
package org.apache.sling.nosql.generic.resource.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;

/**
 * Enhances ValueMap that adds special support for deep path access.
 * Additionally date and binary types are converted to string and back when reading.
 * Besides this only primitive types String, Integer, Long, Double, Boolean and arrays of them are supported.
 */
class NoSqlValueMap extends ValueMapDecorator implements ModifiableValueMap {
    
    private final Resource resource;
    private final NoSqlResourceProvider resourceProvider;
    
    public NoSqlValueMap(Map<String,Object> map, Resource resource, NoSqlResourceProvider resourceProvider) {
        super(convertForWriteAll(map));
        this.resource = resource;
        this.resourceProvider = resourceProvider;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String name, Class<T> type) {
        
        if (type == Date.class) {
            Calendar value = get(name, Calendar.class);
            if (value != null) {
                return (T)value.getTime();
            }
        }
        else if (type == InputStream.class) {
            // Support conversion from byte array to InputStream
            byte[] data = get(name, byte[].class);
            if (data != null) {
                return (T)new ByteArrayInputStream(data);
            }
            else {
                return null;
            }
        }
        else if ( type == null ) {
            return (T) super.get(name);
        }
        return super.get(name, type);
    }
    
    @Override
    public Object put(String key, Object value) {
        Object result = super.put(key, convertForWrite(value));
        resourceProvider.markAsChanged(resource);
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void putAll(Map<? extends String, ?> map) {
        super.putAll((Map<? extends String, ?>)convertForWriteAll((Map<String, Object>)map));
        resourceProvider.markAsChanged(resource);
    }
    
    @Override
    public Object remove(Object key) {
        Object result = super.remove(key);
        resourceProvider.markAsChanged(resource);
        return result;
    }

    @Override
    public void clear() {
        super.clear();
        resourceProvider.markAsChanged(resource);
    }

    private static Object convertForWrite(Object value) {
        if (value instanceof Date) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime((Date)value);
            value = calendar;
        }
        else if (value instanceof InputStream) {
            // Store InputStream values as byte array
            try {
                value = convertForWrite(IOUtils.toByteArray((InputStream)value));
            } catch (IOException ex) {
                throw new RuntimeException("Unable to convert input stream to byte array.");
            }
        }
        else if (value != null && !isValidType(value.getClass())) {
            throw new IllegalArgumentException("Data type not supported for NoSqlValueMap: " + value.getClass());
        }
        return value;
    }
    
    static boolean isValidType(Class clazz) {
        if (clazz.isArray()) {
            if (clazz.getComponentType() == byte.class) {
                // byte only supported as array
                return true;
            }
            return isValidType(clazz.getComponentType());
        }
        else {
            return clazz == String.class
                    || clazz == Integer.class
                    || clazz == Long.class
                    || clazz == Double.class
                    || clazz == Boolean.class
                    || Calendar.class.isAssignableFrom(clazz);
        }
    }
    
    public static Map<String, Object> convertForWriteAll(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            map.put(entry.getKey(), convertForWrite(entry.getValue()));
        }
        return map;
    }

}
