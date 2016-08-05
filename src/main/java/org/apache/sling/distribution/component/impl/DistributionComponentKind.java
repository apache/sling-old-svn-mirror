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
package org.apache.sling.distribution.component.impl;

import static org.apache.sling.distribution.resources.DistributionResourceTypes.AGENT_LIST_RESOURCE_TYPE;
import static org.apache.sling.distribution.resources.DistributionResourceTypes.AGENT_RESOURCE_TYPE;
import static org.apache.sling.distribution.resources.DistributionResourceTypes.DEFAULT_SERVICE_RESOURCE_TYPE;
import static org.apache.sling.distribution.resources.DistributionResourceTypes.EXPORTER_LIST_RESOURCE_TYPE;
import static org.apache.sling.distribution.resources.DistributionResourceTypes.EXPORTER_RESOURCE_TYPE;
import static org.apache.sling.distribution.resources.DistributionResourceTypes.IMPORTER_LIST_RESOURCE_TYPE;
import static org.apache.sling.distribution.resources.DistributionResourceTypes.IMPORTER_RESOURCE_TYPE;
import static org.apache.sling.distribution.resources.DistributionResourceTypes.TRIGGER_LIST_RESOURCE_TYPE;
import static org.apache.sling.distribution.resources.DistributionResourceTypes.TRIGGER_RESOURCE_TYPE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.agent.impl.DistributionRequestAuthorizationStrategy;
import org.apache.sling.distribution.agent.impl.ForwardDistributionAgentFactory;
import org.apache.sling.distribution.agent.impl.PrivilegeDistributionRequestAuthorizationStrategy;
import org.apache.sling.distribution.agent.impl.QueueDistributionAgentFactory;
import org.apache.sling.distribution.agent.impl.ReverseDistributionAgentFactory;
import org.apache.sling.distribution.agent.impl.SimpleDistributionAgentFactory;
import org.apache.sling.distribution.agent.impl.SyncDistributionAgentFactory;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.impl.exporter.AgentDistributionPackageExporterFactory;
import org.apache.sling.distribution.packaging.impl.exporter.LocalDistributionPackageExporterFactory;
import org.apache.sling.distribution.packaging.impl.exporter.RemoteDistributionPackageExporterFactory;
import org.apache.sling.distribution.packaging.impl.importer.LocalDistributionPackageImporterFactory;
import org.apache.sling.distribution.packaging.impl.importer.RemoteDistributionPackageImporterFactory;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.serialization.impl.vlt.VaultDistributionPackageBuilderFactory;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.apache.sling.distribution.transport.impl.UserCredentialsDistributionTransportSecretProvider;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.distribution.trigger.impl.DistributionEventDistributeDistributionTriggerFactory;
import org.apache.sling.distribution.trigger.impl.JcrEventDistributionTriggerFactory;
import org.apache.sling.distribution.trigger.impl.PersistedJcrEventDistributionTriggerFactory;
import org.apache.sling.distribution.trigger.impl.ResourceEventDistributionTriggerFactory;
import org.apache.sling.distribution.trigger.impl.ScheduledDistributionTriggerFactory;

/**
 * Enum that represents the main distribution component kinds that can be configured for distribution.
 */
@SuppressWarnings( "serial" )
public enum DistributionComponentKind {

    AGENT("agent", AGENT_RESOURCE_TYPE, AGENT_LIST_RESOURCE_TYPE, DistributionAgent.class, new HashMap<String, Class<?>>() {
        {
            put("simple", SimpleDistributionAgentFactory.class);
            put("sync", SyncDistributionAgentFactory.class);
            put("forward", ForwardDistributionAgentFactory.class);
            put("reverse", ReverseDistributionAgentFactory.class);
            put("queue", QueueDistributionAgentFactory.class);
        }
    }),

    IMPORTER("importer", IMPORTER_RESOURCE_TYPE, IMPORTER_LIST_RESOURCE_TYPE, DistributionPackageImporter.class, new HashMap<String, Class<?>>() {
        {
            put("local", LocalDistributionPackageImporterFactory.class);
            put("remote", RemoteDistributionPackageImporterFactory.class);
        }
    }),

    EXPORTER("exporter", EXPORTER_RESOURCE_TYPE, EXPORTER_LIST_RESOURCE_TYPE, DistributionPackageExporter.class, new HashMap<String, Class<?>>() {
        {
            put("local", LocalDistributionPackageExporterFactory.class);
            put("remote", RemoteDistributionPackageExporterFactory.class);
            put("agent", AgentDistributionPackageExporterFactory.class);
        }
    }),

