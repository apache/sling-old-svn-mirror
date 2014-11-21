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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.*;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.agent.DistributionRequestAuthorizationStrategy;
import org.apache.sling.distribution.agent.impl.PrivilegeDistributionRequestAuthorizationStrategy;
import org.apache.sling.distribution.agent.impl.SimpleDistributionAgent;
import org.apache.sling.distribution.communication.DistributionActionType;
import org.apache.sling.distribution.component.DistributionComponent;
import org.apache.sling.distribution.component.DistributionComponentFactory;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.impl.exporter.AgentDistributionPackageExporter;
import org.apache.sling.distribution.packaging.impl.exporter.LocalDistributionPackageExporter;
import org.apache.sling.distribution.packaging.impl.exporter.RemoteDistributionPackageExporter;
import org.apache.sling.distribution.packaging.impl.importer.LocalDistributionPackageImporter;
import org.apache.sling.distribution.packaging.impl.importer.RemoteDistributionPackageImporter;
import org.apache.sling.distribution.queue.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.PriorityPathQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.SingleQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.jobhandling.JobHandlingDistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.simple.SimpleDistributionQueueProvider;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.serialization.impl.ResourceSharedDistributionPackageBuilder;
import org.apache.sling.distribution.serialization.impl.vlt.FileVaultDistributionPackageBuilder;
import org.apache.sling.distribution.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.distribution.transport.authentication.impl.UserCredentialsTransportAuthenticationProvider;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.distribution.trigger.impl.ChainDistributeDistributionTrigger;
import org.apache.sling.distribution.trigger.impl.JcrEventDistributionTrigger;
import org.apache.sling.distribution.trigger.impl.PersistingJcrEventDistributionTrigger;
import org.apache.sling.distribution.trigger.impl.RemoteEventDistributionTrigger;
import org.apache.sling.distribution.trigger.impl.ResourceEventDistributionTrigger;
import org.apache.sling.distribution.trigger.impl.ScheduledDistributionTrigger;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.apache.sling.distribution.component.impl.DefaultDistributionComponentFactoryConstants.*;

/**
 * A generic factory for distribution components using a compact configuration, already existing OSGi services
 * for the components to be wired can be used as well as directly instantiated components (called by type name).
 * <p/>
 * Currently supported components are {@link org.apache.sling.distribution.agent.DistributionAgent}s,
 * {@link org.apache.sling.distribution.trigger.DistributionTrigger}s and
 * {@link org.apache.sling.distribution.transport.authentication.TransportAuthenticationProvider}s
 */
@Component(metatype = true,
        label = "Generic Distribution Components Factory",
        description = "OSGi configuration Distribution Component factory",
        specVersion = "1.1",
        immediate = true
)
@Service(DistributionComponentFactory.class)
@Property(name = "name", value = "default")
public class DefaultDistributionComponentFactory implements DistributionComponentFactory {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private DistributionEventFactory distributionEventFactory;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private SlingRepository repository;

    @Reference
    private Packaging packaging;

    @Reference
    private Scheduler scheduler;

    @Reference
    private JobManager jobManager;

