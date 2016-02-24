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
package org.apache.sling.nosql.generic.adapter;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Transforms NoSqlData maps to a valid form for couchbase JSON document.
 * All arrays have to be transformed to lists.
 */
final class MapConverter {

    private MapConverter() {
        // static methods only
    }

    /**
     * @param map Map with multi-valued arrays
     * @return Map with multi-valued lists
     */
    public static Map<String, Object> mapArrayToList(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue().getClass().isArray()) {
                Class componentType = entry.getValue().getClass().getComponentType();
                if (componentType == int.class) {
                    entry.setValue(Arrays.asList(ArrayUtils.toObject((int[]) entry.getValue())));
                }
                else if (componentType == long.class) {
                    entry.setValue(Arrays.asList(ArrayUtils.toObject((long[]) entry.getValue())));
                }
                else if (componentType == double.class) {
                    entry.setValue(Arrays.asList(ArrayUtils.toObject((double[]) entry.getValue())));
                }
                else if (componentType == boolean.class) {
                    entry.setValue(Arrays.asList(ArrayUtils.toObject((boolean[]) entry.getValue())));
                }
                else {
                    entry.setValue(Arrays.asList((Object[]) entry.getValue()));
                }
            }
        }
        return map;
    }

    /**
     * @param map Map with multi-valued lists
     * @return Map with multi-valued arrays
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> mapListToArray(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof List) {
                List list = (List) entry.getValue();
                if (list.size() == 0) {
                    entry.setValue(null);
                }
                else {
                    Class type = list.get(0).getClass();
                    entry.setValue(list.toArray((Object[]) Array.newInstance(type, list.size())));
                }
            }
        }
        return map;
    }

}
