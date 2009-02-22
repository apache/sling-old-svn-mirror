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
package org.apache.sling.jcr.jcrinstall.osgi.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.apache.sling.jcr.jcrinstall.osgi.impl.propertyconverter.PropertyConverter;
import org.apache.sling.jcr.jcrinstall.osgi.impl.propertyconverter.PropertyValue;
import org.apache.sling.jcr.jcrinstall.osgi.impl.propertyconverter.ValueConverterException;

/** Reads a Dictionary from an InputStream, using the
 *  syntax of the Properties class, enhanced to support
 *  multivalued properties and types supported by the PropertyConverter
 */
public class DictionaryReader {
    private final PropertyConverter converter = new PropertyConverter();
    
    /** Read Dictionary from the given InputStream,
     *  which is *not* closed before returning
     */
    public Dictionary<?,?> load(InputStream is) throws IOException {
        final Properties p = new Properties();
        p.load(is);
        return convert(p);
    }
    
    /** Convert Properties to Dictionary. Properties having
     *  a name that ends with [] are assumed to be comma-separated
     *  lists of values, and are converted to an Array. The []
     *  is removed from the property name. 
     */
    public Dictionary<?,?> convert(Properties p) throws ValueConverterException {
        final Hashtable <String, Object> result = new Hashtable<String, Object>();
        
        for(Map.Entry<Object, Object> e : p.entrySet()) {
            final PropertyValue v = converter.convert((String)e.getKey(), (String)e.getValue());
            result.put(v.getKey(), v.getValue());
        }
        
        return result;
    }
}
