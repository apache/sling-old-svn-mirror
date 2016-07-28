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
package org.apache.sling.distribution.resources.impl;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.service.cm.ConfigurationAdmin;

/**
 * An utility class for manipulating maps of osgi properties
 */
public class OsgiUtils {
    /**
     * Encode the value for the ldap filter: \, *, (, and ) should be escaped.
     */
    public static String escape(final String value) {
        return value.replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    /**
     * Used to serialize a map for logging purposes
     */
    public static String osgiPropertyMapToString(Map<String, Object> map) {
        String result = "";
        if (map == null) {
            return result;
        }

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result += entry.getKey() + "=";

            if (entry.getValue() == null) {
                result += safeString(entry.getValue());
            } else if (entry.getValue().getClass().isArray()) {
                Object[] array = (Object[]) entry.getValue();
                for (Object obj : array) {
                    result += safeString(obj) + ",";
                }
            } else {
                result += safeString(entry.getValue());
            }

            result += "\n";
        }

        return result;
    }

    private static String safeString(Object obj) {
        return obj == null ? "null" : obj.toString();
    }


    /**
     * Create a filter for selecting configs of a certain factory
     */
    public static String getFilter(String configFactory, String propertyName, String propertyValue) {
        if (propertyName != null && propertyValue != null) {
            return "(&(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + OsgiUtils.escape(configFactory) + ")("
                    + OsgiUtils.escape(propertyName) + "=" + OsgiUtils.escape(propertyValue) + "))";
        } else if (configFactory != null) {
            return "(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + OsgiUtils.escape(configFactory) + ")";
        }

        return null;
    }


    /**
     * Transform a Dictionary into a Map
     */
    public static <K, V> Map<K, V> fromDictionary(Dictionary<K, V> dictionary) {
        if (dictionary == null) {
            return null;
        }
        Map<K, V> map = new HashMap<K, V>(dictionary.size());
        Enumeration<K> keys = dictionary.keys();
        while (keys.hasMoreElements()) {
            K key = keys.nextElement();
            map.put(key, dictionary.get(key));
        }
        return map;
    }

    /**
     * Transform a Map into a Dictionary
     */
    public static <K, V> Dictionary<K, V> toDictionary(Map<K, V> map) {
        if (map == null) {
            return null;
        }
        Dictionary<K, V> dictionary = new Hashtable<K, V>(map.size());
        for (Map.Entry<K, V> entry : map.entrySet()) {
            dictionary.put(entry.getKey(), entry.getValue());
        }

        return dictionary;
    }

    /**
     * Filter out object types that are not compatible with osgi configs
     */
    public static Map<String, Object> sanitize(Map<String, Object> map) {
        Map<String, Object> result = new HashMap<String, Object>();
        if (map == null) {
            return result;
        }

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }

            // skip jcr: stuff
            if (entry.getKey().contains(":")) {
                continue;
            }

            Class<?> valueClass = entry.getValue().getClass();
            Object value = entry.getValue();
            if (valueClass.isArray()) {
                valueClass = valueClass.getComponentType();

                // fix string arrays that come as Object[]
                if (valueClass.equals(Object.class)) {
                    Object[] array = (Object[]) value;
                    value = Arrays.asList(array).toArray(new String[array.length]);
                    valueClass = String.class;
                }
            }

            if (valueClass.isPrimitive()
                    || valueClass.equals(String.class)
                    || valueClass.equals(Boolean.class)
                    || valueClass.equals(Integer.class)) {
                result.put(entry.getKey(), value);
            }
        }

        return result;

    }
}
