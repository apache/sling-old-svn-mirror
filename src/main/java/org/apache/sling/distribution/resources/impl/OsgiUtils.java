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

import java.util.Map;

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

    public static String osgiPropertyMapToString(Map<String, Object> map) {
        String result = "";
        if (map == null) {
            return result;
        }

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result += entry.getKey() + "=";

            if (entry.getValue() == null) {
                result += safeString(entry.getValue());
            }
            else if (entry.getValue().getClass().isArray()) {
                Object[] array = (Object[]) entry.getValue();
                for (Object obj : array) {
                    result += safeString(obj) + ",";
                }
            }
            else {
                result += safeString(entry.getValue());
            }

            result += "\n";
        }

        return result;
    }

    private static String safeString(Object obj) {
        return obj == null? "null" : obj.toString();
    }
}
