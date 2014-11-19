/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.validation.api;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.sling.validation.api.exceptions.NonExistingTypeException;

/**
 * Defines value types stored in a {@link ResourceProperty}.
 */
public enum Type {

    BOOLEAN("boolean"),
    DATE("date"),
    INT("int"),
    LONG("long"),
    DOUBLE("double"),
    FLOAT("float"),
    CHAR("char"),
    STRING("string");

    /**
     * optimises usage of the SimpleDateFormatter since multiple date validations can occur per thread
     */
    private static final ThreadLocal<SimpleDateFormat[]> FORMATTERS_THREAD_LOCAL = new ThreadLocal<SimpleDateFormat[]>() {

        protected SimpleDateFormat[] initialValue() {
            return new SimpleDateFormat[]{
                    new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z"),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
                    new SimpleDateFormat("yyyy-MM-dd"),
                    new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"),
                    new SimpleDateFormat("dd.MM.yyyy")
            };
        }

    };

    private String name;

    private Type(String name) {
        this.name = name;
    }

    /**
     * Retrieves the name of the type as a {@code String}.
     *
     * @return this type's name
     */
    public String getName() {
        return name;
    }

    /**
     * Validates if the provided data has the correct representation for this {@code Type}.
     *
     * @param data the data to check
     * @return {@code true} if the data format conforms to this {@code Type}, {@code false} otherwise
     */
    public boolean isValid(String data) {
        boolean valid = false;
        switch (this) {
            case BOOLEAN:
                valid = Boolean.parseBoolean(data);
                break;
            case DATE:
                for (SimpleDateFormat sdf : FORMATTERS_THREAD_LOCAL.get()) {
                    Date d = null;
                    try {
                        d = sdf.parse(data);
                    } catch (ParseException e) {
                        // do nothing
                    }
                    if (d != null) {
                        valid = true;
                        break;
                    }
                }
                break;
            case INT:
                try {
                    Integer.parseInt(data);
                    valid = true;
                } catch (NumberFormatException e) {
                    // do nothing
                }
                break;
            case LONG:
                try {
                    Long.parseLong(data);
                    valid = true;
                } catch (NumberFormatException e) {
                    // do nothing
                }
                break;
            case FLOAT:
                try {
                    Float.parseFloat(data);
                    valid = true;
                } catch (NumberFormatException e) {
                    // do nothing
                }
                break;
            case DOUBLE:
                try {
                    Double.parseDouble(data);
                    valid = true;
                } catch (NumberFormatException e) {
                    // do nothing
                }
                break;
            case CHAR:
                try {
                    valid = data.length() == 1;
                } catch (NumberFormatException e) {
                    // do nothing
                }
                break;
            case STRING:
                valid = true;
        }
        return valid;
    }

    /**
     * Returns the enum constant having its value equal to <code>value</code>.
     *
     * @param value the value of a registered constant
     * @return the constant, if found; <code>null</code> otherwise
     */
    public static Type getType(String value) {
        if (value != null) {
            for (Type t : Type.values()) {
                if (value.equals(t.getName())) {
                    return t;
                }
            }
        }
        throw new NonExistingTypeException(String.format("Type %s is not defined.", value));
    }
}
