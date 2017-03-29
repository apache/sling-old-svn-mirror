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
package org.apache.sling.event.impl;

import java.lang.reflect.Field;

public class TestUtil {

    private static Object getSetField(final Object obj, final String fieldName, final boolean isGet, final Object value) {
        Class<?> clazz = obj.getClass();
        while ( clazz != null ) {
            try {
                final Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);

                if ( isGet ) {
                    return field.get(obj);
                } else {
                    field.set(obj, value);
                    return null;
                }
            } catch ( final Exception ignore ) {
                // ignore
            }
            clazz = clazz.getSuperclass();
        }
        throw new RuntimeException("Field " + fieldName + " not found on object " + obj);
    }

    public static void setFieldValue(final Object obj, final String fieldName, final Object value) {
        getSetField(obj, fieldName, false, value);
    }

    public static Object getFieldValue(final Object obj, final String fieldName) {
        return getSetField(obj, fieldName, true, null);
    }
}
