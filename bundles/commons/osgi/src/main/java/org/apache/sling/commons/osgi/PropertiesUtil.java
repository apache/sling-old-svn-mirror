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
package org.apache.sling.commons.osgi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The <code>PropertiesUtil</code> is a utility class providing some
 * useful utility methods for converting property types.
 *
 * @since 2.1
 */
public class PropertiesUtil {

    /**
     * Returns the boolean value of the parameter or the
     * <code>defaultValue</code> if the parameter is <code>null</code>.
     * If the parameter is not a <code>Boolean</code> it is converted
     * by calling <code>Boolean.valueOf</code> on the string value of the
     * object.
     * @param propValue the property value or <code>null</code>
     * @param defaultValue the default boolean value
     * @return Boolean value
     */
    public static boolean toBoolean(Object propValue, boolean defaultValue) {
        propValue = toObject(propValue);
        if (propValue instanceof Boolean) {
            return (Boolean) propValue;
        } else if (propValue != null) {
            return Boolean.parseBoolean(String.valueOf(propValue));
        }

        return defaultValue;
    }

    /**
     * Returns the parameter as a string or the
     * <code>defaultValue</code> if the parameter is <code>null</code>.
     * @param propValue the property value or <code>null</code>
     * @param defaultValue the default string value
     * @return String value
     */
    public static String toString(Object propValue, String defaultValue) {
        propValue = toObject(propValue);
        return (propValue != null) ? propValue.toString() : defaultValue;
    }

    /**
     * Returns the parameter as a long or the
     * <code>defaultValue</code> if the parameter is <code>null</code> or if
     * the parameter is not a <code>Long</code> and cannot be converted to
     * a <code>Long</code> from the parameter's string value.
     * @param propValue the property value or <code>null</code>
     * @param defaultValue the default long value
     * @return Long value
     */
    public static long toLong(Object propValue, long defaultValue) {
        propValue = toObject(propValue);
        if (propValue instanceof Long) {
            return (Long) propValue;
        } else if (propValue != null) {
            try {
                return Long.parseLong(String.valueOf(propValue));
            } catch (NumberFormatException nfe) {
                // don't care, fall through to default value
            }
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
     * @return Integer value
     */
    public static int toInteger(Object propValue, int defaultValue) {
        propValue = toObject(propValue);
        if (propValue instanceof Integer) {
            return (Integer) propValue;
        } else if (propValue != null) {
            try {
                return Integer.parseInt(String.valueOf(propValue));
            } catch (NumberFormatException nfe) {
                // don't care, fall through to default value
            }
        }

        return defaultValue;
    }

    /**
     * Returns the parameter as a double or the
     * <code>defaultValue</code> if the parameter is <code>null</code> or if
     * the parameter is not a <code>Double</code> and cannot be converted to
     * a <code>Double</code> from the parameter's string value.
     * @param propValue the property value or <code>null</code>
     * @param defaultValue the default double value
     * @return Double value
     */
    public static double toDouble(Object propValue, double defaultValue) {
        propValue = toObject(propValue);
        if (propValue instanceof Double) {
            return (Double) propValue;
        } else if (propValue != null) {
            try {
                return Double.parseDouble(String.valueOf(propValue));
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
     * @return Object value
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

    /**
     * Returns the parameter as an array of Strings. If
     * the parameter is a scalar value its string value is returned as a single
     * element array. If the parameter is an array, the elements are converted to
     * String objects and returned as an array. If the parameter is a collection, the
     * collection elements are converted to String objects and returned as an array.
     * Otherwise (if the parameter is <code>null</code>) <code>null</code> is
     * returned.
     * @param propValue The object to convert.
     * @return String array value
     */
    public static String[] toStringArray(Object propValue) {
        return toStringArray(propValue, null);
    }

    /**
     * Returns the parameter as an array of Strings. If
     * the parameter is a scalar value its string value is returned as a single
     * element array. If the parameter is an array, the elements are converted to
     * String objects and returned as an array. If the parameter is a collection, the
     * collection elements are converted to String objects and returned as an array.
     * Otherwise (if the property is <code>null</code>) a provided default value is
     * returned.
     * @param propValue The object to convert.
     * @param defaultArray The default array to return.
     * @return String array value
     */
    public static String[] toStringArray(Object propValue, String[] defaultArray) {
        if (propValue == null) {
            // no value at all
            return defaultArray;

        } else if (propValue instanceof String) {
            // single string
            return new String[] { (String) propValue };

        } else if (propValue instanceof String[]) {
            // String[]
            return (String[]) propValue;

        } else if (propValue.getClass().isArray()) {
            // other array
            Object[] valueArray = (Object[]) propValue;
            List<String> values = new ArrayList<String>(valueArray.length);
            for (Object value : valueArray) {
                if (value != null) {
                    values.add(value.toString());
                }
            }
            return values.toArray(new String[values.size()]);

        } else if (propValue instanceof Collection<?>) {
            // collection
            Collection<?> valueCollection = (Collection<?>) propValue;
            List<String> valueList = new ArrayList<String>(valueCollection.size());
            for (Object value : valueCollection) {
                if (value != null) {
                    valueList.add(value.toString());
                }
            }
            return valueList.toArray(new String[valueList.size()]);
        }

        return defaultArray;
    }

    /**
     * Returns the parameter as a map with string keys and string values.
     *
     * The parameter is considered as a collection whose entries are of the form
     * key=value. The conversion has following rules
     * <ul>
     *     <li>Entries are of the form key=value</li>
     *     <li>key is trimmed</li>
     *     <li>value is trimmed. If a trimmed value results in an empty string it is treated as null</li>
     *     <li>Malformed entries like 'foo','foo=' are ignored</li>
     *     <li>Map entries maintain the input order</li>
     * </ul>
     *
     * Otherwise (if the property is <code>null</code>) a provided default value is
     * returned.
     * @param propValue The object to convert.
     * @param defaultArray The default array converted to map.
     * @return Map value
     */
    public static Map<String, String> toMap(Object propValue, String[] defaultArray) {
        String[] arrayValue = toStringArray(propValue, defaultArray);

        if (arrayValue == null) {
            return null;
        }

        //in property values
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (String kv : arrayValue) {
            int indexOfEqual = kv.indexOf('=');
            if (indexOfEqual > 0) {
                String key = trimToNull(kv.substring(0, indexOfEqual));
                String value = trimToNull(kv.substring(indexOfEqual + 1));
                if (key != null) {
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    private static String trimToNull(String str)    {
        String ts = trim(str);
        return isEmpty(ts) ? null : ts;
    }

    private static String trim(String str){
        return str == null ? null : str.trim();
    }

    private static boolean isEmpty(String str){
        return str == null || str.length() == 0;
    }

}
