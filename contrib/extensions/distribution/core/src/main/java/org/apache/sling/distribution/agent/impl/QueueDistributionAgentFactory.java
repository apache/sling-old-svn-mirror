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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import org.apache.sling.distribution.packaging.impl.exporter.LocalDistributionPackageExporter;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.MultipleQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.SelectiveQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.SingleQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.jobhandling.JobHandlingDistributionQueueProvider;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
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
        label = "Apache Sling Distribution Agent - Queue Agents Factory",
        description = "OSGi configuration factory for queueing agents",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
@Reference(name = "triggers", referenceInterface = DistributionTrigger.class,
        policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
        bind = "bindDistributionTrigger", unbind = "unbindDistributionTrigger")
public class QueueDistributionAgentFactory extends AbstractDistributionAgentFactory {
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


    @Property(name = "requestAuthorizationStrategy.target", label = "Request Authorization Strategy", description = "The target reference for the DistributionRequestAuthorizationStrategy used to authorize the access to distribution process," +
            "e.g. use target=(name=...) to bind to services by name.")
    @Reference(name = "requestAuthorizationStrategy")
    private DistributionRequestAuthorizationStrategy requestAuthorizationStrategy;


    @Property(name = "packageBuilder.target", label = "Package Builder", description = "The target reference for the DistributionPackageBuilder used to create distribution packages, " +
            "e.g. use target=(name=...) to bind to services by name.")
    @Reference(name = "packageBuilder")
    private DistributionPackageBuilder packageBuilder;

    @Property(value = DEFAULT_TRIGGER_TARGET, label = "Triggers", description = "The target reference for DistributionTrigger used to trigger distribution, " +
            "e.g. use target=(name=...) to bind to services by name.")
    public static final String TRIGGERS_TARGET = "triggers.target";

    @Property(cardinality = 100, label = "Selective queues", description = "List of selective queues that should used for specific paths." +
            "The selector format is  {queuePrefix}|{mainQueueMatcher}={pathMatcher}, e.g. french|publish.*=/content/fr.*")
    public static final String SELECTIVE_QUEUES = "selectiveQueues";


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


        Map<String, String> selectiveQueues = PropertiesUtil.toMap(config.get(SELECTIVE_QUEUES), new String[0]);
        selectiveQueues = SettingsUtils.removeEmptyEntries(selectiveQueues);


        DistributionQueueProvider queueProvider = new JobHandlingDistributionQueueProvider(agentName, jobManager, context);
        DistributionQueueDispatchingStrategy exportQueueStrategy = null;


        if (selectiveQueues != null) {
            exportQueueStrategy = new SelectiveQueueDispatchingStrategy(selectiveQueues, new String[] { DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME });
        } else {
            exportQueueStrategy = new SingleQueueDispatchingStrategy();
        }

        DistributionPackageExporter packageExporter = new LocalDistributionPackageExporter(packageBuilder);
        DistributionRequestType[] allowedRequests = new DistributionRequestType[]{DistributionRequestType.ADD, DistributionRequestType.DELETE};

        return new SimpleDistributionAgent(agentName, false, null,
                serviceName, null, packageExporter, requestAuthorizationStrategy,
                queueProvider, exportQueueStrategy, null, distributionEventFactory, resourceResolverFactory, distributionLog, allowedRequests, allowedRoots, 0);
    }
}
