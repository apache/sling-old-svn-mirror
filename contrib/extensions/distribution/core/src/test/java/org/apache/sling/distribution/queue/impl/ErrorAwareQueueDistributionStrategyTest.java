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

import java.util.Dictionary;
import java.util.Iterator;

import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link ErrorAwareQueueDispatchingStrategy}
 */
public class ErrorAwareQueueDistributionStrategyTest {

    @Test
    public void testPackageAdditionWithSucceedingItemDelivery() throws Exception {
        ErrorAwareQueueDispatchingStrategy errorAwareDistributionStrategy = new ErrorAwareQueueDispatchingStrategy();
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(queue);
        when(queue.add(any(DistributionQueueItem.class))).thenReturn(true);
        DistributionQueueItemStatus state = mock(DistributionQueueItemStatus.class);
        when(queue.getItem(any(String.class))).thenReturn(new DistributionQueueEntry(null, state));

        Iterable<DistributionQueueItemStatus> returnedStates = errorAwareDistributionStrategy.add(distributionPackage, queueProvider);
        assertNotNull(returnedStates);
        Iterator<DistributionQueueItemStatus> iterator = returnedStates.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
    }

    @Test
    public void testPackageAdditionWithFailingItemDelivery() throws Exception {
        ErrorAwareQueueDispatchingStrategy errorAwareDistributionStrategy = new ErrorAwareQueueDispatchingStrategy();
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);
        DistributionQueueItem queueItem = mock(DistributionQueueItem.class);

        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(queue);
        when(queue.add(queueItem)).thenReturn(true);
        DistributionQueueItemStatus state = mock(DistributionQueueItemStatus.class);
        when(queue.getItem(any(String.class))).thenReturn(new DistributionQueueEntry(null, state));

        Iterable<DistributionQueueItemStatus> returnedStates = errorAwareDistributionStrategy.add(distributionPackage, queueProvider);
        assertNotNull(returnedStates);
        Iterator<DistributionQueueItemStatus> iterator = returnedStates.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
    }

    @Test
    public void testPackageAdditionWithMultipleFailingItemsDeliveryAndErrorQueue() throws Exception {
        ErrorAwareQueueDispatchingStrategy errorAwareDistributionStrategy = new ErrorAwareQueueDispatchingStrategy();
        ComponentContext context = mock(ComponentContext.class);
        Dictionary properties = mock(Dictionary.class);
        when(properties.get("attempts.threshold")).thenReturn(new String[]{"1"});
        when(properties.get("stuck.handling")).thenReturn(new String[]{"ERROR"});
        when(context.getProperties()).thenReturn(properties);
        errorAwareDistributionStrategy.activate(context);
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);
        DistributionQueueItem queueItem = mock(DistributionQueueItem.class);

        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(queue);
        when(queue.add(queueItem)).thenReturn(true);
        DistributionQueueItemStatus state = mock(DistributionQueueItemStatus.class);
        when(state.getAttempts()).thenReturn(2);
        when(queue.getHead()).thenReturn(new DistributionQueueEntry(queueItem, state));
        DistributionQueue errorQueue = mock(DistributionQueue.class);
        when(errorQueue.add(queueItem)).thenReturn(true);
        when(queueProvider.getQueue(ErrorAwareQueueDispatchingStrategy.ERROR_QUEUE_NAME)).thenReturn(errorQueue);

        Iterable<DistributionQueueItemStatus> returnedStates = errorAwareDistributionStrategy.add(distributionPackage, queueProvider);
        assertNotNull(returnedStates);
        Iterator<DistributionQueueItemStatus> iterator = returnedStates.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
    }

    @Test
    public void testPackageAdditionWithMultipleFailingItemsDeliveryAndDropFromQueue() throws Exception {
        ErrorAwareQueueDispatchingStrategy errorAwareDistributionStrategy = new ErrorAwareQueueDispatchingStrategy();
        ComponentContext context = mock(ComponentContext.class);
        Dictionary properties = mock(Dictionary.class);
        when(properties.get("attempts.threshold")).thenReturn(new String[]{"1"});
        when(properties.get("stuck.handling")).thenReturn(new String[]{"DROP"});
        when(context.getProperties()).thenReturn(properties);
        errorAwareDistributionStrategy.activate(context);
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);

        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(queue);
        when(queue.add(any(DistributionQueueItem.class))).thenReturn(true);
        DistributionQueueItemStatus state = mock(DistributionQueueItemStatus.class);
        when(state.getAttempts()).thenReturn(2);
        when(queue.getHead()).thenReturn(new DistributionQueueEntry(mock(DistributionQueueItem.class), state));
        when(queue.getItem(any(String.class))).thenReturn(new DistributionQueueEntry(null, state));

        Iterable<DistributionQueueItemStatus> returnedState = errorAwareDistributionStrategy.add(distributionPackage, queueProvider);
        assertNotNull(returnedState);
    }

    @Test
    public void testPackageAdditionWithNotNullItemStateFromTheQueue() throws Exception {
        ErrorAwareQueueDispatchingStrategy errorAwareDistributionStrategy = new ErrorAwareQueueDispatchingStrategy();
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(queue);
        when(queue.add(any(DistributionQueueItem.class))).thenReturn(true);
        DistributionQueueItemStatus state = mock(DistributionQueueItemStatus.class);
        when(queue.getItem(any(String.class))).thenReturn(new DistributionQueueEntry(null, state));

        Iterable<DistributionQueueItemStatus> returnedStates = errorAwareDistributionStrategy.add(distributionPackage, queueProvider);
        assertNotNull(returnedStates);
        Iterator<DistributionQueueItemStatus> iterator = returnedStates.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
    }
}
