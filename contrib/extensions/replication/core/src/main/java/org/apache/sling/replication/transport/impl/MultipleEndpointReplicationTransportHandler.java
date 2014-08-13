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
import java.util.List;

import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.transport.ReplicationTransportException;
import org.apache.sling.replication.transport.ReplicationTransportHandler;

/**
 * {@link org.apache.sling.replication.transport.ReplicationTransportHandler} supporting delivery / retrieval from multiple
 * endpoints.
 */
public class MultipleEndpointReplicationTransportHandler implements ReplicationTransportHandler {

    private final List<ReplicationTransportHandler> transportHelpers;
    private final TransportEndpointStrategyType endpointStrategyType;
    private int lastSuccessfulEnpointId = 0;

    public MultipleEndpointReplicationTransportHandler(List<ReplicationTransportHandler> transportHelpers,
                                                       TransportEndpointStrategyType endpointStrategyType) {
        this.transportHelpers = transportHelpers;
        this.endpointStrategyType = endpointStrategyType;
    }

    private List<ReplicationPackage> doTransport(boolean isPolling,
                                                 ReplicationPackage replicationPackage) throws ReplicationTransportException {

        int offset = 0;
        if (endpointStrategyType.equals(TransportEndpointStrategyType.One)) {
            offset = lastSuccessfulEnpointId;
        }

        int length = transportHelpers.size();
        List<ReplicationPackage> result = new ArrayList<ReplicationPackage>();

        for (int i = 0; i < length; i++) {
            int currentId = (offset + i) % length;

            ReplicationTransportHandler transportHelper = transportHelpers.get(currentId);
            if (!isPolling) {
                transportHelper.deliverPackage(replicationPackage);
            } else {
                List<ReplicationPackage> retrievedPackages = transportHelper.retrievePackage();
                result.addAll(retrievedPackages);
            }

            lastSuccessfulEnpointId = currentId;
            if (endpointStrategyType.equals(TransportEndpointStrategyType.One))
                break;
        }

        return result;
    }

    public void deliverPackage(ReplicationPackage replicationPackage) throws ReplicationTransportException {
        doTransport(false, replicationPackage);
    }

    public List<ReplicationPackage> retrievePackage() throws ReplicationTransportException {
        return doTransport(true, null);
    }


}
