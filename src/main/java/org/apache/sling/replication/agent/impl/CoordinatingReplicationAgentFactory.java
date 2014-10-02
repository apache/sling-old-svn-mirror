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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.ReplicationComponentProvider;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An OSGi service factory for {@link org.apache.sling.replication.agent.impl.CoordinatingReplicationAgent}s.
 */
@Component(metatype = true,
        label = "Coordinating Replication Agents Factory",
        description = "OSGi configuration based ReplicationAgent service factory",
        name = CoordinatingReplicationAgentFactory.SERVICE_PID,
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
public class CoordinatingReplicationAgentFactory implements ReplicationComponentListener {

    private final Logger log = LoggerFactory.getLogger(getClass());

    static final String SERVICE_PID = "org.apache.sling.replication.agent.impl.CoordinatingReplicationAgentFactory";

    @Property(boolValue = true, label = "Enabled")
    private static final String ENABLED = "enabled";

    @Property(label = "Name")
    public static final String NAME = "name";

    @Property(boolValue = true, label = "Replicate using aggregated paths")
    public static final String USE_AGGREGATE_PATHS = "useAggregatePaths";

    @Property(label = "Package Exporter", cardinality = 100)
    public static final String PACKAGE_EXPORTER = "packageExporter";

    @Property(label = "Package Importer", cardinality = 100)
    public static final String PACKAGE_IMPORTER = "packageImporter";

    @Property(label = "Queue Provider", cardinality = 100)
    public static final String QUEUE_PROVIDER = "queueProvider";

    @Property(label = "Queue Distribution Strategy", cardinality = 100)
    public static final String QUEUE_DISTRIBUTION_STRATEGY = "queueDistributionStrategy";

    @Reference
    private SlingSettingsService settingsService;

    @Reference
    private ReplicationComponentProvider componentProvider;

    private ServiceRegistration agentReg;
    private ServiceRegistration listenerReg;

    private BundleContext savedContext;
    private Map<String, Object> savedConfig;

    @Activate
    public void activate(BundleContext context, Map<String, Object> config) throws Exception {
        log.debug("activating agent with config {}", config);

        savedContext = context;
        savedConfig = config;

        // inject configuration
        Dictionary<String, Object> props = new Hashtable<String, Object>();

        boolean enabled = PropertiesUtil.toBoolean(config.get(ENABLED), true);
        String name = PropertiesUtil.toString(config.get(NAME), null);

        if (enabled) {
            props.put(ENABLED, true);
            props.put(NAME, name);

            if (listenerReg == null) {
                listenerReg = context.registerService(ReplicationComponentListener.class.getName(), this, props);
            }

            if (agentReg == null) {
                Map<String, Object> properties = new HashMap<String, Object>();
                properties.putAll(config);

                properties.put("type", "coordinating");
                CoordinatingReplicationAgent agent = (CoordinatingReplicationAgent) componentProvider.createComponent(ReplicationAgent.class, properties);

                log.debug("activated agent {}", agent != null ? agent.getName() : null);

                if (agent != null) {
                    props.put(NAME, agent.getName());

                    // register agent service
                    agentReg = context.registerService(ReplicationAgent.class.getName(), agent, props);
                    agent.enable();
                }
            }
        }
    }

    @Deactivate
    private void deactivate(BundleContext context) {
        log.debug("deactivating agent");
        if (agentReg != null) {
            ServiceReference reference = agentReg.getReference();
            CoordinatingReplicationAgent replicationAgent = (CoordinatingReplicationAgent) context.getService(reference);
            replicationAgent.disable();
            agentReg.unregister();
            agentReg = null;
        }
        if (listenerReg != null) {
            listenerReg.unregister();
            listenerReg = null;
        }
    }

    private void refresh(boolean isBinding) {
        try {
            if (savedContext != null && savedConfig != null) {
                if (isBinding && agentReg == null) {
                    activate(savedContext, savedConfig);
                } else if (!isBinding && agentReg != null) {
                    deactivate(savedContext);
                }
            }

        } catch (Exception e) {
            log.error("Cannot refresh agent", e);
        }
    }

    public <ComponentType> void componentBind(ComponentType component, String componentName) {
        refresh(true);
    }

    public <ComponentType> void componentUnbind(ComponentType component, String componentName) {
        refresh(false);
    }
}
