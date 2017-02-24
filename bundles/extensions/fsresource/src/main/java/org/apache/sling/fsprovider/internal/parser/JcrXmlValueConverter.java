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
package org.apache.sling.fsprovider.internal.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.util.ISO8601;

/**
 * Parses JCR XML files that contains content fragments.
 */
class JcrXmlValueConverter {
    
    private static final Pattern TYPE_PREFIX = Pattern.compile("^\\{([^\\{\\}]+)\\}(.+)$");
    private static final Pattern VALUE_ARRAY = Pattern.compile("^\\[(.*)\\]$");
    
    private JcrXmlValueConverter() {
        // static methods only
    }
    
    /**
     * Parse JSON value from XML Attribute.
     * @param value XML attribute value
     * @return Value object
     */
    public static Object parseValue(final String rawValue) {
        String value = rawValue;
        String[] valueArray = null;
        
        if (rawValue == null) {
            return null;
        }
        
        // detect type prefix
        String typePrefix = null;
        Matcher typePrefixMatcher = TYPE_PREFIX.matcher(value);
        if (typePrefixMatcher.matches()) {
            typePrefix = typePrefixMatcher.group(1);
            value = typePrefixMatcher.group(2);
        }
        
        // check for array
        Matcher arrayMatcher = VALUE_ARRAY.matcher(value);
        if (arrayMatcher.matches()) {
            value = null;
            valueArray = splitPreserveAllTokens(arrayMatcher.group(1), ',');
        }

        // convert values
        if (valueArray != null) {
            Object[] result = new Object[valueArray.length];
            for (int i=0; i<valueArray.length; i++) {
                result[i] = convertValue(valueArray[i], typePrefix, true);
            }
            return result;
        }
        else {
            return convertValue(value, typePrefix, false);
        }
    }
    
    /**
     * Split string preserving all tokens - but ignore separators that are escaped with \.
     * @param str Combined string
     * @param sep Separator
     * @return Tokens
     */
    private static String[] splitPreserveAllTokens(String str, char sep) {
        final int len = str.length();
        if (len == 0) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        final List<String> list = new ArrayList<String>();
        int i = 0, start = 0;
        boolean match = false;
        boolean lastMatch = false;
        boolean escaped = false;
        while (i < len) {
            if (str.charAt(i) == '\\' && !escaped) {
                escaped = true;
            }
            else {
                if (str.charAt(i) == sep && !escaped) {
                    lastMatch = true;
                    list.add(str.substring(start, i));
                    match = false;
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                escaped = false;
            }
            i++;
        }
        if (match || lastMatch) {
            list.add(str.substring(start, i));
        }
        return list.toArray(new String[list.size()]);        
    }
    
    /**
     * Parse value depending on type prefix.
     * @param value Value
     * @param typePrefix Type prefix
     * @param inArray Value is in array
     * @return Value object
     */
    private static Object convertValue(final String value, final String typePrefix, final boolean inArray) {
        if (typePrefix == null || StringUtils.equals(typePrefix, "Name")) {
            return deescapeStringValue(value, inArray);
        }
        else if (StringUtils.equals(typePrefix, "Boolean")) {
            return Boolean.valueOf(value);
        }
        else if (StringUtils.equals(typePrefix, "Long")) {
            return Long.valueOf(value);
        }
        else if (StringUtils.equals(typePrefix, "Decimal")) {
            return Double.valueOf(value);
        }
        else if (StringUtils.equals(typePrefix, "Date")) {
            return ISO8601.parse(value);
        }
        else {
            throw new IllegalArgumentException("Unexpected type prefix: " + typePrefix);
        }
    }
    
    /**
     * De-escape string value.
     * @param value Escaped string value
     * @param inArray In array
     * @return De-escaped string value
     */
    private static String deescapeStringValue(final String value, final boolean inArray) {
        String descapedValue = value;
        if (inArray) {
          descapedValue = StringUtils.replace(descapedValue, "\\,", ",");
        }
        else if (StringUtils.startsWith(descapedValue, "\\{") || StringUtils.startsWith(descapedValue, "\\[")) {
            descapedValue = descapedValue.substring(1);
        }
        return StringUtils.replace(descapedValue, "\\\\", "\\");
    }
        
}
