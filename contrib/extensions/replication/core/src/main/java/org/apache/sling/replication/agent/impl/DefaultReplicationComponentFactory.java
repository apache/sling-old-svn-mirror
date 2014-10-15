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
package org.apache.sling.replication.agent.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.ReplicationComponentFactory;
import org.apache.sling.replication.agent.ReplicationComponentProvider;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.packaging.ReplicationPackageExporter;
import org.apache.sling.replication.packaging.ReplicationPackageExporterStrategy;
import org.apache.sling.replication.packaging.ReplicationPackageImporter;
import org.apache.sling.replication.packaging.impl.exporter.AgentReplicationPackageExporter;
import org.apache.sling.replication.packaging.impl.exporter.LocalReplicationPackageExporter;
import org.apache.sling.replication.packaging.impl.exporter.RemoteReplicationPackageExporter;
import org.apache.sling.replication.packaging.impl.exporter.strategy.DefaultReplicationPackageExporterStrategy;
import org.apache.sling.replication.packaging.impl.exporter.strategy.PrivilegeReplicationPackageExporterStrategy;
import org.apache.sling.replication.packaging.impl.importer.LocalReplicationPackageImporter;
import org.apache.sling.replication.packaging.impl.importer.RemoteReplicationPackageImporter;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.impl.vlt.FileVaultReplicationPackageBuilder;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.transport.authentication.impl.UserCredentialsTransportAuthenticationProvider;
import org.apache.sling.replication.trigger.ReplicationTrigger;
import org.apache.sling.replication.trigger.impl.ChainReplicateReplicationTrigger;
import org.apache.sling.replication.trigger.impl.JcrEventReplicationTrigger;
import org.apache.sling.replication.trigger.impl.PersistingJcrEventReplicationTrigger;
import org.apache.sling.replication.trigger.impl.RemoteEventReplicationTrigger;
import org.apache.sling.replication.trigger.impl.ResourceEventReplicationTrigger;
import org.apache.sling.replication.trigger.impl.ScheduledReplicationTrigger;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A generic factory for replication components using a compact configuration, already existing OSGi services
 * for the components to be wired can be used as well as directly instantiated components (called by type name).
 * <p/>
 * Currently supported components are {@link org.apache.sling.replication.agent.ReplicationAgent}s,
 * {@link org.apache.sling.replication.trigger.ReplicationTrigger}s and
 * {@link org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider}s
 */
@Component(metatype = true,
        label = "Generic Replication Components Factory",
        description = "OSGi configuration Replication Component factory",
        specVersion = "1.1",
        immediate = true
)
@Service(ReplicationComponentFactory.class)
public class DefaultReplicationComponentFactory implements ReplicationComponentFactory, ReplicationComponentProvider {

    public static final String COMPONENT_TYPE = "type";
    public static final String NAME = "name";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ReplicationEventFactory replicationEventFactory;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private SlingRepository repository;

    @Reference
    private Packaging packaging;

    @Reference
    private Scheduler scheduler;

    private BundleContext bundleContext;

    @Activate
    private void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public <ComponentType> ComponentType createComponent(Class<ComponentType> type, Map<String, Object> properties,
                                                         ReplicationComponentProvider componentProvider) {

        if (componentProvider == null) {
            componentProvider = this;
        }
        try {
            if (type.isAssignableFrom(ReplicationAgent.class)) {
                return (ComponentType) createAgent(properties, componentProvider);
            } else if (type.isAssignableFrom(ReplicationTrigger.class)) {
                return (ComponentType) createTrigger(properties, componentProvider);
            } else if (type.isAssignableFrom(TransportAuthenticationProvider.class)) {
                return (ComponentType) createTransportAuthenticationProvider(properties, componentProvider);
            } else if (type.isAssignableFrom(ReplicationPackageImporter.class)) {
                return (ComponentType) createImporter(properties, componentProvider);
            } else if (type.isAssignableFrom(ReplicationPackageExporter.class)) {
                return (ComponentType) createExporter(properties, componentProvider);
            }
        }
        catch (IllegalArgumentException e) {
            log.debug("Cannot create component", e);
        }


        return null;
    }

    public ReplicationAgent createAgent(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {

        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), "simple");

