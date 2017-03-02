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
package org.apache.sling.fscontentparser.impl;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.fscontentparser.ParseException;
import org.apache.sling.fscontentparser.ParserOptions;

/**
 * Helper parsing logic based on parser options.
 */
class ParserHelper {

    static final String JCR_PRIMARYTYPE = "jcr:primaryType";

    static final String ECMA_DATE_FORMAT = "EEE MMM dd yyyy HH:mm:ss 'GMT'Z";
    static final Locale DATE_FORMAT_LOCALE = Locale.US;

    private final ParserOptions options;
    private final DateFormat calendarFormat;

    public ParserHelper(ParserOptions options) {
        this.options = options;
        if (options.isDetectCalendarValues()) {
            this.calendarFormat = new SimpleDateFormat(ECMA_DATE_FORMAT, DATE_FORMAT_LOCALE);
        }
        else {
            this.calendarFormat = null;
        }
    }

    public void ensureDefaultPrimaryType(Map<String, Object> map) {
        String defaultPrimaryType = options.getDefaultPrimaryType();
        if (defaultPrimaryType != null) {
            if (!map.containsKey(JCR_PRIMARYTYPE)) {
                map.put(JCR_PRIMARYTYPE, defaultPrimaryType);
            }
        }
    }

    public Calendar tryParseCalendar(String value) {
        if (options.isDetectCalendarValues() && !StringUtils.isBlank(value)) {
            synchronized (calendarFormat) {
                try {
                    Date date = calendarFormat.parse(value);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    return calendar;
                }
                catch (java.text.ParseException ex) {
                    // ignore
                }
            }
        }
        return null;
    }
    
    public boolean ignoreProperty(String propertyName) {
        return ignoreNames(options.getIgnorePropertyNames(), propertyName);
    }
    
    public boolean ignoreResource(String resourceName) {
        return ignoreNames(options.getIgnoreResourceNames(), resourceName);
    }
    
    private boolean ignoreNames(Set<String> names, String name) {
        return names != null && names.contains(name);
    }

    public String cleanupPropertyName(String propertyName) {
        Set<String> prefixes = options.getRemovePropertyNamePrefixes();
        if (prefixes != null) {
            for (String prefix : prefixes) {
                if (StringUtils.startsWith(propertyName, prefix)) {
                    return StringUtils.substringAfter(propertyName, prefix);
                  }
            }
        }
        return propertyName;
    }
    
    public Object convertSingleTypeArray(Object[] values) {
        if (values.length == 0) {
            return values;
        }
        Class<?> itemType = null;
        for (Object value : values) {
            if (value == null) {
                throw new ParseException("Multivalue array must not contain null values.");
            }
            if (value instanceof Map) {
                throw new ParseException("Multivalue array must not contain maps/objects.");
            }
            if (itemType == null) {
                itemType = value.getClass();
            }
            else if (itemType != value.getClass()) {
                throw new ParseException("Multivalue array must not contain values with different types "
                        + "(" + itemType.getName() + ", " + value.getClass().getName() + ").");
            }
        }
        Object convertedArray = Array.newInstance(itemType, values.length);
        for (int i=0; i<values.length; i++) {
            Array.set(convertedArray, i, values[i]);
        }
        return convertedArray;
    }
    
}
