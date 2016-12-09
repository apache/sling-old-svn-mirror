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
package org.apache.sling.caconfig.resource.impl.util;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public final class MapUtil {
    
    private MapUtil() {
        // static methods only
    }
    
    /**
     * Produce trace output for properties map.
     * @param properties Properties
     * @return Debug output
     */
    public static final String traceOutput(Map<String,Object> properties) {
        SortedSet<String> propertyNames = new TreeSet<>(properties.keySet());
        PropertiesFilterUtil.removeIgnoredProperties(propertyNames);
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        Iterator<String> propertyNameIterator = propertyNames.iterator();
        while (propertyNameIterator.hasNext()) {
            String propertyName = propertyNameIterator.next();
            sb.append(propertyName).append(": ");
            appendValue(sb, properties.get(propertyName));
            if (propertyNameIterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }
    
    private static void appendValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        }
        else if (value.getClass().isArray()) {
            sb.append("[");
            for (int i = 0; i < Array.getLength(value); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                appendValue(sb, Array.get(value, i));
            }
            sb.append("]");
        }
        else if (value instanceof String) {
            sb.append("'").append(value.toString()).append("'");
        }
        else {
            sb.append(value.toString());
        }
    }

}
