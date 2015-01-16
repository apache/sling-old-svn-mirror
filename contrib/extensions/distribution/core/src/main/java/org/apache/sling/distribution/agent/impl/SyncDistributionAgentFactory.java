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
package org.apache.sling.distribution.agent.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.component.impl.DistributionComponentUtils;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.impl.exporter.RemoteDistributionPackageExporter;
import org.apache.sling.distribution.packaging.impl.importer.RemoteDistributionPackageImporter;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.MultipleQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.SingleQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.jobhandling.JobHandlingDistributionQueueProvider;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.apache.sling.distribution.transport.impl.TransportEndpointStrategyType;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * An OSGi service factory for {@link org.apache.sling.distribution.agent.DistributionAgent}s which references already existing OSGi services.
 */
@Component(metatype = true,
        label = "Sling Distribution - Sync Agents Factory",
        description = "OSGi configuration factory for syncing agents",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
@Reference(name = "triggers", referenceInterface = DistributionTrigger.class,
        policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
        bind = "bindDistributionTrigger", unbind = "unbindDistributionTrigger")
public class SyncDistributionAgentFactory extends AbstractDistributionAgentFactory {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(label = "Name")
    public static final String NAME = DistributionComponentUtils.PN_NAME;

    @Property(boolValue = true, label = "Enabled")
    private static final String ENABLED = "enabled";


    @Property(label = "Service Name")
    public static final String SERVICE_NAME = "serviceName";

    /**
     * endpoints property
     */
    @Property(cardinality = 100)
    public static final String EXPORTER_ENDPOINTS = "packageExporter.endpoints";

    /**
     * endpoints property
     */
    @Property(cardinality = 100)
    public static final String IMPORTER_ENDPOINTS = "packageImporter.endpoints";



    @Property(label = "Use multiple queues", boolValue = false)
    public static final String USE_MULTIPLE_QUEUES = "useMultipleQueues";

    @Reference
    private Packaging packaging;

    @Property(name = "requestAuthorizationStrategy.target")
    @Reference(name = "requestAuthorizationStrategy")
    private DistributionRequestAuthorizationStrategy requestAuthorizationStrategy;


    @Property(name = "transportSecretProvider.target")
    @Reference(name = "transportSecretProvider")
    DistributionTransportSecretProvider transportSecretProvider;


    @Property(name = "packageBuilder.target")
    @Reference(name = "packageBuilder")
    private DistributionPackageBuilder packageBuilder;

    @Property(value = DEFAULT_TRIGGER_TARGET)
    public static final String TRIGGERS_TARGET = "triggers.target";

    @Reference
    private DistributionEventFactory distributionEventFactory;

    @Reference
    private SlingSettingsService settingsService;

    @Reference
    private JobManager jobManager;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;


    @Activate
    protected void activate(BundleContext context, Map<String, Object> config) {
        super.activate(context, config);
    }

    protected void bindDistributionTrigger(DistributionTrigger distributionTrigger, Map<String, Object> config) {
        super.bindDistributionTrigger(distributionTrigger, config);

    }

    protected void unbindDistributionTrigger(DistributionTrigger distributionTrigger, Map<String, Object> config) {
        super.unbindDistributionTrigger(distributionTrigger, config);
    }

    @Deactivate
    protected void deactivate(BundleContext context) {
        super.deactivate(context);
    }


    @Override
    protected SimpleDistributionAgent createAgent(String agentName, BundleContext context, Map<String, Object> config) {
        String serviceName = PropertiesUtil.toString(config.get(SERVICE_NAME), null);


        Object exporterEndpointsValue = config.get(EXPORTER_ENDPOINTS);
        Object importerEndpointsValue = config.get(IMPORTER_ENDPOINTS);

        String[] exporterEndpoints = PropertiesUtil.toStringArray(exporterEndpointsValue, new String[0]);
        Map<String, String> importerEndpointsMap = SettingsUtils.toUriMap(importerEndpointsValue);

        boolean useMultipleQueues = PropertiesUtil.toBoolean(config.get(USE_MULTIPLE_QUEUES), false);

        DistributionQueueDispatchingStrategy dispatchingStrategy;
        DistributionPackageImporter packageImporter;

        if (useMultipleQueues) {
            java.util.Set<String> var = importerEndpointsMap.keySet();
            String[] queueNames = var.toArray(new String[var.size()]);
            dispatchingStrategy = new MultipleQueueDispatchingStrategy(queueNames);
            packageImporter = new RemoteDistributionPackageImporter(transportSecretProvider, importerEndpointsMap, TransportEndpointStrategyType.One);
        } else {
            dispatchingStrategy = new SingleQueueDispatchingStrategy();
            packageImporter = new RemoteDistributionPackageImporter(transportSecretProvider, importerEndpointsMap, TransportEndpointStrategyType.All);
        }

        DistributionPackageExporter packageExporter = new RemoteDistributionPackageExporter(packageBuilder, transportSecretProvider, exporterEndpoints, TransportEndpointStrategyType.All, 1);
        DistributionQueueProvider queueProvider =  new JobHandlingDistributionQueueProvider(agentName, jobManager, context);

        return new SimpleDistributionAgent(agentName, false, serviceName,
                packageImporter, packageExporter, requestAuthorizationStrategy,
                queueProvider, dispatchingStrategy, distributionEventFactory, resourceResolverFactory);

    }
}
