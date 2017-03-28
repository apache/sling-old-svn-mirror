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
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

public class JsonRenderer
{
    /** Rendering options */
    static public class Options {
        int indent;
        private boolean indentIsPositive;
        int initialIndent;
        boolean arraysForChildren;

        public static final String DEFAULT_CHILDREN_KEY = "__children__";
        public static final String DEFAULT_CHILD_NAME_KEY = "__name__";

        String childrenKey = DEFAULT_CHILDREN_KEY;
        String childNameKey = DEFAULT_CHILD_NAME_KEY;

        /** Clients use JSONRenderer.options() to create objects */
        private Options() {
        }

        Options(Options opt) {
            this.indent = opt.indent;
            this.indentIsPositive = opt.indentIsPositive;
            this.initialIndent = opt.initialIndent;
            this.arraysForChildren = opt.arraysForChildren;
        }

        public Options withIndent(int n) {
            indent = n;
            indentIsPositive = indent > 0;
            return this;
        }

        public Options withInitialIndent(int n) {
            initialIndent = n;
            return this;
        }

        public Options withArraysForChildren(boolean b) {
            arraysForChildren = b;
            return this;
        }

        public Options withChildNameKey(String key) {
            childNameKey = key;
            return this;
        }

        public Options withChildrenKey(String key) {
            childrenKey = key;
            return this;
        }

        boolean hasIndent() {
            return indentIsPositive;
        }
    }

    /** Return an Options object with default values */
    public Options options() {
        return new Options();
    }

    /** Write N spaces to sb for indentation */
    private void indent(StringBuilder sb, int howMuch) {
        for (int i=0; i < howMuch; i++) {
            sb.append(' ');
        }
    }

