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
package org.apache.sling.commons.json.jcr;

import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

/**
 * Dumps JCR Items as JSON data. The dump methods are threadsafe.
 */
public class JsonItemWriter {

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
    public JsonItemWriter(Set<String> propertyNamesToIgnore) {
        this.propertyNamesToIgnore = propertyNamesToIgnore;
    }

    /**
     * Dump all Nodes of given NodeIterator in JSON
     * 
     * @throws JSONException
     */
    public void dump(NodeIterator it, Writer out) throws RepositoryException,
            JSONException {
        final JSONWriter w = new JSONWriter(out);
        w.array();
        while (it.hasNext()) {
            dump(it.nextNode(), w, 1, 1);
        }
        w.endArray();
    }

    /** Dump given node in JSON, optionally recursing into its child nodes */
    public void dump(Node node, Writer w, int maxRecursionLevels)
            throws RepositoryException, JSONException {
        dump(node, w, maxRecursionLevels, false);
    }

    /**
     * Dump given node in JSON, optionally recursing into its child nodes
     * @param tidy if <code>true</code> the json dump is nicely formatted
     */
    public void dump(Node node, Writer w, int maxRecursionLevels, boolean tidy)
            throws RepositoryException, JSONException {
        JSONWriter jw = new JSONWriter(w);
        jw.setTidy(tidy);
        dump(node, jw, 0, maxRecursionLevels);
    }

    /** Dump given property in JSON */
    public void dump(Property p, Writer w) throws JSONException,
            ValueFormatException, RepositoryException {
        final JSONWriter jw = new JSONWriter(w);
        jw.object();
        writeProperty(jw, p);
        jw.endObject();
    }

    /** Dump given node in JSON, optionally recursing into its child nodes */
    protected void dump(Node node, JSONWriter w, int currentRecursionLevel,
            int maxRecursionLevels) throws RepositoryException, JSONException {

        w.object();
        PropertyIterator props = node.getProperties();

        // the node's actual properties
        while (props.hasNext()) {
            Property prop = props.nextProperty();

            if (propertyNamesToIgnore != null
                && propertyNamesToIgnore.contains(prop.getName())) {
                continue;
            }

            writeProperty(w, prop);
        }

        // the child nodes
        if (recursionLevelActive(currentRecursionLevel, maxRecursionLevels)) {
            final NodeIterator children = node.getNodes();
            while (children.hasNext()) {
                final Node n = children.nextNode();
                dumpSingleNode(n, w, currentRecursionLevel, maxRecursionLevels);
            }
        }

        w.endObject();
    }

    /** Dump a single node */
    protected void dumpSingleNode(Node n, JSONWriter w,
            int currentRecursionLevel, int maxRecursionLevels)
            throws RepositoryException, JSONException {
        if (recursionLevelActive(currentRecursionLevel, maxRecursionLevels)) {
            w.key(n.getName());
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
    protected void writeProperty(JSONWriter w, Property p)
            throws ValueFormatException, RepositoryException, JSONException {
        // special handling for binaries: we dump the length and not the length
        if (p.getType() == PropertyType.BINARY) {
            // TODO for now we mark binary properties with an initial colon in
            // their name
            // (colon is not allowed as a JCR property name)
            // in the name, and the value should be the size of the binary data
            w.key(":" + p.getName());
            if (!p.isMultiple()) {
                w.value(p.getLength());
            } else {
                final long[] sizes = p.getLengths();
                w.array();
                for (int i = 0; i < sizes.length; i++) {
                    w.value(sizes[i]);
                }
                w.endArray();
            }
            return;
        }
        w.key(p.getName());

        if (!p.isMultiple()) {
            dumpValue(w, p.getValue());
        } else {
            w.array();
            for (Value v : p.getValues()) {
                dumpValue(w, v);
            }
            w.endArray();
        }
    }

    /**
     * Writes the given value to the JSON writer. currently the following
     * conversions are done: <table>
     * <tr>
     * <th>JSR Property Type</th>
     * <th>JSON Value Type</th>
     * </tr>
     * <tr>
     * <td>BINARY</td>
     * <td>always 0 as long</td>
     * </tr>
     * <tr>
     * <td>DATE</td>
     * <td>converted date string as defined by ECMA</td>
     * </tr>
     * <tr>
     * <td>BOOLEAN</td>
     * <td>boolean</td>
     * </tr>
     * <tr>
     * <td>LONG</td>
     * <td>long</td>
     * </tr>
     * <tr>
     * <td>DOUBLE</td>
     * <td>double</td>
     * </tr>
     * <tr>
     * <td><i>all other</li>
     * </td>
     * <td>string</td>
     * </tr>
     * </table> <sup>1</sup> Currently not implemented and uses 0 as default.
     * 
     * @param w json writer
     * @param v value to dump
     */
    protected void dumpValue(JSONWriter w, Value v)
            throws ValueFormatException, IllegalStateException,
            RepositoryException, JSONException {

        switch (v.getType()) {
            case PropertyType.BINARY:
                w.value(0);
                break;

            case PropertyType.DATE:
                w.value(format(v.getDate()));
                break;

            case PropertyType.BOOLEAN:
                w.value(v.getBoolean());
                break;

            case PropertyType.LONG:
                w.value(v.getLong());
                break;

            case PropertyType.DOUBLE:
                w.value(v.getDouble());

                break;
            default:
                w.value(v.getString());
        }
    }

    public static synchronized String format(Calendar date) {
        if (calendarFormat == null) {
            calendarFormat = new SimpleDateFormat(ECMA_DATE_FORMAT, DATE_FORMAT_LOCALE);
        }
        return calendarFormat.format(date.getTime());
    }
}
