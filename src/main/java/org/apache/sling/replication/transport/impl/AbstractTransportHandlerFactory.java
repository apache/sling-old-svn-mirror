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
package org.apache.sling.replication.transport.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.transport.TransportHandler;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProviderFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public abstract class AbstractTransportHandlerFactory {

    private ServiceRegistration serviceRegistration;

    protected void activate(BundleContext context, Map<String, ?> config) throws Exception {

        // inject configuration
        Dictionary<String, Object> props = new Hashtable<String, Object>();

        boolean enabled = PropertiesUtil.toBoolean(config.get(ReplicationAgentConfiguration.ENABLED), true);
        if (enabled) {
            String name = PropertiesUtil
                    .toString(config.get(ReplicationAgentConfiguration.NAME), String.valueOf(new Random().nextInt(1000)));
            props.put(ReplicationAgentConfiguration.NAME, name);

            Map<String, String> authenticationProperties = PropertiesUtil.toMap(config.get(ReplicationAgentConfiguration.AUTHENTICATION_PROPERTIES), new String[0]);
            props.put(ReplicationAgentConfiguration.AUTHENTICATION_PROPERTIES, authenticationProperties);

            String[] endpoints = PropertiesUtil.toStringArray(config.get(ReplicationAgentConfiguration.ENDPOINT), new String[0]);
            props.put(ReplicationAgentConfiguration.ENDPOINT, endpoints);

            String endpointStrategyName = PropertiesUtil.toString(config.get(ReplicationAgentConfiguration.ENDPOINT_STRATEGY), "All");
            props.put(ReplicationAgentConfiguration.ENDPOINT_STRATEGY, endpointStrategyName);

            TransportEndpointStrategyType transportEndpointStrategyType = TransportEndpointStrategyType.valueOf(endpointStrategyName);


            TransportAuthenticationProviderFactory transportAuthenticationProviderFactory = getAuthenticationFactory();
            TransportAuthenticationProvider transportAuthenticationProvider = null;
            if (transportAuthenticationProviderFactory != null) {
                transportAuthenticationProvider = transportAuthenticationProviderFactory.createAuthenticationProvider(authenticationProperties);
            }

            List<ReplicationEndpoint> replicationEndpoints = new ArrayList<ReplicationEndpoint>();

            for (String endpoint : endpoints) {
                if (endpoint != null && endpoint.length() > 0) {
                    replicationEndpoints.add(new ReplicationEndpoint(endpoint));
                }
            }

            // register transport handler
            TransportHandler transportHandler = createTransportHandler(config,
                    props,
                    transportAuthenticationProvider,
                    replicationEndpoints.toArray(new ReplicationEndpoint[replicationEndpoints.size()]),
                    transportEndpointStrategyType);


            if (transportHandler == null) {
                throw new Exception("could not create transport handler");
            }

            serviceRegistration = context.registerService(TransportHandler.class.getName(), transportHandler, props);
        }
    }

    protected void deactivate() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }

    protected abstract TransportHandler createTransportHandler(Map<String, ?> config,
                                                               Dictionary<String, Object> props,
                                                               TransportAuthenticationProvider transportAuthenticationProvider,
                                                               ReplicationEndpoint[] endpoints,
                                                               TransportEndpointStrategyType endpointStrategyType);

    protected abstract TransportAuthenticationProviderFactory getAuthenticationFactory();

}
