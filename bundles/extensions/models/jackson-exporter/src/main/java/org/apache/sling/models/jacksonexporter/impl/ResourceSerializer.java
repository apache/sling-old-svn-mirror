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
package org.apache.sling.models.jacksonexporter.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;

public class ResourceSerializer extends JsonSerializer<Resource> implements ResolvableSerializer {

    private final int maxRecursionLevels;
    private JsonSerializer<Object> calendarSerializer;

    public ResourceSerializer(int maxRecursionLevels) {
        this.maxRecursionLevels = maxRecursionLevels;
    }

    @Override
    public void serialize(final Resource value, final JsonGenerator jgen, final SerializerProvider provider) throws IOException {
        create(value, jgen, 0, provider);
    }

    /** Dump given resource in JSON, optionally recursing into its objects */
    private void create(final Resource resource, final JsonGenerator jgen, final int currentRecursionLevel,
                                     final SerializerProvider provider) throws IOException {
        jgen.writeStartObject();

        final ValueMap valueMap = resource.adaptTo(ValueMap.class);

        final Map propertyMap = (valueMap != null) ? valueMap : resource.adaptTo(Map.class);

        if (propertyMap == null) {

            // no map available, try string
            final String value = resource.adaptTo(String.class);
            if (value != null) {

                // single value property or just plain String resource or...
                jgen.writeStringField(resource.getName(), value);

            } else {

                // Try multi-value "property"
                final String[] values = resource.adaptTo(String[].class);
                if (values != null) {
                    jgen.writeArrayFieldStart(resource.getName());
                    for (final String s : values) {
                        jgen.writeString(s);
                    }
                    jgen.writeEndArray();
                }

            }

        } else {

            @SuppressWarnings("unchecked")
            final Iterator<Map.Entry> props = propertyMap.entrySet().iterator();

            // the node's actual properties
            while (props.hasNext()) {
                final Map.Entry prop = props.next();

                if (prop.getValue() != null) {
                    createProperty(jgen, valueMap, prop.getKey().toString(), prop.getValue(), provider);
                }
            }
        }

        // the child nodes
        if (recursionLevelActive(currentRecursionLevel)) {
            for (final Resource n : resource.getChildren()) {
                jgen.writeObjectFieldStart(n.getName());
                create(n, jgen, currentRecursionLevel + 1, provider);
            }
        }

        jgen.writeEndObject();
    }

    /**
     * Write a single property
     */
    private void createProperty(final JsonGenerator jgen, final ValueMap valueMap, final String key, final Object value,
                                final SerializerProvider provider)
            throws IOException {
        Object[] values = null;
        if (value.getClass().isArray()) {
            final int length = Array.getLength(value);
            // write out empty array
            if ( length == 0 ) {
                jgen.writeArrayFieldStart(key);
                jgen.writeEndArray();
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
                jgen.writeNumberField(":" + key, getLength(valueMap, -1, key, (InputStream)value));
            } else {
                jgen.writeArrayFieldStart(":" + key);
                for (int i = 0; i < values.length; i++) {
                    jgen.writeNumber(getLength(valueMap, i, key, (InputStream)values[i]));
                }
                jgen.writeEndArray();
            }
            return;
        }

        if (!value.getClass().isArray()) {
            jgen.writeFieldName(key);
            writeValue(jgen, value, provider);
        } else {
            jgen.writeArrayFieldStart(key);
            for (Object v : values) {
                writeValue(jgen, v, provider);
            }
            jgen.writeEndArray();
        }
    }

    /** true if the current recursion level is active */
    private boolean recursionLevelActive(final int currentRecursionLevel) {
        return maxRecursionLevels < 0 || currentRecursionLevel < maxRecursionLevels;
    }

    private long getLength(final ValueMap valueMap, final int index, final String key, final InputStream stream) {
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

    /** Dump only a value in the correct format */
    private void writeValue(final JsonGenerator jgen, final Object value, final SerializerProvider provider) throws IOException {
        if (value instanceof InputStream) {
            // input stream is already handled
            jgen.writeNumber(0);
        } else if (value instanceof Calendar) {
            calendarSerializer.serialize(value, jgen, provider);
        } else if (value instanceof Boolean) {
            jgen.writeBoolean(((Boolean)value).booleanValue());
        } else if (value instanceof Long) {
            jgen.writeNumber(((Long)value).longValue());
        } else if (value instanceof Integer) {
            jgen.writeNumber(((Integer)value).intValue());
        } else if (value instanceof Double) {
            jgen.writeNumber(((Double)value).doubleValue());
        } else if (value != null) {
            jgen.writeString(value.toString());
        } else {
            jgen.writeString(""); // assume empty string
        }
    }

    @Override
    public void resolve(SerializerProvider provider) throws JsonMappingException {
        this.calendarSerializer = provider.findValueSerializer(Calendar.class, null);
    }
}
