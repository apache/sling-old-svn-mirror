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
package org.apache.sling.contextaware.config.impl.metadata;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.RankedServices;
import org.apache.sling.contextaware.config.spi.ConfigurationMetadataProvider;
import org.apache.sling.contextaware.config.spi.metadata.ConfigurationMetadata;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * Detects all {@link ConfigurationMetadataProvider} implementations in the container
 * and consolidates their result based on service ranking.
 */
@Component(service = ConfigurationMetadataProviderMultiplexer.class,
reference={
        @Reference(name="configurationMetadataProvider", service=ConfigurationMetadataProvider.class,
                bind="bindConfigurationMetadataProvider", unbind="unbindConfigurationMetadataProvider",
                cardinality=ReferenceCardinality.MULTIPLE,
                policy=ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
})
public class ConfigurationMetadataProviderMultiplexer implements ConfigurationMetadataProvider {
    
    private RankedServices<ConfigurationMetadataProvider> items = new RankedServices<>(Order.DESCENDING);
        
    protected void bindConfigurationMetadataProvider(ConfigurationMetadataProvider configurationMetadataProvider, Map<String, Object> props) {
        items.bind(configurationMetadataProvider, props);
    }
    
    protected void unbindConfigurationMetadataProvider(ConfigurationMetadataProvider configurationMetadataProvider, Map<String, Object> props) {
        items.unbind(configurationMetadataProvider, props);
    }

    /**
     * Merge configuration names from all providers.
     */
    @Override
    public SortedSet<String> getConfigurationNames() {
        SortedSet<String> configNames = new TreeSet<>();
        for (ConfigurationMetadataProvider item : items) {
            configNames.addAll(item.getConfigurationNames());
        }
        return configNames;
    }

    /**
     * Get configuration metadata from first provider (ranking priority) that has an answer.
     */
    @Override
    public ConfigurationMetadata getConfigurationMetadata(String configName) {
        for (ConfigurationMetadataProvider item : items) {
            ConfigurationMetadata configMetadata = item.getConfigurationMetadata(configName);
            if (configMetadata != null) {
                return configMetadata;
            }
        }
        return null;
    }

}
