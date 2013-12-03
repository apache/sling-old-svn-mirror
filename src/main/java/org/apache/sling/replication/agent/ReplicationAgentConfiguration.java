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

import java.util.Arrays;
import java.util.Dictionary;

import org.apache.sling.commons.osgi.PropertiesUtil;

/**
 * configuration for {@link ReplicationAgent}s
 */
public class ReplicationAgentConfiguration {

    public static final String TRANSPORT = "TransportHandler.target";

    public static final String AUTHENTICATION_FACTORY = "AuthenticationHandlerFactory.target";

    public static final String QUEUEPROVIDER = "ReplicationQueueProvider.target";

    public static final String PACKAGING = "ReplicationPackageBuilder.target";

    public static final String NAME = "name";

    public static final String ENDPOINT = "endpoint";

    public static final String AUTHENTICATION_PROPERTIES = "authentication.properties";

    public static final String DISTRIBUTION = "ReplicationQueueDistributionStrategy.target";

    private final String name;

    private final String endpoint;

    private final String targetTransportHandler;

    private final String targetReplicationBuilder;

    private final String targetReplicationQueueProvider;

    private final String targetAuthenticationHandlerFactory;

    private final String[] authenticationProperties;

    public ReplicationAgentConfiguration(Dictionary<?, ?> dictionary) {
        this.name = PropertiesUtil.toString(dictionary.get(NAME), "");
        this.endpoint = PropertiesUtil.toString(dictionary.get(ENDPOINT), "");
        this.targetAuthenticationHandlerFactory = PropertiesUtil.toString(
                        dictionary.get(AUTHENTICATION_FACTORY), "");
        this.targetReplicationBuilder = PropertiesUtil.toString(dictionary.get(PACKAGING), "");
        this.targetReplicationQueueProvider = PropertiesUtil.toString(
                        dictionary.get(QUEUEPROVIDER), "");
        this.targetTransportHandler = PropertiesUtil.toString(dictionary.get(TRANSPORT), "");
        String[] ap = PropertiesUtil.toStringArray(dictionary.get(AUTHENTICATION_PROPERTIES));
        this.authenticationProperties = ap != null ? ap : new String[0];
    }

    public String[] getAuthenticationProperties() {
        return authenticationProperties;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getName() {
        return name;
    }

    public String getTargetAuthenticationHandlerFactory() {
        return targetAuthenticationHandlerFactory;
    }

    public String getTargetReplicationBuilder() {
        return targetReplicationBuilder;
    }

    public String getTargetReplicationQueueProvider() {
        return targetReplicationQueueProvider;
    }

    public String getTargetTransportHandler() {
        return targetTransportHandler;
    }

    @Override
    public String toString() {
        return "{\"name\":\"" + name + "\", \"endpoint\":\"" + endpoint + "\", \"targetTransportHandler\":\""
                        + targetTransportHandler + "\", \"targetReplicationBuilder\":\""
                        + targetReplicationBuilder + "\", \"targetReplicationQueueProvider\":\""
                        + targetReplicationQueueProvider + "\", \"targetAuthenticationHandlerFactory\":\""
                        + targetAuthenticationHandlerFactory + "\", \"authenticationProperties\":\""
                        + Arrays.toString(authenticationProperties) + "\"}";
    }

}
