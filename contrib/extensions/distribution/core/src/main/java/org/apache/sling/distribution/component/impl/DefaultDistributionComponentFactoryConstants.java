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

/**
 *  Contains the string constants used by {@link DefaultDistributionComponentFactory}
 */
public final class DefaultDistributionComponentFactoryConstants {
    /**
     * the type of the component
     */
    public static final String COMPONENT_TYPE = "type";

    /**
     * the name of the component
     */
    public static final String COMPONENT_NAME = "name";

    /**
     * type for components referencing OSGi services
     */
    public static final String COMPONENT_TYPE_SERVICE = "service";

    /**
     * component enabled
     */
    public static final String COMPONENT_ENABLED = "enabled";


    /**
     * distribution agent component
     */
    public static final String COMPONENT_AGENT = "agent";

    /**
     * package exporter component
     */
    public static final String COMPONENT_PACKAGE_EXPORTER = "packageExporter";

    /**
     * package importer component
     */
    public static final String COMPONENT_PACKAGE_IMPORTER = "packageImporter";

    /**
     * request authorization strategy component
     */
    public static final String COMPONENT_REQUEST_AUTHORIZATION_STRATEGY = "requestAuthorizationStrategy";

    /**
     * queue distribution strategy component
     */
    public static final String COMPONENT_QUEUE_DISTRIBUTION_STRATEGY = "queueDistributionStrategy";

    /**
     * queue provider component
     */
    public static final String COMPONENT_QUEUE_PROVIDER = "queueProvider";

    /**
     * trigger component
     */
    public static final String COMPONENT_TRIGGER = "trigger";

    /**
     * package builder component
     */
    public static final String COMPONENT_PACKAGE_BUILDER = "packageBuilder";

    /**
     * transport authentication provider component
     */
    public static final String COMPONENT_TRANSPORT_AUTHENTICATION_PROVIDER = "transportAuthenticationProvider";

    /**
     * simple distribution agent type
     */
    public static final String AGENT_SIMPLE = "simple";

    /**
     * service user property
     */
    public static final String AGENT_SIMPLE_PROPERTY_SERVICE_NAME = "serviceName";

    /**
     * 'passive' property for agents (for defining "queueing agents")
     */
    public static final String AGENT_SIMPLE_PROPERTY_IS_PASSIVE = "isPassive";

    /**
     * local package exporter type
     */
    public static final String PACKAGE_EXPORTER_LOCAL = "local";

    /**
     * remote package exporter type
     */
    public static final String PACKAGE_EXPORTER_REMOTE = "remote";

    /**
     * endpoints property
     */
    public static final String PACKAGE_EXPORTER_REMOTE_PROPERTY_ENDPOINTS = "endpoints";

    /**
     * endpoint strategy property
     */
    public static final String PACKAGE_EXPORTER_REMOTE_PROPERTY_ENDPOINTS_STRATEGY = "endpoints.strategy";

    /**
     * no. of items to poll property
     */
    public static final String PACKAGE_EXPORTER_REMOTE_PROPERTY_POLL_ITEMS = "poll.items";

    /**
     * package exporter's agent property
     */
    public static final String PACKAGE_EXPORTER_AGENT = "agent";

    /**
     * local package importer type
     */
    public static final String PACKAGE_IMPORTER_LOCAL = "local";

    /**
     * remote package importer type
     */
    public static final String PACKAGE_IMPORTER_REMOTE = "remote";

    /**
     * endpoints property
     */
    public static final String PACKAGE_IMPORTER_REMOTE_PROPERTY_ENDPOINTS = "endpoints";

    /**
     * endpoint strategy property
     */
    public static final String PACKAGE_IMPORTER_REMOTE_PROPERTY_ENDPOINTS_STRATEGY = "endpoints.strategy";

    /**
     * user property
     */
    public static final String TRANSPORT_AUTHENTICATION_PROVIDER_USER = "user";

    /**
     * username property
     */
    public static final String TRANSPORT_AUTHENTICATION_PROVIDER_USER_PROPERTY_USERNAME = "username";

    /**
     * password property
     */
    public static final String TRANSPORT_AUTHENTICATION_PROVIDER_USER_PROPERTY_PASSWORD = "password";

