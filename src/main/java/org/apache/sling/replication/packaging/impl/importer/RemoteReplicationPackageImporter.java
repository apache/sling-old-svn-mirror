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
package org.apache.sling.replication.packaging.impl.importer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.packaging.ReplicationPackageImporter;
import org.apache.sling.replication.serialization.ReplicationPackageReadingException;
import org.apache.sling.replication.transport.ReplicationTransportHandler;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.transport.impl.MultipleEndpointReplicationTransportHandler;
import org.apache.sling.replication.transport.impl.SimpleHttpReplicationTransportHandler;
import org.apache.sling.replication.transport.impl.TransportEndpointStrategyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remote implementation of {@link org.apache.sling.replication.packaging.ReplicationPackageImporter}
 */
public class RemoteReplicationPackageImporter implements ReplicationPackageImporter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ReplicationTransportHandler transportHandler;


    public RemoteReplicationPackageImporter(TransportAuthenticationProvider transportAuthenticationProvider,
                                            String[] endpoints,
                                            String  transportEndpointStrategyName) {

        if (transportAuthenticationProvider == null) {
            throw new IllegalArgumentException("transportAuthenticationProviderFactory is required");
        }

        TransportEndpointStrategyType transportEndpointStrategyType = TransportEndpointStrategyType.valueOf(transportEndpointStrategyName);


        List<ReplicationTransportHandler> transportHandlers = new ArrayList<ReplicationTransportHandler>();

        for (String endpoint : endpoints) {
            if (endpoint != null && endpoint.length() > 0) {
                transportHandlers.add(new SimpleHttpReplicationTransportHandler(transportAuthenticationProvider,
                        new ReplicationEndpoint(endpoint), null, -1));
            }
        }
        transportHandler = new MultipleEndpointReplicationTransportHandler(transportHandlers,
                transportEndpointStrategyType);

    }

    public boolean importPackage(ResourceResolver resourceResolver, ReplicationPackage replicationPackage) {
        boolean result = false;
        try {
            transportHandler.deliverPackage(resourceResolver, replicationPackage);
            result = true;
        } catch (Exception e) {
            log.error("failed in importing package {} ", replicationPackage, e);
        }
        return result;
    }

    public ReplicationPackage readPackage(ResourceResolver resourceResolver, InputStream stream) throws ReplicationPackageReadingException {
        return null;
    }

}
