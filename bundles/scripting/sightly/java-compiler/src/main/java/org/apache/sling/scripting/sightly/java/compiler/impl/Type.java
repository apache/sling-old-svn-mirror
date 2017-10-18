/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.apache.sling.scripting.sightly.java.compiler.impl;

/**
 * Type inferred for an expression
 */
public class Type {

    public static final Type UNKNOWN = new Type("Object", false, null);
    public static final Type STRING = new Type("String", false, null);
    public static final Type LONG = new Type("long", true, 0);
    public static final Type DOUBLE = new Type("double", true, 0.0d);
    public static final Type BOOLEAN = new Type("boolean", true, false);
    public static final Type MAP = new Type("java.util.Map", false, null);

    private String nativeClass;
    private final boolean isPrimitive;
    private final Object defaultValue;

    private Type(String nativeClass, boolean isPrimitive, Object defaultValue) {
        this.nativeClass = nativeClass;
        this.isPrimitive = isPrimitive;
        this.defaultValue = defaultValue;
    }

    public String getNativeClass() {
        return nativeClass;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public boolean isPrimitive() {
        return isPrimitive;
    }

    public static Type dynamic(String type) {
        return new Type(type, false, null);
    }
}