    /** Render the supplied JSONObject to a String, in
     *  the simplest possible way.
     */
    public String toString(JsonObject jo) {
        try {
            StringWriter writer = new StringWriter();
            Json.createGenerator(writer).write(jo).close();
            return writer.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** Make a JSON text of the supplied JSONArray. For compactness, no
     *  unnecessary whitespace is added. If it is not possible to produce a
     *  syntactically correct JSON text then null will be returned instead. This
     *  could occur if the array contains an invalid number.
     *  <p>Warning: This method assumes that the data structure is acyclical.
     *
     *  @return a printable, displayable, transmittable
     *  representation of the array.
     */
    public String toString(JsonArray ja) {
        try {
            return '[' + join(ja,",") + ']';
        } catch (Exception e) {
            return null;
        }
    }

    /** Quote the supplied string for JSON */
    public String quote(String string) {
        if (string == null || string.length() == 0) {
            return "\"\"";
        }

        char          b;
        char          c = 0;
        int           i;
        int           len = string.length();
        StringBuilder sb = new StringBuilder(len + 2);
        String        t;

        sb.append('"');
        for (i = 0; i < len; i += 1) {
            b = c;
            c = string.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    sb.append('\\');
                    sb.append(c);
                    break;
                case '/':
                    if (b == '<') {
                        sb.append('\\');
                    }
                    sb.append(c);
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    if (c < ' ' || (c >= '\u0080' && c < '\u00a0') ||
                            (c >= '\u2000' && c < '\u2100')) {
                        t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /** Quote the supplied string for JSON, to the supplied Writer */
    public void quote(Writer w, String string) throws IOException {
        w.write(quote(string));
    }

    /**
     * Make a JSON text of an Object value. 
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     * @param value The value to be serialized.
     * @return a printable, displayable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws JSONException If the value is or contains an invalid number.
     */
    public String valueToString(Object value) {
        // TODO call the other valueToString instead
        if (value == null || value.equals(null)) {
            return "null";
        }
        if (value instanceof JsonString) {
            quote(((JsonString)value).getString());
        }
        if (value instanceof Number) {
            return numberToString((Number) value);
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof JsonObject || value instanceof JsonArray) {
            StringWriter writer = new StringWriter();
            Json.createGenerator(writer).write((JsonValue) value).close();
            return writer.toString();
        }
        return quote(value.toString());
    }

    /** Make a JSON String of an Object value, with rendering options
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     * @param value The value to be serialized.
     * @return a printable, displayable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws JSONException If the object contains an invalid number.
     */
    public String valueToString(Object value, Options opt) {
        if (value == null || value.equals(null)) {
            return "null";
        }
        if (value instanceof JsonString) {
            return quote(((JsonString)value).getString());
        }
        if (value instanceof Number) {
            return numberToString((Number) value);
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof JsonObject) {
            return prettyPrint((JsonObject)value, opt);
        }
        if (value instanceof JsonArray) {
            return prettyPrint((JsonArray)value, opt);
        }
        return quote(value.toString());

    }

    /**
     * Produce a string from a Number.
     * @param  n A Number
     * @return A String.
     * @throws JSONException If n is a non-finite number.
     */
    public String numberToString(Number n) {
        if (n == null) {
            throw new NullPointerException("Null pointer");
        }
        testNumberValidity(n);

        // Shave off trailing zeros and decimal point, if possible.

        String s = n.toString();
        if (s.indexOf('.') > 0 && s.indexOf('e') < 0 && s.indexOf('E') < 0) {
            while (s.endsWith("0")) {
                s = s.substring(0, s.length() - 1);
            }
            if (s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
            }
        }
        return s;
    }

    /** Decide whether o must be skipped and added to a, when rendering a JSONObject */
    private boolean skipChildObject(JsonArrayBuilder a, Options  opt, String key, Object value) {
        if(opt.arraysForChildren && (value instanceof JsonObject)) {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add(opt.childNameKey, key);
            for (Map.Entry<String, JsonValue> entry : ((JsonObject) value).entrySet()) {
                builder.add(entry.getKey(), entry.getValue());
            }
            a.add(builder);
            return true;
        }
        return false;
    }

    /**
     * Make a prettyprinted JSON text of this JSONObject.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     * @return a printable, displayable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws IllegalArgumentException If the object contains an invalid number.
     */
    public String prettyPrint(JsonObject jo, Options opt) {
        int n = jo.size();
        if (n == 0) {
            return "{}";
        }
        final JsonArrayBuilder children = Json.createArrayBuilder();
        Iterator<String> keys = jo.keySet().iterator();
        StringBuilder sb = new StringBuilder("{");
        int newindent = opt.initialIndent + opt.indent;
        String o;
        if (n == 1) {
            o = keys.next();
            final Object v = jo.get(o);
            if(!skipChildObject(children, opt, o, v)) {
                sb.append(quote(o));
                sb.append(": ");
                sb.append(valueToString(v, opt));
            }
        } else {
            while (keys.hasNext()) {
                o = keys.next();
                final Object v = jo.get(o);
                if(skipChildObject(children, opt, o, v)) {
                    continue;
                }
                if (sb.length() > 1) {
                    sb.append(",\n");
                } else {
                    sb.append('\n');
                }
                indent(sb, newindent);
                sb.append(quote(o.toString()));
                sb.append(": ");
                sb.append(valueToString(v,
                        options().withIndent(opt.indent).withInitialIndent(newindent)));
            }
            if (sb.length() > 1) {
                sb.append('\n');
                indent(sb, newindent);
            }
        }

        /** Render children if any were skipped (in "children in arrays" mode) */
        JsonArray childrenArray = children.build();
        if(childrenArray.size() > 0) {
            if (sb.length() > 1) {
                sb.append(",\n");
            } else {
                sb.append('\n');
            }
            final Options childOpt = new Options(opt);
            childOpt.withInitialIndent(childOpt.initialIndent + newindent);
            indent(sb, childOpt.initialIndent);
            sb.append(quote(opt.childrenKey)).append(":");
            sb.append(prettyPrint(childrenArray, childOpt));
        }

        sb.append('}');
        return sb.toString();
    }

    /** Pretty-print a JSONArray */
    public String prettyPrint(JsonArray ja, Options opt) {
        int len = ja.size();
        if (len == 0) {
            return "[]";
        }
        int i;
        StringBuilder sb = new StringBuilder("[");
        if (len == 1) {
            sb.append(valueToString(ja.get(0), opt));
        } else {
            final int newindent = opt.initialIndent + opt.indent;
            if(opt.hasIndent()) {
                sb.append('\n');
            }
            for (i = 0; i < len; i += 1) {
                if (i > 0) {
                    sb.append(',');
                    if(opt.hasIndent()) {
                        sb.append('\n');
                    }
                }
                indent(sb, newindent);
                sb.append(valueToString(ja.get(i), opt));
            }
            if(opt.hasIndent()) {
                sb.append('\n');
            }
            indent(sb, opt.initialIndent);
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Throw an exception if the object is an NaN or infinite number.
     * @param o The object to test.
     * @throws IllegalArgumentException If o is a non-finite number.
     */
    public void testNumberValidity(Object o) {
        if (o != null) {
            if (o instanceof Double) {
                if (((Double)o).isInfinite() || ((Double)o).isNaN()) {
                    throw new IllegalArgumentException(
                        "JSON does not allow non-finite numbers");
                }
            } else if (o instanceof Float) {
                if (((Float)o).isInfinite() || ((Float)o).isNaN()) {
                    throw new IllegalArgumentException(
                        "JSON does not allow non-finite numbers.");
                }
            }
        }
    }

    /**
     * Make a string from the contents of this JSONArray. The
     * <code>separator</code> string is inserted between each element.
     * Warning: This method assumes that the data structure is acyclical.
     * @param separator A string that will be inserted between the elements.
     * @return a string.
     * @throws JSONException If the array contains an invalid number.
     */
    public String join(JsonArray ja, String separator) {
        final int len = ja.size();
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < len; i += 1) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(valueToString(ja.get(i)));
        }
        return sb.toString();
    }

    /**
     * Write the contents of the supplied JSONObject as JSON text to a writer.
     * For compactness, no whitespace is added.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     *
     * @return The writer.
     * @throws IOException
     */
    public Writer write(Writer writer, JsonObject jo) throws IOException{
       Json.createGenerator(writer).write(jo).flush();
        
       return writer;
    }

    /**
     * Write the contents of the supplied JSONArray as JSON text to a writer.
     * For compactness, no whitespace is added.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     *
     * @return The writer.
     * @throws IOException
     */
    public Writer write(Writer writer, JsonArray ja) throws IOException {
        Json.createGenerator(writer).write(ja).flush();
        return writer;
    }

    /**
     * Produce a string from a double. The string "null" will be returned if
     * the number is not finite.
     * @param  d A double.
     * @return A String.
     */
    public String doubleToString(double d) {
        if (Double.isInfinite(d) || Double.isNaN(d)) {
            return "null";
        }

        // Shave off trailing zeros and decimal point, if possible.

        String s = Double.toString(d);
        if (s.indexOf('.') > 0 && s.indexOf('e') < 0 && s.indexOf('E') < 0) {
            while (s.endsWith("0")) {
                s = s.substring(0, s.length() - 1);
            }
            if (s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
            }
        }
        return s;
    }
}
