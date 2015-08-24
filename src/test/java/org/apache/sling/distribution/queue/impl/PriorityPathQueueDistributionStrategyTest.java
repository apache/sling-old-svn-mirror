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
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link PriorityPathQueueDispatchingStrategy}
 */
public class PriorityPathQueueDistributionStrategyTest {

    @Test
    public void testPackageAdditionWithSucceedingItemDelivery() throws Exception {
        PriorityPathQueueDispatchingStrategy priorityPathDistributionStrategy = new PriorityPathQueueDispatchingStrategy(new String[]{"/content", "/apps"});

        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionPackageInfo packageInfo = new DistributionPackageInfo("type");
        packageInfo.put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, new String[]{"/etc"});
        when(distributionPackage.getInfo()).thenReturn(packageInfo);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(queue);
        when(queue.add(any(DistributionQueueItem.class))).thenReturn(true);
        DistributionQueueItemStatus state = mock(DistributionQueueItemStatus.class);
        when(queue.getItem(any(String.class))).thenReturn(new DistributionQueueEntry(null, state));

        Iterable<DistributionQueueItemStatus> returnedStates = priorityPathDistributionStrategy.add(distributionPackage, queueProvider);
        assertNotNull(returnedStates);
        Iterator<DistributionQueueItemStatus> iterator = returnedStates.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
    }

    @Test
    public void testPackageAdditionWithSucceedingItemDeliveryOnPriorityPath() throws Exception {
        PriorityPathQueueDispatchingStrategy priorityPathDistributionStrategy = new PriorityPathQueueDispatchingStrategy(new String[]{"/content", "/apps"});

        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionPackageInfo packageInfo = new DistributionPackageInfo("type");
        packageInfo.put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, new String[]{"/content/sample1"});
        when(distributionPackage.getInfo()).thenReturn(packageInfo);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queueProvider.getQueue("/content")).thenReturn(queue);
        when(queue.add(any(DistributionQueueItem.class))).thenReturn(true);
        DistributionQueueItemStatus state = mock(DistributionQueueItemStatus.class);
        when(queue.getItem(any(String.class))).thenReturn(new DistributionQueueEntry(null, state));

        Iterable<DistributionQueueItemStatus> returnedStates = priorityPathDistributionStrategy.add(distributionPackage, queueProvider);
        assertNotNull(returnedStates);
        Iterator<DistributionQueueItemStatus> iterator = returnedStates.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
    }

    @Test
    public void testPackageAdditionWithFailingItemDelivery() throws Exception {
        PriorityPathQueueDispatchingStrategy priorityPathDistributionStrategy = new PriorityPathQueueDispatchingStrategy(new String[]{"/content", "/apps"});

        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionPackageInfo info = new DistributionPackageInfo("type");
        when(distributionPackage.getInfo()).thenReturn(info);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(queue);
        when(queue.add(any(DistributionQueueItem.class))).thenReturn(false);

        Iterable<DistributionQueueItemStatus> returnedStates = priorityPathDistributionStrategy.add(distributionPackage, queueProvider);
        assertNotNull(returnedStates);
        Iterator<DistributionQueueItemStatus> iterator = returnedStates.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
    }

    @Test
    public void testPackageAdditionWithFailingItemDeliveryOnPriorityPath() throws Exception {
        PriorityPathQueueDispatchingStrategy priorityPathDistributionStrategy = new PriorityPathQueueDispatchingStrategy(new String[]{"/content", "/apps"});

        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionPackageInfo packageInfo = new DistributionPackageInfo("type");
        packageInfo.put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, new String[]{"/content/sample2"});
        when(distributionPackage.getInfo()).thenReturn(packageInfo);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);

        when(queueProvider.getQueue("/content")).thenReturn(queue);
        when(queue.add(any(DistributionQueueItem.class))).thenReturn(false);

        Iterable<DistributionQueueItemStatus> returnedStates = priorityPathDistributionStrategy.add(distributionPackage, queueProvider);
        assertNotNull(returnedStates);
        Iterator<DistributionQueueItemStatus> iterator = returnedStates.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
    }

    @Test
    public void testPackageAdditionWithNotNullItemStateFromTheQueue() throws Exception {
        PriorityPathQueueDispatchingStrategy priorityPathDistributionStrategy = new PriorityPathQueueDispatchingStrategy(new String[]{"/content", "/apps"});

        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionPackageInfo packageInfo = new DistributionPackageInfo("type");
        packageInfo.put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, new String[]{"/etc"});
        when(distributionPackage.getInfo()).thenReturn(packageInfo);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(queue);
        when(queue.add(any(DistributionQueueItem.class))).thenReturn(true);
        DistributionQueueItemStatus state = mock(DistributionQueueItemStatus.class);
        when(queue.getItem(any(String.class))).thenReturn(new DistributionQueueEntry(null, state));

        Iterable<DistributionQueueItemStatus> returnedStates = priorityPathDistributionStrategy.add(distributionPackage, queueProvider);
        assertNotNull(returnedStates);
        Iterator<DistributionQueueItemStatus> iterator = returnedStates.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
    }

    @Test
    public void testPackageAdditionWithNotNullItemStateFromTheQueueOnPriorityPath() throws Exception {
        PriorityPathQueueDispatchingStrategy priorityPathDistributionStrategy = new PriorityPathQueueDispatchingStrategy(new String[]{"/content", "/apps"});

        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionPackageInfo packageInfo = new DistributionPackageInfo("type");
        packageInfo.put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, new String[]{"/apps"});
        when(distributionPackage.getInfo()).thenReturn(packageInfo);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue queue = mock(DistributionQueue.class);

        when(queueProvider.getQueue("/apps")).thenReturn(queue);
        when(queue.add(any(DistributionQueueItem.class))).thenReturn(true);
        DistributionQueueItemStatus state = mock(DistributionQueueItemStatus.class);
        when(queue.getItem(any(String.class))).thenReturn(new DistributionQueueEntry(null, state));

        Iterable<DistributionQueueItemStatus> returnedStates = priorityPathDistributionStrategy.add(distributionPackage, queueProvider);
        assertNotNull(returnedStates);
        Iterator<DistributionQueueItemStatus> iterator = returnedStates.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
    }
}
