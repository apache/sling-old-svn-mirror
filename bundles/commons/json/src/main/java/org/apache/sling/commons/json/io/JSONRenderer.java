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
package org.apache.sling.commons.json.io;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONString;

/** Various JSON-to-String primitives, used by other classes
 *  when outputting/formatting JSON.
 *  
 *  Streaming variants of some methods are provided.
 *  The existing code in this module is often not streaming, but
 *  we should write newer code using streams, as much as
 *  possible.
 */
public class JSONRenderer {
    
    /** Rendering options */
    static public class Options {
        int indent;
        int initialIndent;
        
        /** Clients use JSONRenderer.options() to create objects */
        private Options() {
        }
        
        public Options withIndent(int n) {
            indent = n;
            return this;
        }
        
        public Options withInitialIndent(int n) {
            initialIndent = n;
            return this;
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
    public String toString(JSONObject jo) {
        try {
            final Iterator<String> keys = jo.keys();
            final StringBuffer sb = new StringBuffer("{");

            while (keys.hasNext()) {
                if (sb.length() > 1) {
                    sb.append(',');
                }
                String o = keys.next();
                sb.append(quote(o));
                sb.append(':');
                sb.append(valueToString(jo.get(o)));
            }
            sb.append('}');
            return sb.toString();
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
    public String toString(JSONArray ja) {
        try {
            return '[' + join(ja,",") + ']';
        } catch (Exception e) {
            return null;
        }
    }

    /** Quote the supplied string for JSON */
    public String quote(String string) {
        final StringWriter sw = new StringWriter();
        try {
            quote(sw, string);
        } catch(IOException ioex) {
            throw new RuntimeException("IOException in quote()", ioex);
        }
        return sw.toString();
    }
    
    /** Quote the supplied string for JSON, to the supplied Writer */
    public void quote(Writer w, String string) throws IOException {
        if (string == null || string.length() == 0) {
            w.write("\"\"");
            return;
        }
    
        char         b;
        char         c = 0;
        int          i;
        int          len = string.length();
        String       t;
    
        w.write('"');
        for (i = 0; i < len; i += 1) {
            b = c;
            c = string.charAt(i);
            switch (c) {
            case '\\':
            case '"':
                w.write('\\');
                w.write(c);
                break;
            case '/':
                if (b == '<') {
                    w.write('\\');
                }
                w.write(c);
                break;
            case '\b':
                w.write("\\b");
                break;
            case '\t':
                w.write("\\t");
                break;
            case '\n':
                w.write("\\n");
                break;
            case '\f':
                w.write("\\f");
                break;
            case '\r':
                w.write("\\r");
                break;
            default:
                if (c < ' ' || (c >= '\u0080' && c < '\u00a0') ||
                               (c >= '\u2000' && c < '\u2100')) {
                    t = "000" + Integer.toHexString(c);
                    w.write("\\u" + t.substring(t.length() - 4));
                } else {
                    w.write(c);
                }
            }
        }
        w.write('"');
    }
    
    /**
     * Make a JSON text of an Object value. If the object has an
     * value.toJSONString() method, then that method will be used to produce
     * the JSON text. The method is required to produce a strictly
     * conforming text. If the object does not contain a toJSONString
     * method (which is the most common case), then a text will be
     * produced by the rules.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     * @param value The value to be serialized.
     * @return a printable, displayable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws JSONException If the value is or contains an invalid number.
     */
    public String valueToString(Object value) throws JSONException {
        // TODO call the other valueToString instead
        if (value == null || value.equals(null)) {
            return "null";
        }
        if (value instanceof JSONString) {
            Object o;
            try {
                o = ((JSONString)value).toJSONString();
            } catch (Exception e) {
                throw new JSONException(e);
            }
            if (o instanceof String) {
                return (String)o;
            }
            throw new JSONException("Bad value from toJSONString: " + o);
        }
        if (value instanceof Number) {
            return numberToString((Number) value);
        }
        if (value instanceof Boolean || value instanceof JSONObject ||
                value instanceof JSONArray) {
            return value.toString();
        }
        return quote(value.toString());
    }
    
    /** Make a JSON String of an Object value, with rendering options
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     * @param value The value to be serialized.
     * @param indentFactor The number of spaces to add to each level of
     *  indentation.
     * @param indent The indentation of the top level.
     * @return a printable, displayable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws JSONException If the object contains an invalid number.
     */
    public String valueToString(Object value, Options opt) throws JSONException {
        if (value == null || value.equals(null)) {
            return "null";
        }
        try {
            if (value instanceof JSONString) {
                Object o = ((JSONString)value).toJSONString();
                if (o instanceof String) {
                    return (String)o;
                }
            }
        } catch (Exception e) {
            /* forget about it */
        }
        if (value instanceof Number) {
            return numberToString((Number) value);
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof JSONObject) {
            return prettyPrint((JSONObject)value, opt);
        }
        if (value instanceof JSONArray) {
            return prettyPrint((JSONArray)value, opt);
        }
        return quote(value.toString());
        
    }
    
    /**
     * Produce a string from a Number.
     * @param  n A Number
     * @return A String.
     * @throws JSONException If n is a non-finite number.
     */
    public String numberToString(Number n)
            throws JSONException {
        if (n == null) {
            throw new JSONException("Null pointer");
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
    
    /**
     * Make a prettyprinted JSON text of this JSONObject.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     * @param indentFactor The number of spaces to add to each level of
     *  indentation.
     * @param indent The indentation of the top level.
     * @return a printable, displayable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws JSONException If the object contains an invalid number.
     */
    public String prettyPrint(JSONObject jo, Options opt) throws JSONException {
        int i;
        int n = jo.length();
        if (n == 0) {
            return "{}";
        }
        Iterator<String> keys = jo.keys();
        StringBuilder sb = new StringBuilder("{");
        int newindent = opt.initialIndent + opt.indent;
        String       o;
        if (n == 1) {
            o = keys.next();
            sb.append(quote(o));
            sb.append(": ");
            sb.append(valueToString(jo.get(o), opt));
        } else {
            while (keys.hasNext()) {
                o = keys.next();
                if (sb.length() > 1) {
                    sb.append(",\n");
                } else {
                    sb.append('\n');
                }
                indent(sb, newindent);
                sb.append(quote(o.toString()));
                sb.append(": ");
                sb.append(valueToString(jo.get(o), 
                        options().withIndent(opt.indent).withInitialIndent(newindent)));
            }
            if (sb.length() > 1) {
                sb.append('\n');
                indent(sb, opt.indent);
            }
        }
        sb.append('}');
        return sb.toString();
    }

    public String prettyPrint(JSONArray ja, Options opt) throws JSONException {
        int len = ja.length();
        if (len == 0) {
            return "[]";
        }
        int i;
        StringBuilder sb = new StringBuilder("[");
        if (len == 1) {
            sb.append(valueToString(ja.get(0), opt));
        } else {
            final int newindent = opt.initialIndent + opt.indent;
            sb.append('\n');
            for (i = 0; i < len; i += 1) {
                if (i > 0) {
                    sb.append(",\n");
                }
                indent(sb, newindent);
                sb.append(valueToString(ja.get(i), opt));
            }
            sb.append('\n');
            indent(sb, opt.initialIndent);
        }
        sb.append(']');
        return sb.toString();
    }
    
    /**
     * Throw an exception if the object is an NaN or infinite number.
     * @param o The object to test.
     * @throws JSONException If o is a non-finite number.
     */
    public void testNumberValidity(Object o) throws JSONException {
        if (o != null) {
            if (o instanceof Double) {
                if (((Double)o).isInfinite() || ((Double)o).isNaN()) {
                    throw new JSONException(
                        "JSON does not allow non-finite numbers");
                }
            } else if (o instanceof Float) {
                if (((Float)o).isInfinite() || ((Float)o).isNaN()) {
                    throw new JSONException(
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
    public String join(JSONArray ja, String separator) throws JSONException {
        final int len = ja.length();
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < len; i += 1) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(JSONObject.valueToString(ja.get(i)));
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
     * @throws JSONException
     */
    public Writer write(Writer writer, JSONObject jo) throws JSONException {
       try {
           boolean  b = false;
           Iterator<String> keys = jo.keys();
           writer.write('{');

           while (keys.hasNext()) {
               if (b) {
                   writer.write(',');
               }
               String k = keys.next();
               writer.write(quote(k));
               writer.write(':');
               final Object v = jo.get(k);
               if (v instanceof JSONObject) {
                   ((JSONObject)v).write(writer);
               } else if (v instanceof JSONArray) {
                   ((JSONArray)v).write(writer);
               } else {
                   writer.write(valueToString(v));
               }
               b = true;
           }
           writer.write('}');
           return writer;
       } catch (IOException e) {
           throw new JSONException(e);
       }
    }
    
    /**
     * Write the contents of the supplied JSONArray as JSON text to a writer.
     * For compactness, no whitespace is added.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     *
     * @return The writer.
     * @throws JSONException
     */
    public Writer write(Writer writer, JSONArray ja) throws JSONException {
        try {
            boolean b = false;
            int len = ja.length();

            writer.write('[');

            for (int i = 0; i < len; i += 1) {
                if (b) {
                    writer.write(',');
                }
                final Object v = ja.get(i);
                if (v instanceof JSONObject) {
                    ((JSONObject)v).write(writer);
                } else if (v instanceof JSONArray) {
                    ((JSONArray)v).write(writer);
                } else {
                    writer.write(JSONObject.valueToString(v));
                }
                b = true;
            }
            writer.write(']');
            return writer;
        } catch (IOException e) {
           throw new JSONException(e);
        }
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