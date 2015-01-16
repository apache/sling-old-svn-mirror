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

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.component.impl.DistributionComponentUtils;
import org.apache.sling.distribution.resources.impl.OsgiUtils;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An abstract OSGi service factory for registering {@link org.apache.sling.distribution.agent.impl.SimpleDistributionAgent}s
 */
public abstract class AbstractDistributionAgentFactory {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String NAME = DistributionComponentUtils.PN_NAME;

    private static final String ENABLED = "enabled";

    protected static final String DEFAULT_TRIGGER_TARGET = "(name=)";

    private ServiceRegistration componentReg;
    private BundleContext savedContext;
    private Map<String, Object> savedConfig;
    private String agentName;
    List<DistributionTrigger> triggers = new CopyOnWriteArrayList<DistributionTrigger>();

    private SimpleDistributionAgent agent;

    protected void activate(BundleContext context, Map<String, Object> config) {
        log.info("activating with config {}", OsgiUtils.osgiPropertyMapToString(config));


        savedContext = context;
        savedConfig = config;

        // inject configuration
        Dictionary<String, Object> props = new Hashtable<String, Object>();

        boolean enabled = PropertiesUtil.toBoolean(config.get(ENABLED), true);

        if (enabled) {
            props.put(ENABLED, true);

            agentName = PropertiesUtil.toString(config.get(NAME), null);
            props.put(NAME, agentName);

            if (componentReg == null) {

                try {

                   agent = createAgent(agentName, context, config);
                }
                catch (IllegalArgumentException e) {
                    log.warn("cannot create agent", e);
                }


                if (agent != null) {

                    // register agent service
                    componentReg = context.registerService(DistributionAgent.class.getName(), agent, props);
                    agent.enable();
                }

                log.info("activated agent {}", agentName);

            }
        }
    }

    protected void bindDistributionTrigger(DistributionTrigger distributionTrigger, Map<String, Object> config) {
        triggers.add(distributionTrigger);
        if (agent != null) {
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
                ((SimpleDistributionAgent) service).disable();

            }

            componentReg.unregister();
            componentReg = null;
            agent = null;
        }

        log.info("deactivated agent {}", agentName);


    }


    protected abstract SimpleDistributionAgent createAgent(String agentName, BundleContext context, Map<String, Object> config);

}