        if ("simple".equals(factory)) {

            Map<String, Object> importerProperties = extractMap("packageImporter", properties);
            ReplicationPackageImporter packageImporter = createImporter(importerProperties, componentProvider);

            Map<String, Object> exporterProperties = extractMap("packageExporter", properties);
            ReplicationPackageExporter packageExporter = createExporter(exporterProperties, componentProvider);

            Map<String, Object> exporterStrategyProperties = extractMap("packageExporterStrategy", properties);
            ReplicationPackageExporterStrategy packageExporterStrategy = createExporterStrategy(exporterStrategyProperties, componentProvider);

            Map<String, Object> queueDistributionStrategyProperties = extractMap("queueDistributionStrategy", properties);
            ReplicationQueueDistributionStrategy queueDistributionStrategy = createDistributionStrategy(queueDistributionStrategyProperties, componentProvider);

            Map<String, Object> queueProviderProperties = extractMap("queueProvider", properties);
            ReplicationQueueProvider queueProvider = createQueueProvider(queueProviderProperties, componentProvider);

            List<Map<String, Object>> triggersProperties = extractMapList("trigger", properties);
            List<ReplicationTrigger> triggers = createTriggerList(triggersProperties, componentProvider);

            String name = PropertiesUtil.toString(properties.get(SimpleReplicationAgentFactory.NAME), String.valueOf(new Random().nextInt(1000)));

            String serviceName = PropertiesUtil.toString(properties.get(SimpleReplicationAgentFactory.SERVICE_NAME), null);


            boolean isPassive = PropertiesUtil.toBoolean(properties.get(SimpleReplicationAgentFactory.IS_PASSIVE), false);


            return new SimpleReplicationAgent(name, isPassive, serviceName,
                    packageImporter, packageExporter, packageExporterStrategy,
                    queueProvider, queueDistributionStrategy, replicationEventFactory, resourceResolverFactory, triggers);

        }

