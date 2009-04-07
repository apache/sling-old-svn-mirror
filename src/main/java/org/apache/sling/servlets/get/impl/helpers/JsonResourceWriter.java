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
package org.apache.sling.servlets.get.impl.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

/**
 * Dumps JCR Items as JSON data. The dump methods are threadsafe.
 */
public class JsonResourceWriter {

    private static DateFormat calendarFormat;

    private final Set<String> propertyNamesToIgnore;

    /** Used to format date values */
    public static final String ECMA_DATE_FORMAT = "EEE MMM dd yyyy HH:mm:ss 'GMT'Z";

    /** Used to format date values */
    public static final Locale DATE_FORMAT_LOCALE = Locale.US;


    /**
     * Create a JsonItemWriter
     *
     * @param propertyNamesToIgnore if not null, a property having a name from
     *            this set of values is ignored. TODO we should use a filtering
     *            interface to make the selection of which Nodes and Properties
     *            to dump more flexible.
     */
    public JsonResourceWriter(Set<String> propertyNamesToIgnore) {
        this.propertyNamesToIgnore = propertyNamesToIgnore;
    }

    /** Dump given resource in JSON, optionally recursing into its object */
    public void dump(Resource resource, Writer w, int maxRecursionLevels)
            throws JSONException {
        dump(resource, w, maxRecursionLevels, false);
    }

    /**
     * Dump given resource in JSON, optionally recursing into its objects
     * @param tidy if <code>true</code> the json dump is nicely formatted
     */
    public void dump(Resource resource, Writer w, int maxRecursionLevels, boolean tidy)
            throws JSONException {
        JSONWriter jw = new JSONWriter(w);
        jw.setTidy(tidy);
        dump(resource, jw, 0, maxRecursionLevels);
    }

    /** Dump given resource in JSON, optionally recursing into its objects */
    protected void dump(Resource resource, JSONWriter w,
            int currentRecursionLevel, int maxRecursionLevels)
            throws JSONException {

        final ValueMap valueMap = resource.adaptTo(ValueMap.class);

        @SuppressWarnings("unchecked")
        final Map propertyMap = (valueMap != null)
                ? valueMap
                : resource.adaptTo(Map.class);

        w.object();

        if (propertyMap == null) {

            // no map available, try string
            final String value = resource.adaptTo(String.class);
            if (value != null) {
                w.key(ResourceUtil.getName(resource));
                w.value(value);
            }

        } else {

            @SuppressWarnings("unchecked")
            final Iterator<Map.Entry> props = propertyMap.entrySet().iterator();

            // the node's actual properties
            while (props.hasNext()) {
                @SuppressWarnings("unchecked")
                final Map.Entry prop = props.next();

                if (propertyNamesToIgnore != null
                    && propertyNamesToIgnore.contains(prop.getKey())) {
                    continue;
                }

                writeProperty(w, valueMap, prop.getKey().toString(),
                    prop.getValue());
            }
        }

        // the child nodes
        if (recursionLevelActive(currentRecursionLevel, maxRecursionLevels)) {
            final Iterator<Resource> children = ResourceUtil.listChildren(resource);
            while (children.hasNext()) {
                final Resource n = children.next();
                dumpSingleResource(n, w, currentRecursionLevel,
                    maxRecursionLevels);
            }
        }

        w.endObject();
    }
    
    /** Dump a single node */
    protected void dumpSingleResource(Resource n, JSONWriter w,
            int currentRecursionLevel, int maxRecursionLevels)
            throws JSONException {
        if (recursionLevelActive(currentRecursionLevel, maxRecursionLevels)) {
            w.key(ResourceUtil.getName(n));
            dump(n, w, currentRecursionLevel + 1, maxRecursionLevels);
        }
    }

    /** true if the current recursion level is active */
    protected boolean recursionLevelActive(int currentRecursionLevel,
            int maxRecursionLevels) {
        return maxRecursionLevels < 0
            || currentRecursionLevel < maxRecursionLevels;
    }

    /**
     * Write a single property
     */
    protected void writeProperty(JSONWriter w,
                                 ValueMap valueMap,
                                 String key,
                                 Object value)
    throws JSONException {
        Object[] values = null;
        if (value.getClass().isArray()) {
            values = (Object[])value;
            // write out empty array
            if ( values.length == 0 ) {
                w.key(key);
                w.array();
                w.endArray();
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
            w.key(":" + key);
            if (values == null) {
                writeLength(w, valueMap, -1, key, (InputStream)value);
            } else {
                w.array();
                for (int i = 0; i < values.length; i++) {
                    writeLength(w, valueMap, i, key, (InputStream)values[i]);
                }
                w.endArray();
            }
            return;
        }
        w.key(key);

        if (!value.getClass().isArray()) {
            dumpValue(w, value);
        } else {
            w.array();
            for (Object v : values) {
                dumpValue(w, v);
            }
            w.endArray();
        }
    }

    private void writeLength(JSONWriter w,
                             ValueMap   valueMap,
                             int        index,
                             String     key,
                             InputStream stream)
    throws JSONException {
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
        w.value(length);
    }

    protected void dumpValue(JSONWriter w, Object value)
    throws JSONException {
        if ( value instanceof InputStream ) {
            // input stream is already handled
            w.value(0);
        } else if ( value instanceof Calendar ) {
            w.value(format((Calendar)value));
        } else if ( value instanceof Boolean ) {
            w.value(((Boolean)value).booleanValue());
        } else if ( value instanceof Long ) {
            w.value(((Long)value).longValue());
        } else if ( value instanceof Integer ) {
            w.value(((Integer)value).longValue());
        } else if ( value instanceof Double ) {
            w.value(((Double)value).doubleValue());
        } else {
            w.value(value.toString());
        }
    }

    public static synchronized String format(Calendar date) {
        if (calendarFormat == null) {
            calendarFormat = new SimpleDateFormat(ECMA_DATE_FORMAT, DATE_FORMAT_LOCALE);
        }
        return calendarFormat.format(date.getTime());
    }
}
