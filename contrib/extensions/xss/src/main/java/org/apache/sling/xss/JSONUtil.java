/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or
 * more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the
 * Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 ******************************************************************************/
package org.apache.sling.xss;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;

/**
 * JSON utilities
 * <p/>
 * Support for handling xss protected values with JSON objects and JSON writers.
 */
public final class JSONUtil {

    /**
     * Key suffix for XSS protected properties
     */
    public static final String KEY_SUFFIX_XSS = "_xss";

    // no instantiation
    private JSONUtil() {
    }

    /**
     * Puts a xss protected value into a JSON object.
     * The value is put under the provided key.
     *
     * @param object JSON object
     * @param key    Key to write
     * @param value  Value to write
     * @param xss    XSS protection filter
     * @throws JSONException        If value could not be put into the object
     * @throws NullPointerException If xss protection filter is <code>null</code>
     */
    public static void putProtected(final JSONObject object, final String key, final String value, final XSSFilter xss)
            throws JSONException {
        final String xssValue = xss.filter(ProtectionContext.PLAIN_HTML_CONTENT, value);
        object.put(key, xssValue);
    }

    /**
     * Puts a value into a JSON object
     * In addition, the xss protected value is put under the provided key appended by {@link #KEY_SUFFIX_XSS}
     *
     * @param object JSON object
     * @param key    Key to write
     * @param value  Value to write
     * @param xss    XSS protection filter
     * @throws JSONException        If value could not be put into the object
     * @throws NullPointerException If xss protection filter is <code>null</code>
     */
    public static void putWithProtected(final JSONObject object, final String key, final String value, final XSSFilter xss)
            throws JSONException {
        putProtected(object, key + KEY_SUFFIX_XSS, value, xss);
        object.put(key, value);
    }

    /**
     * Writes a xss protected value into a JSON writer.
     * The value is written under the provided key.
     *
     * @param writer JSON writer
     * @param key    Key to write
     * @param value  Value to write
     * @param xss    XSS protection filter
     * @throws JSONException        If value could not be written
     * @throws NullPointerException If xss protection filter is <code>null</code>
     */
    public static void writeProtected(final JSONWriter writer, final String key, final String value, final XSSFilter xss)
            throws JSONException {
        final String xssValue = xss.filter(ProtectionContext.PLAIN_HTML_CONTENT, value);
        writer.key(key).value(xssValue);
    }

    /**
     * Writes a xss protected value array into a JSON writer.
     * The values are written under the provided key.
     *
     * @param writer The JSON writer.
     * @param key    Key to use.
     * @param values The value arrays.
     * @param xss    The XSS protection filter.
     * @throws JSONException        If an JSON specific error occurs.
     * @throws NullPointerException If xss protection filter is <code>null</code>
     */
    public static void writeProtected(JSONWriter writer, String key,
                                      String[] values, XSSFilter xss) throws JSONException {
        writer.key(key);
        writer.array();
        for (String value : values) {
            String xssValue = xss.filter(ProtectionContext.PLAIN_HTML_CONTENT, value);
            writer.value(xssValue);
        }
        writer.endArray();
    }

    /**
     * Writes a value into a JSON write
     * In addition, the xss protected value is written with the provided key appended by {@link #KEY_SUFFIX_XSS}
     *
     * @param writer JSON writer
     * @param key    Key to write
     * @param value  Value to write
     * @param xss    XSS protection filter
     * @throws JSONException        If value could not be written
     * @throws NullPointerException If xss protection filter is <code>null</code>
     */
    public static void writeWithProtected(final JSONWriter writer, final String key, final String value, final XSSFilter xss)
            throws JSONException {
        writeProtected(writer, key + KEY_SUFFIX_XSS, value, xss);
        writer.key(key).value(value);
    }

    /**
     * Writes a value array into a JSON write.
     * In addition, the xss protected values are written with the provided key
     * appended by {@link #KEY_SUFFIX_XSS}
     *
     * @param writer The JSON writer to use.
     * @param key    The key to write.
     * @param values The value array.
     * @param xss    The xss protection filter.
     * @throws JSONException        If a JSON specific error occurs.
     * @throws NullPointerException If xss protection filter is <code>null</code>
     */
    public static void writeWithProtected(JSONWriter writer, String key,
                                          String[] values, XSSFilter xss) throws JSONException {

        writeProtected(writer, key + KEY_SUFFIX_XSS, values, xss);
        // and the non-xss array variant
        writer.key(key);
        writer.array();
        for (String value : values) {
            writer.value(value);
        }
        writer.endArray();
    }
}
