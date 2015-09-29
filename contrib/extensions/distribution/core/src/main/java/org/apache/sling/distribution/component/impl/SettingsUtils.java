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
package org.apache.sling.distribution.component.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.sling.commons.osgi.PropertiesUtil;

//TODO: Consider removing it

/**
 * Utility class that provides parsing from linear set of properties into a tree of properties
 */
public class SettingsUtils {

    private static final String COMPONENT_ROOT = "";
    private static final char COMPONENT_DELIM = '/';
    private static final char COMPONENT_MAP_BEGIN = '[';
    private static final char COMPONENT_MAP_END = ']';
    private static final char COMPONENT_MAP_DELIM = '=';


    /**
     * packageExporter/propertyKey=propertyValue
     * packageExporter/endpoint[flag]=propertyValue
     *
     * packageExporter/packageBuilder/propertyKey=propertyValue
     *
     * propertyKey=propertyValue
     * trigger[0]/propertyKey=propertyValue
     * trigger[0]/propertyKey=propertyValue
     *
     * @param lines the property lines
     * @return a {@link Map} of the property names -> property values
     */
    public static Map<String, Object> parseLines(String[] lines) {
        Map<String, Object> result = new HashMap<String, Object>();

        Map<String, List<String>> linesMap = toLinesMap(lines);

        for (Map.Entry<String, List<String>> entry : linesMap.entrySet()) {
            String componentName = entry.getKey();
            List<String> var = entry.getValue();
            String[] componentLines = var.toArray(new String[var.size()]);

            if (COMPONENT_ROOT.equals(componentName)) {
                Map<String, String> map = PropertiesUtil.toMap(componentLines, new String[0]);
                result.putAll(map);
            } else {
                Map<String, Object> componentMap = parseLines(componentLines);
                result.put(componentName, componentMap);
            }
        }

        result = collapseMap(result);
        return result;
    }

    private static Map<String, List<String>> toLinesMap(String[] lines) {
        Map<String, List<String>> result = new HashMap<String, List<String>>();


        for (String line : lines) {
            int firstMapDelim = line.indexOf(COMPONENT_MAP_DELIM);
            if (firstMapDelim < 0) {
                continue;
            }

            int firstDelim = line.substring(0, firstMapDelim).indexOf(COMPONENT_DELIM);
            String key = COMPONENT_ROOT;
            String value = line;

            if (firstDelim >= 0) {
                key = line.substring(0, firstDelim);
                value = line.substring(firstDelim + 1);
            }


            if (!result.containsKey(key)) {
                result.put(key, new ArrayList<String>());
            }

            List<String> exitingLines = result.get(key);
            exitingLines.add(value);

        }

        return result;
    }

    private static Map<String, Object> collapseMap(Map<String, Object> valueMap) {

        Map<String, Object> result = new HashMap<String, Object>();

        for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            int beginDelim = key.indexOf(COMPONENT_MAP_BEGIN);
            int endDelim = key.indexOf(COMPONENT_MAP_END);

            if (beginDelim >= 0 && endDelim > beginDelim) {
                String newKey = key.substring(0, beginDelim);
                String partialKey = key.substring(beginDelim + 1, endDelim);


                boolean isNumber = isNumber(partialKey);

                if (!result.containsKey(newKey)) {
                    result.put(newKey, isNumber ? new ArrayList<Object>() : new HashMap<String, Object>());
                }

                Object existingObject = result.get(newKey);
                if (existingObject instanceof Map) {
                    ((Map) existingObject).put(partialKey, value);
                } else if (existingObject instanceof List) {
                    ((List) existingObject).add(value);
                } else {
                    // skip if there is already something else in there
                }
            } else {
                result.put(key, value);
            }
        }

        return result;

    }

    private static boolean isNumber(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    public static <AType> Map<String, AType> toMap(List<AType> aList, String prefix) {
        Map<String, AType> result = new TreeMap<String, AType>();
        for (int i = 0; i < aList.size(); i++) {
            result.put(prefix + i, aList.get(i));
        }

        return result;
    }


    public static Map<String, String> toUriMap(Object obj) {
        Map<String, String> uriMap = PropertiesUtil.toMap(obj, new String[0]);

        if (uriMap.size() == 0) {
            String[] endpoints = PropertiesUtil.toStringArray(obj, new String[0]);
            endpoints = removeEmptyEntries(endpoints);
            uriMap = toMap(Arrays.asList(endpoints), "endpoint");
        }
        return uriMap;
    }

    public static String[] removeEmptyEntries(String[] array) {
        if (array == null || array.length == 0) {
            return array;
        }

        List<String> result = new ArrayList<String>();
        for (String entry : array) {
            entry = removeEmptyEntry(entry);

            if (entry != null) {
                result.add(entry);
            }
        }

        if (result.size() == 0) {
            return null;
        }

        return result.toArray(new String[0]);
    }

    public static String[] removeEmptyEntries(String[] array, String[] defaultArray) {
        String[] result = removeEmptyEntries(array);
        if (result == null) {
            return defaultArray;
        } else {
            return result;
        }
    }



    public static String removeEmptyEntry(String entry) {
        if (entry == null) {
            return null;
        }

        entry = entry.trim();


        if (entry.length() == 0) {
            return null;
        }

        return entry;
    }
}
