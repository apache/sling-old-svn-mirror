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
import java.util.Enumeration;
import java.util.Map;

import org.apache.sling.commons.osgi.PropertiesUtil;

/**
 * configuration for {@link ReplicationAgent}s
 */
public class ReplicationAgentConfiguration {

    public static final String TRANSPORT = "TransportHandler.target";

    public static final String TRANSPORT_AUTHENTICATION_FACTORY = "TransportAuthenticationProviderFactory.target";

    public static final String QUEUEPROVIDER = "ReplicationQueueProvider.target";

    public static final String PACKAGING = "ReplicationPackageBuilder.target";

    public static final String NAME = "name";

    public static final String ENDPOINT = "endpoint";

    public static final String AUTHENTICATION_PROPERTIES = "authentication.properties";

    public static final String QUEUE_DISTRIBUTION = "ReplicationQueueDistributionStrategy.target";
    public static final String RULES = "rules";

    public static final String ENABLED = "enabled";

    public static final String USE_AGGREGATE_PATHS = "useAggregatePaths";

    public static final String[] COMPONENTS = { TRANSPORT, PACKAGING };

    private final boolean enabled;

    private final String name;

    private final String endpoint;

    private final String targetTransportHandler;

    private final String targetReplicationPackageBuilder;

    private final String targetReplicationQueueProvider;

    private final String targetReplicationQueueDistributionStrategy;

    private final String targetAuthenticationHandlerFactory;

    private final String[] authenticationProperties;

    private final String[] rules;

    private final boolean useAggregatePaths;

    private final Dictionary<String, Dictionary> componentConfiguration;

    public ReplicationAgentConfiguration(Dictionary<?, ?> dictionary, Dictionary<String, Dictionary> componentConfiguration) {
        this.name = PropertiesUtil.toString(dictionary.get(NAME), "");
        this.enabled = PropertiesUtil.toBoolean(dictionary.get(ENABLED), true);
        this.endpoint = PropertiesUtil.toString(dictionary.get(ENDPOINT), "");
        this.targetAuthenticationHandlerFactory = PropertiesUtil.toString(
                dictionary.get(TRANSPORT_AUTHENTICATION_FACTORY), "");
        this.targetReplicationPackageBuilder = PropertiesUtil.toString(dictionary.get(PACKAGING), "");
        this.targetReplicationQueueProvider = PropertiesUtil.toString(
                dictionary.get(QUEUEPROVIDER), "");
        this.targetReplicationQueueDistributionStrategy = PropertiesUtil.toString(dictionary.get(QUEUE_DISTRIBUTION), "");
        this.targetTransportHandler = PropertiesUtil.toString(dictionary.get(TRANSPORT), "");
        String[] ap = PropertiesUtil.toStringArray(dictionary.get(AUTHENTICATION_PROPERTIES));
        this.authenticationProperties = ap != null ? ap : new String[0];
        this.rules = PropertiesUtil.toStringArray(dictionary.get(RULES), new String[0]);
        this.useAggregatePaths = PropertiesUtil.toBoolean(dictionary.get(USE_AGGREGATE_PATHS), true);

        this.componentConfiguration = componentConfiguration;
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

    public String getTargetReplicationPackageBuilder() {
        return targetReplicationPackageBuilder;
    }

    public String getTargetReplicationQueueProvider() {
        return targetReplicationQueueProvider;
    }

    public String getTargetTransportHandler() {
        return targetTransportHandler;
    }

    public String getTargetReplicationQueueDistributionStrategy() { return targetReplicationQueueDistributionStrategy; }

    @Override
    public String toString() {
        String result = "{\"";

        result += NAME + "\":\"" + name + "\", \""
                + ENDPOINT + "\":\"" + endpoint + "\", \""
                + TRANSPORT + "\":\"" + targetTransportHandler + "\", \""
                + PACKAGING + "\":\"" + targetReplicationPackageBuilder + "\", \""
                + QUEUEPROVIDER + "\":\"" + targetReplicationQueueProvider + "\", \""
                + QUEUE_DISTRIBUTION + "\":\"" + targetReplicationQueueDistributionStrategy+ "\", \""
                + TRANSPORT_AUTHENTICATION_FACTORY + "\":\"" + targetAuthenticationHandlerFactory + "\", \""
                + USE_AGGREGATE_PATHS + "\":\"" + useAggregatePaths + "\", \""
                + AUTHENTICATION_PROPERTIES + "\":\"" + Arrays.toString(authenticationProperties) + "\", \"";

        result += toComponentString();

        result += RULES + "\":\"" + Arrays.toString(rules) + "\"}";
        return result;
    }


    private String toComponentString() {

        String result = "";

        if(componentConfiguration == null)
            return result;

        for (String component : COMPONENTS){
            Dictionary properties = componentConfiguration.get(component);
            if(properties == null) continue;

            Enumeration keys = properties.keys();

            while (keys.hasMoreElements()){
                String key = (String) keys.nextElement();
                Object value = properties.get(key);

                if(key.equals("service.pid")) continue;

                result += component + "." + key + "\":\"" +  PropertiesUtil.toString(value, "")  + "\", \"";
            }
        }

        return  result;
    }

    public String toSimpleString() {
        String result = "{";

        result += "\"" + NAME + "\": \"" + name + "\""
                + ", \"" + ENABLED + "\": " + enabled;

        result += "}";

        return result;
    }
}
