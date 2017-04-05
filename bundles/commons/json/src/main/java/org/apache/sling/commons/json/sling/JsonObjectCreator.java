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
package org.apache.sling.commons.json.sling;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

/**
 * Creates a JSONObject from a resource
 *
 */
@Deprecated
public abstract class JsonObjectCreator {

    /**
     * Dump given resource in JSON, optionally recursing into its objects
     */
    public static JSONObject create(final Resource resource, final int maxRecursionLevels)
            throws JSONException {
        return create(resource, 0, maxRecursionLevels);
    }


    /** Dump given resource in JSON, optionally recursing into its objects */
    private static JSONObject create(final Resource resource,
            final int currentRecursionLevel,
            final int maxRecursionLevels)
    throws JSONException {
        final ValueMap valueMap = resource.adaptTo(ValueMap.class);

        final Map propertyMap = (valueMap != null)
                ? valueMap
                : resource.adaptTo(Map.class);

        final JSONObject obj = new JSONObject();

        if (propertyMap == null) {

            // no map available, try string
            final String value = resource.adaptTo(String.class);
            if (value != null) {

                // single value property or just plain String resource or...
                obj.put(ResourceUtil.getName(resource), value);

            } else {

                // Try multi-value "property"
                final String[] values = resource.adaptTo(String[].class);
                if (values != null) {
                    obj.put(ResourceUtil.getName(resource), new JSONArray(Arrays.asList(values)));
                }

            }

        } else {

            @SuppressWarnings("unchecked")
            final Iterator<Map.Entry> props = propertyMap.entrySet().iterator();

            // the node's actual properties
            while (props.hasNext()) {
                final Map.Entry prop = props.next();

                if ( prop.getValue() != null ) {
                    createProperty(obj, valueMap, prop.getKey().toString(),
                        prop.getValue());
                }
            }
        }

        // the child nodes
        if (recursionLevelActive(currentRecursionLevel, maxRecursionLevels)) {
            final Iterator<Resource> children = ResourceUtil.listChildren(resource);
            while (children.hasNext()) {
                final Resource n = children.next();
                createSingleResource(n, obj, currentRecursionLevel,
                    maxRecursionLevels);
            }
        }

        return obj;
    }

    /** Used to format date values */
    private static final String ECMA_DATE_FORMAT = "EEE MMM dd yyyy HH:mm:ss 'GMT'Z";

    /** The Locale used to format date values */
    static final Locale DATE_FORMAT_LOCALE = Locale.US;


    private static String format(final Calendar date) {
        DateFormat formatter = new SimpleDateFormat(ECMA_DATE_FORMAT, DATE_FORMAT_LOCALE);
        formatter.setTimeZone(date.getTimeZone());
        return formatter.format(date.getTime());
    }

    /** Dump only a value in the correct format */
    private static Object getValue(final Object value) {
        if ( value instanceof InputStream ) {
            // input stream is already handled
            return 0;
        } else if ( value instanceof Calendar ) {
            return format((Calendar)value);
        } else if ( value instanceof Boolean ) {
            return value;
        } else if ( value instanceof Long ) {
            return value;
        } else if ( value instanceof Integer ) {
            return value;
        } else if ( value instanceof Double ) {
            return value;
        } else if ( value != null ) {
            return value.toString();
        } else {
            return ""; // assume empty string
        }
    }

    /** Dump a single node */
    private static void createSingleResource(final Resource n, final JSONObject parent,
            final int currentRecursionLevel, final int maxRecursionLevels)
            throws JSONException {
        if (recursionLevelActive(currentRecursionLevel, maxRecursionLevels)) {
            parent.put(ResourceUtil.getName(n), create(n, currentRecursionLevel + 1, maxRecursionLevels));
        }
    }

    /** true if the current recursion level is active */
    private static boolean recursionLevelActive(final int currentRecursionLevel,
            final int maxRecursionLevels) {
        return maxRecursionLevels < 0
            || currentRecursionLevel < maxRecursionLevels;
    }

    /**
     * Write a single property
     */
    private static void createProperty(final JSONObject obj,
                                 final ValueMap valueMap,
                                 final String key,
                                 final Object value)
    throws JSONException {
        Object[] values = null;
        if (value.getClass().isArray()) {
            final int length = Array.getLength(value);
            // write out empty array
            if ( length == 0 ) {
                obj.put(key, new JSONArray());
                return;
            }
            values = new Object[Array.getLength(value)];
            for(int i=0; i<length; i++) {
                values[i] = Array.get(value, i);
            }
        }

        // special handling for binaries: we dump the length and not the data!
        if (value instanceof InputStream
            || (values != null && values[0] instanceof InputStream)) {
            // TODO for now we mark binary properties with an initial colon in
            // their name
            // (colon is not allowed as a JCR property name)
            // in the name, and the value should be the size of the binary data
            if (values == null) {
                obj.put(":" + key, getLength(valueMap, -1, key, (InputStream)value));
            } else {
                final JSONArray result = new JSONArray();
                for (int i = 0; i < values.length; i++) {
                    result.put(getLength(valueMap, i, key, (InputStream)values[i]));
                }
                obj.put(":" + key, result);
            }
            return;
        }

        if (!value.getClass().isArray()) {
            obj.put(key, getValue(value));
        } else {
            final JSONArray result = new JSONArray();
            for (Object v : values) {
                result.put(getValue(v));
            }
            obj.put(key, result);
        }
    }

    private static long getLength(final ValueMap    valueMap,
                           final int         index,
                           final String      key,
                           final InputStream stream) {
        try {
            stream.close();
        } catch (IOException ignore) {}
        long length = -1;
        if ( valueMap != null ) {
            if ( index == -1 ) {
                length = valueMap.get(key, length);
            } else {
                Long[] lengths = valueMap.get(key, Long[].class);
                if ( lengths != null && lengths.length > index ) {
                    length = lengths[index];
                }
            }
        }
        return length;
    }
}
