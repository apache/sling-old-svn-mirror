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
package org.apache.sling.jcr.jcrinstall.osgi.impl.propertyconverter;

import java.util.ArrayList;

/** Convert arrays of Strings */
class StringArrayConverter implements ValueConverter {

    public boolean appliesTo(String key) {
        return key.endsWith("[]") || key.endsWith("[string]");
    }

    public PropertyValue convert(String key, String value) throws ValueConverterException {
        PropertyValue result = null;
        
        final int pos = key.lastIndexOf('[');
        key = key.substring(0, pos).trim();
        
        if(value.trim().length() == 0) {
            result = new PropertyValue(key, new String[0]);
        } else {
            result = new PropertyValue(key, splitWithEscapes(value, ',')); 
        }
        
        return result;
    }

    /** Split string, ignoring separators that directly follow a backslash.
     *  All values are trimmed
     */
    static String [] splitWithEscapes(String str, char separator) {
        final ArrayList<String> a = new ArrayList<String>();
        StringBuffer current = new StringBuffer();
        char lastChar = 0;
        for(int i=0; i < str.length(); i++) {
            final char c = str.charAt(i);
            
            if(c == separator) {
                if(lastChar == '\\') {
                    // replace lastchar with c
                    current.setCharAt(current.length() - 1, c);
                } else if(current.length() > 0) {
                    a.add(current.toString());
                    current = new StringBuffer();
                }
            } else {
                current.append(c);
            }
            
            lastChar = c;
        }
        if(current.length() > 0) {
            a.add(current.toString());
        }
        
        final String [] result = new String[a.size()];
        int i=0;
        for(String s : a) {
            result[i++] = s.trim();
        }
        return result;
    }
}
