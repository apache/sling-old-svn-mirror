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
package org.apache.sling.testing.resourceresolver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;

/**
 * ValueMap for mocked resources to mimick JCR-like behavior.
 * <p>Implements the following conversions:</p>
 * <ul>
 * <li>Converts all Date values to Calendar objects internally and vice versa.</li>
 * <li>Converts InputStream to byte array and vice versa.</li>
 * </ul>
 */
public class MockValueMap extends DeepReadModifiableValueMapDecorator implements ModifiableValueMap {
    
    public MockValueMap(Resource resource) {
        this(resource, new HashMap<String, Object>());
    }

    public MockValueMap(Resource resource, Map<String,Object> map) {
        super(resource, new ValueMapDecorator(convertForWriteAll(map)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String name, Class<T> type) {
        
        if (type == Calendar.class) {
            // Support conversion of String to Calendar if value conforms to ISO8601 date format
            Object value = get(name);
            if (value instanceof String) {
                return (T)ISO8601.parse((String)value);
            }
        }
        else if (type == Date.class) {
            // Support conversion from Calendar to Date
            Calendar calendar = get(name, Calendar.class);
            if (calendar != null) {
                return (T)calendar.getTime();
            }
            else {
                return null;
            }
        }
        else if (type == InputStream.class) {
            // Support conversion from byte array to InputStream
            byte[] data = get(name, byte[].class);
            if (data!=null) {
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
        return super.put(key, convertForWrite(value));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void putAll(Map<? extends String, ?> map) {
        super.putAll((Map<? extends String, ?>)convertForWriteAll((Map<String, Object>)map));
    }
    
    private static Object convertForWrite(Object value) {
        if (value instanceof Date) {
            // Store Date values as Calendar values 
            Calendar calendar = Calendar.getInstance();
            calendar.setTime((Date)value);
            value = calendar;
        }
        else if (value instanceof InputStream) {
            // Store InputStream values as byte array
            try {
                value = IOUtils.toByteArray((InputStream)value);
            } catch (IOException ex) {
                throw new RuntimeException("Unable to convert input stream to byte array.");
            }
        }
        return value;
    }
    
    private static Map<String, Object> convertForWriteAll(Map<String, Object> map) {
        Map<String,Object> newMap = new HashMap<String, Object>();
        if (map != null) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                newMap.put(entry.getKey(), convertForWrite(entry.getValue()));
            }
        }
        return newMap;
    }

}
