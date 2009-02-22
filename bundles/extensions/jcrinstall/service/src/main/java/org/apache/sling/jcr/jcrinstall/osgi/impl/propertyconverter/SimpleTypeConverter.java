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

/** Base class for simple type conversions using (type) marker */
abstract class SimpleTypeConverter implements ValueConverter {

    private final String typePattern;
    
    SimpleTypeConverter(String typeName) {
        this.typePattern = "(" + typeName + ")";
    }
    
    public boolean appliesTo(String key) {
        return key.endsWith(typePattern);
    }

    public PropertyValue convert(final String key, String value) throws ValueConverterException {
        final int pos = key.indexOf(typePattern);
        final String modifiedKey = key.substring(0, pos).trim();
        return new PropertyValue(modifiedKey, convertValue(key, value));
    }
    
    protected abstract Object convertValue(String key, String value) throws ValueConverterException;
}
