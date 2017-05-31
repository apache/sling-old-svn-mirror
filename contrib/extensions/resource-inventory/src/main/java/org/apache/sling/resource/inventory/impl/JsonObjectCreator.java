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
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.LoggerFactory;

/**
 * Creates a JSONObject from a resource
 *
 */
public abstract class JsonObjectCreator {

    /** Dump given resource in JSON, optionally recursing into its objects */
    public static JsonObjectBuilder create(final Resource resource) {
        final ValueMap valueMap = resource.adaptTo(ValueMap.class);

        @SuppressWarnings("unchecked")
        final Map propertyMap = (valueMap != null)
                ? valueMap
                : resource.adaptTo(Map.class);

        final JsonObjectBuilder obj = Json.createObjectBuilder();

        if (propertyMap == null) {

            // no map available, try string
            final String value = resource.adaptTo(String.class);
            if (value != null) {

                // single value property or just plain String resource or...
                obj.add(resource.getName(), value);

            } else {

                // Try multi-value "property"
                final String[] values = resource.adaptTo(String[].class);
                if (values != null) {
                    JsonArrayBuilder array = Json.createArrayBuilder();
                    for (String v : values) {
                        array.add(v);
                    }
                    obj.add(resource.getName(), array);
                }

            }
            if ( resource.getResourceType() != null ) {
                obj.add("sling:resourceType", resource.getResourceType());
            }
            if ( resource.getResourceSuperType() != null ) {
                obj.add("sling:resourceSuperType", resource.getResourceSuperType());
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
    private static JsonValue getValue(final Object value) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if ( value instanceof InputStream ) {
            // input stream is already handled
            builder.add("key", 0);
        } else if ( value instanceof Calendar ) {
            builder.add("key",format((Calendar)value));
        } else if ( value instanceof Boolean ) {
            builder.add("key", (Boolean) value);
        } else if ( value instanceof Long ) {
            builder.add("key", (Long) value);
        } else if ( value instanceof Integer ) {
            builder.add("key", (Integer) value);
        } else if ( value != null ) {
            builder.add("key", value.toString());
        } else {
            builder.add("key", ""); // assume empty string
        }
        return builder.build().get("key");
    }

    /** Dump a single node */
    private static void createSingleResource(final Resource n, final JsonObjectBuilder parent) {
        parent.add(n.getName(), create(n));
    }

    /**
     * Write a single property
     */
    private static void createProperty(final JsonObjectBuilder obj,
                                 final ValueMap valueMap,
                                 final String key,
                                 final Object value) {
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
                obj.add(key, Json.createArrayBuilder());
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
            try {
                if (values == null) {
                    obj.add(":" + key, getLength(valueMap, -1, key, (InputStream)value));
                } else {
                    final JsonArrayBuilder result = Json.createArrayBuilder();
                    for (int i = 0; i < values.length; i++) {
                        result.add(getLength(valueMap, i, key, (InputStream)values[i]));
                    }
                    obj.add(":" + key, result);
                }
            } catch ( final JsonException ignore ) {
                // we ignore this
                LoggerFactory.getLogger(JsonObjectCreator.class).warn("Unable to create JSON value", ignore);
            }
            return;
        }

        try {
            if (!value.getClass().isArray()) {
                obj.add(key, getValue(value));
            } else {
                final JsonArrayBuilder result = Json.createArrayBuilder();
                for (Object v : values) {
                    result.add(getValue(v));
                }
                obj.add(key, result);
            }
        } catch ( final JsonException ignore ) {
            // we ignore this
            LoggerFactory.getLogger(JsonObjectCreator.class).warn("Unable to create JSON value", ignore);
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
