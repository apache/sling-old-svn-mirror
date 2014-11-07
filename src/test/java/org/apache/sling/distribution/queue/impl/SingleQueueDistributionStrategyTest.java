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
package org.apache.sling.distribution.queue.impl;

import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.queue.*;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link SingleQueueDistributionStrategy}
 */
public class SingleQueueDistributionStrategyTest {

    @Test
    public void testPackageAdditionWithSucceedingItemDelivery() throws Exception {
        SingleQueueDistributionStrategy singleQueueDistributionStrategy = new SingleQueueDistributionStrategy();
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queueProvider.getQueue(DistributionQueueDistributionStrategy.DEFAULT_QUEUE_NAME)).thenReturn(queue);
        when(queue.add(any(DistributionQueueItem.class))).thenReturn(true);

        boolean returnedState = singleQueueDistributionStrategy.add(distributionPackage, queueProvider);
        assertTrue(returnedState);
    }

    @Test
    public void testPackageAdditionWithFailingItemDelivery() throws Exception {
        SingleQueueDistributionStrategy singleQueueDistributionStrategy = new SingleQueueDistributionStrategy();
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);
        DistributionQueueItem queueItem = mock(DistributionQueueItem.class);
        when(queueProvider.getQueue(DistributionQueueDistributionStrategy.DEFAULT_QUEUE_NAME)).thenReturn(queue);
        when(queue.add(queueItem)).thenReturn(true);
        DistributionQueueItemState state = mock(DistributionQueueItemState.class);
        when(state.isSuccessful()).thenReturn(false);
        when(queue.getStatus(queueItem)).thenReturn(state);
        boolean returnedState = singleQueueDistributionStrategy.add(distributionPackage, queueProvider);
        assertFalse(returnedState);
    }

    @Test
    public void testPackageAdditionWithNullItemStateFromTheQueue() throws Exception {
        SingleQueueDistributionStrategy singleQueueDistributionStrategy = new SingleQueueDistributionStrategy();
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queueProvider.getQueue(DistributionQueueDistributionStrategy.DEFAULT_QUEUE_NAME)).thenReturn(queue);
        when(queue.add(any(DistributionQueueItem.class))).thenReturn(true);
        boolean returnedState = singleQueueDistributionStrategy.add(distributionPackage, queueProvider);
        assertTrue(returnedState);
    }

    @Test
    public void testPackageAdditionWithNotNullItemStateFromTheQueue() throws Exception {
        SingleQueueDistributionStrategy singleQueueDistributionStrategy = new SingleQueueDistributionStrategy();
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queueProvider.getQueue(DistributionQueueDistributionStrategy.DEFAULT_QUEUE_NAME)).thenReturn(queue);
        when(queue.add(any(DistributionQueueItem.class))).thenReturn(true);

        boolean returnedState = singleQueueDistributionStrategy.add(distributionPackage, queueProvider);
        assertTrue(returnedState);
    }
}