    QUEUE_PROVIDER("queueProvider", DEFAULT_SERVICE_RESOURCE_TYPE, DEFAULT_SERVICE_RESOURCE_TYPE, DistributionQueueProvider.class, new HashMap<String, Class<?>>() {
        {
            put("simple", SimpleDistributionAgentFactory.class);
            put("sync", SyncDistributionAgentFactory.class);
            put("forward", ForwardDistributionAgentFactory.class);
            put("reverse", ReverseDistributionAgentFactory.class);
            put("queue", QueueDistributionAgentFactory.class);
        }
    }),

    QUEUE_STRATEGY("queueStrategy", DEFAULT_SERVICE_RESOURCE_TYPE, DEFAULT_SERVICE_RESOURCE_TYPE, DistributionQueueDispatchingStrategy.class, new HashMap<String, Class<?>>() {
        {
            put("simple", SimpleDistributionAgentFactory.class);
            put("sync", SyncDistributionAgentFactory.class);
            put("forward", ForwardDistributionAgentFactory.class);
            put("reverse", ReverseDistributionAgentFactory.class);
            put("queue", QueueDistributionAgentFactory.class);
        }
    }),

    TRANSPORT_SECRET_PROVIDER("transportSecretProvider", DEFAULT_SERVICE_RESOURCE_TYPE, DEFAULT_SERVICE_RESOURCE_TYPE, DistributionTransportSecretProvider.class, new HashMap<String, Class<?>>() {
        {
            put("user", UserCredentialsDistributionTransportSecretProvider.class);
        }
    }),

    PACKAGE_BUILDER("packageBuilder", DEFAULT_SERVICE_RESOURCE_TYPE, DEFAULT_SERVICE_RESOURCE_TYPE, DistributionPackageBuilder.class, new HashMap<String, Class<?>>() {
        {
            put("filevlt", VaultDistributionPackageBuilderFactory.class);
            put("jcrvlt", VaultDistributionPackageBuilderFactory.class);
        }
    }),

    REQUEST_AUTHORIZATION("requestAuthorization", DEFAULT_SERVICE_RESOURCE_TYPE, DEFAULT_SERVICE_RESOURCE_TYPE, DistributionRequestAuthorizationStrategy.class, new HashMap<String, Class<?>>() {
        {
            put("privilege", PrivilegeDistributionRequestAuthorizationStrategy.class);
        }
    }),

    TRIGGER("trigger", TRIGGER_RESOURCE_TYPE, TRIGGER_LIST_RESOURCE_TYPE, DistributionTrigger.class, new HashMap<String, Class<?>>() {
        {
            put("resourceEvent", ResourceEventDistributionTriggerFactory.class);
            put("scheduledEvent", ScheduledDistributionTriggerFactory.class);
            put("distributionEvent", DistributionEventDistributeDistributionTriggerFactory.class);
            put("persistedJcrEvent", PersistedJcrEventDistributionTriggerFactory.class);
            put("jcrEvent", JcrEventDistributionTriggerFactory.class);
        }
    });

    private final String name;

    private final String resourceType;

    private final String rootResourceType;

    private final Class<?> type;

    private final Map<String, Class<?>> factoryMap;

    DistributionComponentKind(String name,
                              String resourceType,
                              String rootResourceType,
                              Class<?> type,
                              Map<String, Class<?>> factoryMap) {
        this.name = name;
        this.resourceType = resourceType;
        this.rootResourceType = rootResourceType;
        this.type = type;
        this.factoryMap = factoryMap;
    }

    public Class<?> asClass() {
        return type;
    }

    public static DistributionComponentKind fromClass(Class<?> type) {
        for (DistributionComponentKind kind : values()) {
            Class<?> kindClass = kind.asClass();

            if (kindClass.equals(type)) {
                return kind;
            }
        }

        return null;
    }

    public static DistributionComponentKind fromName(String name) {
        for (DistributionComponentKind kind : values()) {

            if (kind.getName().equals(name)) {
                return kind;
            }
        }

        return null;
    }

    public String getName() {
        return name;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getRootResourceType() {
        return rootResourceType;
    }

    public String getFactory(String type) {
        Class<?> factory = factoryMap.get(type);
        if (factory != null) {
            return factory.getName();
        }
        return null;
    }

    public List<String> getFactories() {
        List<String> result = new ArrayList<String>();
        for (Class<?> factory : factoryMap.values()) {
            result.add(factory.getName());
        }
        return result;
    }

    public String getType(String factory) {
        for (Entry<String, Class<?>> factoryEntry : factoryMap.entrySet()) {
            String type = factoryEntry.getKey();
            Class<?> factoryClass = factoryEntry.getValue();

            if (factoryClass.getName().equals(factory)) {
                return type;
            }
        }
        return null;
    }

}
