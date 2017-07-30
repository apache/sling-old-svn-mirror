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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.agent.impl.DistributionRequestAuthorizationStrategy;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.apache.sling.distribution.trigger.DistributionTrigger;

/**
 * {@link DistributionComponentProvider} OSGi service.
 */
@Component
@Property(name = "name", value = "default")
@References({
        @Reference(name = "distributionAgent", referenceInterface = DistributionAgent.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "distributionPackageImporter", referenceInterface = DistributionPackageImporter.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "distributionPackageExporter", referenceInterface = DistributionPackageExporter.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "distributionQueueProvider", referenceInterface = DistributionQueueProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "distributionQueueDistributionStrategy", referenceInterface = DistributionQueueDispatchingStrategy.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "distributionTransportSecretProvider", referenceInterface = DistributionTransportSecretProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "distributionTrigger", referenceInterface = DistributionTrigger.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "distributionRequestAuthorizationStrategy", referenceInterface = DistributionRequestAuthorizationStrategy.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "distributionPackageBuilder", referenceInterface = DistributionPackageBuilder.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
})
@Service(DistributionComponentProvider.class)
public class DefaultDistributionComponentProvider implements DistributionComponentProvider {

    private static final String NAME = DistributionComponentConstants.PN_NAME;

    private final Map<String, DistributionComponent<DistributionAgent>> distributionAgentMap = new ConcurrentHashMap<String, DistributionComponent<DistributionAgent>>();

    private final Map<String, DistributionComponent<DistributionQueueProvider>> distributionQueueProviderMap = new ConcurrentHashMap<String, DistributionComponent<DistributionQueueProvider>>();

    private final Map<String, DistributionComponent<DistributionQueueDispatchingStrategy>> distributionQueueDistributionStrategyMap = new ConcurrentHashMap<String, DistributionComponent<DistributionQueueDispatchingStrategy>>();

    private final Map<String, DistributionComponent<DistributionTransportSecretProvider>> distributionTransportSecretProviderMap = new ConcurrentHashMap<String, DistributionComponent<DistributionTransportSecretProvider>>();

    private final Map<String, DistributionComponent<DistributionPackageImporter>> distributionPackageImporterMap = new ConcurrentHashMap<String, DistributionComponent<DistributionPackageImporter>>();

    private final Map<String, DistributionComponent<DistributionPackageExporter>> distributionPackageExporterMap = new ConcurrentHashMap<String, DistributionComponent<DistributionPackageExporter>>();

    private final Map<String, DistributionComponent<DistributionPackageBuilder>> distributionPackageBuilderMap = new ConcurrentHashMap<String, DistributionComponent<DistributionPackageBuilder>>();

    private final Map<String, DistributionComponent<DistributionTrigger>> distributionTriggerMap = new ConcurrentHashMap<String, DistributionComponent<DistributionTrigger>>();

    private final Map<String, DistributionComponent<DistributionRequestAuthorizationStrategy>> distributionRequestAuthorizationStrategy = new ConcurrentHashMap<String, DistributionComponent<DistributionRequestAuthorizationStrategy>>();

    public DistributionComponent<?> getComponent(DistributionComponentKind kind, String componentName) {
        Map<String, DistributionComponent<?>> componentMap = getComponentMap(kind.asClass());
        return componentMap.get(componentName);
    }

    public List<DistributionComponent<?>> getComponents(DistributionComponentKind kind) {
        Map<String, DistributionComponent<?>> componentMap = getComponentMap(kind.asClass());

        List<DistributionComponent<?>> componentList = new ArrayList<DistributionComponent<?>>();
        componentList.addAll(componentMap.values());

        return componentList;
    }

    public <ComponentType> ComponentType getService(Class<ComponentType> type, String componentName) {
        Map<String, DistributionComponent<?>> componentMap = getComponentMap(type);
        DistributionComponent<?> component = componentMap.get(componentName);

        if (component == null) {
            return null;
        }

        // safe cast driven by the input type?
        return type.cast(component.getService());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Map<String, DistributionComponent<?>> getComponentMap(Class<?> type) {
        if (type.isAssignableFrom(DistributionAgent.class)) {
            return (Map) distributionAgentMap;
        } else if (type.isAssignableFrom(DistributionPackageExporter.class)) {
            return (Map) distributionPackageExporterMap;
        } else if (type.isAssignableFrom(DistributionPackageImporter.class)) {
            return (Map) distributionPackageImporterMap;
        } else if (type.isAssignableFrom(DistributionQueueProvider.class)) {
            return (Map) distributionQueueProviderMap;
        } else if (type.isAssignableFrom(DistributionQueueDispatchingStrategy.class)) {
            return (Map) distributionQueueDistributionStrategyMap;
        } else if (type.isAssignableFrom(DistributionTransportSecretProvider.class)) {
            return (Map) distributionTransportSecretProviderMap;
        } else if (type.isAssignableFrom(DistributionPackageBuilder.class)) {
            return (Map) distributionPackageBuilderMap;
        } else if (type.isAssignableFrom(DistributionTrigger.class)) {
            return (Map) distributionTriggerMap;
        } else if (type.isAssignableFrom(DistributionRequestAuthorizationStrategy.class)) {
            return (Map) distributionRequestAuthorizationStrategy;
        }

        throw new IllegalArgumentException(String.format("Components of type: %s are not supported", type));
    }

    // (un)binding methods

    public void bindDistributionQueueProvider(DistributionQueueProvider distributionQueueProvider, Map<String, Object> config) {
        put(DistributionQueueProvider.class, distributionQueueProvider, config);
    }

    public void unbindDistributionQueueProvider(DistributionQueueProvider distributionQueueProvider, Map<String, Object> config) {
        remove(DistributionQueueProvider.class, distributionQueueProvider, config);
    }

    public void bindDistributionQueueDistributionStrategy(DistributionQueueDispatchingStrategy distributionQueueDispatchingStrategy, Map<String, Object> config) {
        put(DistributionQueueDispatchingStrategy.class, distributionQueueDispatchingStrategy, config);
    }

    public void unbindDistributionQueueDistributionStrategy(DistributionQueueDispatchingStrategy distributionQueueDispatchingStrategy, Map<String, Object> config) {
        remove(DistributionQueueDispatchingStrategy.class, distributionQueueDispatchingStrategy, config);
    }

    public void bindDistributionTransportSecretProvider(DistributionTransportSecretProvider distributionTransportSecretProvider, Map<String, Object> config) {
        put(DistributionTransportSecretProvider.class, distributionTransportSecretProvider, config);
    }

    public void unbindDistributionTransportSecretProvider(DistributionTransportSecretProvider distributionTransportSecretProvider, Map<String, Object> config) {
        remove(DistributionTransportSecretProvider.class, distributionTransportSecretProvider, config);
    }

    public void bindDistributionPackageImporter(DistributionPackageImporter distributionPackageImporter, Map<String, Object> config) {
        put(DistributionPackageImporter.class, distributionPackageImporter, config);
    }

    public void unbindDistributionPackageImporter(DistributionPackageImporter distributionPackageImporter, Map<String, Object> config) {
        remove(DistributionPackageImporter.class, distributionPackageImporter, config);
    }

    public void bindDistributionPackageExporter(DistributionPackageExporter distributionPackageExporter, Map<String, Object> config) {
        put(DistributionPackageExporter.class, distributionPackageExporter, config);
    }

    public void unbindDistributionPackageExporter(DistributionPackageExporter distributionPackageExporter, Map<String, Object> config) {
        remove(DistributionPackageExporter.class, distributionPackageExporter, config);
    }

    public void bindDistributionAgent(DistributionAgent distributionAgent, Map<String, Object> config) {
        put(DistributionAgent.class, distributionAgent, config);
    }

    public void unbindDistributionAgent(DistributionAgent distributionAgent, Map<String, Object> config) {
        remove(DistributionAgent.class, distributionAgent, config);
    }

    public void bindDistributionPackageBuilder(DistributionPackageBuilder distributionPackageBuilder, Map<String, Object> config) {
        put(DistributionPackageBuilder.class, distributionPackageBuilder, config);
    }

    public void unbindDistributionPackageBuilder(DistributionPackageBuilder distributionPackageBuilder, Map<String, Object> config) {
        remove(DistributionPackageBuilder.class, distributionPackageBuilder, config);
    }

    public void bindDistributionTrigger(DistributionTrigger distributionTrigger, Map<String, Object> config) {
        put(DistributionTrigger.class, distributionTrigger, config);
    }

    public void unbindDistributionTrigger(DistributionTrigger distributionTrigger, Map<String, Object> config) {
        remove(DistributionTrigger.class, distributionTrigger, config);
    }

    public void bindDistributionRequestAuthorizationStrategy(DistributionRequestAuthorizationStrategy distributionRequestAuthorizationStrategy, Map<String, Object> config) {
        put(DistributionRequestAuthorizationStrategy.class, distributionRequestAuthorizationStrategy, config);
    }

    public void unbindDistributionRequestAuthorizationStrategy(DistributionRequestAuthorizationStrategy distributionRequestAuthorizationStrategy, Map<String, Object> config) {
        remove(DistributionRequestAuthorizationStrategy.class, distributionRequestAuthorizationStrategy, config);
    }


    // internals

    private <ComponentType> void put(Class<ComponentType> typeClass, ComponentType service, Map<String, Object> config) {
        Map<String, DistributionComponent<?>> componentMap = getComponentMap(typeClass);

        String name = PropertiesUtil.toString(config.get(NAME), null);
        DistributionComponentKind kind = DistributionComponentKind.fromClass(typeClass);
        if (name != null && kind != null) {
            componentMap.put(name, new DistributionComponent<ComponentType>(kind, name, service, config));
        }
    }

    private <ComponentType> void remove(Class<ComponentType> typeClass, ComponentType service, Map<String, Object> config) {
        Map<String, DistributionComponent<?>> componentMap = getComponentMap(typeClass);

        String name = PropertiesUtil.toString(config.get(NAME), null);
        if (name != null) {
            componentMap.remove(name);
        }
    }

}
