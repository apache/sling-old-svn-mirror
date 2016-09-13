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
package org.apache.sling.contextaware.config.resource.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.RankedServices;
import org.apache.sling.contextaware.config.resource.spi.ConfigurationResourceResolvingStrategy;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * Detects all {@link ConfigurationResourceResolvingStrategy} implementations in the container
 * and consolidates their result based on service ranking.
 */
@Component(service = ConfigurationResourceResolvingStrategyMultiplexer.class,
reference={
        @Reference(name="configurationResourceResolvingStrategy", service=ConfigurationResourceResolvingStrategy.class,
                bind="bindConfigurationResourceResolvingStrategy", unbind="unbindConfigurationResourceResolvingStrategy",
                cardinality=ReferenceCardinality.MULTIPLE,
                policy=ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
})
public class ConfigurationResourceResolvingStrategyMultiplexer implements ConfigurationResourceResolvingStrategy {
    
    private RankedServices<ConfigurationResourceResolvingStrategy> items = new RankedServices<>(Order.DESCENDING);
        
    protected void bindConfigurationResourceResolvingStrategy(ConfigurationResourceResolvingStrategy contextPathStrategy, Map<String, Object> props) {
        items.bind(contextPathStrategy, props);
    }
    
    protected void unbindConfigurationResourceResolvingStrategy(ConfigurationResourceResolvingStrategy contextPathStrategy, Map<String, Object> props) {
        items.unbind(contextPathStrategy, props);
    }

    /**
     * Gets the configuration resource from the first implementation that has an answer.
     */
    @Override
    public Resource getResource(Resource resource, String bucketName, String configName) {
        for (ConfigurationResourceResolvingStrategy item : items) {
            Resource result = item.getResource(resource, bucketName, configName);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Merge the configuration resources from all implementations into a combined list.
     */
    @Override
    public Collection<Resource> getResourceCollection(Resource resource, String bucketName, String configName) {
        List<Resource> result = new ArrayList<>();
        for (ConfigurationResourceResolvingStrategy item : items) {
            result.addAll(item.getResourceCollection(resource, bucketName, configName));
        }
        return result;
    }

}
