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
package org.apache.sling.servlets.get.impl.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;

public abstract class JsonObjectCreator {

    /**
     * Dump given resource in JSON, optionally recursing into its objects
     */
    public static JsonObjectBuilder create(final Resource resource, final int maxRecursionLevels) {
        return create(resource, 0, maxRecursionLevels);
    }


    /** Dump given resource in JSON, optionally recursing into its objects */
    private static JsonObjectBuilder create(final Resource resource,
            final int currentRecursionLevel,
            final int maxRecursionLevels) {
        final ValueMap valueMap = resource.adaptTo(ValueMap.class);

        final Map propertyMap = (valueMap != null)
                ? valueMap
                : resource.adaptTo(Map.class);

        final JsonObjectBuilder obj = Json.createObjectBuilder();

        if (propertyMap == null) {

            // no map available, try string
            final String value = resource.adaptTo(String.class);
            if (value != null) {

                // single value property or just plain String resource or...
                obj.add(ResourceUtil.getName(resource), value.toString());

            } else {

                // Try multi-value "property"
                final String[] values = resource.adaptTo(String[].class);
                if (values != null) {
                    JsonArrayBuilder builder = Json.createArrayBuilder();
                    for (String v : values)
                    {
                        builder.add(v);
                    }
                    obj.add(ResourceUtil.getName(resource), builder);
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


    public static String format(final Calendar date) {
        DateFormat formatter = new SimpleDateFormat(ECMA_DATE_FORMAT, DATE_FORMAT_LOCALE);
        formatter.setTimeZone(date.getTimeZone());
        return formatter.format(date.getTime());
    }

    /** Dump only a value in the correct format */
    public static JsonValue getValue(final Object value) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if ( value instanceof InputStream ) {
            // input stream is already handled
            builder.add("entry", 0);
        } else if ( value instanceof Calendar ) {
            builder.add("entry", format((Calendar)value));
        } else if ( value instanceof Boolean ) {
            builder.add("entry", (Boolean) value);
        } else if ( value instanceof Long ) {
            builder.add("entry", (Long) value);
        } else if ( value instanceof Integer ) {
            builder.add("entry", (Integer) value);
        } else if ( value instanceof Double ) {
            builder.add("entry", (Double) value);
        } else if ( value != null ) {
            builder.add("entry", value.toString());
        } else {
            builder.add("entry", "");
        }
        return builder.build().get("entry");
    }

    /** Dump a single node */
    private static void createSingleResource(final Resource n, final JsonObjectBuilder parent,
            final int currentRecursionLevel, final int maxRecursionLevels) {
        if (recursionLevelActive(currentRecursionLevel, maxRecursionLevels)) {
            parent.add(ResourceUtil.getName(n), create(n, currentRecursionLevel + 1, maxRecursionLevels));
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
    public static void createProperty(final JsonObjectBuilder obj,
                                 final ValueMap valueMap,
                                 final String key,
                                 final Object value) {
        Object[] values = null;
        if (value.getClass().isArray()) {
            final int length = Array.getLength(value);
            // write out empty array
            if ( length == 0 ) {
                obj.add(key, Json.createArrayBuilder());
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
                obj.add(":" + key, getLength(valueMap, -1, key, (InputStream)value));
            } else {
                final JsonArrayBuilder result = Json.createArrayBuilder();
                for (int i = 0; i < values.length; i++) {
                    result.add(getLength(valueMap, i, key, (InputStream)values[i]));
                }
                obj.add(":" + key, result);
            }
            return;
        }

        if (!value.getClass().isArray()) {
            obj.add(key, getValue(value));
        } else {
            final JsonArrayBuilder result = Json.createArrayBuilder();
            for (Object v : values) {
                result.add(getValue(v));
            }
            obj.add(key, result);
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
