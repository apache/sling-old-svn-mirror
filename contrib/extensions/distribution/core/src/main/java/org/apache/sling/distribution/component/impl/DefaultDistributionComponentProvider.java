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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.component.DistributionComponent;
import org.apache.sling.distribution.component.DistributionComponentProvider;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.queue.DistributionQueueDistributionStrategy;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.transport.authentication.TransportAuthenticationProvider;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * default implementation of {@link org.apache.sling.distribution.component.DistributionComponentProvider} (as an OSGi service).
 */
@Component
@Service(DistributionComponentProvider.class)
@Property(name = "name", value = "default")
@References({
        @Reference(name = "distributionAgent", referenceInterface = DistributionAgent.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "distributionPackageImporter", referenceInterface = DistributionPackageImporter.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "distributionPackageExporter", referenceInterface = DistributionPackageExporter.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "distributionQueueProvider", referenceInterface = DistributionQueueProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "distributionQueueDistributionStrategy", referenceInterface = DistributionQueueDistributionStrategy.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "transportAuthenticationProvider", referenceInterface = TransportAuthenticationProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
})
public class DefaultDistributionComponentProvider implements DistributionComponentProvider {

    public static final String COMPONENT_TYPE = "type";
    public static final String NAME = "name";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Map<String, DistributionAgent> distributionAgentMap = new ConcurrentHashMap<String, DistributionAgent>();
    private Map<String, DistributionQueueProvider> distributionQueueProviderMap = new ConcurrentHashMap<String, DistributionQueueProvider>();
    private Map<String, DistributionQueueDistributionStrategy> distributionQueueDistributionStrategyMap = new ConcurrentHashMap<String, DistributionQueueDistributionStrategy>();
    private Map<String, TransportAuthenticationProvider> transportAuthenticationProviderMap = new ConcurrentHashMap<String, TransportAuthenticationProvider>();
    private Map<String, DistributionPackageImporter> distributionPackageImporterMap = new ConcurrentHashMap<String, DistributionPackageImporter>();
    private Map<String, DistributionPackageExporter> distributionPackageExporterMap = new ConcurrentHashMap<String, DistributionPackageExporter>();
    private BundleContext bundleContext;

    public <ComponentType extends DistributionComponent> ComponentType getComponent(@Nonnull Class<ComponentType> type,
                                                                                   @Nullable String componentName) {
        if (type.isAssignableFrom(DistributionPackageExporter.class)) {
            return (ComponentType) distributionPackageExporterMap.get(componentName);
        } else if (type.isAssignableFrom(DistributionPackageImporter.class)) {
            return (ComponentType) distributionPackageImporterMap.get(componentName);
        } else if (type.isAssignableFrom(DistributionQueueProvider.class)) {
            return (ComponentType) distributionQueueProviderMap.get(componentName);
        } else if (type.isAssignableFrom(DistributionQueueDistributionStrategy.class)) {
            return (ComponentType) distributionQueueDistributionStrategyMap.get(componentName);
        } else if (type.isAssignableFrom(TransportAuthenticationProvider.class)) {
            return (ComponentType) transportAuthenticationProviderMap.get(componentName);
        }

        return null;
    }

    private void bindDistributionQueueProvider(DistributionQueueProvider distributionQueueProvider, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            distributionQueueProviderMap.put(name, distributionQueueProvider);
        }
    }

    private void unbindDistributionQueueProvider(DistributionQueueProvider distributionQueueProvider, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            distributionQueueProviderMap.remove(name);
        }
    }

    private void bindDistributionQueueDistributionStrategy(DistributionQueueDistributionStrategy distributionQueueDistributionStrategy, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            distributionQueueDistributionStrategyMap.put(name, distributionQueueDistributionStrategy);
        }
    }

    private void unbindDistributionQueueDistributionStrategy(DistributionQueueDistributionStrategy distributionQueueDistributionStrategy, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            distributionQueueDistributionStrategyMap.remove(name);
        }
    }

    private void bindTransportAuthenticationProvider(TransportAuthenticationProvider transportAuthenticationProvider, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            transportAuthenticationProviderMap.put(name, transportAuthenticationProvider);

        }

    }

    private void unbindTransportAuthenticationProvider(TransportAuthenticationProvider transportAuthenticationProvider, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            transportAuthenticationProviderMap.remove(name);

        }
    }

    private void bindDistributionPackageImporter(DistributionPackageImporter distributionPackageImporter, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            distributionPackageImporterMap.put(name, distributionPackageImporter);

        }
    }

    private void unbindDistributionPackageImporter(DistributionPackageImporter distributionPackageImporter, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            distributionPackageImporterMap.remove(name);
        }
    }

    private void bindDistributionPackageExporter(DistributionPackageExporter distributionPackageExporter, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            distributionPackageExporterMap.put(name, distributionPackageExporter);
        }
    }

    private void unbindDistributionPackageExporter(DistributionPackageExporter distributionPackageExporter, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            distributionPackageExporterMap.remove(name);
        }

    }

    private void bindDistributionAgent(DistributionAgent distributionAgent, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            distributionAgentMap.put(name, distributionAgent);
        }
    }

    private void unbindDistributionAgent(DistributionAgent distributionAgent, Map<String, Object> config) {

        String name = (String) config.get("name");
        if (name != null) {
            distributionAgentMap.remove(name);
        }

    }


}
