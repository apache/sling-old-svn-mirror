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
package org.apache.sling.replication.agent;

import org.apache.sling.replication.packaging.ReplicationPackageExporter;
import org.apache.sling.replication.packaging.ReplicationPackageImporter;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.trigger.ReplicationTrigger;

import java.util.List;
import java.util.Map;

/**
 * factory for {@link org.apache.sling.replication.agent.ReplicationComponent}s
 */
public interface ReplicationComponentFactory {

    String COMPONENT_SEPARATOR = ".";
    String COMPONENT_TYPE = "type";
    String COMPONENT_NAME = "name";
    String COMPONENT_TYPE_SERVICE = "service";
    String COMPONENT_ENABLED = "enabled";


    // Names of the configurable components
    String COMPONENT_AGENT = "agent";
    String COMPONENT_PACKAGE_EXPORTER = "packageExporter";
    String COMPONENT_PACKAGE_IMPORTER = "packageImporter";
    String COMPONENT_REQUEST_AUTHORIZATION_STRATEGY = "requestAuthorizationStrategy";
    String COMPONENT_QUEUE_DISTRIBUTION_STRATEGY = "queueDistributionStrategy";
    String COMPONENT_QUEUE_PROVIDER = "queueProvider";
    String COMPONENT_TRIGGER = "trigger";
    String COMPONENT_PACKAGE_BUILDER = "packageBuilder";
    String COMPONENT_TRANSPORT_AUTHENTICATION_PROVIDER = "transportAuthenticationProvider";

    String AGENT_SIMPLE = "simple";
    String AGENT_SIMPLE_PROPERTY_SERVICE_NAME = "serviceName";
    String AGENT_SIMPLE_PROPERTY_IS_PASSIVE = "isPassive";

    String PACKAGE_EXPORTER_LOCAL = "local";

    String PACKAGE_EXPORTER_REMOTE = "remote";
    String PACKAGE_EXPORTER_REMOTE_PROPERTY_ENDPOINTS = "endpoints";
    String PACKAGE_EXPORTER_REMOTE_PROPERTY_ENDPOINTS_STRATEGY = "endpoints.strategy";
    String PACKAGE_EXPORTER_REMOTE_PROPERTY_POLL_ITEMS = "poll.items";

    String PACKAGE_EXPORTER_AGENT = "agent";

    String PACKAGE_IMPORTER_LOCAL = "local";

    String PACKAGE_IMPORTER_REMOTE = "remote";
    String PACKAGE_IMPORTER_REMOTE_PROPERTY_ENDPOINTS = "endpoints";
    String PACKAGE_IMPORTER_REMOTE_PROPERTY_ENDPOINTS_STRATEGY = "endpoints.strategy";

    String TRANSPORT_AUTHENTICATION_PROVIDER_USER = "user";
    String TRANSPORT_AUTHENTICATION_PROVIDER_USER_PROPERTY_USERNAME = "username";
    String TRANSPORT_AUTHENTICATION_PROVIDER_USER_PROPERTY_PASSWORD = "password";

    String TRIGGER_REMOTE_EVENT = "remoteEvent";
    String TRIGGER_REMOTE_EVENT_PROPERTY_ENDPOINT = "endpoint";


    String TRIGGER_RESOURCE_EVENT = "resourceEvent";
    String TRIGGER_RESOURCE_EVENT_PROPERTY_PATH = "path";

    String TRIGGER_SCHEDULED_EVENT = "scheduledEvent";
    String TRIGGER_SCHEDULED_EVENT_PROPERTY_ACTION = "action";
    String TRIGGER_SCHEDULED_EVENT_PROPERTY_PATH = "path";
    String TRIGGER_SCHEDULED_EVENT_PROPERTY_SECONDS = "seconds";

    String TRIGGER_REPLICATION_EVENT = "replicationEvent";
    String TRIGGER_REPLICATION_EVENT_PROPERTY_PATH = "path";


    String TRIGGER_JCR_EVENT = "jcrEvent";
    String TRIGGER_JCR_EVENT_PROPERTY_PATH = "path";
    String TRIGGER_JCR_EVENT_PROPERTY_SERVICE_NAME = "servicename";


    String TRIGGER_PERSISTED_JCR_EVENT = "persistedJcrEvent";
    String TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_PATH = "path";
    String TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_SERVICE_NAME = "servicename";
    String TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_NUGGETS_PATH = "nuggets.path";


    String REQUEST_AUTHORIZATION_STRATEGY_PRIVILEGE = "privilege";
    String REQUEST_AUTHORIZATION_STRATEGY_PRIVILEGE_PROPERTY_JCR_PRIVILEGE = "jcrPrivilege";

    String PACKAGE_BUILDER_FILEVLT = "vlt";


    /**
     * create a {@link org.apache.sling.replication.agent.ReplicationComponent}
     *
     * @param type              the <code>Class</code> of the component to be created
     * @param properties        the properties to be supplied for the initialization of the component
     * @param componentProvider the {@link org.apache.sling.replication.agent.ReplicationComponentProvider} used to eventually
     *                          wire additional required {@link org.apache.sling.replication.agent.ReplicationComponent}s
     * @param <ComponentType>   the actual type of the {@link org.apache.sling.replication.agent.ReplicationComponent}
     *                          to be created
     * @return
     */
    <ComponentType> ComponentType createComponent(java.lang.Class<ComponentType> type,
                                                  Map<String, Object> properties,
                                                  ReplicationComponentProvider componentProvider);
}
