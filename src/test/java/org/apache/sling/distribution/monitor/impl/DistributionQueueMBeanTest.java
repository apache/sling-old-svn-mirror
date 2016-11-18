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
package org.apache.sling.distribution.monitor.impl;

import static org.apache.sling.distribution.queue.DistributionQueueItemState.QUEUED;
import static org.apache.sling.distribution.queue.DistributionQueueState.PAUSED;
import static org.apache.sling.distribution.queue.DistributionQueueState.RUNNING;
import static org.apache.sling.distribution.queue.DistributionQueueType.ORDERED;
import static org.apache.sling.distribution.queue.DistributionQueueType.PARALLEL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.HashMap;

import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueStatus;
import org.junit.Test;

/**
 * Test case for {@link DistributionQueueMBean}
 */
public class DistributionQueueMBeanTest {

    @Test
    public void verifyMBeanExposedValuesWhenHeadIsNull() {
        DistributionQueueStatus status = new DistributionQueueStatus(0, PAUSED);

        DistributionQueue distributionQueue = mock(DistributionQueue.class);
        when(distributionQueue.getName()).thenReturn("#distributionQueue");
        when(distributionQueue.getHead()).thenReturn(null);
        when(distributionQueue.getType()).thenReturn(ORDERED);
        when(distributionQueue.getStatus()).thenReturn(status);

        DistributionQueueMBean mBean = new DistributionQueueMBeanImpl(distributionQueue);

        assertEquals(distributionQueue.getName(), mBean.getName());
        assertEquals(distributionQueue.getType().name().toLowerCase(), mBean.getType());
        assertEquals(0, mBean.getSize());
        assertTrue(mBean.isEmpty());
        assertNull(mBean.getHeadId());
        assertEquals(-1, mBean.getHeadDequeuingAttempts());
        assertNull(mBean.getHeadStatus());
        assertNull(mBean.getHeadEnqueuingDate());
    }

    @Test
    public void verifyMBeanExposedValuesWhenHeadIsNotNull() {
        DistributionQueueStatus status = new DistributionQueueStatus(1, RUNNING);
        Calendar joined = Calendar.getInstance();
        DistributionQueueEntry entry = new DistributionQueueEntry("#entry",
                                                                  new DistributionQueueItem("#package", 1000L, new HashMap<String, Object>()),
                                                                  new DistributionQueueItemStatus(joined, QUEUED, 1, "#queue"));

        DistributionQueue distributionQueue = mock(DistributionQueue.class);
        when(distributionQueue.getName()).thenReturn("#distributionQueue");
        when(distributionQueue.getHead()).thenReturn(entry);
        when(distributionQueue.getType()).thenReturn(PARALLEL);
        when(distributionQueue.getStatus()).thenReturn(status);

        DistributionQueueMBean mBean = new DistributionQueueMBeanImpl(distributionQueue);

        assertEquals(distributionQueue.getName(), mBean.getName());
        assertEquals(distributionQueue.getType().name().toLowerCase(), mBean.getType());
        assertEquals(1, mBean.getSize());
        assertFalse(mBean.isEmpty());
        assertEquals("#entry", mBean.getHeadId());
        assertEquals(1, mBean.getHeadDequeuingAttempts());
        assertEquals(QUEUED.name().toLowerCase(), mBean.getHeadStatus());
        assertEquals(joined, mBean.getHeadEnqueuingDate());
    }

}
