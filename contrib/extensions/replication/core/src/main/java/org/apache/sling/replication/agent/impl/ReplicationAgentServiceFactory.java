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
import org.apache.sling.replication.agent.AgentConfigurationException;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.queue.impl.SingleQueueDistributionStrategy;
import org.apache.sling.replication.queue.impl.jobhandling.JobHandlingReplicationQueueProvider;
import org.apache.sling.replication.rule.ReplicationRuleEngine;
import org.apache.sling.replication.serialization.ReplicationPackageExporter;
import org.apache.sling.replication.serialization.ReplicationPackageImporter;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An OSGi service factory for {@link ReplicationAgent}s
 */
@Component(metatype = true,
        label = "Replication Agents Factory",
        description = "OSGi configuration based ReplicationAgent service factory",
        name = ReplicationAgentServiceFactory.SERVICE_PID,
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
public class ReplicationAgentServiceFactory {

    private final Logger log = LoggerFactory.getLogger(getClass());

    static final String SERVICE_PID = "org.apache.sling.replication.agent.impl.ReplicationAgentServiceFactory";

    private static final String QUEUEPROVIDER = ReplicationAgentConfiguration.QUEUEPROVIDER;

    private static final String QUEUE_DISTRIBUTION = ReplicationAgentConfiguration.QUEUE_DISTRIBUTION;


    private static final String DEFAULT_QUEUEPROVIDER = "(name="
            + JobHandlingReplicationQueueProvider.NAME + ")";

    private static final String DEFAULT_DISTRIBUTION = "(name="
            + SingleQueueDistributionStrategy.NAME + ")";

    @Property(boolValue = true, label = "Enabled")
    private static final String ENABLED = ReplicationAgentConfiguration.ENABLED;

    @Property(label = "Name")
    private static final String NAME = ReplicationAgentConfiguration.NAME;

    @Property(label = "Rules")
    private static final String RULES = ReplicationAgentConfiguration.RULES;

    @Property(boolValue = true, label = "Replicate using aggregated paths")
    private static final String USE_AGGREGATE_PATHS = ReplicationAgentConfiguration.USE_AGGREGATE_PATHS;

    @Property(label = "Target ReplicationPackageExporter", name = "ReplicationPackageExporter.target", value = "(name=vlt)")
    @Reference(name = "ReplicationPackageExporter", target = "(name=vlt)", policy = ReferencePolicy.DYNAMIC)
    private ReplicationPackageExporter packageExporter;

    @Property(label = "Target ReplicationPackageImporter", name = "ReplicationPackageImporter.target", value = "(name=default)")
    @Reference(name = "ReplicationPackageImporter", target = "(name=default)", policy = ReferencePolicy.DYNAMIC)
    private ReplicationPackageImporter packageImporter;

    @Property(label = "Target ReplicationQueueProvider", name = QUEUEPROVIDER, value = DEFAULT_QUEUEPROVIDER)
    @Reference(name = "ReplicationQueueProvider", target = DEFAULT_QUEUEPROVIDER, policy = ReferencePolicy.DYNAMIC)
    private volatile ReplicationQueueProvider queueProvider;

    @Property(label = "Target QueueDistributionStrategy", name = QUEUE_DISTRIBUTION, value = DEFAULT_DISTRIBUTION)
    @Reference(name = "ReplicationQueueDistributionStrategy", target = DEFAULT_DISTRIBUTION, policy = ReferencePolicy.DYNAMIC)
    private volatile ReplicationQueueDistributionStrategy queueDistributionStrategy;

    @Property(label = "Runmodes")
    private static final String RUNMODES = ReplicationAgentConfiguration.RUNMODES;

    private ServiceRegistration agentReg;

    @Reference
    private ReplicationRuleEngine replicationRuleEngine;

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


            String queue = PropertiesUtil.toString(config.get(QUEUEPROVIDER), "");
            props.put(QUEUEPROVIDER, queue);

            String distribution = PropertiesUtil.toString(config.get(QUEUE_DISTRIBUTION), "");
            props.put(QUEUE_DISTRIBUTION, distribution);

            String[] rules = PropertiesUtil.toStringArray(config.get(RULES), new String[0]);
            props.put(RULES, rules);


            boolean useAggregatePaths = PropertiesUtil.toBoolean(config.get(USE_AGGREGATE_PATHS), true);
            props.put(USE_AGGREGATE_PATHS, useAggregatePaths);

            boolean isPassive = PropertiesUtil.toBoolean(config.get("isPassive"), false);
            props.put(USE_AGGREGATE_PATHS, useAggregatePaths);

            // check configuration is valid
            if (name == null || packageExporter == null || packageImporter == null || queueProvider == null || queueDistributionStrategy == null) {
                throw new AgentConfigurationException("configuration for this agent is not valid");
            }


            if (log.isInfoEnabled()) {
                log.info("bound services for {} :  {} - {} - {} - {} - {} - {}", new Object[]{name,
                        packageImporter, packageExporter, queueProvider, queueDistributionStrategy});
            }

            ReplicationAgent agent = new SimpleReplicationAgent(name, rules, useAggregatePaths, isPassive,
                    packageImporter, packageExporter, queueProvider, queueDistributionStrategy, replicationEventFactory, replicationRuleEngine);


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
            ReplicationAgent replicationAgent = (ReplicationAgent) context.getService(reference);
            replicationAgent.disable();
            agentReg.unregister();
        }

    }
}
