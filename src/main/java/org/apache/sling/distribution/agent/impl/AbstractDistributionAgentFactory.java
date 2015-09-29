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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.log.DistributionLog;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.resources.impl.OsgiUtils;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract OSGi service factory for registering {@link org.apache.sling.distribution.agent.impl.SimpleDistributionAgent}s
 */
public abstract class AbstractDistributionAgentFactory {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String NAME = DistributionComponentConstants.PN_NAME;

    private static final String ENABLED = "enabled";

    protected static final String DEFAULT_TRIGGER_TARGET = "(name=)";

    private static final String TRIGGERS_TARGET = "triggers.target";

    protected static final String LOG_LEVEL = "log.level";


    private ServiceRegistration componentReg;
    private Map<String, Object> savedConfig;
    private String agentName;
    private List<DistributionTrigger> triggers = new CopyOnWriteArrayList<DistributionTrigger>();
    private boolean triggersEnabled = false;

    private SimpleDistributionAgent agent;

    protected void activate(BundleContext context, Map<String, Object> config) {
        log.info("activating with config {}", OsgiUtils.osgiPropertyMapToString(config));

        // inject configuration
        Dictionary<String, Object> props = new Hashtable<String, Object>();

        boolean enabled = PropertiesUtil.toBoolean(config.get(ENABLED), true);
        String triggersTarget = PropertiesUtil.toString(config.get(TRIGGERS_TARGET), null);
        triggersEnabled = triggersTarget != null && triggersTarget.trim().length() > 0;
        agentName = PropertiesUtil.toString(config.get(NAME), null);


        if (enabled && agentName != null) {

            for (Map.Entry<String, Object> entry : config.entrySet()) {
                // skip service and component related properties
                if (entry.getKey().startsWith("service.") || entry.getKey().startsWith("component.")) {
                    continue;
                }

                props.put(entry.getKey(), entry.getValue());

            }

            if (componentReg == null) {

                DefaultDistributionLog distributionLog = null;
                try {

                    String logLevel = PropertiesUtil.toString(config.get(LOG_LEVEL), DefaultDistributionLog.LogLevel.INFO.name());
                    DefaultDistributionLog.LogLevel level = DefaultDistributionLog.LogLevel.valueOf(logLevel.trim().toUpperCase());
                    if (level == null) {
                        level = DefaultDistributionLog.LogLevel.INFO;
                    }


                    distributionLog = new DefaultDistributionLog(DistributionComponentKind.AGENT, agentName, SimpleDistributionAgent.class, level);

                    agent = createAgent(agentName, context, config, distributionLog);
                } catch (Throwable t) {
                    if (distributionLog != null) {
                        distributionLog.error("Cannot create agent", t);
                    }
                    log.error("Cannot create agent", t);
                }


                if (agent != null) {

                    // register agent service
                    componentReg = context.registerService(DistributionAgent.class.getName(), agent, props);
                    agent.enable();

                    if (triggersEnabled) {
                        for (DistributionTrigger trigger : triggers) {
                            agent.enableTrigger(trigger);
                        }
                    }

                }

                log.info("activated agent {}", agentName);

            }
        }
    }

    protected void bindDistributionTrigger(DistributionTrigger distributionTrigger, Map<String, Object> config) {
        triggers.add(distributionTrigger);
        if (agent != null && triggersEnabled) {
            agent.enableTrigger(distributionTrigger);
        }

    }

    protected void unbindDistributionTrigger(DistributionTrigger distributionTrigger, Map<String, Object> config) {
        triggers.remove(distributionTrigger);

        if (agent != null) {
            agent.disableTrigger(distributionTrigger);
        }
    }


    protected void deactivate(BundleContext context) {
        if (componentReg != null) {
            ServiceReference reference = componentReg.getReference();
            Object service = context.getService(reference);
            if (service instanceof SimpleDistributionAgent) {
                SimpleDistributionAgent agent = (SimpleDistributionAgent) service;
                for (DistributionTrigger trigger : triggers) {
                    agent.disableTrigger(trigger);
                }
                triggers.clear();
                triggersEnabled = false;
                agent.disable();
            }

            componentReg.unregister();
            componentReg = null;
            agent = null;
        }

        log.info("deactivated agent {}", agentName);
    }


    protected abstract SimpleDistributionAgent createAgent(String agentName, BundleContext context, Map<String, Object> config, DefaultDistributionLog distributionLog);

}
