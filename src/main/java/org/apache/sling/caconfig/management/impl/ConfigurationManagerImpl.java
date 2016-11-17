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

import static org.apache.sling.caconfig.impl.ConfigurationNameConstants.CONFIGS_PARENT_NAME;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.ResettableIterator;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.iterators.ListIteratorWrapper;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.impl.ConfigurationInheritanceStrategyMultiplexer;
import org.apache.sling.caconfig.impl.metadata.ConfigurationMetadataProviderMultiplexer;
import org.apache.sling.caconfig.management.ConfigurationData;
import org.apache.sling.caconfig.management.ConfigurationManager;
import org.apache.sling.caconfig.resource.impl.ConfigurationResourceResolvingStrategyMultiplexer;
import org.apache.sling.caconfig.spi.ConfigurationPersistenceException;
import org.apache.sling.caconfig.spi.metadata.ConfigurationMetadata;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = ConfigurationManager.class)
public class ConfigurationManagerImpl implements ConfigurationManager {
    
    @Reference
    private ConfigurationResourceResolvingStrategyMultiplexer configurationResourceResolvingStrategy;
    @Reference
    private ConfigurationMetadataProviderMultiplexer configurationMetadataProvider;
    @Reference
    private ConfigurationPersistenceStrategyMultiplexer configurationPersistenceStrategy;
    @Reference
    private ConfigurationInheritanceStrategyMultiplexer configurationInheritanceStrategy;

    @SuppressWarnings("unchecked")
    @Override
    public ConfigurationData get(Resource resource, String configName) {
        ConfigurationMetadata configMetadata = configurationMetadataProvider.getConfigurationMetadata(configName);
        Iterator<Resource> configResourceInheritanceChain = configurationResourceResolvingStrategy
                .getResourceInheritanceChain(resource, CONFIGS_PARENT_NAME, configName);
        ResettableIterator resettableConfigResourceInheritanceChain = new ListIteratorWrapper(configResourceInheritanceChain);
        Resource configResource = applyPersistenceAndInheritance(resettableConfigResourceInheritanceChain);
        if (configResource != null) {
            // get writeback resource for "reverse inheritance detection"
            Resource writebackConfigResource = null;
            String writebackConfigResourcePath = configurationResourceResolvingStrategy.getResourcePath(resource, CONFIGS_PARENT_NAME, configName);
            if (writebackConfigResourcePath != null) {
                writebackConfigResource = configResource.getResourceResolver().getResource(writebackConfigResourcePath);
                if (writebackConfigResource != null) {
                    writebackConfigResource = configurationPersistenceStrategy.getResource(writebackConfigResource);
                }
            }
            resettableConfigResourceInheritanceChain.reset();
            return new ConfigurationDataImpl(configMetadata, configResource, writebackConfigResource,
                    applyPersistence(resettableConfigResourceInheritanceChain));
        }
        if (configMetadata != null) {
            // if no config resource found but config metadata exist return empty config data with default values
            return new ConfigurationDataImpl(configMetadata);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<ConfigurationData> getCollection(Resource resource, String configName) {
        ConfigurationMetadata configMetadata = configurationMetadataProvider.getConfigurationMetadata(configName);
        String writebackConfigResourceCollectionParentPath = configurationResourceResolvingStrategy.getResourceCollectionParentPath(resource, CONFIGS_PARENT_NAME, configName);
        List<ConfigurationData> configData = new ArrayList<>();

        Collection<Iterator<Resource>> configResourceInheritanceChains = configurationResourceResolvingStrategy
                .getResourceCollectionInheritanceChain(resource, CONFIGS_PARENT_NAME, configName);
        for (Iterator<Resource> configResourceInheritanceChain : configResourceInheritanceChains) {
            ResettableIterator resettableConfigResourceInheritanceChain = new ListIteratorWrapper(configResourceInheritanceChain);
            Resource configResource = applyPersistenceAndInheritance(resettableConfigResourceInheritanceChain);
            if (configResource != null) {
                // get writeback resource for "reverse inheritance detection"
                Resource writebackConfigResource = null;
                if (writebackConfigResourceCollectionParentPath != null) {
                    resettableConfigResourceInheritanceChain.reset();
                    Resource untransformedConfigResource = (Resource)resettableConfigResourceInheritanceChain.next();
                    writebackConfigResource = configResource.getResourceResolver().getResource(
                            writebackConfigResourceCollectionParentPath + "/" + untransformedConfigResource.getName());
                    if (writebackConfigResource != null) {
                        writebackConfigResource = configurationPersistenceStrategy.getResource(writebackConfigResource);
                    }
                }
                resettableConfigResourceInheritanceChain.reset();
                configData.add(new ConfigurationDataImpl(configMetadata, configResource, writebackConfigResource,
                        applyPersistence(resettableConfigResourceInheritanceChain)));
            }
        }
        return configData;
    }
    
    @SuppressWarnings("unchecked")
    private Iterator<Resource> applyPersistence(Iterator<Resource> configResourceInheritanceChain) {
        if (configResourceInheritanceChain == null) {
            return null;
        }
        return IteratorUtils.transformedIterator(configResourceInheritanceChain,
                new Transformer() {
                    @Override
                    public Object transform(Object input) {
                        return configurationPersistenceStrategy.getResource((Resource)input);
                    }
                });
    }

    private Resource applyPersistenceAndInheritance(Iterator<Resource> configResourceInheritanceChain) {
        if (configResourceInheritanceChain == null) {
            return null;
        }
        
        // apply configuration persistence transformation
        Iterator<Resource> transformedConfigResources = applyPersistence(configResourceInheritanceChain);
        
        // apply resource inheritance
        return configurationInheritanceStrategy.getResource(transformedConfigResources);
    }

    @Override
    public void persist(Resource resource, String configName, Map<String,Object> values) {
        String configResourcePath = configurationResourceResolvingStrategy.getResourcePath(resource, CONFIGS_PARENT_NAME, configName);
        if (configResourcePath == null) {
            throw new ConfigurationPersistenceException("Unable to persist configuration: Configuration resolving strategy returned no path.");
        }
        if (!configurationPersistenceStrategy.persist(resource.getResourceResolver(), configResourcePath, values)) {
            throw new ConfigurationPersistenceException("Unable to persist configuration: No persistence strategy found.");
        }
    }

    @Override
    public void persistCollection(Resource resource, String configName, Collection<Map<String,Object>> values) {
        String configResourceParentPath = configurationResourceResolvingStrategy.getResourceCollectionParentPath(resource, CONFIGS_PARENT_NAME, configName);
        if (configResourceParentPath == null) {
            throw new ConfigurationPersistenceException("Unable to persist configuration collection: Configuration resolving strategy returned no parent path.");
        }
        if (!configurationPersistenceStrategy.persistCollection(resource.getResourceResolver(), configResourceParentPath, values)) {
            throw new ConfigurationPersistenceException("Unable to persist configuration: No persistence strategy found.");
        }
    }

    @Override
    public ConfigurationData newCollectionItem(String configName) {
        ConfigurationMetadata configMetadata = configurationMetadataProvider.getConfigurationMetadata(configName);
        if (configMetadata != null) {
            return new ConfigurationDataImpl(configMetadata);
        }
        return null;
    }

}