    private BundleContext bundleContext;

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public <ComponentType extends DistributionComponent> ComponentType createComponent(@Nonnull Class<ComponentType> type,
                                                                                      @Nonnull Map<String, Object> properties,
                                                                                      DistributionComponentFactory componentFactory) {


        DistributionComponentFactoryWrapper wrappingComponentFactory = new DistributionComponentFactoryWrapper(componentFactory);

        if (type.isAssignableFrom(DistributionAgent.class)) {
            return (ComponentType) createAgent(properties, wrappingComponentFactory);
        } else if (type.isAssignableFrom(DistributionTrigger.class)) {
            return (ComponentType) createTrigger(properties, wrappingComponentFactory);
        } else if (type.isAssignableFrom(TransportAuthenticationProvider.class)) {
            return (ComponentType) createTransportAuthenticationProvider(properties, wrappingComponentFactory);
        } else if (type.isAssignableFrom(DistributionPackageImporter.class)) {
            return (ComponentType) createImporter(properties, wrappingComponentFactory);
        } else if (type.isAssignableFrom(DistributionPackageExporter.class)) {
            return (ComponentType) createExporter(properties, wrappingComponentFactory);
        } else if (type.isAssignableFrom(DistributionQueueDispatchingStrategy.class)) {
            return (ComponentType) createDispatchingStrategy(properties, wrappingComponentFactory);
        } else if (type.isAssignableFrom(DistributionQueueProvider.class)) {
            return (ComponentType) createQueueProvider(properties, wrappingComponentFactory);
        } else if (type.isAssignableFrom(DistributionRequestAuthorizationStrategy.class)) {
            return (ComponentType) createAuthorizationStrategy(properties, wrappingComponentFactory);
        }

        return null;
    }

