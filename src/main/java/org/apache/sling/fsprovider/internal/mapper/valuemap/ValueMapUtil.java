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
package org.apache.sling.fsprovider.internal.mapper.valuemap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ValueMap;

public final class ValueMapUtil {
    
    private ValueMapUtil() {
        // static methods only
    }
    
    /**
     * Convert map to value map.
     * @param content Content map.
     * @return Value map.
     */
    public static ValueMap toValueMap(Map<String,Object> content) {
        Map<String,Object> props = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : content.entrySet()) {
            if (entry.getValue() instanceof Collection) {
                // convert lists to arrays
                props.put(entry.getKey(), ((Collection)entry.getValue()).toArray());
            }
            else {
                props.put(entry.getKey(), entry.getValue());
            }
        }
        
        return new ValueMapDecorator(props);
    }

}
