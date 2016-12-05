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

import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.caconfig.spi.ConfigurationCollectionPersistData;
import org.apache.sling.caconfig.spi.ConfigurationPersistData;
import org.apache.sling.caconfig.spi.ConfigurationPersistenceStrategy;
import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.RankedServices;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * Detects all {@link ConfigurationPersistenceStrategy} implementations in the container
 * and consolidates their result based on service ranking.
 */
@Component(service = ConfigurationPersistenceStrategyMultiplexer.class,
reference={
        @Reference(name="configurationResourcePersistenceStrategy", service=ConfigurationPersistenceStrategy.class,
                bind="bindConfigurationResourcePersistenceStrategy", unbind="unbindConfigurationResourcePersistenceStrategy",
                cardinality=ReferenceCardinality.MULTIPLE,
                policy=ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
})
public class ConfigurationPersistenceStrategyMultiplexer implements ConfigurationPersistenceStrategy {
    
    private RankedServices<ConfigurationPersistenceStrategy> items = new RankedServices<>(Order.DESCENDING);
        
    protected void bindConfigurationResourcePersistenceStrategy(ConfigurationPersistenceStrategy configurationResourcePersistenceStrategy, Map<String, Object> props) {
        items.bind(configurationResourcePersistenceStrategy, props);
    }
    
    protected void unbindConfigurationResourcePersistenceStrategy(ConfigurationPersistenceStrategy configurationResourcePersistenceStrategy, Map<String, Object> props) {
        items.unbind(configurationResourcePersistenceStrategy, props);
    }

    /**
     * Transform the configuration resource by the first implementation that has an answer.
     */    
    @Override
    public Resource getResource(Resource resource) {
        for (ConfigurationPersistenceStrategy item : items) {
            Resource result = item.getResource(resource);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public String getResourcePath(String resourcePath) {
        for (ConfigurationPersistenceStrategy item : items) {
            String result = item.getResourcePath(resourcePath);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Persist configuration data with the first implementation that accepts it.
     */
    @Override
    public boolean persistConfiguration(ResourceResolver resourceResolver, String configResourcePath,
            ConfigurationPersistData data) {
        for (ConfigurationPersistenceStrategy item : items) {
            if (item.persistConfiguration(resourceResolver, configResourcePath, data)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Persist configuration data with the first implementation that accepts it.
     */
    @Override
    public boolean persistConfigurationCollection(ResourceResolver resourceResolver, String configResourceCollectionParentPath,
            ConfigurationCollectionPersistData data) {
        for (ConfigurationPersistenceStrategy item : items) {
            if (item.persistConfigurationCollection(resourceResolver, configResourceCollectionParentPath, data)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean deleteConfiguration(ResourceResolver resourceResolver, String configResourcePath) {
        for (ConfigurationPersistenceStrategy item : items) {
            if (item.deleteConfiguration(resourceResolver, configResourcePath)) {
                return true;
            }
        }
        return false;
    }
    
    

}
