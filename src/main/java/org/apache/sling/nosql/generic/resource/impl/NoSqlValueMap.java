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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

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
        
        if (type == Calendar.class) {
            Date date = get(name, Date.class);
            if (date != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                return (T)calendar;
            }
            else {
                return null;
            }
        }
        else if (type == Date.class) {
            Object value = get(name);
            if (value instanceof String) {
                try {
                    return (T)getISO8601Format().parse((String)value);
                } catch (ParseException e) {
                    return null;
                }
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
        else if (type == byte[].class) {
            // Support conversion from base64 string to byte array
            Object value = get(name);
            if (value instanceof String) {
                return (T)DatatypeConverter.parseBase64Binary((String)value);
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
        if (value instanceof Calendar) {
            value = getISO8601Format().format(((Calendar)value).getTime());
        }
        if (value instanceof Date) {
            value = getISO8601Format().format((Date)value);
        }
        else if (value instanceof InputStream) {
            // Store InputStream values as byte array
            try {
                value = convertForWrite(IOUtils.toByteArray((InputStream)value));
            } catch (IOException ex) {
                throw new RuntimeException("Unable to convert input stream to byte array.");
            }
        }
        else if (value instanceof byte[]) {
            value = DatatypeConverter.printBase64Binary((byte[])value);
        }
        else if (value != null && !isValidPrimitveType(value.getClass())) {
            throw new IllegalArgumentException("Data type not supported for NoSqlValueMap: " + value.getClass());
        }
        return value;
    }
    
    static boolean isValidPrimitveType(Class clazz) {
        if (clazz.isArray()) {
            return isValidPrimitveType(clazz.getComponentType());
        }
        else {
            return clazz == String.class
                    || clazz == Integer.class
                    || clazz == Long.class
                    || clazz == Double.class
                    || clazz == Boolean.class;
        }
    }
    
    public static Map<String, Object> convertForWriteAll(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            map.put(entry.getKey(), convertForWrite(entry.getValue()));
        }
        return map;
    }

    private static DateFormat getISO8601Format() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
    }

}
