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
package org.apache.sling.replication.component.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import org.apache.sling.replication.agent.ReplicationRequestAuthorizationStrategy;
import org.apache.sling.replication.agent.impl.PrivilegeReplicationRequestAuthorizationStrategy;
import org.apache.sling.replication.agent.impl.SimpleReplicationAgent;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.component.ReplicationComponent;
import org.apache.sling.replication.component.ReplicationComponentFactory;
import org.apache.sling.replication.component.ReplicationComponentProvider;
import org.apache.sling.replication.event.impl.ReplicationEventFactory;
import org.apache.sling.replication.packaging.ReplicationPackageExporter;
import org.apache.sling.replication.packaging.ReplicationPackageImporter;
import org.apache.sling.replication.packaging.impl.exporter.AgentReplicationPackageExporter;
import org.apache.sling.replication.packaging.impl.exporter.LocalReplicationPackageExporter;
import org.apache.sling.replication.packaging.impl.exporter.RemoteReplicationPackageExporter;
import org.apache.sling.replication.packaging.impl.importer.LocalReplicationPackageImporter;
import org.apache.sling.replication.packaging.impl.importer.RemoteReplicationPackageImporter;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.impl.ResourceSharedReplicationPackageBuilder;
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
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public <ComponentType extends ReplicationComponent> ComponentType createComponent(@Nonnull Class<ComponentType> type,
                                                                                      @Nonnull Map<String, Object> properties,
                                                         @Nullable ReplicationComponentProvider componentProvider) {

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
            log.warn("Cannot create component of type {} with properties {}", new Object[]{type, properties}, e);
        }

        return null;
    }

    public ReplicationAgent createAgent(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {

        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), ReplicationComponentFactory.AGENT_SIMPLE);

        if (ReplicationComponentFactory.AGENT_SIMPLE.equals(factory)) {

            Map<String, Object> exporterProperties = extractMap(COMPONENT_PACKAGE_EXPORTER, properties);
            ReplicationPackageExporter packageExporter = createExporter(exporterProperties, componentProvider);

            Map<String, Object> importerProperties = extractMap(COMPONENT_PACKAGE_IMPORTER, properties);
            ReplicationPackageImporter packageImporter = createImporter(importerProperties, componentProvider);

            Map<String, Object> authorizationStrategyProperties = extractMap(COMPONENT_REQUEST_AUTHORIZATION_STRATEGY, properties);
            ReplicationRequestAuthorizationStrategy packageExporterStrategy = createAuthorizationStrategy(authorizationStrategyProperties, componentProvider);

            Map<String, Object> queueDistributionStrategyProperties = extractMap(COMPONENT_QUEUE_DISTRIBUTION_STRATEGY, properties);
            ReplicationQueueDistributionStrategy queueDistributionStrategy = createDistributionStrategy(queueDistributionStrategyProperties, componentProvider);

            Map<String, Object> queueProviderProperties = extractMap(COMPONENT_QUEUE_PROVIDER, properties);
            ReplicationQueueProvider queueProvider = createQueueProvider(queueProviderProperties, componentProvider);

            List<Map<String, Object>> triggersProperties = extractMapList(COMPONENT_TRIGGER, properties);
            List<ReplicationTrigger> triggers = createTriggerList(triggersProperties, componentProvider);

            String name = PropertiesUtil.toString(properties.get(COMPONENT_NAME), String.valueOf(new Random().nextInt(1000)));

            String serviceName = PropertiesUtil.toString(properties.get(ReplicationComponentFactory.AGENT_SIMPLE_PROPERTY_SERVICE_NAME), null);

            boolean isPassive = PropertiesUtil.toBoolean(properties.get(ReplicationComponentFactory.AGENT_SIMPLE_PROPERTY_IS_PASSIVE), false);


            return new SimpleReplicationAgent(name, isPassive, serviceName,
                    packageImporter, packageExporter, packageExporterStrategy,
                    queueProvider, queueDistributionStrategy, replicationEventFactory, resourceResolverFactory, triggers);

        }

        return null;

    }

    private ReplicationRequestAuthorizationStrategy createAuthorizationStrategy(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {

        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), COMPONENT_TYPE_SERVICE);

        if (COMPONENT_TYPE_SERVICE.equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(COMPONENT_NAME), null);
            return componentProvider.getComponent(ReplicationRequestAuthorizationStrategy.class, name);

        }
        else if (REQUEST_AUTHORIZATION_STRATEGY_PRIVILEGE.equals(factory)) {
            String jcrPrivilege = PropertiesUtil.toString(properties.get(REQUEST_AUTHORIZATION_STRATEGY_PRIVILEGE_PROPERTY_JCR_PRIVILEGE), null);
            return new PrivilegeReplicationRequestAuthorizationStrategy(jcrPrivilege);
        }

        return null;
    }


    public ReplicationPackageExporter createExporter(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {

        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), COMPONENT_TYPE_SERVICE);

        if (COMPONENT_TYPE_SERVICE.equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(COMPONENT_NAME), null);
            return componentProvider.getComponent(ReplicationPackageExporter.class, name);

        } else if (PACKAGE_EXPORTER_LOCAL.equals(factory)) {
            Map<String, Object> builderProperties = extractMap(COMPONENT_PACKAGE_BUILDER, properties);
            ReplicationPackageBuilder packageBuilder = createBuilder(builderProperties);
            return new LocalReplicationPackageExporter(packageBuilder);
        } else if (PACKAGE_EXPORTER_REMOTE.equals(factory)) {
            Map<String, Object> authenticationProviderProperties = extractMap(COMPONENT_TRANSPORT_AUTHENTICATION_PROVIDER, properties);
            TransportAuthenticationProvider authenticationProvider = createTransportAuthenticationProvider(authenticationProviderProperties, componentProvider);

            Map<String, Object> builderProperties = extractMap(COMPONENT_PACKAGE_BUILDER, properties);
            ReplicationPackageBuilder packageBuilder = createBuilder(builderProperties);


            String[] endpoints = PropertiesUtil.toStringArray(properties.get(PACKAGE_EXPORTER_REMOTE_PROPERTY_ENDPOINTS), new String[0]);
            String endpointStrategyName = PropertiesUtil.toString(properties.get(PACKAGE_EXPORTER_REMOTE_PROPERTY_ENDPOINTS_STRATEGY), "One");
            int pollItems = PropertiesUtil.toInteger(properties.get(PACKAGE_EXPORTER_REMOTE_PROPERTY_POLL_ITEMS), Integer.MAX_VALUE);

            return new  RemoteReplicationPackageExporter(packageBuilder, authenticationProvider, endpoints, endpointStrategyName, pollItems);
        } else if (PACKAGE_EXPORTER_AGENT.equals(factory)) {
            Map<String, Object> builderProperties = extractMap(COMPONENT_PACKAGE_BUILDER, properties);
            ReplicationPackageBuilder packageBuilder = createBuilder(builderProperties);
            ReplicationAgent agent = componentProvider.getComponent(ReplicationAgent.class, null);

            return new AgentReplicationPackageExporter(properties, agent, packageBuilder);
        }


        return null;
    }

    public ReplicationPackageImporter createImporter(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {

        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), COMPONENT_TYPE_SERVICE);

        if (COMPONENT_TYPE_SERVICE.equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(COMPONENT_NAME), null);
            return componentProvider.getComponent(ReplicationPackageImporter.class, name);
        } else if (PACKAGE_IMPORTER_LOCAL.equals(factory)) {
            Map<String, Object> builderProperties = extractMap(COMPONENT_PACKAGE_BUILDER, properties);
            ReplicationPackageBuilder packageBuilder = createBuilder(builderProperties);
            return new LocalReplicationPackageImporter(packageBuilder, replicationEventFactory);
        } else if (PACKAGE_IMPORTER_REMOTE.equals(factory)) {
            Map<String, Object> authenticationProviderProperties = extractMap(COMPONENT_TRANSPORT_AUTHENTICATION_PROVIDER, properties);
            TransportAuthenticationProvider authenticationProvider = createTransportAuthenticationProvider(authenticationProviderProperties, componentProvider);

            String[] endpoints = PropertiesUtil.toStringArray(properties.get(PACKAGE_EXPORTER_REMOTE_PROPERTY_ENDPOINTS), new String[0]);
            String endpointStrategyName = PropertiesUtil.toString(properties.get(PACKAGE_EXPORTER_REMOTE_PROPERTY_ENDPOINTS_STRATEGY), "One");

            return new RemoteReplicationPackageImporter(authenticationProvider, endpoints, endpointStrategyName);
        }

        return null;
    }

    public ReplicationQueueProvider createQueueProvider(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), COMPONENT_TYPE_SERVICE);

        if (COMPONENT_TYPE_SERVICE.equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(COMPONENT_NAME), null);
            return componentProvider.getComponent(ReplicationQueueProvider.class, name);
        }

        return null;
    }

    public ReplicationQueueDistributionStrategy createDistributionStrategy(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), COMPONENT_TYPE_SERVICE);

        if (COMPONENT_TYPE_SERVICE.equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(COMPONENT_NAME), null);
            return componentProvider.getComponent(ReplicationQueueDistributionStrategy.class, name);

        }

        return null;
    }

    public TransportAuthenticationProvider createTransportAuthenticationProvider(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), COMPONENT_TYPE_SERVICE);

        if (COMPONENT_TYPE_SERVICE.equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(COMPONENT_NAME), null);
            return componentProvider.getComponent(TransportAuthenticationProvider.class, name);

        } else if (TRANSPORT_AUTHENTICATION_PROVIDER_USER.equals(factory)) {
            String username = PropertiesUtil.toString(properties.get(TRANSPORT_AUTHENTICATION_PROVIDER_USER_PROPERTY_USERNAME), "").trim();
            String password = PropertiesUtil.toString(properties.get(TRANSPORT_AUTHENTICATION_PROVIDER_USER_PROPERTY_PASSWORD), "").trim();
            return new UserCredentialsTransportAuthenticationProvider(username, password);
        }

        return null;
    }

    public ReplicationPackageBuilder createBuilder(Map<String, Object> properties) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), COMPONENT_TYPE_SERVICE);

        if (PACKAGE_BUILDER_FILEVLT.equals(factory)) {
            return new ResourceSharedReplicationPackageBuilder(new FileVaultReplicationPackageBuilder(packaging, replicationEventFactory));
        }

        return null;
    }


    private ReplicationTrigger createTrigger(Map<String, Object> properties, ReplicationComponentProvider componentProvider) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), COMPONENT_TYPE_SERVICE);

        if (COMPONENT_TYPE_SERVICE.equals(factory)) {
            String name = PropertiesUtil.toString(properties.get(COMPONENT_NAME), null);
            return componentProvider.getComponent(ReplicationTrigger.class, name);

        } else if (TRIGGER_REMOTE_EVENT.equals(factory)) {
            Map<String, Object> authenticationProviderProperties = extractMap(COMPONENT_TRANSPORT_AUTHENTICATION_PROVIDER, properties);

            TransportAuthenticationProvider authenticationProvider = createTransportAuthenticationProvider(authenticationProviderProperties, componentProvider);
            String endpoint = PropertiesUtil.toString(properties.get(TRIGGER_REMOTE_EVENT_PROPERTY_ENDPOINT), null);

            return new RemoteEventReplicationTrigger(endpoint, authenticationProvider, scheduler);
        } else if (TRIGGER_RESOURCE_EVENT.equals(factory)) {
            String path = PropertiesUtil.toString(properties.get(TRIGGER_RESOURCE_EVENT_PROPERTY_PATH), null);

            return new ResourceEventReplicationTrigger(path, bundleContext);
        } else if (TRIGGER_SCHEDULED_EVENT.equals(factory)) {
            String action = PropertiesUtil.toString(properties.get(TRIGGER_SCHEDULED_EVENT_PROPERTY_ACTION), ReplicationActionType.POLL.name());
            String path = PropertiesUtil.toString(properties.get(TRIGGER_SCHEDULED_EVENT_PROPERTY_PATH), "/");
            int interval = PropertiesUtil.toInteger(properties.get(TRIGGER_SCHEDULED_EVENT_PROPERTY_SECONDS), 30);

            return new ScheduledReplicationTrigger(action, path, interval, scheduler);
        } else if (TRIGGER_REPLICATION_EVENT.equals(factory)) {
            String path = PropertiesUtil.toString(properties.get(TRIGGER_REPLICATION_EVENT_PROPERTY_PATH), null);

            return new ChainReplicateReplicationTrigger(path, bundleContext);
        } else if (TRIGGER_JCR_EVENT.equals(factory)) {
            String path = PropertiesUtil.toString(properties.get(TRIGGER_JCR_EVENT_PROPERTY_PATH), null);
            String serviceName = PropertiesUtil.toString(properties.get(TRIGGER_JCR_EVENT_PROPERTY_SERVICE_NAME), null);

            return new JcrEventReplicationTrigger(repository, path, serviceName);
        } else if (TRIGGER_PERSISTED_JCR_EVENT.equals(factory)) {
            String path = PropertiesUtil.toString(properties.get(TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_PATH), null);
            String serviceName = PropertiesUtil.toString(properties.get(TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_SERVICE_NAME), null);
            String nuggetsPath = PropertiesUtil.toString(properties.get(TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_NUGGETS_PATH), null);

            return new PersistingJcrEventReplicationTrigger(repository, path, serviceName, nuggetsPath);
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

    public <ComponentType extends ReplicationComponent> ComponentType getComponent(@Nonnull Class<ComponentType> type,
                                                                                   @Nullable String componentName) {
        return null;
    }
}
