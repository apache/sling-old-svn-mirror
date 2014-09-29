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

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.event.ReplicationEvent;
import org.apache.sling.replication.event.ReplicationEventType;
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
 * {@link org.apache.sling.replication.trigger.ReplicationTrigger} for chain replication upon a certain {@link org.apache.sling.replication.event.ReplicationEventType}
 */
public class ChainReplicateReplicationTrigger implements ReplicationTrigger {

    public static final String TYPE = "replicateEvent";
    public static final String PATH = "path";


    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String pathPrefix;

    private final BundleContext bundleContext;
    private final Map<String, ServiceRegistration> registrations = new ConcurrentHashMap<String, ServiceRegistration>();


    public ChainReplicateReplicationTrigger(Map<String, Object> config, BundleContext bundleContext) {
        this(PropertiesUtil.toString(config.get(PATH), null), bundleContext);
    }

    public ChainReplicateReplicationTrigger(String pathPrefix, BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.pathPrefix = pathPrefix;
    }

    protected void deactivate() {
        for (Map.Entry<String, ServiceRegistration> entry : registrations.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().unregister();
            }
        }
        registrations.clear();
    }

    public void register(String handlerId, ReplicationTriggerRequestHandler requestHandler) {
        // register an event handler on replication package install (on a certain path) which triggers the chain replication of that same package
        Dictionary<String, Object> properties = new Hashtable<String, Object>();

        // TODO : make it possible to configure the type of event handled here, currently 'package-installed' is hardcoded
        properties.put(EventConstants.EVENT_TOPIC, ReplicationEvent.getTopic(ReplicationEventType.PACKAGE_INSTALLED));
        log.info("handler {} will chain replication on path '{}'", handlerId, pathPrefix);

//            properties.put(EventConstants.EVENT_FILTER, "(path=" + path + "/*)");
        if (bundleContext != null) {
            ServiceRegistration triggerPathEventRegistration = bundleContext.registerService(EventHandler.class.getName(),
                    new TriggerAgentEventListener(requestHandler, pathPrefix), properties);
            registrations.put(handlerId, triggerPathEventRegistration);
        } else {
            log.error("cannot register trigger since bundle context is null");

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
        private final String path;

        public TriggerAgentEventListener(ReplicationTriggerRequestHandler requestHandler, String path) {
            this.requestHandler = requestHandler;
            this.path = path;
        }

        public void handleEvent(Event event) {
            Object actionProperty = event.getProperty("replication.action");
            Object pathProperty = event.getProperty("replication.path");
            if (actionProperty != null && pathProperty != null) {
                String[] paths = (String[]) pathProperty;
                for (String p : paths) {
                    if (p.startsWith(path)) {
                        log.info("triggering chain replication from event {}", event);

                        ReplicationActionType action = ReplicationActionType.valueOf(String.valueOf(actionProperty));
                        requestHandler.handle(new ReplicationRequest(System.currentTimeMillis(), action, paths));
                        break;
                    }
                }
            }
        }
    }
}
