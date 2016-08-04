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

import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.event.DistributionEventProperties;
import org.apache.sling.distribution.event.DistributionEventTopics;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.sling.distribution.trigger.DistributionTrigger} for chain distribution upon a certain {@link org.apache.sling.distribution.event.DistributionEventTopics}
 */
public class DistributionEventDistributeDistributionTrigger implements DistributionTrigger {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String pathPrefix;

    private final BundleContext bundleContext;
    private final Map<String, ServiceRegistration> registrations = new ConcurrentHashMap<String, ServiceRegistration>();


    public DistributionEventDistributeDistributionTrigger(String pathPrefix, BundleContext bundleContext) {
        if (pathPrefix == null) {
            throw new IllegalArgumentException("path is required");
        }
        this.bundleContext = bundleContext;
        this.pathPrefix = pathPrefix;
    }

    public void register(@Nonnull DistributionRequestHandler requestHandler) throws DistributionException {
        // register an event handler on distribution package install (on a certain path) which triggers the chain distribution of that same package
        Dictionary<String, Object> properties = new Hashtable<String, Object>();

        // TODO : make it possible to configure the type of event handled here, currently 'package-installed' is hardcoded
        properties.put(EventConstants.EVENT_TOPIC, DistributionEventTopics.AGENT_PACKAGE_DISTRIBUTED);
        log.info("handler {} will chain distribute on path '{}'", requestHandler, pathPrefix);

        if (bundleContext != null) {
            ServiceRegistration triggerPathEventRegistration = bundleContext.registerService(EventHandler.class.getName(),
                    new TriggerAgentEventListener(requestHandler, pathPrefix), properties);
            if (triggerPathEventRegistration != null) {
                registrations.put(requestHandler.toString(), triggerPathEventRegistration);
            }
        } else {
            throw new DistributionException("cannot register trigger since bundle context is null");
        }
    }

    public void unregister(@Nonnull DistributionRequestHandler requestHandler) throws DistributionException {
        ServiceRegistration serviceRegistration = registrations.get(requestHandler.toString());
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
    }

    public void disable() {
        for (Map.Entry<String, ServiceRegistration> entry : registrations.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().unregister();
            }
        }
        registrations.clear();
    }

    private class TriggerAgentEventListener implements EventHandler {

        private final DistributionRequestHandler requestHandler;
        private final String path;

        public TriggerAgentEventListener(DistributionRequestHandler requestHandler, String path) {
            this.requestHandler = requestHandler;
            this.path = path;
        }

        public void handleEvent(Event event) {
            Object actionProperty = event.getProperty(DistributionEventProperties.DISTRIBUTION_TYPE);
            Object pathProperty = event.getProperty(DistributionEventProperties.DISTRIBUTION_PATHS);
            if (actionProperty != null && pathProperty != null) {
                String[] paths = (String[]) pathProperty;
                for (String p : paths) {
                    if (p.startsWith(path)) {
                        log.info("triggering chain distribution from event {}", event);

                        DistributionRequestType action = DistributionRequestType.valueOf(String.valueOf(actionProperty));
                        requestHandler.handle(null, new SimpleDistributionRequest(action, paths));
                        break;
                    }
                }
            }
        }
    }
}
