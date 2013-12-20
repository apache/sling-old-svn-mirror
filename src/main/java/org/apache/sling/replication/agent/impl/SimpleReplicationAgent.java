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

import java.net.URI;
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
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic implementation of a {@link ReplicationAgent}
 */
public class SimpleReplicationAgent implements ReplicationAgent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ReplicationPackageBuilder packageBuilder;

    private final ReplicationQueueProvider queueProvider;

    private final TransportHandler transportHandler;

    private final TransportAuthenticationProvider<?, ?> transportAuthenticationProvider;

    private final ReplicationQueueDistributionStrategy queueDistributionStrategy;

    private final String name;

    private final String endpoint;

    private final String[] rules;

    public SimpleReplicationAgent(String name, String endpoint, String[] rules,
                                  TransportHandler transportHandler, ReplicationPackageBuilder packageBuilder,
                                  ReplicationQueueProvider queueProvider,
                                  TransportAuthenticationProvider<?, ?> transportAuthenticationProvider,
                                  ReplicationQueueDistributionStrategy queueDistributionHandler) {
        this.name = name;
        this.endpoint = endpoint;
        this.rules = rules;
        this.transportHandler = transportHandler;
        this.packageBuilder = packageBuilder;
        this.queueProvider = queueProvider;
        this.transportAuthenticationProvider = transportAuthenticationProvider;
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
            if (state != null) {
                replicationResponse.setStatus(state.getItemState().toString());
                replicationResponse.setSuccessful(state.isSuccessful());
            } else {
                replicationResponse.setStatus(ReplicationQueueItemState.ItemState.ERROR.toString());
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("an error happened during queue processing", e);
            }
            replicationResponse.setSuccessful(false);
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
            if (transportHandler != null || (endpoint != null && endpoint.length() > 0)) {
                transportHandler.transport(item, new ReplicationEndpoint(endpoint),
                        transportAuthenticationProvider);
                return true;
            } else {
                if (log.isInfoEnabled()) {
                    log.info("agent {} processing skipped", name);
                }
                return false;
            }
        } catch (ReplicationTransportException e) {
            throw new AgentReplicationException(e);
        }
    }

    public URI getEndpoint() {
        return new ReplicationEndpoint(endpoint).getUri();
    }

    public String getName() {
        return name;
    }

    public ReplicationQueue getQueue(String name) throws ReplicationQueueException {
        ReplicationQueue queue;
        if (name != null && name.length() > 0) {
            queue = queueProvider.getQueue(this, name);
        } else {
            queue = queueProvider.getDefaultQueue(this);
        }
        return queue;
    }

    public String[] getRules() {
        return rules;
    }
}
