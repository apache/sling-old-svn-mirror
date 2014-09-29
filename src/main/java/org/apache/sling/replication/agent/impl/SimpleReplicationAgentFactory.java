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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.packaging.ReplicationPackageExporter;
import org.apache.sling.replication.packaging.ReplicationPackageImporter;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.queue.impl.SingleQueueDistributionStrategy;
import org.apache.sling.replication.queue.impl.jobhandling.JobHandlingReplicationQueueProvider;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An OSGi service factory for {@link ReplicationAgent}s which references already existing OSGi services.
 */
@Component(metatype = true,
        label = "Simple Replication Agents Factory",
        description = "OSGi configuration based ReplicationAgent service factory",
        name = SimpleReplicationAgentFactory.SERVICE_PID,
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
public class SimpleReplicationAgentFactory {

    public static final String PACKAGE_EXPORTER_TARGET = "ReplicationPackageExporter.target";

    public static final String PACKAGE_IMPORTER_TARGET = "ReplicationPackageImporter.target";

    public static final String QUEUEPROVIDER_TARGET = "ReplicationQueueProvider.target";

    public static final String QUEUE_DISTRIBUTION_TARGET = "ReplicationQueueDistributionStrategy.target";

    private final Logger log = LoggerFactory.getLogger(getClass());

    static final String SERVICE_PID = "org.apache.sling.replication.agent.impl.SimpleReplicationAgentFactory";

    private static final String DEFAULT_QUEUEPROVIDER = "(name=" + JobHandlingReplicationQueueProvider.NAME + ")";

    private static final String DEFAULT_DISTRIBUTION = "(name=" + SingleQueueDistributionStrategy.NAME + ")";

    @Property(boolValue = true, label = "Enabled")
    private static final String ENABLED = "enabled";

    @Property(label = "Name")
    public static final String NAME = "name";

    @Property(label = "Triggers")
    public static final String TRIGGERS = "triggers";

    @Property(boolValue = true, label = "Replicate using aggregated paths")
    public static final String USE_AGGREGATE_PATHS = "useAggregatePaths";

    @Property(boolValue = false, label = "Replicate using aggregated paths")
    public static final String IS_PASSIVE = "isPassive";

    @Property(label = "Target ReplicationPackageExporter", name = PACKAGE_EXPORTER_TARGET)
    @Reference(name = "ReplicationPackageExporter", policy = ReferencePolicy.DYNAMIC)
    private ReplicationPackageExporter packageExporter;

    @Property(label = "Target ReplicationPackageImporter", name = PACKAGE_IMPORTER_TARGET)
    @Reference(name = "ReplicationPackageImporter", policy = ReferencePolicy.DYNAMIC)
    private ReplicationPackageImporter packageImporter;

    @Property(label = "Target ReplicationQueueProvider", name = QUEUEPROVIDER_TARGET, value = DEFAULT_QUEUEPROVIDER)
    @Reference(name = "ReplicationQueueProvider", target = DEFAULT_QUEUEPROVIDER, policy = ReferencePolicy.DYNAMIC)
    private volatile ReplicationQueueProvider queueProvider;

    @Property(label = "Target QueueDistributionStrategy", name = QUEUE_DISTRIBUTION_TARGET, value = DEFAULT_DISTRIBUTION)
    @Reference(name = "ReplicationQueueDistributionStrategy", target = DEFAULT_DISTRIBUTION, policy = ReferencePolicy.DYNAMIC)
    private volatile ReplicationQueueDistributionStrategy queueDistributionStrategy;

    @Property(label = "Runmodes")
    private static final String RUNMODES = "runModes";

    private ServiceRegistration agentReg;

    @Reference
    private ReplicationEventFactory replicationEventFactory;

    @Reference
    private SlingSettingsService settingsService;

    @Activate
    public void activate(BundleContext context, Map<String, ?> config) throws Exception {

        // inject configuration
        Dictionary<String, Object> props = new Hashtable<String, Object>();

        boolean enabled = PropertiesUtil.toBoolean(config.get(ENABLED), true);

        if (enabled) {
            props.put(ENABLED, true);

            String[] runModes = PropertiesUtil.toStringArray(config.get(RUNMODES), new String[0]);
            props.put(RUNMODES, runModes);

            String name = PropertiesUtil
                    .toString(config.get(NAME), String.valueOf(new Random().nextInt(1000)));
            props.put(NAME, name);


            String queue = PropertiesUtil.toString(config.get(QUEUEPROVIDER_TARGET), DEFAULT_QUEUEPROVIDER);
            props.put(QUEUEPROVIDER_TARGET, queue);

            String distribution = PropertiesUtil.toString(config.get(QUEUE_DISTRIBUTION_TARGET), DEFAULT_DISTRIBUTION);
            props.put(QUEUE_DISTRIBUTION_TARGET, distribution);

            String[] triggers = PropertiesUtil.toStringArray(config.get(TRIGGERS), new String[0]);
            props.put(TRIGGERS, triggers);


            boolean useAggregatePaths = PropertiesUtil.toBoolean(config.get(USE_AGGREGATE_PATHS), true);
            props.put(USE_AGGREGATE_PATHS, useAggregatePaths);

            boolean isPassive = PropertiesUtil.toBoolean(config.get(IS_PASSIVE), false);
            props.put(IS_PASSIVE, isPassive);

            // check configuration is valid
            if (name == null || packageExporter == null || packageImporter == null || queueProvider == null || queueDistributionStrategy == null) {
                throw new Exception("configuration for this agent is not valid");
            }

            log.info("bound services for {} :  {} - {} - {} - {} - {} - {}", new Object[]{name,
                    packageImporter, packageExporter, queueProvider, queueDistributionStrategy});

            SimpleReplicationAgent agent = new SimpleReplicationAgent(name, useAggregatePaths, isPassive,
                    packageImporter, packageExporter, queueProvider, queueDistributionStrategy, replicationEventFactory,
                    null); // TODO : enable triggers again

            // only enable if instance runmodes match configured ones
            if (matchRunmodes(runModes)) {
                // register agent service
                agentReg = context.registerService(ReplicationAgent.class.getName(), agent, props);
                agent.enable();
            }
        }
    }

    private boolean matchRunmodes(String[] configuredRunModes) {
        boolean match = configuredRunModes == null || configuredRunModes.length == 0;
        if (!match) {
            Set<String> activeRunModes = settingsService.getRunModes();
            for (String activeRunMode : activeRunModes) {
                for (String configuredRunMode : configuredRunModes) {
                    if (activeRunMode.equals(configuredRunMode)) {
                        match = true;
                        break;
                    }
                }
            }
        }
        return match;
    }

    @Deactivate
    private void deactivate(BundleContext context) {
        if (agentReg != null) {
            ServiceReference reference = agentReg.getReference();
            SimpleReplicationAgent replicationAgent = (SimpleReplicationAgent) context.getService(reference);
            replicationAgent.disable();
            agentReg.unregister();
        }

    }
}
