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
package org.apache.sling.testing.mock.osgi;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.OsgiMetadata;

/**
 * Map util methods.
 */
final class MapUtil {

    public static <T, U> Dictionary<T, U> toDictionary(Map<T, U> map) {
        if (map == null) {
            return null;
        }
        return new Hashtable<T, U>(map);
    }

    public static <T, U> Map<T, U> toMap(Dictionary<T, U> dictionary) {
        if (dictionary == null) {
            return null;
        }
        Map<T, U> map = new HashMap<T, U>();
        Enumeration<T> keys = dictionary.keys();
        while (keys.hasMoreElements()) {
            T key = keys.nextElement();
            map.put(key, dictionary.get(key));
        }
        return map;
    }

    public static Dictionary<String, Object> propertiesMergeWithOsgiMetadata(Object target, Dictionary<String, Object> properties) {
        Dictionary<String, Object> mergedProperties = new Hashtable<String, Object>();
        
        OsgiMetadata metadata = OsgiMetadataUtil.getMetadata(target.getClass());
        if (metadata != null && metadata.getProperties() != null) {
            for (Map.Entry<String, Object> entry : metadata.getProperties().entrySet()) {
                mergedProperties.put(entry.getKey(), entry.getValue());
            }
        }
        
        if (properties != null) {
            Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                mergedProperties.put(key, properties.get(key));
            }
        }
        
        return mergedProperties;
    }
    
    public static Map<String, Object> propertiesMergeWithOsgiMetadata(Object target, Map<String, Object> properties) {
        Map<String, Object> mergedProperties = new HashMap<String, Object>();
        
        OsgiMetadata metadata = OsgiMetadataUtil.getMetadata(target.getClass());
        if (metadata != null && metadata.getProperties() != null) {
            mergedProperties.putAll(metadata.getProperties());
        }
        
        if (properties != null) {
            mergedProperties.putAll(properties);
        }
        
        return mergedProperties;
    }
    
}
