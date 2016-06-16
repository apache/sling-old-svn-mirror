/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.osgi.framework.Constants;

/**
 * Aggregator for all runtime extensions.
 */
@Component
@Service(ExtensionRegistryService.class)
@Reference(
        policy = ReferencePolicy.DYNAMIC,
        referenceInterface = RuntimeExtension.class,
        name = "extensionService",
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE
)
public class ExtensionRegistryService {

    private volatile Map<String, RuntimeExtension> mapping = new HashMap<String, RuntimeExtension>();
    private Map<String, Integer> mappingPriorities = new HashMap<String, Integer>(10, 0.9f);

    public Map<String, RuntimeExtension> extensions() {
        return mapping;
    }

    protected synchronized void bindExtensionService(RuntimeExtension extension, Map<String, Object> properties) {
        Integer newPriority = PropertiesUtil.toInteger(properties.get(Constants.SERVICE_RANKING), 0);
        String extensionName = PropertiesUtil.toString(properties.get(RuntimeExtension.NAME), "");
        Integer priority = PropertiesUtil.toInteger(mappingPriorities.get(extensionName), 0);
        if (newPriority > priority) {
                mapping = Collections.unmodifiableMap(add(mapping, extension, extensionName));
                mappingPriorities.put(extensionName, newPriority);
        } else {
            if (!mapping.containsKey(extensionName)) {
                mapping = Collections.unmodifiableMap(add(mapping, extension, extensionName));
                mappingPriorities.put(extensionName, newPriority);
            }
        }

    }

    protected synchronized void unbindExtensionService(RuntimeExtension extension, Map<String, Object> properties) {
        String extensionName = PropertiesUtil.toString(properties.get(RuntimeExtension.NAME), "");
        mappingPriorities.remove(extensionName);
        mapping = Collections.unmodifiableMap(remove(mapping, extensionName));
    }

    private Map<String, RuntimeExtension> add(Map<String, RuntimeExtension> oldMap, RuntimeExtension extension, String extensionName) {
        HashMap<String, RuntimeExtension> newMap = new HashMap<String, RuntimeExtension>(oldMap);
        newMap.put(extensionName, extension);
        return newMap;
    }

    private Map<String, RuntimeExtension> remove(Map<String, RuntimeExtension> oldMap, String extensionName) {
        HashMap<String, RuntimeExtension> newMap = new HashMap<String, RuntimeExtension>(oldMap);
        newMap.remove(extensionName);
        return newMap;
    }
}
