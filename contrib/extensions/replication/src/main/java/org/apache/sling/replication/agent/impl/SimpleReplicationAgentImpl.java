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
package org.apache.sling.replication.agent.impl;

import org.apache.sling.replication.agent.AgentReplicationException;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.communication.ReplicationResponse;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.queue.ReplicationQueueException;
import org.apache.sling.replication.queue.ReplicationQueueItemState;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;
import org.apache.sling.replication.transport.ReplicationTransportException;
import org.apache.sling.replication.transport.TransportHandler;
import org.apache.sling.replication.transport.authentication.AuthenticationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic implementation of a {@link ReplicationAgent}
 */
public class SimpleReplicationAgentImpl implements ReplicationAgent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ReplicationPackageBuilder packageBuilder;

    private ReplicationQueueProvider queueProvider;

    private TransportHandler transportHandler;

    private AuthenticationHandler<?, ?> authenticationHandler;

    private ReplicationQueueDistributionStrategy queueDistributionStrategy;

    private String name;

    private String endpoint;

    public SimpleReplicationAgentImpl(String name, String endpoint,
                    TransportHandler transportHandler, ReplicationPackageBuilder packageBuilder,
                    ReplicationQueueProvider queueProvider,
                    AuthenticationHandler<?, ?> authenticationHandler,
                    ReplicationQueueDistributionStrategy queueDistributionHandler) {
        this.name = name;
        this.endpoint = endpoint;
        this.transportHandler = transportHandler;
        this.packageBuilder = packageBuilder;
        this.queueProvider = queueProvider;
        this.authenticationHandler = authenticationHandler;
        this.queueDistributionStrategy = queueDistributionHandler;
    }

    public ReplicationResponse execute(ReplicationRequest replicationRequest)
                    throws AgentReplicationException {

        // create package from request
        ReplicationPackage replicationPackage;
        try {
            replicationPackage = packageBuilder.createPackage(replicationRequest);
        } catch (ReplicationPackageBuildingException e) {
            throw new AgentReplicationException(e);
        }

        ReplicationResponse replicationResponse = new ReplicationResponse();

        // send the replication package to the queue distribution handler
        try {
            ReplicationQueueItemState state = queueDistributionStrategy.add(replicationPackage,
                            this, queueProvider);
            replicationResponse.setStatus(state.getItemState().toString());
            replicationResponse.setSuccessfull(state.isSuccessfull());
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("an error happened during queue processing", e);
            }
            replicationResponse.setSuccessfull(false);
        }

        return replicationResponse;
    }

    public void send(ReplicationRequest replicationRequest) throws AgentReplicationException {
        // create package from request
        ReplicationPackage replicationPackage;
        try {
            replicationPackage = packageBuilder.createPackage(replicationRequest);
        } catch (ReplicationPackageBuildingException e) {
            throw new AgentReplicationException(e);
        }
        try {
            queueDistributionStrategy.offer(replicationPackage, this, queueProvider);
        } catch (ReplicationQueueException e) {
            throw new AgentReplicationException(e);
        }
    }

    public boolean process(ReplicationPackage item) throws AgentReplicationException {
        try {
            if (transportHandler != null) {
                transportHandler.transport(item, new ReplicationEndpoint(endpoint),
                                authenticationHandler);
                return true;
            } else {
                if (log.isWarnEnabled()) {
                    log.warn("could not process an item as a transport handler is not bound to agent {}",
                                    name);
                }
                return false;
            }
        } catch (ReplicationTransportException e) {
            throw new AgentReplicationException(e);
        }
    }

    public String getName() {
        return name;
    }

    public ReplicationQueue getQueue(String name) throws ReplicationQueueException {
        ReplicationQueue queue;
        if (name != null) {
            queue = queueProvider.getOrCreateQueue(this, name);
        } else {
            queue = queueProvider.getOrCreateDefaultQueue(this);
        }
        return queue;
    }

}
