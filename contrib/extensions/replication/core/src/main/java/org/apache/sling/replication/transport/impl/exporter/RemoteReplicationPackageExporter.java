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
package org.apache.sling.replication.transport.impl.exporter;

import org.apache.felix.scr.annotations.*;
import org.apache.http.client.fluent.Executor;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.serialization.*;
import org.apache.sling.replication.transport.ReplicationTransportHandler;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProviderFactory;
import org.apache.sling.replication.transport.impl.MultipleEndpointReplicationTransportHandler;
import org.apache.sling.replication.transport.impl.SimpleHttpReplicationTransportHandler;
import org.apache.sling.replication.transport.impl.TransportEndpointStrategyType;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link org.apache.sling.replication.serialization.ReplicationPackageExporter}
 */
@Component(label = "Remote Replication Package Exporter", configurationFactory = true)
@Service(value = ReplicationPackageExporter.class)
public class RemoteReplicationPackageExporter implements ReplicationPackageExporter {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property
    private static final String NAME = "name";

    @Property(name = ReplicationAgentConfiguration.TRANSPORT_AUTHENTICATION_FACTORY)
    @Reference(name = "TransportAuthenticationProviderFactory", policy = ReferencePolicy.DYNAMIC)
    private TransportAuthenticationProviderFactory transportAuthenticationProviderFactory;


    @Property(label = "Target ReplicationPackageBuilder", name = "ReplicationPackageBuilder.target")
    @Reference(name = "ReplicationPackageBuilder", policy = ReferencePolicy.STATIC)
    private ReplicationPackageBuilder packageBuilder;

    @Property(name = "poll items", description = "number of subsequent poll requests to make", intValue = 1)
    private static final String POLL_ITEMS = "poll.items";

    @Property(options = {
            @PropertyOption(name = "All",
                    value = "all endpoints"
            ),
            @PropertyOption(name = "One",
                    value = "one endpoint"
            )},
            value = "One"
    )
    private static final String ENDPOINT_STRATEGY = ReplicationAgentConfiguration.ENDPOINT_STRATEGY;

    int pollItems;
    ReplicationTransportHandler transportHandler;

    @Activate
    protected void activate(BundleContext context, Map<String, ?> config) throws Exception {

        Map<String, String> authenticationProperties = PropertiesUtil.toMap(config.get(ReplicationAgentConfiguration.AUTHENTICATION_PROPERTIES), new String[0]);

        TransportAuthenticationProvider<Executor, Executor> transportAuthenticationProvider = (TransportAuthenticationProvider<Executor, Executor>) transportAuthenticationProviderFactory.createAuthenticationProvider(authenticationProperties);

        String[] endpoints = PropertiesUtil.toStringArray(config.get(ReplicationAgentConfiguration.ENDPOINT), new String[0]);

        pollItems = PropertiesUtil.toInteger(config.get(POLL_ITEMS), 1);

        String endpointStrategyName = PropertiesUtil.toString(config.get(ReplicationAgentConfiguration.ENDPOINT_STRATEGY), "One");
        TransportEndpointStrategyType transportEndpointStrategyType = TransportEndpointStrategyType.valueOf(endpointStrategyName);

        List<ReplicationTransportHandler> transportHandlers = new ArrayList<ReplicationTransportHandler>();

        for (String endpoint : endpoints) {
            if (endpoint != null && endpoint.length() > 0) {
                transportHandlers.add(new SimpleHttpReplicationTransportHandler(transportAuthenticationProvider,
                        new ReplicationEndpoint(endpoint), packageBuilder, pollItems));
            }
        }
        transportHandler = new MultipleEndpointReplicationTransportHandler(transportHandlers,
                transportEndpointStrategyType);
    }

    @Deactivate
    protected void deactivate() {
    }

    public List<ReplicationPackage> exportPackage(ReplicationRequest replicationRequest) throws ReplicationPackageBuildingException {
        try {
            return transportHandler.retrievePackage();
        } catch (Exception e) {
            throw new ReplicationPackageBuildingException(e);
        }
    }

    public ReplicationPackage exportPackageById(String replicationPackageId) {
        return packageBuilder.getPackage(replicationPackageId);
    }
}
