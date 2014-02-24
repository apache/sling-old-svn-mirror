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

package org.apache.sling.commons.log.logback.internal.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.xml.sax.InputSource;

public class Util {

    public static List<String> toList(Object values) {
        if (values == null) {
            return Collections.emptyList();
        }

        Object[] valueArray;
        if (values.getClass().isArray()) {
            valueArray = (Object[]) values;
        } else if (values instanceof Collection<?>) {
            valueArray = ((Collection<?>) values).toArray();
        } else {
            valueArray = new Object[] {
                values
            };
        }

        List<String> valuesList = new ArrayList<String>(valueArray.length);
        for (Object valueObject : valueArray) {
            if (valueObject != null) {
                String[] splitValues = valueObject.toString().split(",");
                for (String value : splitValues) {
                    value = value.trim();
                    if (value.length() > 0) {
                        valuesList.add(value);
                    }
                }
            }
        }

        return valuesList;
    }

    public static void close(InputSource is) {
        Closeable c = is.getByteStream();
        if (c == null) {
            c = is.getCharacterStream();
        }
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    //-------------Taken from org.apache.sling.commons.osgi.PropertiesUtil

    /**
     * Returns the boolean value of the parameter or the
     * <code>defaultValue</code> if the parameter is <code>null</code>.
     * If the parameter is not a <code>Boolean</code> it is converted
     * by calling <code>Boolean.valueOf</code> on the string value of the
     * object.
     * @param propValue the property value or <code>null</code>
     * @param defaultValue the default boolean value
     */
    public static boolean toBoolean(Object propValue, boolean defaultValue) {
        propValue = toObject(propValue);
        if (propValue instanceof Boolean) {
            return (Boolean) propValue;
        } else if (propValue != null) {
            return Boolean.valueOf(String.valueOf(propValue));
        }

        return defaultValue;
    }

    /**
     * Returns the parameter as an integer or the
     * <code>defaultValue</code> if the parameter is <code>null</code> or if
     * the parameter is not an <code>Integer</code> and cannot be converted to
     * an <code>Integer</code> from the parameter's string value.
     * @param propValue the property value or <code>null</code>
     * @param defaultValue the default integer value
     */
    public static int toInteger(Object propValue, int defaultValue) {
        propValue = toObject(propValue);
        if (propValue instanceof Integer) {
            return (Integer) propValue;
        } else if (propValue != null) {
            try {
                return Integer.valueOf(String.valueOf(propValue));
            } catch (NumberFormatException nfe) {
                // don't care, fall through to default value
            }
        }

        return defaultValue;
    }

    /**
     * Returns the parameter as a single value. If the
     * parameter is neither an array nor a <code>java.util.Collection</code> the
     * parameter is returned unmodified. If the parameter is a non-empty array,
     * the first array element is returned. If the property is a non-empty
     * <code>java.util.Collection</code>, the first collection element is returned.
     * Otherwise <code>null</code> is returned.
     * @param propValue the parameter to convert.
     */
    public static Object toObject(Object propValue) {
        if (propValue == null) {
            return null;
        } else if (propValue.getClass().isArray()) {
            Object[] prop = (Object[]) propValue;
            return prop.length > 0 ? prop[0] : null;
        } else if (propValue instanceof Collection<?>) {
            Collection<?> prop = (Collection<?>) propValue;
            return prop.isEmpty() ? null : prop.iterator().next();
        } else {
            return propValue;
        }
    }
}
