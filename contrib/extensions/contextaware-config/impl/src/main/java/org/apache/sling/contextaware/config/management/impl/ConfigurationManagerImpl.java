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
package org.apache.sling.contextaware.config.management.impl;

import static org.apache.sling.contextaware.config.impl.ConfigurationNameConstants.CONFIGS_PARENT_NAME;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.contextaware.config.impl.ConfigurationPersistenceStrategyMultiplexer;
import org.apache.sling.contextaware.config.impl.metadata.ConfigurationMetadataProviderMultiplexer;
import org.apache.sling.contextaware.config.management.ConfigurationData;
import org.apache.sling.contextaware.config.management.ConfigurationManager;
import org.apache.sling.contextaware.config.resource.ConfigurationResourceResolver;
import org.apache.sling.contextaware.config.spi.metadata.ConfigurationMetadata;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = ConfigurationManager.class)
public class ConfigurationManagerImpl implements ConfigurationManager {
    
    @Reference
    private ConfigurationResourceResolver configurationResourceResolver;
    @Reference
    private ConfigurationMetadataProviderMultiplexer configurationMetadataProvider;
    @Reference
    private ConfigurationPersistenceStrategyMultiplexer configurationPersistenceStrategy;

    @Override
    public ConfigurationData getConfigurationData(Resource resource, String configName) {
        ConfigurationMetadata configMetadata = configurationMetadataProvider.getConfigurationMetadata(configName);
        Resource configResource = configurationResourceResolver.getResource(resource, CONFIGS_PARENT_NAME, configName);
        if (configResource != null) {
            return new ConfigurationDataImpl(configurationPersistenceStrategy.getResource(configResource), configMetadata);
        }
        if (configMetadata != null) {
            // if no config resource found but config metadata exist return empty config data with default values
            return new ConfigurationDataImpl(configMetadata);
        }
        return null;
    }

    @Override
    public Collection<ConfigurationData> getConfigurationDataCollection(Resource resource, String configName) {
        ConfigurationMetadata configMetadata = configurationMetadataProvider.getConfigurationMetadata(configName);
        Collection<Resource> configResources = configurationResourceResolver.getResourceCollection(resource, CONFIGS_PARENT_NAME, configName);
        List<ConfigurationData> configData = new ArrayList<>();
        for (Resource configResource : configResources) {
            configData.add(new ConfigurationDataImpl(configurationPersistenceStrategy.getResource(configResource), configMetadata));
        }
        return configData;
    }

    @Override
    public void writeConfigurationData(Resource resource, String configName, ValueMap values) {
        // TODO: implement
    }

    @Override
    public void writeConfigurationDataCollection(Resource resource, String configName, Collection<ValueMap> values) {
        // TODO: implement
    }

    @Override
    public ConfigurationData newConfigurationDataItem(String configName) {
        ConfigurationMetadata configMetadata = configurationMetadataProvider.getConfigurationMetadata(configName);
        if (configMetadata != null) {
            return new ConfigurationDataImpl(configMetadata);
        }
        return null;
    }

}
