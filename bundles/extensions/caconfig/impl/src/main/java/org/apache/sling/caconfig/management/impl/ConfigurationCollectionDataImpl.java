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
package org.apache.sling.caconfig.management.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.caconfig.management.ConfigurationCollectionData;
import org.apache.sling.caconfig.management.ConfigurationData;
import org.apache.sling.caconfig.management.ConfigurationManagementSettings;
import org.apache.sling.caconfig.resource.impl.def.ConfigurationResourceNameConstants;

final class ConfigurationCollectionDataImpl implements ConfigurationCollectionData {
    
    private final String configName;
    private final Collection<ConfigurationData> items;
    private final String resourcePath;
    private final Map<String,Object> properties;
    private final ConfigurationManagementSettings configurationManagementSettings;
    private Map<String,Object> filteredPropertiesCache;
    
    public ConfigurationCollectionDataImpl(String configName, Collection<ConfigurationData> items,
            String resourcePath, Map<String, Object> properties,
            ConfigurationManagementSettings configurationManagementSettings) {
        this.configName = configName;
        this.items = items;
        this.resourcePath = resourcePath;
        this.properties = properties;
        this.configurationManagementSettings = configurationManagementSettings;
    }

    @Override
    public String getConfigName() {
        return configName;
    }

    @Override
    public Collection<ConfigurationData> getItems() {
        return items;
    }

    @Override
    public String getResourcePath() {
        return resourcePath;
    }

    @Override
    public Map<String, Object> getProperties() {
        if (filteredPropertiesCache == null) {
            filteredPropertiesCache = new HashMap<>();
            if (properties != null) {
                filteredPropertiesCache.putAll(properties);
                PropertiesFilterUtil.removeIgnoredProperties(filteredPropertiesCache, configurationManagementSettings);
            }
            else {
                filteredPropertiesCache.put(ConfigurationResourceNameConstants.PROPERTY_CONFIG_COLLECTION_INHERIT, true);
            }
        }
        return filteredPropertiesCache;
    }

}
