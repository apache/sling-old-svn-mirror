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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.impl.exporter.LocalDistributionPackageExporter;
import org.apache.sling.distribution.packaging.impl.importer.RemoteDistributionPackageImporter;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.ErrorQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.MultipleQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.SelectiveQueueDispatchingStrategy;
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

/**
 * An OSGi service factory for {@link org.apache.sling.distribution.agent.DistributionAgent}s which references already existing OSGi services.
 */
@Component(metatype = true,
        label = "Apache Sling Distribution Agent - Forward Agents Factory",
        description = "OSGi configuration factory for forward agents",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
@Reference(name = "triggers", referenceInterface = DistributionTrigger.class,
        policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
        bind = "bindDistributionTrigger", unbind = "unbindDistributionTrigger")
public class ForwardDistributionAgentFactory extends AbstractDistributionAgentFactory {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(label = "Name", description = "The name of the agent.")
    public static final String NAME = DistributionComponentConstants.PN_NAME;

    @Property(label = "Title", description = "The display friendly title of the agent.")
    public static final String TITLE = "title";

    @Property(label = "Details", description = "The display friendly details of the agent.")
    public static final String DETAILS = "details";


    @Property(boolValue = true, label = "Enabled", description = "Whether or not to start the distribution agent.")
    private static final String ENABLED = "enabled";


    @Property(label = "Service Name", description = "The name of the service used to access the repository.")
    public static final String SERVICE_NAME = "serviceName";

    @Property(options = {
            @PropertyOption(name = "debug", value = "debug"), @PropertyOption(name = "info", value = "info"), @PropertyOption(name = "warn", value = "warn"),
            @PropertyOption(name = "error", value = "error")},
            value = "info",
            label = "Log Level", description = "The log level recorded in the transient log accessible via http."
    )
    public static final String LOG_LEVEL = AbstractDistributionAgentFactory.LOG_LEVEL;


    @Property(cardinality = 100, label = "Allowed roots", description = "If set the agent will allow only distribution requests under the specified roots.")
    private static final String ALLOWED_ROOTS = "allowed.roots";


    @Property(boolValue = true, label = "Queue Processing Enabled", description = "Whether or not the distribution agent should process packages in the queues.")
    public static final String QUEUE_PROCESSING_ENABLED = "queue.processing.enabled";


    /**
     * endpoints property
     */
    @Property(cardinality = 100, label = "Importer Endpoints", description = "List of endpoints to which packages are sent (imported). " +
            "The list can be given as a map in case a queue should be configured for each endpoint, e.g. queueName=http://...")
    public static final String IMPORTER_ENDPOINTS = "packageImporter.endpoints";


    @Property(boolValue = false, label = "Use multiple queues", description = "Whether or not to use an individual queue for each importer endpoint. " +
            "If the queue names are not specified by importer endpoints definition then they are autogenerated.")
    public static final String USE_MULTIPLE_QUEUES = "useMultipleQueues";

    @Property(cardinality = 100, label = "Passive queues", description = "List of queues that should be disabled." +
            "These queues will gather all the packages until they are removed explicitly.")
    public static final String PASSIVE_QUEUES = "passiveQueues";

    @Property(cardinality = 100, label = "Selective queues", description = "List of selective queues that should used for specific paths." +
            "The selector format is  {queuePrefix}|{mainQueueMatcher}={pathMatcher}, e.g. french|publish.*=/content/fr.*")
    public static final String SELECTIVE_QUEUES = "selectiveQueues";

    @Property(options = {
            @PropertyOption(name = "none", value = "none"), @PropertyOption(name = "errorQueue", value = "errorQueue")},
            value = "none",
            label = "Retry Strategy", description = "The strategy to apply after a certain number of failed retries."
    )
    public static final String RETRY_STRATEGY = "retry.strategy";

    @Property(intValue = 100, label = "Retry attempts", description = "The number of times to retry until the retry strategy is applied.")
    public static final String RETRY_ATTEMPTS = "retry.attempts";


    @Property(name = "requestAuthorizationStrategy.target", label = "Request Authorization Strategy", description = "The target reference for the DistributionRequestAuthorizationStrategy used to authorize the access to distribution process," +
            "e.g. use target=(name=...) to bind to services by name.")
    @Reference(name = "requestAuthorizationStrategy")
    private DistributionRequestAuthorizationStrategy requestAuthorizationStrategy;


    @Property(name = "transportSecretProvider.target", label = "Transport Secret Provider", description = "The target reference for the DistributionTransportSecretProvider used to obtain the credentials used for accessing the remote endpoints, " +
            "e.g. use target=(name=...) to bind to services by name.")
    @Reference(name = "transportSecretProvider")
    DistributionTransportSecretProvider transportSecretProvider;


    @Property(name = "packageBuilder.target", label = "Package Builder", description = "The target reference for the DistributionPackageBuilder used to create distribution packages, " +
            "e.g. use target=(name=...) to bind to services by name.")
    @Reference(name = "packageBuilder")
    private DistributionPackageBuilder packageBuilder;

    @Property(value = DEFAULT_TRIGGER_TARGET, label = "Triggers", description = "The target reference for DistributionTrigger used to trigger distribution, " +
            "e.g. use target=(name=...) to bind to services by name.")
    public static final String TRIGGERS_TARGET = "triggers.target";

    @Reference
    private Packaging packaging;

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
    protected SimpleDistributionAgent createAgent(String agentName, BundleContext context, Map<String, Object> config, DefaultDistributionLog distributionLog) {
        String serviceName = PropertiesUtil.toString(config.get(SERVICE_NAME), null);
        String[] allowedRoots = PropertiesUtil.toStringArray(config.get(ALLOWED_ROOTS), null);
        allowedRoots = SettingsUtils.removeEmptyEntries(allowedRoots);

        boolean queueProcessingEnabled = PropertiesUtil.toBoolean(config.get(QUEUE_PROCESSING_ENABLED), true);

        String[] passiveQueues = PropertiesUtil.toStringArray(config.get(PASSIVE_QUEUES), new String[0]);
        passiveQueues = SettingsUtils.removeEmptyEntries(passiveQueues, new String[0]);

        Map<String, String> selectiveQueues = PropertiesUtil.toMap(config.get(SELECTIVE_QUEUES), new String[0]);
        selectiveQueues = SettingsUtils.removeEmptyEntries(selectiveQueues);


        DistributionPackageExporter packageExporter = new LocalDistributionPackageExporter(packageBuilder);
        DistributionQueueProvider queueProvider = new JobHandlingDistributionQueueProvider(agentName, jobManager, context);

        DistributionQueueDispatchingStrategy exportQueueStrategy = null;
        DistributionQueueDispatchingStrategy importQueueStrategy = null;

        DistributionPackageImporter packageImporter = null;
        Map<String, String> importerEndpointsMap = SettingsUtils.toUriMap(config.get(IMPORTER_ENDPOINTS));
        boolean useMultipleQueues = PropertiesUtil.toBoolean(config.get(USE_MULTIPLE_QUEUES), false);
        Set<String> processingQueues = new HashSet<String>();


        if (useMultipleQueues) {
            Set<String> queuesMap = new TreeSet<String>();
            queuesMap.addAll(importerEndpointsMap.keySet());
            queuesMap.addAll(Arrays.asList(passiveQueues));
            String[] queueNames = queuesMap.toArray(new String[0]);

            if (selectiveQueues != null) {
                SelectiveQueueDispatchingStrategy dispatchingStrategy = new SelectiveQueueDispatchingStrategy(selectiveQueues, queueNames);
                Map<String, String> queueAliases = dispatchingStrategy.getMatchingQueues(null);
                importerEndpointsMap = SettingsUtils.expandUriMap(importerEndpointsMap, queueAliases);
                exportQueueStrategy = dispatchingStrategy;
            } else {
                exportQueueStrategy = new MultipleQueueDispatchingStrategy(queueNames);
            }

            processingQueues.addAll(importerEndpointsMap.keySet());
            processingQueues.removeAll(Arrays.asList(passiveQueues));


            packageImporter = new RemoteDistributionPackageImporter(distributionLog, transportSecretProvider, importerEndpointsMap, TransportEndpointStrategyType.One);
        } else {
            exportQueueStrategy = new SingleQueueDispatchingStrategy();
            processingQueues.addAll(exportQueueStrategy.getQueueNames());
            packageImporter = new RemoteDistributionPackageImporter(distributionLog, transportSecretProvider, importerEndpointsMap, TransportEndpointStrategyType.All);
        }

        DistributionRequestType[] allowedRequests = new DistributionRequestType[]{DistributionRequestType.ADD, DistributionRequestType.DELETE};


        String retryStrategy = SettingsUtils.removeEmptyEntry(PropertiesUtil.toString(config.get(RETRY_STRATEGY), null));
        int retryAttepts = PropertiesUtil.toInteger(config.get(RETRY_ATTEMPTS), 100);


        if ("errorQueue".equals(retryStrategy)) {
            importQueueStrategy = new ErrorQueueDispatchingStrategy(processingQueues.toArray(new String[0]));
        }


        return new SimpleDistributionAgent(agentName, queueProcessingEnabled, processingQueues,
                serviceName, packageImporter, packageExporter, requestAuthorizationStrategy,
                queueProvider, exportQueueStrategy, importQueueStrategy, distributionEventFactory, resourceResolverFactory, distributionLog, allowedRequests, allowedRoots, retryAttepts);


    }
}
