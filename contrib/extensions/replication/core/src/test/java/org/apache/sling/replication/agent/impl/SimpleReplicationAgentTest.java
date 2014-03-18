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
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.queue.ReplicationQueueItemState;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.queue.impl.simple.SimpleReplicationQueue;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.transport.TransportHandler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link SimpleReplicationAgent}
 */
public class SimpleReplicationAgentTest {

    @Test
    public void testSyncReplicationWithFailingDistributionStrategy() throws Exception {
        String name = "sample-agent";
        TransportHandler transportHandler = mock(TransportHandler.class);
        ReplicationPackageBuilder packageBuilder = mock(ReplicationPackageBuilder.class);
        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        ReplicationQueueDistributionStrategy distributionHandler = mock(ReplicationQueueDistributionStrategy.class);
        ReplicationEventFactory replicationEventFactory = mock(ReplicationEventFactory.class);
        SimpleReplicationAgent agent = new SimpleReplicationAgent(name, new String[0], true,
                transportHandler, packageBuilder, queueProvider, distributionHandler, replicationEventFactory, null);
        ReplicationRequest request = new ReplicationRequest(System.nanoTime(),
                ReplicationActionType.ADD, "/");
        ReplicationPackage replicationPackage = mock(ReplicationPackage.class);
        when(replicationPackage.getPaths()).thenReturn(new String[]{"/"});
        when(packageBuilder.createPackage(request)).thenReturn(replicationPackage);
        when(queueProvider.getDefaultQueue(agent.getName())).thenReturn(
                new SimpleReplicationQueue(agent.getName(), "name"));
        ReplicationResponse response = agent.execute(request);
        assertNotNull(response);
        assertEquals("ERROR", response.getStatus());
    }

    @Test
    public void testSyncReplicationWithWorkingDistributionStrategy() throws Exception {
        String name = "sample-agent";
        TransportHandler transportHandler = mock(TransportHandler.class);
        ReplicationPackageBuilder packageBuilder = mock(ReplicationPackageBuilder.class);
        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        ReplicationQueueDistributionStrategy distributionHandler = mock(ReplicationQueueDistributionStrategy.class);
        ReplicationEventFactory replicationEventFactory = mock(ReplicationEventFactory.class);
        SimpleReplicationAgent agent = new SimpleReplicationAgent(name, new String[0], true,
                transportHandler, packageBuilder, queueProvider, distributionHandler, replicationEventFactory, null);
        ReplicationRequest request = new ReplicationRequest(System.nanoTime(),
                ReplicationActionType.ADD, "/");
        ReplicationPackage replicationPackage = mock(ReplicationPackage.class);
        when(replicationPackage.getPaths()).thenReturn(new String[]{"/"});
        ReplicationQueueItemState state = new ReplicationQueueItemState();
        state.setItemState(ReplicationQueueItemState.ItemState.SUCCEEDED);
        when(distributionHandler.add(any(String.class), any(ReplicationQueueItem.class), eq(queueProvider))).thenReturn(state);
        when(packageBuilder.createPackage(any(ReplicationRequest.class))).thenReturn(replicationPackage);
        when(queueProvider.getDefaultQueue(agent.getName())).thenReturn(
                new SimpleReplicationQueue(agent.getName(), "name"));
        ReplicationResponse response = agent.execute(request);
        assertNotNull(response);
        assertEquals("SUCCEEDED", response.getStatus());
    }

    @Test
    public void testAsyncReplication() throws Exception {
        String name = "sample-agent";
        TransportHandler transportHandler = mock(TransportHandler.class);
        ReplicationPackageBuilder packageBuilder = mock(ReplicationPackageBuilder.class);
        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        ReplicationQueueDistributionStrategy distributionHandler = mock(ReplicationQueueDistributionStrategy.class);
        ReplicationEventFactory replicationEventFactory = mock(ReplicationEventFactory.class);
        SimpleReplicationAgent agent = new SimpleReplicationAgent(name, new String[0], true,
                transportHandler, packageBuilder, queueProvider, distributionHandler, replicationEventFactory, null);
        ReplicationRequest request = new ReplicationRequest(System.nanoTime(),
                ReplicationActionType.ADD, "/");
        ReplicationPackage replicationPackage = mock(ReplicationPackage.class);
        when(replicationPackage.getPaths()).thenReturn(new String[]{"/"});
        when(packageBuilder.createPackage(request)).thenReturn(replicationPackage);
        when(queueProvider.getDefaultQueue(agent.getName())).thenReturn(
                new SimpleReplicationQueue(agent.getName(), "name"));
        agent.send(request);
    }

    @Test
    public void testGetDefaultQueue() throws Exception {
        String name = "sample-agent";
        TransportHandler transportHandler = mock(TransportHandler.class);
        ReplicationPackageBuilder packageBuilder = mock(ReplicationPackageBuilder.class);
        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        ReplicationQueueDistributionStrategy distributionHandler = mock(ReplicationQueueDistributionStrategy.class);
        SimpleReplicationAgent agent = new SimpleReplicationAgent(name, new String[0], true,
                transportHandler, packageBuilder, queueProvider, distributionHandler, null, null);
        ReplicationQueue queue = mock(ReplicationQueue.class);
        when(queueProvider.getDefaultQueue(agent.getName())).thenReturn(queue);
        assertNotNull(agent.getQueue(null));
    }

    @Test
    public void testGetExistingNamedQueue() throws Exception {
        String name = "sample-agent";
        String endpoint = "/tmp";
        TransportHandler transportHandler = mock(TransportHandler.class);
        ReplicationPackageBuilder packageBuilder = mock(ReplicationPackageBuilder.class);
        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        ReplicationQueueDistributionStrategy distributionHandler = mock(ReplicationQueueDistributionStrategy.class);
        SimpleReplicationAgent agent = new SimpleReplicationAgent(name, new String[0], true,
                transportHandler, packageBuilder, queueProvider, distributionHandler, null, null);
        ReplicationQueue queue = mock(ReplicationQueue.class);
        when(queueProvider.getQueue(agent.getName(), "priority")).thenReturn(queue);
        assertNotNull(agent.getQueue("priority"));
    }

    @Test
    public void testGetNonExistingNamedQueue() throws Exception {
        String name = "sample-agent";
        TransportHandler transportHandler = mock(TransportHandler.class);
        ReplicationPackageBuilder packageBuilder = mock(ReplicationPackageBuilder.class);
        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        ReplicationQueueDistributionStrategy distributionHandler = mock(ReplicationQueueDistributionStrategy.class);
        SimpleReplicationAgent agent = new SimpleReplicationAgent(name, new String[0], true,
                transportHandler, packageBuilder, queueProvider, distributionHandler, null, null);
        ReplicationQueue queue = mock(ReplicationQueue.class);
        when(queueProvider.getQueue(agent.getName(), "priority")).thenReturn(queue);
        assertNull(agent.getQueue("weird"));
    }
}
