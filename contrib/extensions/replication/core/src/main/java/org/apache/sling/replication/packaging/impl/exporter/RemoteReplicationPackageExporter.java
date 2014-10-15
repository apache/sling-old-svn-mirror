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
package org.apache.sling.replication.packaging.impl.exporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.packaging.ReplicationPackageExporter;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;
import org.apache.sling.replication.transport.ReplicationTransportHandler;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.transport.impl.MultipleEndpointReplicationTransportHandler;
import org.apache.sling.replication.transport.impl.ReplicationTransportConstants;
import org.apache.sling.replication.transport.impl.SimpleHttpReplicationTransportHandler;
import org.apache.sling.replication.transport.impl.TransportEndpointStrategyType;

/**
 * Default implementation of {@link org.apache.sling.replication.packaging.ReplicationPackageExporter}
 */
public class RemoteReplicationPackageExporter implements ReplicationPackageExporter {


    public static final String NAME = "remote";
    public static final String POLL_ITEMS = "poll.items";


    private final ReplicationPackageBuilder packageBuilder;

    ReplicationTransportHandler transportHandler;


    public RemoteReplicationPackageExporter(Map<String, Object> config,
                                            ReplicationPackageBuilder packageBuilder,
                                            TransportAuthenticationProvider transportAuthenticationProvider) {

        this(packageBuilder, transportAuthenticationProvider,
                PropertiesUtil.toStringArray(config.get(ReplicationTransportConstants.ENDPOINTS), new String[0]),
                PropertiesUtil.toString(config.get(ReplicationTransportConstants.ENDPOINT_STRATEGY), "One"),
                PropertiesUtil.toInteger(config.get(POLL_ITEMS), Integer.MAX_VALUE));
    }

    public RemoteReplicationPackageExporter(ReplicationPackageBuilder packageBuilder,
                                            TransportAuthenticationProvider transportAuthenticationProvider,
                                            String[] endpoints,
                                            String transportEndpointStrategyName,
                                            int pollItems) {
        if (packageBuilder == null) {
            throw new IllegalArgumentException("packageBuilder is required");
        }

        this.packageBuilder = packageBuilder;

        TransportEndpointStrategyType transportEndpointStrategyType = TransportEndpointStrategyType.valueOf(transportEndpointStrategyName);

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

    public List<ReplicationPackage> exportPackage(ResourceResolver resourceResolver, ReplicationRequest replicationRequest) throws ReplicationPackageBuildingException {
        try {
            return transportHandler.retrievePackages(resourceResolver, replicationRequest);
        } catch (Exception e) {
            throw new ReplicationPackageBuildingException(e);
        }
    }

    public ReplicationPackage exportPackageById(ResourceResolver resourceResolver, String replicationPackageId) {
        return packageBuilder.getPackage(resourceResolver, replicationPackageId);
    }
}
