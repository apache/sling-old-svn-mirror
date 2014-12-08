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

import java.util.Iterator;

import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link SingleQueueDispatchingStrategy}
 */
public class SingleQueueDistributionStrategyTest {

    @Test
    public void testPackageAdditionWithSucceedingItemDelivery() throws Exception {
        SingleQueueDispatchingStrategy singleQueueDistributionStrategy = new SingleQueueDispatchingStrategy();
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(queue);
        when(queue.add(any(DistributionQueueItem.class))).thenReturn(true);
        DistributionQueueItemState state = mock(DistributionQueueItemState.class);
        when(queue.getStatus(any(DistributionQueueItem.class))).thenReturn(state);

        Iterable<DistributionQueueItemState> returnedStates = singleQueueDistributionStrategy.add(distributionPackage, queueProvider);
        assertNotNull(returnedStates);
        Iterator<DistributionQueueItemState> iterator = returnedStates.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
    }

    @Test
    public void testPackageAdditionWithFailingItemDelivery() throws Exception {
        SingleQueueDispatchingStrategy singleQueueDistributionStrategy = new SingleQueueDispatchingStrategy();
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);
        DistributionQueueItem queueItem = mock(DistributionQueueItem.class);
        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(queue);
        when(queue.add(queueItem)).thenReturn(true);
        DistributionQueueItemState state = mock(DistributionQueueItemState.class);
        when(state.isSuccessful()).thenReturn(false);
        when(queue.getStatus(queueItem)).thenReturn(state);
        Iterable<DistributionQueueItemState> returnedStates = singleQueueDistributionStrategy.add(distributionPackage, queueProvider);
        assertNotNull(returnedStates);
        Iterator<DistributionQueueItemState> iterator = returnedStates.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
    }

    @Test
    public void testPackageAdditionWithNotNullItemStateFromTheQueue() throws Exception {
        SingleQueueDispatchingStrategy singleQueueDistributionStrategy = new SingleQueueDispatchingStrategy();
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(queue);
        when(queue.add(any(DistributionQueueItem.class))).thenReturn(true);
        DistributionQueueItemState state = mock(DistributionQueueItemState.class);
        when(queue.getStatus(any(DistributionQueueItem.class))).thenReturn(state);

        Iterable<DistributionQueueItemState> returnedStates = singleQueueDistributionStrategy.add(distributionPackage, queueProvider);
        assertNotNull(returnedStates);
        Iterator<DistributionQueueItemState> iterator = returnedStates.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
    }
}
