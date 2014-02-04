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

import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.transport.ReplicationTransportException;
import org.apache.sling.replication.transport.TransportHandler;

public abstract class AbstractTransportHandler implements TransportHandler {
    private final ReplicationEndpoint[] endpoints;
    private final TransportEndpointStrategyType endpointStrategyType;

    private int lastSuccessfulEnpointId = 0;

    public AbstractTransportHandler(ReplicationEndpoint[] endpoints, TransportEndpointStrategyType endpointStrategyType) {
        this.endpoints = endpoints;
        this.endpointStrategyType = endpointStrategyType;
    }

    public void transport(ReplicationPackage replicationPackage)
            throws ReplicationTransportException {

        ReplicationTransportException lastException = null;
        int offset = 0;
        if (endpointStrategyType.equals(TransportEndpointStrategyType.OneSuccessful)) {
            offset = lastSuccessfulEnpointId;
        }

        for (int i = 0; i < endpoints.length; i++) {
            int currentId = (offset + i) % endpoints.length;

            ReplicationEndpoint replicationEndpoint = endpoints[currentId];
            try {
                deliverPackage(replicationPackage, replicationEndpoint);
                lastSuccessfulEnpointId = currentId;
                if (endpointStrategyType.equals(TransportEndpointStrategyType.FirstSuccessful) ||
                        endpointStrategyType.equals(TransportEndpointStrategyType.OneSuccessful))
                    return;
            } catch (ReplicationTransportException ex) {
                lastException = ex;
            }
        }

        if (lastException != null)
            throw lastException;

    }

    private void deliverPackage(ReplicationPackage replicationPackage, ReplicationEndpoint replicationEndpoint)
            throws ReplicationTransportException {
        if (!validateEndpoint(replicationEndpoint))
            throw new ReplicationTransportException("invalid endpoint " + replicationEndpoint.getUri());

        try {
            deliverPackageToEndpoint(replicationPackage, replicationEndpoint);
        } catch (Exception e) {
            throw new ReplicationTransportException(e);
        }
    }

    protected abstract void deliverPackageToEndpoint(ReplicationPackage replicationPackage,
                                                     ReplicationEndpoint replicationEndpoint) throws Exception;

    protected abstract boolean validateEndpoint(ReplicationEndpoint endpoint);

}
