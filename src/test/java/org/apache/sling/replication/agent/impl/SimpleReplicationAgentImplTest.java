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

import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.communication.ReplicationResponse;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.queue.impl.SingleQueueDistributionStrategy;
import org.apache.sling.replication.queue.impl.simple.SimpleReplicationQueue;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.transport.TransportHandler;
import org.apache.sling.replication.transport.authentication.AuthenticationHandler;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link SimpleReplicationAgentImpl}
 */
public class SimpleReplicationAgentImplTest {

    @Test
    public void testProcess() throws Exception {
        String name = "sample-agent";
        String endpoint = "/tmp";
        TransportHandler transportHandler = mock(TransportHandler.class);
        ReplicationPackageBuilder packageBuilder = mock(ReplicationPackageBuilder.class);
        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        AuthenticationHandler authenticationHandler = mock(AuthenticationHandler.class);
        ReplicationQueueDistributionStrategy distributionHandler = new SingleQueueDistributionStrategy();
        SimpleReplicationAgentImpl agent = new SimpleReplicationAgentImpl(name, endpoint,
                        transportHandler, packageBuilder, queueProvider, authenticationHandler, distributionHandler);
        ReplicationPackage item = mock(ReplicationPackage.class);
        assertTrue(agent.process(item));
    }

    @Test
    public void testSyncReplication() throws Exception {
        String name = "sample-agent";
        String endpoint = "/tmp";
        TransportHandler transportHandler = mock(TransportHandler.class);
        ReplicationPackageBuilder packageBuilder = mock(ReplicationPackageBuilder.class);
        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        AuthenticationHandler authenticationHandler = mock(AuthenticationHandler.class);
        ReplicationQueueDistributionStrategy distributionHandler = new SingleQueueDistributionStrategy();
        SimpleReplicationAgentImpl agent = new SimpleReplicationAgentImpl(name, endpoint,
                        transportHandler, packageBuilder, queueProvider, authenticationHandler, distributionHandler);
        ReplicationRequest request = new ReplicationRequest(System.nanoTime(),
                        ReplicationActionType.ACTIVATE, "/");
        ReplicationPackage replicationPackage = mock(ReplicationPackage.class);
        when(packageBuilder.createPackage(request)).thenReturn(replicationPackage);
        when(queueProvider.getOrCreateQueue(agent, replicationPackage)).thenReturn(
                        new SimpleReplicationQueue(agent));
        when(queueProvider.getOrCreateDefaultQueue(agent)).thenReturn(
              new SimpleReplicationQueue(agent));
        ReplicationResponse response = agent.execute(request);
        assertNotNull(response);
    }

    @Test
    public void testAsyncReplication() throws Exception {
        String name = "sample-agent";
        String endpoint = "/tmp";
        TransportHandler transportHandler = mock(TransportHandler.class);
        ReplicationPackageBuilder packageBuilder = mock(ReplicationPackageBuilder.class);
        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        AuthenticationHandler authenticationHandler = mock(AuthenticationHandler.class);
        ReplicationQueueDistributionStrategy distributionHandler = new SingleQueueDistributionStrategy();
        SimpleReplicationAgentImpl agent = new SimpleReplicationAgentImpl(name, endpoint,
                transportHandler, packageBuilder, queueProvider, authenticationHandler, distributionHandler);
        ReplicationRequest request = new ReplicationRequest(System.nanoTime(),
                ReplicationActionType.ACTIVATE, "/");
        ReplicationPackage replicationPackage = mock(ReplicationPackage.class);
        when(packageBuilder.createPackage(request)).thenReturn(replicationPackage);
        when(queueProvider.getOrCreateQueue(agent, replicationPackage)).thenReturn(
                new SimpleReplicationQueue(agent));
        when(queueProvider.getOrCreateDefaultQueue(agent)).thenReturn(
                new SimpleReplicationQueue(agent));
        agent.send(request);
    }
}
