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
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.component.DistributionComponent;
import org.apache.sling.distribution.component.DistributionComponentFactory;
import org.apache.sling.distribution.component.DistributionComponentProvider;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.sling.distribution.component.impl.DefaultDistributionComponentFactoryConstants.COMPONENT_NAME;
import static org.apache.sling.distribution.component.impl.DefaultDistributionComponentFactoryConstants.COMPONENT_TYPE;
import static org.apache.sling.distribution.component.impl.DefaultDistributionComponentFactoryConstants.COMPONENT_TYPE_SERVICE;

/**
 * Manager of all {@link DistributionComponentFactory}s. The manager iterates through all of them to create a suitable component.
 */
@Component
@References({
        @Reference(name = "distributionComponentFactory", referenceInterface = DistributionComponentFactory.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
})
@Service(DistributionComponentManager.class)
public class DistributionComponentFactoryManager implements DistributionComponentManager {


    @Reference
    private EventAdmin eventAdmin;

    Map<String, DistributionComponentFactory> distributionComponentFactoryMap = new ConcurrentHashMap<String, DistributionComponentFactory>();



    protected void bindDistributionComponentFactory(DistributionComponentFactory distributionComponentFactory, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            distributionComponentFactoryMap.put(name, distributionComponentFactory);
            postRefreshEvent();
        }
    }

    protected void unbindDistributionComponentFactory(DistributionComponentFactory distributionComponentFactory, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            distributionComponentFactoryMap.remove(name);
            postRefreshEvent();
        }
    }

    private void postRefreshEvent() {
        eventAdmin.postEvent(new Event(TOPIC_DISTRIBUTION_COMPONENT_REFRESH, null));
    }



    public <ComponentType extends DistributionComponent> ComponentType createComponent(@Nonnull Class<ComponentType> type,
                                                                                               @Nonnull Map<String, Object> properties,
                                                                                               final DistributionComponentProvider componentProvider) {
        return createComponentInternal(type, properties, componentProvider);

    }

    public <ComponentType extends DistributionComponent> ComponentType createComponent(@Nonnull Class<ComponentType> type,
                                                                                       @Nonnull Map<String, Object> properties) {
        return createComponentInternal(type, properties, null);

    }





    private  <ComponentType extends DistributionComponent> ComponentType createComponentInternal(@Nonnull Class<ComponentType> type,
                                                                                       @Nonnull Map<String, Object> properties,
                                                                                       final DistributionComponentProvider componentProvider) {

        // try to see if the required component is already available
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), COMPONENT_TYPE_SERVICE);
        if (componentProvider != null && COMPONENT_TYPE_SERVICE.equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(COMPONENT_NAME), null);

            return componentProvider.getComponent(type, name);
        }

        for (DistributionComponentFactory distributionComponentFactory : distributionComponentFactoryMap.values()) {
            ComponentType component = distributionComponentFactory.createComponent(type, properties, new DistributionComponentFactory() {
                public <ComponentType extends DistributionComponent> ComponentType createComponent(@Nonnull Class<ComponentType> type, @Nonnull Map<String, Object> properties, @Nullable DistributionComponentFactory subComponentFactory) {

                    // try to see if the required component is already available
                    String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), COMPONENT_TYPE_SERVICE);
                    if (componentProvider != null && COMPONENT_TYPE_SERVICE.equals(factory)) {
                        String name = PropertiesUtil.toString(properties.get(COMPONENT_NAME), null);

                        return componentProvider.getComponent(type, name);
                    }

                    return createComponentInternal(type, properties, componentProvider);
                }
            });
            if (component != null) {
                return component;
            }
        }

        return null;
    }
}
