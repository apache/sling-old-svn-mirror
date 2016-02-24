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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.nosql.generic.adapter.NoSqlAdapter;
import org.apache.sling.nosql.generic.adapter.NoSqlData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special adapter wrapper that converts all Calendar and byte[] values in ValueMap to String values
 * when passing to the underlying NoSql adapter and back to typed values when reading from it.
 * This is required because too many implementations access ValueMap without type specifier so
 * we cannot only rely on the type conversion in the typed get methods of a ValueMap.
 */
class ValueMapConvertingNoSqlAdapter implements NoSqlAdapter {
    
    private static final String PREFIX_CALENDAR = "{{calendar}}";
    private static final String PREFIX_BYTE_ARRAY = "{{bytes}}";

    private final NoSqlAdapter delegate;
    
    private static final Logger log = LoggerFactory.getLogger(ValueMapConvertingNoSqlAdapter.class);

    public ValueMapConvertingNoSqlAdapter(NoSqlAdapter delegate) {
        this.delegate = delegate;
    }

    public boolean validPath(String path) {
        return delegate.validPath(path);
    }

    public NoSqlData get(String path) {
        return deserializeUnsupportedTypes(delegate.get(path));
    }

    public Iterator<NoSqlData> getChildren(String parentPath) {
        return deserializeUnsupportedTypes(delegate.getChildren(parentPath));
    }

    public boolean store(NoSqlData data) {
        return delegate.store(serializeUnsupportedTypes(data));
    }

    public boolean deleteRecursive(String path) {
        return delegate.deleteRecursive(path);
    }

    public Iterator<NoSqlData> query(String query, String language) {
        return deserializeUnsupportedTypes(delegate.query(query, language));
    }
    
    private Iterator<NoSqlData> deserializeUnsupportedTypes(final Iterator<NoSqlData> source) {
        if (source == null) {
            return null;
        }
        return new Iterator<NoSqlData>() {
            @Override
            public boolean hasNext() {
                return source.hasNext();
            }
            @Override
            public NoSqlData next() {
                return deserializeUnsupportedTypes(source.next());
            }
            @Override
            public void remove() {
                source.remove();
            }
        };
    }
    
    private NoSqlData serializeUnsupportedTypes(NoSqlData data) {
        if (data == null) {
            return null;
        }
        
        Map<String,Object> serializedMap = new HashMap<String, Object>();
        
        for (Map.Entry<String, Object> entry : data.getProperties().entrySet()) {
            Object serializedValue = entry.getValue();
            
            // Calendar.class
            if (entry.getValue() instanceof Calendar) {
                serializedValue = PREFIX_CALENDAR + getISO8601Format().format(((Calendar)entry.getValue()).getTime());
            }
            
            // byte[].class
            else if (entry.getValue() instanceof byte[]) {
                serializedValue = PREFIX_BYTE_ARRAY + DatatypeConverter.printBase64Binary((byte[])entry.getValue());
            }
            
            serializedMap.put(entry.getKey(), serializedValue);
        }
        
        return new NoSqlData(data.getPath(), serializedMap);
    }
    
    private NoSqlData deserializeUnsupportedTypes(NoSqlData data) {
        if (data == null) {
            return null;
        }
        
        Map<String,Object> deserializedMap = new HashMap<String, Object>();

        for (Map.Entry<String, Object> entry : data.getProperties().entrySet()) {
            Object deserializedValue = entry.getValue();
            if (entry.getValue() instanceof String) {
                String value = (String)entry.getValue();
                
                // Calendar.class
                if (value.indexOf(PREFIX_CALENDAR) == 0) {
                    String calendarValue = value.substring(PREFIX_CALENDAR.length());
                    try {
                        Date date = getISO8601Format().parse((String)calendarValue);
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(date);
                        deserializedValue = calendar;
                    }
                    catch (ParseException ex) {
                        log.warn("Unable to parse serialized calendar value: " + entry.getValue(), ex);
                    }
                }
                
                // byte[].class
                else if (value.indexOf(PREFIX_BYTE_ARRAY) == 0) {
                    String byteArrayValue = value.substring(PREFIX_BYTE_ARRAY.length());
                    deserializedValue = DatatypeConverter.parseBase64Binary(byteArrayValue);
                }
                
            }
            deserializedMap.put(entry.getKey(), deserializedValue);
        }
        
        return new NoSqlData(data.getPath(), deserializedMap);
    }
    
    @Override
    public void checkConnection() throws LoginException {
        delegate.checkConnection();
    }

    @Override
    public void createIndexDefinitions() {
        delegate.createIndexDefinitions();
    }

    private static DateFormat getISO8601Format() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
    }

}