        return null;

    }

    private ReplicationPackageExporterStrategy createExporterStrategy(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {

        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), "service");

        if ("service".equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(NAME), null);
            return componentProvider.getComponent(ReplicationPackageExporterStrategy.class, name);

        }
        else if (DefaultReplicationPackageExporterStrategy.NAME.equals(factory)) {
            return new DefaultReplicationPackageExporterStrategy();
        }
        else if (PrivilegeReplicationPackageExporterStrategy.NAME.equals(factory)) {
            return new PrivilegeReplicationPackageExporterStrategy(properties);
        }

        return null;
    }


    public ReplicationPackageExporter createExporter(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {

        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), "service");

        if ("service".equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(NAME), null);
            return componentProvider.getComponent(ReplicationPackageExporter.class, name);

        } else if (LocalReplicationPackageExporter.NAME.equals(factory)) {
            Map<String, Object> builderProperties = extractMap("packageBuilder", properties);
            ReplicationPackageBuilder packageBuilder = createBuilder(builderProperties);
            return new LocalReplicationPackageExporter(packageBuilder);
        } else if (RemoteReplicationPackageExporter.NAME.equals(factory)) {
            Map<String, Object> authenticationProviderProperties = extractMap("authenticationProvider", properties);
            TransportAuthenticationProvider authenticationProvider = createTransportAuthenticationProvider(authenticationProviderProperties, componentProvider);

            Map<String, Object> builderProperties = extractMap("packageBuilder", properties);
            ReplicationPackageBuilder packageBuilder = createBuilder(builderProperties);

            return new  RemoteReplicationPackageExporter(properties, packageBuilder, authenticationProvider);
        } else if (AgentReplicationPackageExporter.NAME.equals(factory)) {
            Map<String, Object> builderProperties = extractMap("packageBuilder", properties);
            ReplicationPackageBuilder packageBuilder = createBuilder(builderProperties);

            Map<String, Object> agentProperties = extractMap("agent", properties);
            String agentName = PropertiesUtil.toString(agentProperties.get(NAME), null);
            ReplicationAgent agent = componentProvider.getComponent(ReplicationAgent.class, agentName);

            return new AgentReplicationPackageExporter(properties, agent, packageBuilder);
        }


        return null;
    }

    public ReplicationPackageImporter createImporter(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {

        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), "service");

        if ("service".equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(NAME), null);
            return componentProvider.getComponent(ReplicationPackageImporter.class, name);
        } else if (LocalReplicationPackageImporter.NAME.equals(factory)) {
            Map<String, Object> builderProperties = extractMap("packageBuilder", properties);
            ReplicationPackageBuilder packageBuilder = createBuilder(builderProperties);
            return new LocalReplicationPackageImporter(properties, packageBuilder, replicationEventFactory);
        } else if (RemoteReplicationPackageImporter.NAME.equals(factory)) {
            Map<String, Object> authenticationProviderProperties = extractMap("authenticationProvider", properties);
            TransportAuthenticationProvider authenticationProvider = createTransportAuthenticationProvider(authenticationProviderProperties, componentProvider);

            return new RemoteReplicationPackageImporter(properties, authenticationProvider);
        }

        return null;
    }

    public ReplicationQueueProvider createQueueProvider(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), "service");

        if ("service".equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(NAME), null);
            return componentProvider.getComponent(ReplicationQueueProvider.class, name);
        }

        return null;
    }

    public ReplicationQueueDistributionStrategy createDistributionStrategy(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), "service");

        if ("service".equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(NAME), null);
            return componentProvider.getComponent(ReplicationQueueDistributionStrategy.class, name);

        }

        return null;
    }

    public TransportAuthenticationProvider createTransportAuthenticationProvider(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), "service");

        if ("service".equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(NAME), null);
            return componentProvider.getComponent(TransportAuthenticationProvider.class, name);

        } else if ("user".equals(factory)) {
            return new UserCredentialsTransportAuthenticationProvider(properties);
        }

        return null;
    }

    public ReplicationPackageBuilder createBuilder(Map<String, Object> properties) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), "service");

        if (FileVaultReplicationPackageBuilder.NAME.equals(factory)) {
            return new FileVaultReplicationPackageBuilder(packaging, replicationEventFactory);
        }

        return null;
    }


    private ReplicationTrigger createTrigger(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), "service");

        if ("service".equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(NAME), null);
            return componentProvider.getComponent(ReplicationTrigger.class, name);

        } else if (RemoteEventReplicationTrigger.TYPE.equals(factory)) {
            Map<String, Object> authenticationProviderProperties = extractMap("authenticationProvider", properties);

            TransportAuthenticationProvider authenticationProvider = createTransportAuthenticationProvider(authenticationProviderProperties, componentProvider);
            return new RemoteEventReplicationTrigger(properties, authenticationProvider, scheduler);
        } else if (ResourceEventReplicationTrigger.TYPE.equals(factory)) {
            return new ResourceEventReplicationTrigger(properties, bundleContext);
        } else if (ScheduledReplicationTrigger.TYPE.equals(factory)) {
            return new ScheduledReplicationTrigger(properties, scheduler);
        } else if (ChainReplicateReplicationTrigger.TYPE.equals(factory)) {
            return new ChainReplicateReplicationTrigger(properties, bundleContext);
        } else if (JcrEventReplicationTrigger.TYPE.equals(factory)) {
            return new JcrEventReplicationTrigger(properties, repository);
        } else if (PersistingJcrEventReplicationTrigger.TYPE.equals(factory)) {
            return new PersistingJcrEventReplicationTrigger(properties, repository);
        }

        return null;
    }

    private List<ReplicationTrigger> createTriggerList(List<Map<String, Object>> triggersProperties, ReplicationComponentProvider componentProvider) {
        List<ReplicationTrigger> triggers = new ArrayList<ReplicationTrigger>();
        for (Map<String, Object> properties : triggersProperties) {
            triggers.add(createTrigger(properties, componentProvider));
        }

        return triggers;
    }

    Map<String, Object> extractMap(String key, Map<String, Object> objectMap) {
        Map<String, Object> map = SettingsUtils.extractMap(key, objectMap);
        return map == null ? new HashMap<String, Object>() : map;
    }

    List<Map<String, Object>> extractMapList(String key, Map<String, Object> objectMap) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (String mapKey : objectMap.keySet()) {
            if (mapKey.startsWith(key)) {
                result.add(SettingsUtils.extractMap(mapKey, objectMap));
            }
        }
        return result;
    }

    public <ComponentType> ComponentType getComponent(Class<ComponentType> type, String componentName) {
        return null;
    }
}
