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

/**
 * Map util methods.
 */
final class MapUtil {

    public static Dictionary<String, Object> toDictionary(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        return new Hashtable<String, Object>(map);
    }

    public static Map<String, Object> toMap(Dictionary<String, Object> dictionary) {
        if (dictionary == null) {
            return null;
        }
        Map<String,Object> map = new HashMap<String, Object>();
        Enumeration<String> keys = dictionary.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            map.put(key, dictionary.get(key));
        }
        return map;
    }

}
