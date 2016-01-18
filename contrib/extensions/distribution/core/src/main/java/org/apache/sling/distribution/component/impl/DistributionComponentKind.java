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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.agent.impl.DistributionRequestAuthorizationStrategy;
import org.apache.sling.distribution.agent.impl.ForwardDistributionAgentFactory;
import org.apache.sling.distribution.agent.impl.PrivilegeDistributionRequestAuthorizationStrategy;
import org.apache.sling.distribution.agent.impl.QueueDistributionAgentFactory;
import org.apache.sling.distribution.agent.impl.ReverseDistributionAgentFactory;
import org.apache.sling.distribution.agent.impl.SimpleDistributionAgentFactory;
import org.apache.sling.distribution.agent.impl.SyncDistributionAgentFactory;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.impl.exporter.AgentDistributionPackageExporterFactory;
import org.apache.sling.distribution.packaging.impl.exporter.LocalDistributionPackageExporterFactory;
import org.apache.sling.distribution.packaging.impl.exporter.RemoteDistributionPackageExporterFactory;
import org.apache.sling.distribution.packaging.impl.importer.LocalDistributionPackageImporterFactory;
import org.apache.sling.distribution.packaging.impl.importer.RemoteDistributionPackageImporterFactory;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
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
public enum DistributionComponentKind {

    AGENT("agent"),
    IMPORTER("importer"),
    EXPORTER("exporter"),
    QUEUE_PROVIDER("queueProvider"),
    QUEUE_STRATEGY("queueStrategy"),
    TRANSPORT_SECRET_PROVIDER("transportSecretProvider"),
    PACKAGE_BUILDER("packageBuilder"),
    REQUEST_AUTHORIZATION("requestAuthorization"),
    TRIGGER("trigger");


    private static final Map<DistributionComponentKind, Class> classMap = new HashMap<DistributionComponentKind, Class>();
    private static final Map<DistributionComponentKind, Map<String, Class>> factoryMap = new HashMap<DistributionComponentKind, Map<String, Class>>();

    static {
        registerKind(AGENT, DistributionAgent.class);
        registerKind(IMPORTER, DistributionPackageImporter.class);
        registerKind(EXPORTER, DistributionPackageExporter.class);
        registerKind(QUEUE_PROVIDER, DistributionQueueProvider.class);
        registerKind(QUEUE_STRATEGY, DistributionQueueDispatchingStrategy.class);
        registerKind(TRANSPORT_SECRET_PROVIDER, DistributionTransportSecretProvider.class);
        registerKind(REQUEST_AUTHORIZATION, DistributionRequestAuthorizationStrategy.class);
        registerKind(PACKAGE_BUILDER, DistributionPackageBuilder.class);
        registerKind(TRIGGER, DistributionTrigger.class);


        // register "core" factories kind, type -> ComponentFactoryClass
        registerFactory(DistributionComponentKind.AGENT, "simple", SimpleDistributionAgentFactory.class);
        registerFactory(DistributionComponentKind.AGENT, "sync", SyncDistributionAgentFactory.class);
        registerFactory(DistributionComponentKind.AGENT, "forward", ForwardDistributionAgentFactory.class);
        registerFactory(DistributionComponentKind.AGENT, "reverse", ReverseDistributionAgentFactory.class);
        registerFactory(DistributionComponentKind.AGENT, "queue", QueueDistributionAgentFactory.class);

        registerFactory(DistributionComponentKind.EXPORTER, "local", LocalDistributionPackageExporterFactory.class);
        registerFactory(DistributionComponentKind.EXPORTER, "remote", RemoteDistributionPackageExporterFactory.class);
        registerFactory(DistributionComponentKind.EXPORTER, "agent", AgentDistributionPackageExporterFactory.class);

        registerFactory(DistributionComponentKind.IMPORTER, "local", LocalDistributionPackageImporterFactory.class);
        registerFactory(DistributionComponentKind.IMPORTER, "remote", RemoteDistributionPackageImporterFactory.class);

        registerFactory(DistributionComponentKind.PACKAGE_BUILDER, "filevlt", VaultDistributionPackageBuilderFactory.class);
        registerFactory(DistributionComponentKind.PACKAGE_BUILDER, "jcrvlt", VaultDistributionPackageBuilderFactory.class);

        registerFactory(DistributionComponentKind.REQUEST_AUTHORIZATION, "privilege", PrivilegeDistributionRequestAuthorizationStrategy.class);

        registerFactory(DistributionComponentKind.TRANSPORT_SECRET_PROVIDER, "user", UserCredentialsDistributionTransportSecretProvider.class);

        registerFactory(DistributionComponentKind.TRIGGER, "resourceEvent", ResourceEventDistributionTriggerFactory.class);
        registerFactory(DistributionComponentKind.TRIGGER, "scheduledEvent", ScheduledDistributionTriggerFactory.class);
        registerFactory(DistributionComponentKind.TRIGGER, "distributionEvent", DistributionEventDistributeDistributionTriggerFactory.class);
        registerFactory(DistributionComponentKind.TRIGGER, "persistedJcrEvent", PersistedJcrEventDistributionTriggerFactory.class);
        registerFactory(DistributionComponentKind.TRIGGER, "jcrEvent", JcrEventDistributionTriggerFactory.class);


    }

    private final String name;

    DistributionComponentKind(String name) {
        this.name = name;
    }

    public Class asClass() {
        return classMap.get(this);
    }

    public static DistributionComponentKind fromClass(Class type) {
        for (DistributionComponentKind kind : classMap.keySet()) {
            Class kindClass = classMap.get(kind);

            if (kindClass.equals(type)) {
                return kind;
            }
        }

        return null;
    }

    public static DistributionComponentKind fromName(String name) {
        for (DistributionComponentKind kind : classMap.keySet()) {

            if (kind.getName().equals(name)) {
                return kind;
            }
        }

        return null;
    }

    public String getName() {
        return name;
    }

    private static void registerKind(DistributionComponentKind kind, Class kindClass) {
        classMap.put(kind, kindClass);
    }

    private static void registerFactory(DistributionComponentKind kind, String type, Class factoryClass) {

        if (!factoryMap.containsKey(kind)) {
            factoryMap.put(kind, new HashMap<String, Class>());
        }

        Map<String, Class> kindMap = factoryMap.get(kind);

        kindMap.put(type, factoryClass);
    }

    public String getFactory(String type) {
        Class factory = factoryMap.get(this).get(type);
        return factory.getName();
    }

    public List<String> getFactories() {
        List<String> result = new ArrayList<String>();
        for (Class factory : factoryMap.get(this).values()) {
            result.add(factory.getName());
        }

        return result;

    }

    public String getType(String factory) {
        for (String type : factoryMap.get(this).keySet()) {
            Class factoryClass = factoryMap.get(this).get(type);

            if (factoryClass.getName().equals(factory)) {
                return type;
            }
        }

        return null;

    }


}