    /**
     * remote event trigger type
     */
    public static final String TRIGGER_REMOTE_EVENT = "remoteEvent";

    /**
     * remote event endpoint property
     */
    public static final String TRIGGER_REMOTE_EVENT_PROPERTY_ENDPOINT = "endpoint";

    /**
     * resource event trigger type
     */
    public static final String TRIGGER_RESOURCE_EVENT = "resourceEvent";

    /**
     * resource event path property
     */
    public static final String TRIGGER_RESOURCE_EVENT_PROPERTY_PATH = "path";

    /**
     * scheduled trigger type
     */
    public static final String TRIGGER_SCHEDULED_EVENT = "scheduledEvent";

    /**
     * scheduled trigger action property
     */
    public static final String TRIGGER_SCHEDULED_EVENT_PROPERTY_ACTION = "action";

    /**
     * scheduled trigger path property
     */
    public static final String TRIGGER_SCHEDULED_EVENT_PROPERTY_PATH = "path";

    /**
     * scheduled trigger seconds property
     */
    public static final String TRIGGER_SCHEDULED_EVENT_PROPERTY_SECONDS = "seconds";

    /**
     * chain distribution trigger type
     */
    public static final String TRIGGER_DISTRIBUTION_EVENT = "distributionEvent";

    /**
     * chain distribution path property
     */
    public static final String TRIGGER_DISTRIBUTION_EVENT_PROPERTY_PATH = "path";

    /**
     * jcr event trigger type
     */
    public static final String TRIGGER_JCR_EVENT = "jcrEvent";

    /**
     * jcr event trigger path property
     */
    public static final String TRIGGER_JCR_EVENT_PROPERTY_PATH = "path";

    /**
     * jcr event trigger service user property
     */
    public static final String TRIGGER_JCR_EVENT_PROPERTY_SERVICE_NAME = "servicename";

    /**
     * jcr persisting event trigger type
     */
    public static final String TRIGGER_PERSISTED_JCR_EVENT = "persistedJcrEvent";

    /**
     * jcr persisting event trigger path property
     */
    public static final String TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_PATH = "path";

    /**
     * jcr persisting event trigger service user property
     */
    public static final String TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_SERVICE_NAME = "servicename";

    /**
     * jcr persisting event trigger nuggets path property
     */
    public static final String TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_NUGGETS_PATH = "nuggetsPath";

    /**
     * privilege request authorization strategy type
     */
    public static final String REQUEST_AUTHORIZATION_STRATEGY_PRIVILEGE = "privilege";

    /**
     * privilege request authorization strategy jcr privilege property
     */
    public static final String REQUEST_AUTHORIZATION_STRATEGY_PRIVILEGE_PROPERTY_JCR_PRIVILEGE = "jcrPrivilege";

    /**
     * file vault package builder type
     */
    public static final String PACKAGE_BUILDER_FILEVLT = "vlt";

    /**
     * import mode property for file vault package builder
     */
    public static final String PACKAGE_BUILDER_FILEVLT_IMPORT_MODE = "importMode";

    /**
     * ACL handling property for file vault package builder
     */
    public static final String PACKAGE_BUILDER_FILEVLT_ACLHANDLING = "aclHandling";

    /**
     * queue provider job type
     */
    public static final String QUEUE_PROVIDER_JOB = "job";

    /**
     * property for default topics
     */
    public static final String QUEUE_PROVIDER_PROPERTY_QUEUE_PREFIX = "queue.prefix";


    /**
     * queue provider simple type
     */
    public static final String QUEUE_PROVIDER_SIMPLE = "simple";

    /**
     * queue distribution strategy single type
     */
    public static final String QUEUE_DISTRIBUTION_STRATEGY_SINGLE = "single";

    /**
     * queue distribution strategy priority type
     */
    public static final String QUEUE_DISTRIBUTION_STRATEGY_PRIORITY = "priority";

    /**
     * queue distribution strategy priority paths property
     */
    public static final String QUEUE_DISTRIBUTION_STRATEGY_PRIORITY_PROPERTY_PATHS = "priority.paths";

}
