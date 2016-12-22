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
package org.apache.sling.caconfig.resource.impl.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.resource.ValueMap;

public final class PropertyUtil {
    
    private PropertyUtil() {
        // static methods only
    }

    /**
     * Get boolean value from value map with key, or with alternative keys if not set.
     * @param valueMap Value map
     * @param key Primary key
     * @param additionalKeys Alternative keys
     * @return Value
     */
    public static boolean getBooleanValueAdditionalKeys(final ValueMap valueMap, final String key, final String[] additionalKeys) {
        Boolean result = valueMap.get(key, Boolean.class);
        if ( result == null && !ArrayUtils.isEmpty(additionalKeys) ) {
            for(final String name : additionalKeys) {
                result = valueMap.get(name, Boolean.class);
                if ( result != null ) {
                    break;
                }
            }
        }
        return result == null ? false : result.booleanValue();
    }

}