    DistributionAgent createAgent(Map<String, Object> properties, DistributionComponentFactoryWrapper componentFactory) {

        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), AGENT_SIMPLE);

        if (AGENT_SIMPLE.equals(factory)) {
            String agentName = PropertiesUtil.toString(properties.get(COMPONENT_NAME), null);


            Map<String, Object> exporterProperties = extractMap(COMPONENT_PACKAGE_EXPORTER, properties);
            DistributionPackageExporter packageExporter = componentFactory.createComponent(DistributionPackageExporter.class, exporterProperties);

            Map<String, Object> importerProperties = extractMap(COMPONENT_PACKAGE_IMPORTER, properties);
            DistributionPackageImporter packageImporter = componentFactory.createComponent(DistributionPackageImporter.class, importerProperties);

            Map<String, Object> authorizationStrategyProperties = extractMap(COMPONENT_REQUEST_AUTHORIZATION_STRATEGY, properties);
            DistributionRequestAuthorizationStrategy packageExporterStrategy = componentFactory.createComponent(DistributionRequestAuthorizationStrategy.class,
                    authorizationStrategyProperties);

            Map<String, Object> queueDistributionStrategyProperties = extractMap(COMPONENT_QUEUE_DISTRIBUTION_STRATEGY, properties);
            DistributionQueueDispatchingStrategy queueDistributionStrategy = componentFactory.createComponent(DistributionQueueDispatchingStrategy.class, queueDistributionStrategyProperties);

            Map<String, Object> queueProviderProperties = extractMap(COMPONENT_QUEUE_PROVIDER, properties);
            queueProviderProperties.put(QUEUE_PROVIDER_PROPERTY_QUEUE_PREFIX, agentName);
            DistributionQueueProvider queueProvider = componentFactory.createComponent(DistributionQueueProvider.class, queueProviderProperties);

            List<Map<String, Object>> triggersProperties = extractMapList(COMPONENT_TRIGGER, properties);
            List<DistributionTrigger> triggers = new ArrayList<DistributionTrigger>();
            for (Map<String, Object>  triggerProperties : triggersProperties) {
                triggers.add(componentFactory.createComponent(DistributionTrigger.class, triggerProperties));
            }

            String serviceName = PropertiesUtil.toString(properties.get(AGENT_SIMPLE_PROPERTY_SERVICE_NAME), null);

            boolean isPassive = PropertiesUtil.toBoolean(properties.get(AGENT_SIMPLE_PROPERTY_IS_PASSIVE), false);

            return new SimpleDistributionAgent(agentName, isPassive, serviceName,
                    packageImporter, packageExporter, packageExporterStrategy,
                    queueProvider, queueDistributionStrategy, distributionEventFactory, resourceResolverFactory, triggers);

        }

        return null;

    }

    private DistributionRequestAuthorizationStrategy createAuthorizationStrategy(Map<String, Object> properties, DistributionComponentFactoryWrapper componentFactory) {

        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), COMPONENT_TYPE_SERVICE);

        if (REQUEST_AUTHORIZATION_STRATEGY_PRIVILEGE.equals(factory)) {
            String jcrPrivilege = PropertiesUtil.toString(properties.get(REQUEST_AUTHORIZATION_STRATEGY_PRIVILEGE_PROPERTY_JCR_PRIVILEGE), null);
            return new PrivilegeDistributionRequestAuthorizationStrategy(jcrPrivilege);
        }

        return null;
    }


    DistributionPackageExporter createExporter(Map<String, Object> properties, DistributionComponentFactoryWrapper componentFactory) {

        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), COMPONENT_TYPE_SERVICE);

        if (PACKAGE_EXPORTER_LOCAL.equals(factory)) {
            Map<String, Object> builderProperties = extractMap(COMPONENT_PACKAGE_BUILDER, properties);
            DistributionPackageBuilder packageBuilder = createBuilder(builderProperties);
            return new LocalDistributionPackageExporter(packageBuilder);
        } else if (PACKAGE_EXPORTER_REMOTE.equals(factory)) {
            Map<String, Object> authenticationProviderProperties = extractMap(COMPONENT_TRANSPORT_AUTHENTICATION_PROVIDER, properties);
            TransportAuthenticationProvider authenticationProvider = componentFactory.createComponent(TransportAuthenticationProvider.class,
                    authenticationProviderProperties);

            Map<String, Object> builderProperties = extractMap(COMPONENT_PACKAGE_BUILDER, properties);
            DistributionPackageBuilder packageBuilder = createBuilder(builderProperties);


            String[] endpoints = PropertiesUtil.toStringArray(properties.get(PACKAGE_EXPORTER_REMOTE_PROPERTY_ENDPOINTS), new String[0]);
            String endpointStrategyName = PropertiesUtil.toString(properties.get(PACKAGE_EXPORTER_REMOTE_PROPERTY_ENDPOINTS_STRATEGY), "One");
            int pullItems = PropertiesUtil.toInteger(properties.get(PACKAGE_EXPORTER_REMOTE_PROPERTY_PULL_ITEMS), Integer.MAX_VALUE);

            return new RemoteDistributionPackageExporter(packageBuilder, authenticationProvider, endpoints, endpointStrategyName, pullItems);
        } else if (PACKAGE_EXPORTER_AGENT.equals(factory)) {
            Map<String, Object> builderProperties = extractMap(COMPONENT_PACKAGE_BUILDER, properties);
            DistributionPackageBuilder packageBuilder = createBuilder(builderProperties);
            DistributionAgent agent = componentFactory.createComponent(DistributionAgent.class, new HashMap<String, Object>());

            return new AgentDistributionPackageExporter(properties, agent, packageBuilder);
        }


        return null;
    }

    DistributionPackageImporter createImporter(Map<String, Object> properties, DistributionComponentFactoryWrapper componentFactory) {

        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), COMPONENT_TYPE_SERVICE);

        if (PACKAGE_IMPORTER_LOCAL.equals(factory)) {
            Map<String, Object> builderProperties = extractMap(COMPONENT_PACKAGE_BUILDER, properties);
            DistributionPackageBuilder packageBuilder = createBuilder(builderProperties);
            return new LocalDistributionPackageImporter(packageBuilder, distributionEventFactory);
        } else if (PACKAGE_IMPORTER_REMOTE.equals(factory)) {
            Map<String, Object> authenticationProviderProperties = extractMap(COMPONENT_TRANSPORT_AUTHENTICATION_PROVIDER, properties);
            TransportAuthenticationProvider authenticationProvider = componentFactory.createComponent(TransportAuthenticationProvider.class,
                    authenticationProviderProperties);

            String[] endpoints = PropertiesUtil.toStringArray(properties.get(PACKAGE_EXPORTER_REMOTE_PROPERTY_ENDPOINTS), new String[0]);
            String endpointStrategyName = PropertiesUtil.toString(properties.get(PACKAGE_EXPORTER_REMOTE_PROPERTY_ENDPOINTS_STRATEGY), "One");

            return new RemoteDistributionPackageImporter(authenticationProvider, endpoints, endpointStrategyName);
        }

        return null;
    }

    DistributionQueueProvider createQueueProvider(Map<String, Object> properties, DistributionComponentFactoryWrapper componentFactory) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), COMPONENT_TYPE_SERVICE);

        if (QUEUE_PROVIDER_JOB.equals(factory)) {
            String prefix = PropertiesUtil.toString(properties.get(QUEUE_PROVIDER_PROPERTY_QUEUE_PREFIX), null);
            return new JobHandlingDistributionQueueProvider(prefix, jobManager, bundleContext);
        }
        else if (QUEUE_PROVIDER_SIMPLE.equals(factory)) {
            String prefix = PropertiesUtil.toString(properties.get(QUEUE_PROVIDER_PROPERTY_QUEUE_PREFIX), null);

            return new SimpleDistributionQueueProvider(scheduler, prefix);
        }

        return null;
    }

    DistributionQueueDispatchingStrategy createDispatchingStrategy(Map<String, Object> properties, DistributionComponentFactoryWrapper componentFactory) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), COMPONENT_TYPE_SERVICE);

        if (QUEUE_DISTRIBUTION_STRATEGY_SINGLE.equals(factory)) {
            return new SingleQueueDispatchingStrategy();
        }
        else if (QUEUE_DISTRIBUTION_STRATEGY_PRIORITY.equals(factory)) {
            String[] priorityPaths = PropertiesUtil.toStringArray(properties.get(QUEUE_DISTRIBUTION_STRATEGY_PRIORITY_PROPERTY_PATHS), null);

            return new PriorityPathQueueDispatchingStrategy(priorityPaths);
        }

        return null;
    }

    TransportAuthenticationProvider createTransportAuthenticationProvider(Map<String, Object> properties, DistributionComponentFactoryWrapper componentFactory) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), COMPONENT_TYPE_SERVICE);

        if (TRANSPORT_AUTHENTICATION_PROVIDER_USER.equals(factory)) {
            String username = PropertiesUtil.toString(properties.get(TRANSPORT_AUTHENTICATION_PROVIDER_USER_PROPERTY_USERNAME), "").trim();
            String password = PropertiesUtil.toString(properties.get(TRANSPORT_AUTHENTICATION_PROVIDER_USER_PROPERTY_PASSWORD), "").trim();
            return new UserCredentialsTransportAuthenticationProvider(username, password);
        }

        return null;
    }

    DistributionPackageBuilder createBuilder(Map<String, Object> properties) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), COMPONENT_TYPE_SERVICE);

        if (PACKAGE_BUILDER_FILEVLT.equals(factory)) {
            String importMode = PropertiesUtil.toString(properties.get(PACKAGE_BUILDER_FILEVLT_IMPORT_MODE), null);
            String aclHandling = PropertiesUtil.toString(properties.get(PACKAGE_BUILDER_FILEVLT_ACLHANDLING), null);
            if (importMode != null && aclHandling != null) {
                return new ResourceSharedDistributionPackageBuilder(new FileVaultDistributionPackageBuilder(packaging, distributionEventFactory, importMode, aclHandling));
            } else {
                return new ResourceSharedDistributionPackageBuilder(new FileVaultDistributionPackageBuilder(packaging, distributionEventFactory));
            }
        }

        return null;
    }


    DistributionTrigger createTrigger(Map<String, Object> properties, DistributionComponentFactoryWrapper componentFactory) {
        String factory = PropertiesUtil.toString(properties.get(COMPONENT_TYPE), COMPONENT_TYPE_SERVICE);

        if (TRIGGER_REMOTE_EVENT.equals(factory)) {
            Map<String, Object> authenticationProviderProperties = extractMap(COMPONENT_TRANSPORT_AUTHENTICATION_PROVIDER, properties);

            TransportAuthenticationProvider authenticationProvider = componentFactory.createComponent(TransportAuthenticationProvider.class,
                    authenticationProviderProperties);
            String endpoint = PropertiesUtil.toString(properties.get(TRIGGER_REMOTE_EVENT_PROPERTY_ENDPOINT), null);

            return new RemoteEventDistributionTrigger(endpoint, authenticationProvider, scheduler);
        } else if (TRIGGER_RESOURCE_EVENT.equals(factory)) {
            String path = PropertiesUtil.toString(properties.get(TRIGGER_RESOURCE_EVENT_PROPERTY_PATH), null);

            return new ResourceEventDistributionTrigger(path, bundleContext);
        } else if (TRIGGER_SCHEDULED_EVENT.equals(factory)) {
            String action = PropertiesUtil.toString(properties.get(TRIGGER_SCHEDULED_EVENT_PROPERTY_ACTION), DistributionActionType.PULL.name());
            String path = PropertiesUtil.toString(properties.get(TRIGGER_SCHEDULED_EVENT_PROPERTY_PATH), "/");
            int interval = PropertiesUtil.toInteger(properties.get(TRIGGER_SCHEDULED_EVENT_PROPERTY_SECONDS), 30);

            return new ScheduledDistributionTrigger(action, path, interval, scheduler);
        } else if (TRIGGER_DISTRIBUTION_EVENT.equals(factory)) {
            String path = PropertiesUtil.toString(properties.get(TRIGGER_DISTRIBUTION_EVENT_PROPERTY_PATH), null);

            return new ChainDistributeDistributionTrigger(path, bundleContext);
        } else if (TRIGGER_JCR_EVENT.equals(factory)) {
            String path = PropertiesUtil.toString(properties.get(TRIGGER_JCR_EVENT_PROPERTY_PATH), null);
            String serviceName = PropertiesUtil.toString(properties.get(TRIGGER_JCR_EVENT_PROPERTY_SERVICE_NAME), null);

            return new JcrEventDistributionTrigger(repository, path, serviceName);
        } else if (TRIGGER_PERSISTED_JCR_EVENT.equals(factory)) {
            String path = PropertiesUtil.toString(properties.get(TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_PATH), null);
            String serviceName = PropertiesUtil.toString(properties.get(TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_SERVICE_NAME), null);
            String nuggetsPath = PropertiesUtil.toString(properties.get(TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_NUGGETS_PATH), null);

            return new PersistingJcrEventDistributionTrigger(repository, path, serviceName, nuggetsPath);
        }

        return null;
    }



    Map<String, Object> extractMap(String key, Map<String, Object> sourceMap) {
        sourceMap = sourceMap == null ? new HashMap<String, Object>() : sourceMap;

        Object resultMapObject = sourceMap.get(key);

        Map<String, Object> resultMap = null;
        if (resultMapObject instanceof Map) {
            resultMap = (Map) resultMapObject;
        }

        return resultMap == null ? new HashMap<String, Object>() : resultMap;
    }

    List<Map<String, Object>> extractMapList(String key, Map<String, Object> sourceMap) {

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

        sourceMap = sourceMap == null ? new HashMap<String, Object>() : sourceMap;

        Object resultMapObject = sourceMap.get(key);

        if (resultMapObject instanceof Map) {
            Map<String, Object> resultMap = (Map) resultMapObject;
            for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    result.add((Map<String,Object>) entry.getValue());
                }
            }
        }

        return result;

    }


    private class DistributionComponentFactoryWrapper {

        private final DistributionComponentFactory distributionComponentFactory;


        public DistributionComponentFactoryWrapper(DistributionComponentFactory distributionComponentFactory) {

            this.distributionComponentFactory = distributionComponentFactory;
        }

        public <ComponentType extends DistributionComponent> ComponentType createComponent(@Nonnull Class<ComponentType> type, @Nonnull Map<String, Object> properties) {

            return distributionComponentFactory.createComponent(type, properties, null);
        }
    }
}
