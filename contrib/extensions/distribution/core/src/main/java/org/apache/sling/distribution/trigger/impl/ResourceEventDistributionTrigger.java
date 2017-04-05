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
package org.apache.sling.distribution.trigger.impl;

import javax.annotation.Nonnull;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.event.dea.DEAConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.sling.distribution.trigger.DistributionTrigger} for triggering a specific handler (e.g. agent) upon
 * node / properties being changed under a certain path
 */
public class ResourceEventDistributionTrigger implements DistributionTrigger {


    private final Logger log = LoggerFactory.getLogger(getClass());

    private final BundleContext bundleContext;
    private final String path;
    private final Map<String, ServiceRegistration> registrations = new ConcurrentHashMap<String, ServiceRegistration>();


    public ResourceEventDistributionTrigger(String path, BundleContext bundleContext) {
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
        for (ServiceRegistration serviceRegistration : registrations.values()) {
            serviceRegistration.unregister();
        }

        registrations.clear();
    }

    public void register(@Nonnull DistributionRequestHandler requestHandler) throws DistributionException {
        // register an event handler on path which triggers the agent on node / property changes / addition / removals
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(EventConstants.EVENT_TOPIC, new String[]{SlingConstants.TOPIC_RESOURCE_ADDED,
                SlingConstants.TOPIC_RESOURCE_CHANGED, SlingConstants.TOPIC_RESOURCE_REMOVED});
        log.info("trigger agent {} on path '{}'", requestHandler, path);

        properties.put(EventConstants.EVENT_FILTER, "(&(path=" + path + "/*) (!(" + DEAConstants.PROPERTY_APPLICATION + "=*)))");

        ServiceRegistration triggerPathEventRegistration = bundleContext.registerService(EventHandler.class.getName(),
                new TriggerAgentEventListener(requestHandler), properties);
        if (triggerPathEventRegistration != null) {
            registrations.put(requestHandler.toString(), triggerPathEventRegistration);
        } else {
            throw new DistributionException("cannot register event handler service for triggering agent");
        }
    }

    public void unregister(@Nonnull DistributionRequestHandler requestHandler) throws DistributionException {
        ServiceRegistration serviceRegistration = registrations.get(requestHandler.toString());
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
    }

    private class TriggerAgentEventListener implements EventHandler {

        private final DistributionRequestHandler requestHandler;

        public TriggerAgentEventListener(DistributionRequestHandler requestHandler) {
            this.requestHandler = requestHandler;
        }

        public void handleEvent(Event event) {
            DistributionRequestType action = SlingConstants.TOPIC_RESOURCE_REMOVED.equals(event.getTopic()) ?
                    DistributionRequestType.DELETE : DistributionRequestType.ADD;
            log.info("triggering distribution from event {}", event);
            for (String pn : event.getPropertyNames()) {
                log.info("property {} : {}", pn, event.getProperty(pn));
            }

            Object pathProperty = event.getProperty("path");
            if (pathProperty != null) {
                String distributingPath = String.valueOf(pathProperty);
                requestHandler.handle(null, new SimpleDistributionRequest(action, distributingPath));
            }
        }
    }
}
