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
package org.apache.sling.caconfig.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.ResettableListIterator;
import org.apache.commons.collections.iterators.ListIteratorWrapper;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.spi.ConfigurationInheritanceStrategy;
import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.RankedServices;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * Detects all {@link ConfigurationInheritanceStrategy} implementations in the container
 * and consolidates their result based on service ranking.
 */
@Component(service = ConfigurationInheritanceStrategyMultiplexer.class,
reference={
        @Reference(name="configurationInheritanceStrategy", service=ConfigurationInheritanceStrategy.class,
                bind="bindConfigurationInheritanceStrategy", unbind="unbindConfigurationInheritanceStrategy",
                cardinality=ReferenceCardinality.MULTIPLE,
                policy=ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
})
public class ConfigurationInheritanceStrategyMultiplexer implements ConfigurationInheritanceStrategy {

    private RankedServices<ConfigurationInheritanceStrategy> items = new RankedServices<>(Order.DESCENDING);
    
    protected void bindConfigurationInheritanceStrategy(ConfigurationInheritanceStrategy item, Map<String, Object> props) {
        items.bind(item, props);
    }
    
    protected void unbindConfigurationInheritanceStrategy(ConfigurationInheritanceStrategy item, Map<String, Object> props) {
        items.unbind(item, props);
    }

    /**
     * Get result from first strategy implementation that has an answer.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Resource getResource(Iterator<Resource> configResources) {
        List<ConfigurationInheritanceStrategy> itemList = items.getList();
        if (itemList.isEmpty()) {
            return null;
        }
        else if (itemList.size() == 1) {
            return itemList.get(0).getResource(configResources);
        }
        else {
            ResettableListIterator resettableConfigResources = new ListIteratorWrapper(configResources);
            for (ConfigurationInheritanceStrategy item : items) {
                Resource result = item.getResource(resettableConfigResources);
                if (result != null) {
                    return result;
                }
                else {
                    resettableConfigResources.reset();
                }
            }
            return null;
        }
    }

}
