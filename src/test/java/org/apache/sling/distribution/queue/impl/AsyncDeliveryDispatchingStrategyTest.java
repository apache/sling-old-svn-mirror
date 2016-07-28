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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.packaging.impl.SharedDistributionPackage;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.queue.DistributionQueueState;
import org.apache.sling.distribution.queue.DistributionQueueStatus;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AsyncDeliveryDispatchingStrategy}
 */
public class AsyncDeliveryDispatchingStrategyTest {

    @Test
    public void testAddWithNotSharedPackage() throws Exception {
        Map<String, String> deliveryMappings = new HashMap<String, String>();
        deliveryMappings.put("queue1", "delivery1");
        deliveryMappings.put("queue2", "delivery2");
        AsyncDeliveryDispatchingStrategy asyncDeliveryDispatchingStrategy = new AsyncDeliveryDispatchingStrategy(deliveryMappings);
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        try {
            asyncDeliveryDispatchingStrategy.add(distributionPackage, queueProvider);
            fail("should not be able to pass not shared packages");
        } catch (DistributionException e) {
            // expected
        }
    }

    @Test
    public void testAddWithOneItemInRunningQueues() throws Exception {
        Map<String, String> deliveryMappings = new HashMap<String, String>();
        String queue1 = "queue1";
        deliveryMappings.put(queue1, "delivery1");
        String queue2 = "queue2";
        deliveryMappings.put(queue2, "delivery2");
        AsyncDeliveryDispatchingStrategy asyncDeliveryDispatchingStrategy = new AsyncDeliveryDispatchingStrategy(deliveryMappings);

        // setup package
        SharedDistributionPackage distributionPackage = mock(SharedDistributionPackage.class);
        when(distributionPackage.getId()).thenReturn("1221312");
        DistributionPackageInfo info = new DistributionPackageInfo("dummy", new HashMap<String, Object>());
        when(distributionPackage.getInfo()).thenReturn(info);

        // setup queues
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueue dq1 = mock(DistributionQueue.class);
        DistributionQueueItemStatus status = mock(DistributionQueueItemStatus.class);
        DistributionQueueEntry entry = new DistributionQueueEntry("1242112", new DistributionQueueItem(distributionPackage.getId(),
                new HashMap<String, Object>()), status);
        when(dq1.add(any(DistributionQueueItem.class))).thenReturn(entry);
        DistributionQueueStatus status1 = new DistributionQueueStatus(1, DistributionQueueState.RUNNING);
        when(dq1.getStatus()).thenReturn(status1);
        when(queueProvider.getQueue(queue1)).thenReturn(dq1);
        DistributionQueue dq2 = mock(DistributionQueue.class);
        when(dq2.add(any(DistributionQueueItem.class))).thenReturn(entry);
        DistributionQueueStatus status2 = new DistributionQueueStatus(1, DistributionQueueState.RUNNING);
        when(dq2.getStatus()).thenReturn(status2);
        when(queueProvider.getQueue(queue2)).thenReturn(dq2);

        Iterable<DistributionQueueItemStatus> statuses = asyncDeliveryDispatchingStrategy.add(distributionPackage, queueProvider);
        assertNotNull(statuses);

    }

    @Test
    public void testGetQueueNames() throws Exception {
        Map<String, String> deliveryMappings = new HashMap<String, String>();
        deliveryMappings.put("queue1", "delivery1");
        deliveryMappings.put("queue2", "delivery2");
        AsyncDeliveryDispatchingStrategy asyncDeliveryDispatchingStrategy = new AsyncDeliveryDispatchingStrategy(deliveryMappings);
        List<String> queueNames = asyncDeliveryDispatchingStrategy.getQueueNames();
        assertNotNull(queueNames);
        assertEquals(4, queueNames.size());
        assertTrue(queueNames.contains("queue1"));
        assertTrue(queueNames.contains("queue2"));
        assertTrue(queueNames.contains("delivery1"));
        assertTrue(queueNames.contains("delivery2"));
    }
}