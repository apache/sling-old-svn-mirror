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
package org.apache.sling.hamcrest;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

/**
 * Map util methods.
 */
final class MapUtil {
    
    private MapUtil() {
        // static methods only
    }

    /**
     * Convert key/value pairs to map
     * @param args Key/value pairs
     * @return Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object... args) {
        if (args == null || args.length == 0) {
            return Collections.emptyMap();
        }
        if (args.length == 1) {
            if (args[0] instanceof Map) {
                return (Map)args[0];
            }
            else if (args[0] instanceof Dictionary) {
                return toMap((Dictionary)args[0]);
            }
        }
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("args must be an even number of name/values:" + Arrays.asList(args));
        }
        final Map<String, Object> result = new HashMap<String, Object>();
        for (int i=0 ; i < args.length; i+=2) {
            result.put(args[i].toString(), args[i+1]);
        }
        return result;
    }
    
}
