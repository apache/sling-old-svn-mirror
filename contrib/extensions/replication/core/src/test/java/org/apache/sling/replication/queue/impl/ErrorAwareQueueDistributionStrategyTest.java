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
package org.apache.sling.replication.queue.impl;

import java.util.Dictionary;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.queue.ReplicationQueueItemState;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link org.apache.sling.replication.queue.impl.ErrorAwareQueueDistributionStrategy}
 */
public class ErrorAwareQueueDistributionStrategyTest {

    @Test
    public void testPackageAdditionWithSucceedingItemDelivery() throws Exception {
        ErrorAwareQueueDistributionStrategy errorAwareDistributionStrategy = new ErrorAwareQueueDistributionStrategy();
        ReplicationQueueItem replicationPackage = mock(ReplicationQueueItem.class);
        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        ReplicationQueue queue = mock(ReplicationQueue.class);
        when(queueProvider.getDefaultQueue("agentName")).thenReturn(queue);
        when(queue.add(replicationPackage)).thenReturn(true);
        ReplicationQueueItemState state = mock(ReplicationQueueItemState.class);
        when(state.isSuccessful()).thenReturn(true);
        when(queue.getStatus(replicationPackage)).thenReturn(state);
        ReplicationQueueItemState returnedState = errorAwareDistributionStrategy.add("agentName", replicationPackage, queueProvider);
        assertNotNull(returnedState);
        assertTrue(returnedState.isSuccessful());
    }

    @Test
    public void testPackageAdditionWithFailingItemDelivery() throws Exception {
        ErrorAwareQueueDistributionStrategy errorAwareDistributionStrategy = new ErrorAwareQueueDistributionStrategy();
        ReplicationQueueItem replicationPackage = mock(ReplicationQueueItem.class);
        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        ReplicationQueue queue = mock(ReplicationQueue.class);
        when(queueProvider.getDefaultQueue("agentName")).thenReturn(queue);
        when(queue.add(replicationPackage)).thenReturn(true);
        ReplicationQueueItemState state = mock(ReplicationQueueItemState.class);
        when(state.isSuccessful()).thenReturn(false);
        when(queue.getStatus(replicationPackage)).thenReturn(state);
        ReplicationQueueItemState returnedState = errorAwareDistributionStrategy.add("agentName", replicationPackage, queueProvider);
        assertNotNull(returnedState);
        assertFalse(returnedState.isSuccessful());
    }

    @Test
    public void testPackageAdditionWithMultipleFailingItemsDeliveryAndErrorQueue() throws Exception {
        ErrorAwareQueueDistributionStrategy errorAwareDistributionStrategy = new ErrorAwareQueueDistributionStrategy();
        ComponentContext context = mock(ComponentContext.class);
        Dictionary properties = mock(Dictionary.class);
        when(properties.get("attempts.threshold")).thenReturn(new String[]{"1"});
        when(properties.get("stuck.handling")).thenReturn(new String[]{"ERROR"});
        when(context.getProperties()).thenReturn(properties);
        errorAwareDistributionStrategy.activate(context);
        ReplicationQueueItem replicationPackage = mock(ReplicationQueueItem.class);
        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        ReplicationQueue queue = mock(ReplicationQueue.class);
        when(queueProvider.getDefaultQueue("agentName")).thenReturn(queue);
        when(queue.add(replicationPackage)).thenReturn(true);
        when(queue.getHead()).thenReturn(replicationPackage);
        ReplicationQueue errorQueue = mock(ReplicationQueue.class);
        when(errorQueue.add(replicationPackage)).thenReturn(true);
        when(queueProvider.getQueue("agentName", "-error")).thenReturn(errorQueue);
        ReplicationQueueItemState state = mock(ReplicationQueueItemState.class);
        when(state.isSuccessful()).thenReturn(false);
        when(state.getAttempts()).thenReturn(2);
        when(queue.getStatus(any(ReplicationQueueItem.class))).thenReturn(state);
        ReplicationQueueItemState returnedState = errorAwareDistributionStrategy.add("agentName", replicationPackage, queueProvider);
        assertNotNull(returnedState);
        assertFalse(returnedState.isSuccessful());
    }

    @Test
    public void testPackageAdditionWithMultipleFailingItemsDeliveryAndDropFromQueue() throws Exception {
        ErrorAwareQueueDistributionStrategy errorAwareDistributionStrategy = new ErrorAwareQueueDistributionStrategy();
        ComponentContext context = mock(ComponentContext.class);
        Dictionary properties = mock(Dictionary.class);
        when(properties.get("attempts.threshold")).thenReturn(new String[]{"1"});
        when(properties.get("stuck.handling")).thenReturn(new String[]{"DROP"});
        when(context.getProperties()).thenReturn(properties);
        errorAwareDistributionStrategy.activate(context);
        ReplicationQueueItem replicationPackage = mock(ReplicationQueueItem.class);
        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        ReplicationQueue queue = mock(ReplicationQueue.class);
        when(queueProvider.getDefaultQueue("agentName")).thenReturn(queue);
        when(queue.add(replicationPackage)).thenReturn(true);
        when(queue.getHead()).thenReturn(replicationPackage);
        ReplicationQueueItemState state = mock(ReplicationQueueItemState.class);
        when(state.isSuccessful()).thenReturn(false);
        when(state.getAttempts()).thenReturn(2);
        when(queue.getStatus(any(ReplicationQueueItem.class))).thenReturn(state);
        ReplicationQueueItemState returnedState = errorAwareDistributionStrategy.add("agentName", replicationPackage, queueProvider);
        assertNotNull(returnedState);
        assertFalse(returnedState.isSuccessful());
    }

    @Test
    public void testPackageAdditionWithNullItemStateFromTheQueue() throws Exception {
        ErrorAwareQueueDistributionStrategy errorAwareDistributionStrategy = new ErrorAwareQueueDistributionStrategy();
        ReplicationQueueItem replicationPackage = mock(ReplicationQueueItem.class);
        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        ReplicationQueue queue = mock(ReplicationQueue.class);
        when(queueProvider.getDefaultQueue("agentName")).thenReturn(queue);
        when(queue.add(replicationPackage)).thenReturn(true);
        ReplicationQueueItemState returnedState = errorAwareDistributionStrategy.add("agentName", replicationPackage, queueProvider);
        assertNull(returnedState);
    }

    @Test
    public void testPackageAdditionWithNotNullItemStateFromTheQueue() throws Exception {
        ErrorAwareQueueDistributionStrategy errorAwareDistributionStrategy = new ErrorAwareQueueDistributionStrategy();
        ReplicationQueueItem replicationPackage = mock(ReplicationQueueItem.class);
        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        ReplicationQueue queue = mock(ReplicationQueue.class);
        when(queueProvider.getDefaultQueue("agentName")).thenReturn(queue);
        when(queue.add(replicationPackage)).thenReturn(true);
        ReplicationQueueItemState state = mock(ReplicationQueueItemState.class);
        when(queue.getStatus(replicationPackage)).thenReturn(state);
        ReplicationQueueItemState returnedState = errorAwareDistributionStrategy.add("agentName", replicationPackage, queueProvider);
        assertNotNull(returnedState);
    }
}
