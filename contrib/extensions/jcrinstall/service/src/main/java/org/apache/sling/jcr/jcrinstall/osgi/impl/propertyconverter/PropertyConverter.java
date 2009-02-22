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
import java.util.List;

/** Convert key/value Strings to various data types, used
 *  for SLING-707 to enhance the Properties format with multi-value
 *  and typed properties. 
 */
public class PropertyConverter {
    private final List<ValueConverter> converters;
    
    public PropertyConverter() {
        converters = new ArrayList<ValueConverter>();
        
        converters.add(new StringArrayConverter());
        
        converters.add(new SimpleTypeConverter("boolean") {
            protected Object convertValue(String key, String value) throws ValueConverterException {
                try {
                    return Boolean.valueOf(value);
                } catch(Exception e) {
                    throw new ValueConverterException("Invalid Boolean value", key, value);
                }
            }
        });
        
        converters.add(new SimpleTypeConverter("integer") {
            protected Object convertValue(String key, String value) throws ValueConverterException {
                try {
                    return Integer.valueOf(value);
                } catch(Exception e) {
                    throw new ValueConverterException("Invalid Integer value", key, value);
                }
            }
        });
        
        converters.add(new SimpleTypeConverter("double") {
            protected Object convertValue(String key, String value) throws ValueConverterException {
                try {
                    return Double.valueOf(value);
                } catch(Exception e) {
                    throw new ValueConverterException("Invalid Double value", key, value);
                }
            }
        });
        
        // This must be the last one
        converters.add(new DefaultConverter());
    }
    
    public PropertyValue convert(String key, String value) throws ValueConverterException {
        PropertyValue result = null;
        
        for(ValueConverter vc : converters) {
            if(vc.appliesTo(key)) {
                result = vc.convert(key, value);
                break;
            }
        }
        
        return result;
    }
    
}
