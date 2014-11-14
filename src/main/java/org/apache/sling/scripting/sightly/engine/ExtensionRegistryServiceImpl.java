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
package org.apache.sling.scripting.sightly.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.scripting.sightly.api.RuntimeExtension;
import org.osgi.framework.Constants;

/**
 * Implementation for {@link ExtensionRegistryService}.
 */
@Component
@Service(ExtensionRegistryService.class)
@Reference(
        policy = ReferencePolicy.DYNAMIC,
        referenceInterface = RuntimeExtension.class,
        name = "extensionService",
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE
)
@SuppressWarnings("unused")
public class ExtensionRegistryServiceImpl implements ExtensionRegistryService {

    private volatile Map<String, RuntimeExtension> mapping = new HashMap<String, RuntimeExtension>();
    private Map<String, Integer> mappingPriorities = new HashMap<String, Integer>(10, 0.9f);

    @Override
    public Map<String, RuntimeExtension> extensions() {
        return mapping;
    }

    @SuppressWarnings("UnusedDeclaration")
    protected synchronized void bindExtensionService(RuntimeExtension extension, Map<String, Object> properties) {
        Integer newPriority = PropertiesUtil.toInteger(properties.get(Constants.SERVICE_RANKING), 0);
        Integer priority = PropertiesUtil.toInteger(mappingPriorities.get(extension.name()), 0);
        if (newPriority > priority) {
                mapping = Collections.unmodifiableMap(add(mapping, extension));
                mappingPriorities.put(extension.name(), newPriority);
        } else {
            if (!mapping.containsKey(extension.name())) {
                mapping = Collections.unmodifiableMap(add(mapping, extension));
                mappingPriorities.put(extension.name(), newPriority);
            }
        }

    }

    @SuppressWarnings("UnusedDeclaration")
    protected synchronized void unbindExtensionService(RuntimeExtension extension, Map<String, Object> properties) {
        mappingPriorities.remove(extension.name());
        mapping = Collections.unmodifiableMap(remove(mapping, extension));
    }

    private Map<String, RuntimeExtension> add(Map<String, RuntimeExtension> oldMap, RuntimeExtension extension) {
        HashMap<String, RuntimeExtension> newMap = new HashMap<String, RuntimeExtension>(oldMap);
        newMap.put(extension.name(), extension);
        return newMap;
    }

    private Map<String, RuntimeExtension> remove(Map<String, RuntimeExtension> oldMap, RuntimeExtension extension) {
        HashMap<String, RuntimeExtension> newMap = new HashMap<String, RuntimeExtension>(oldMap);
        newMap.remove(extension.name());
        return newMap;
    }
}
