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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Filter internal properties from ConfigManager API output.
 */
public final class PropertiesFilterUtil {

    private static final Set<String> PROPERTIES_TO_IGNORE = new HashSet<>(Arrays.asList(
            "jcr:primaryType",
            "jcr:mixinTypes",
            "jcr:created",
            "jcr:createdBy",
            "jcr:lastModified",
            "jcr:lastModifiedBy",
            "jcr:uuid"));
    
    private PropertiesFilterUtil() {
        // static methods only
    }

    public static void removeIgnoredProperties(Set<String> propertyNames) {
        propertyNames.removeAll(PROPERTIES_TO_IGNORE);
    }

    public static void removeIgnoredProperties(Map<String,Object> props) {
        for (String propertyName : PROPERTIES_TO_IGNORE) {
            props.remove(propertyName);
        }
    }
    
}
