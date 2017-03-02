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
package org.apache.sling.fscontentparser.impl;

import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.UUID;

import javax.jcr.PropertyType;

import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.vault.util.DocViewProperty;

/**
 * Parses JCR XML files that contains content fragments.
 */
class JcrXmlValueConverter {
    
    private JcrXmlValueConverter() {
        // static methods only
    }
    
    /**
     * Parse JSON value from XML Attribute.
     * @param value XML attribute value
     * @return Value object
     */
    public static Object parseValue(final String name, final String value) {
        if (value == null) {
            return null;
        }
        DocViewProperty prop = DocViewProperty.parse(name, value);
        
        // convert values
        if (prop.isMulti) {
            Class<?> arrayType = getType(prop.type);
            if (arrayType == null) {
                return null;
            }
            Object result = Array.newInstance(arrayType, prop.values.length);
            for (int i=0; i<prop.values.length; i++) {
                Array.set(result, i, convertValue(prop.values[i], prop.type, true));
            }
            return result;
        }
        else {
            return convertValue(prop.values[0], prop.type, false);
        }
    }
    
    /**
     * Parse value depending on type prefix.
     * @param value Value
     * @param type Type
     * @param inArray Value is in array
     * @return Value object
     */
    private static Object convertValue(final String value, final int type, final boolean inArray) {
        switch (type) {
            case PropertyType.UNDEFINED:
            case PropertyType.STRING:
            case PropertyType.NAME:
            case PropertyType.PATH:
                return value;
            case PropertyType.BOOLEAN:
                return Boolean.valueOf(value);
            case PropertyType.LONG:
                return Long.valueOf(value);
            case PropertyType.DOUBLE:
            case PropertyType.DECIMAL:
                // TODO: specific handling for BigDecimal? not properly supported in ValueMapDecorator until very recent Sling API versions
                return Double.valueOf(value);
            case PropertyType.DATE:
                return ISO8601.parse(value);
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                return UUID.fromString(value);
            case PropertyType.URI:
                try {
                    return new URI(value);
                }
                catch (URISyntaxException ex) {
                    throw new IllegalArgumentException("Unexpected URI syntax: " + value);
                }
            case PropertyType.BINARY:
                // not supported - ignore value
                return null;
            default:
                throw new IllegalArgumentException("Unexpected type: " + PropertyType.nameFromValue(type));
            
        }
    }
    
    /**
     * Get java type for given JCR type.
     * @param type Type
     * @return Type
     */
    private static Class<?> getType(final int type) {
        switch (type) {
            case PropertyType.UNDEFINED:
            case PropertyType.STRING:
            case PropertyType.NAME:
            case PropertyType.PATH:
                return String.class;
            case PropertyType.BOOLEAN:
                return Boolean.class;
            case PropertyType.LONG:
                return Long.class;
            case PropertyType.DOUBLE:
            case PropertyType.DECIMAL:
                // TODO: specific handling for BigDecimal? not properly supported in ValueMapDecorator until very recent Sling API versions
                return Double.class;
            case PropertyType.DATE:
                return Calendar.class;
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                return UUID.class;
            case PropertyType.URI:
                return URI.class;
            case PropertyType.BINARY:
                // not supported - ignore value
                return null;
            default:
                throw new IllegalArgumentException("Unexpected type: " + PropertyType.nameFromValue(type));
            
        }
    }
    
}
