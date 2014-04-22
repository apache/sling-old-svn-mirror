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
package org.apache.sling.resource.inventory.impl;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

/**
 * Creates a JSONObject from a resource
 *
 */
public abstract class JsonObjectCreator {

    /** Dump given resource in JSON, optionally recursing into its objects */
    public static JSONObject create(final Resource resource)
    throws JSONException {
        final ValueMap valueMap = resource.adaptTo(ValueMap.class);

        @SuppressWarnings("unchecked")
        final Map propertyMap = (valueMap != null)
                ? valueMap
                : resource.adaptTo(Map.class);

        final JSONObject obj = new JSONObject();

        if (propertyMap == null) {

            // no map available, try string
            final String value = resource.adaptTo(String.class);
            if (value != null) {

                // single value property or just plain String resource or...
                obj.put(resource.getName(), value);

            } else {

                // Try multi-value "property"
                final String[] values = resource.adaptTo(String[].class);
                if (values != null) {
                    obj.put(resource.getName(), new JSONArray(Arrays.asList(values)));
                }

            }
            if ( resource.getResourceType() != null ) {
                obj.put("sling:resourceType", resource.getResourceType());
            }
            if ( resource.getResourceSuperType() != null ) {
                obj.put("sling:resourceSuperType", resource.getResourceSuperType());
            }

        } else {

            @SuppressWarnings("unchecked")
            final Iterator<Map.Entry> props = propertyMap.entrySet().iterator();

            // the node's actual properties
            while (props.hasNext()) {
                @SuppressWarnings("unchecked")
                final Map.Entry prop = props.next();

                if ( prop.getValue() != null ) {
                    createProperty(obj, valueMap, prop.getKey().toString(),
                        prop.getValue());
                }
            }
        }

        // the child nodes
        final Iterator<Resource> children = resource.listChildren();
        while (children.hasNext()) {
            final Resource n = children.next();
            createSingleResource(n, obj);
        }

        return obj;
    }

    /** Used to format date values */
    private static final String ECMA_DATE_FORMAT = "EEE MMM dd yyyy HH:mm:ss 'GMT'Z";

    /** Used to format date values */
    private static final Locale DATE_FORMAT_LOCALE = Locale.US;

    private static final DateFormat CALENDAR_FORMAT = new SimpleDateFormat(ECMA_DATE_FORMAT, DATE_FORMAT_LOCALE);

    private static synchronized String format(final Calendar date) {
        return CALENDAR_FORMAT.format(date.getTime());
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
    private static void createSingleResource(final Resource n, final JSONObject parent)
            throws JSONException {
        parent.put(n.getName(), create(n));
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
            if (value instanceof long[]) {
                values = ArrayUtils.toObject((long[])value);
            } else if (value instanceof int[]) {
                values = ArrayUtils.toObject((int[])value);
            } else if (value instanceof double[]) {
                values = ArrayUtils.toObject((double[])value);
            } else if (value instanceof byte[]) {
                values = ArrayUtils.toObject((byte[])value);
            } else if (value instanceof float[]) {
                values = ArrayUtils.toObject((float[])value);
            } else if (value instanceof short[]) {
                values = ArrayUtils.toObject((short[])value);
            } else if (value instanceof long[]) {
                values = ArrayUtils.toObject((long[])value);
            } else if (value instanceof boolean[]) {
                values = ArrayUtils.toObject((boolean[])value);
            } else if (value instanceof char[]) {
                values = ArrayUtils.toObject((char[])value);
            } else {
                values = (Object[]) value;
            }
            // write out empty array
            if ( values.length == 0 ) {
                obj.put(key, new JSONArray());
                return;
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
