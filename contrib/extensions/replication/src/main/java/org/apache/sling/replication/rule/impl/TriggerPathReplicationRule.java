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
package org.apache.sling.replication.rule.impl;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.replication.agent.AgentReplicationException;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.rule.ReplicationRule;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a rule for triggering a specific agent upon node / properties being changed under a certain path
 */
@Component(immediate = true)
@Service(value = ReplicationRule.class)
public class TriggerPathReplicationRule implements ReplicationRule {

    //    private static final Pattern p = Pattern.compile("(\\/\\w+)+\\/?");
    private static final String PREFIX = "trigger on path:";
    private static final String SIGNATURE = "trigger on path: ${path}";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private BundleContext bundleContext;
    private final Map<String, ServiceRegistration> registrations = new HashMap<String, ServiceRegistration>();

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Deactivate
    protected void deactivate() {
        for (Map.Entry<String, ServiceRegistration> entry : registrations.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().unregister();
            }
        }
        registrations.clear();
    }

    public String getSignature() {
        return SIGNATURE;
    }

    public void apply(String ruleString, ReplicationAgent agent) {
        if (signatureMatches(ruleString)) {
            // register an event handler on path which triggers the agent on node / property changes / addition / removals
            Dictionary<String, Object> properties = new Hashtable<String, Object>();
            properties.put(EventConstants.EVENT_TOPIC, new String[]{SlingConstants.TOPIC_RESOURCE_ADDED,
                    SlingConstants.TOPIC_RESOURCE_CHANGED, SlingConstants.TOPIC_RESOURCE_REMOVED});
            String path = ruleString.substring(ruleString.indexOf(':') + 1).trim();
            if (log.isInfoEnabled()) {
                log.info("trigger agent {} on path '{}'", agent.getName(), path);
            }
            properties.put(EventConstants.EVENT_FILTER, "(path=" + path + "/*)");
            if (bundleContext != null) {
                ServiceRegistration triggerPathEventRegistration = bundleContext.registerService(EventHandler.class.getName(), new TriggerAgentEventListener(agent), properties);
                registrations.put(agent.getName() + ruleString, triggerPathEventRegistration);
            } else {
                if (log.isErrorEnabled()) {
                    log.error("cannot register trigger since bundle context is null");
                }
            }
        } else {
            if (log.isWarnEnabled()) {
                log.warn("rule {} doesn't match signature: {}", ruleString, SIGNATURE);
            }
        }

    }

    public boolean signatureMatches(String ruleString) {
        return ruleString.startsWith(PREFIX) && ruleString.substring(PREFIX.length() + 1).matches("\\s*(\\/\\w+)+");
    }

    public void undo(String ruleString, ReplicationAgent agent) {
        if (signatureMatches(ruleString)) {
            ServiceRegistration serviceRegistration = registrations.get(agent.getName() + ruleString);
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
        }
        else {
            if (log.isWarnEnabled()) {
                log.warn("rule {} doesn't match signature: {}", ruleString, SIGNATURE);
            }
        }
    }


    private class TriggerAgentEventListener implements EventHandler {

        private final ReplicationAgent agent;

        public TriggerAgentEventListener(ReplicationAgent agent) {
            this.agent = agent;
        }

        public void handleEvent(Event event) {
            ReplicationActionType action = SlingConstants.TOPIC_RESOURCE_REMOVED.equals(event.getTopic()) ? ReplicationActionType.DELETE : ReplicationActionType.ADD;
            if (log.isInfoEnabled()) {
                log.info("triggering replication from event {}", event);
            }
            Object eventProperty = event.getProperty("path");
            if (eventProperty != null) {
                String replicatingPath = String.valueOf(eventProperty);
                try {
                    agent.send(new ReplicationRequest(System.currentTimeMillis(), action, replicatingPath));
                } catch (AgentReplicationException e) {
                    if (log.isErrorEnabled()) {
                        log.error("triggered replication resulted in an error", e);
                    }
                }
            }
        }
    }
}
