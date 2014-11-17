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
package org.apache.sling.distribution.component.impl;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.distribution.component.DistributionComponent;
import org.apache.sling.distribution.component.DistributionComponentFactory;


import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager of all {@link DistributionComponentFactory}s. The manager iterates through all of them to create a suitable component.
 */
@Component
@References({
        @Reference(name = "distributionComponentFactory", referenceInterface = DistributionComponentFactory.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
})
@Service(DistributionComponentFactoryManager.class)
public class DistributionComponentFactoryManager implements DistributionComponentFactory {



    Map<String, DistributionComponentFactory> distributionComponentFactoryMap = new ConcurrentHashMap<String, DistributionComponentFactory>();



    protected void bindDistributionComponentFactory(DistributionComponentFactory distributionComponentFactory, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            distributionComponentFactoryMap.put(name, distributionComponentFactory);
        }
    }

    protected void unbindDistributionComponentFactory(DistributionComponentFactory distributionComponentFactory, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            distributionComponentFactoryMap.remove(name);
        }
    }

    public <ComponentType extends DistributionComponent> ComponentType createComponent(@Nonnull Class<ComponentType> type, @Nonnull Map<String, Object> properties) {
        for (DistributionComponentFactory distributionComponentFactory : distributionComponentFactoryMap.values()) {
            ComponentType component = distributionComponentFactory.createComponent(type, properties);
            if (component != null) {
                return component;
            }
        }

        return null;
    }
}
