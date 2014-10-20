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
package org.apache.sling.replication.trigger.impl;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.replication.agent.ReplicationComponent;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.trigger.ReplicationTrigger;
import org.apache.sling.replication.trigger.ReplicationTriggerRequestHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.sling.replication.trigger.ReplicationTrigger} for triggering a specific handler (e.g. agent) upon
 * node / properties being changed under a certain path
 */
public class ResourceEventReplicationTrigger implements ReplicationTrigger, ReplicationComponent {


    private final Logger log = LoggerFactory.getLogger(getClass());

    private final BundleContext bundleContext;
    private final String path;
    private final Map<String, ServiceRegistration> registrations = new ConcurrentHashMap<String, ServiceRegistration>();


    public ResourceEventReplicationTrigger(String path, BundleContext bundleContext) {
        if (bundleContext == null) {
            throw new IllegalArgumentException("Invalid bundle context");
        }

        if (path == null) {
            throw new IllegalArgumentException("Path is required");
        }

        this.bundleContext = bundleContext;
        this.path = path;
    }

    public void enable() {

    }

    public void disable() {
        for (Map.Entry<String, ServiceRegistration> entry : registrations.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().unregister();
            }
        }
        registrations.clear();
    }

    public void register(String handlerId, ReplicationTriggerRequestHandler requestHandler) {
        // register an event handler on path which triggers the agent on node / property changes / addition / removals
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(EventConstants.EVENT_TOPIC, new String[]{SlingConstants.TOPIC_RESOURCE_ADDED,
                SlingConstants.TOPIC_RESOURCE_CHANGED, SlingConstants.TOPIC_RESOURCE_REMOVED});
        log.info("trigger agent {} on path '{}'", handlerId, path);

        properties.put(EventConstants.EVENT_FILTER, "(path=" + path + "/*)");
        ServiceRegistration triggerPathEventRegistration = bundleContext.registerService(EventHandler.class.getName(),
                new TriggerAgentEventListener(requestHandler), properties);
        if (triggerPathEventRegistration != null) {
            registrations.put(handlerId, triggerPathEventRegistration);
        } else {
            log.error("cannot register event handler service for triggering agent");
        }
    }

    public void unregister(String handlerId) {
        ServiceRegistration serviceRegistration = registrations.get(handlerId);
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
    }

    private class TriggerAgentEventListener implements EventHandler {

        private final ReplicationTriggerRequestHandler requestHandler;

        public TriggerAgentEventListener(ReplicationTriggerRequestHandler requestHandler) {
            this.requestHandler = requestHandler;
        }

        public void handleEvent(Event event) {
            ReplicationActionType action = SlingConstants.TOPIC_RESOURCE_REMOVED.equals(event.getTopic()) ?
                    ReplicationActionType.DELETE : ReplicationActionType.ADD;
            log.info("triggering replication from event {}", event);

            Object pathProperty = event.getProperty("path");
            if (pathProperty != null) {
                String replicatingPath = String.valueOf(pathProperty);
                requestHandler.handle(new ReplicationRequest(System.currentTimeMillis(), action, replicatingPath));
            }
        }
    }
}
