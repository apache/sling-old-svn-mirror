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
package org.apache.sling.distribution.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.hc.api.Result;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link DistributionQueueHealthCheck}
 */
public class DistributionQueueHealthCheckTest {

    @Test
    public void testWithNoDistributionQueueProvider() throws Exception {
        DistributionQueueHealthCheck distributionQueueHealthCheck = new DistributionQueueHealthCheck();
        distributionQueueHealthCheck.activate(Collections.<String, Object>emptyMap());
        Result result = distributionQueueHealthCheck.execute();
        assertNotNull(result);
        assertTrue(result.isOk());
    }

    @Test
    public void testWithNoItemInTheQueue() throws Exception {
        DistributionQueueHealthCheck distributionQueueHealthCheck = new DistributionQueueHealthCheck();

        distributionQueueHealthCheck.activate(Collections.<String, Object>emptyMap());
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queue.getHead()).thenReturn(null);
        DistributionAgent distributionAgent = mock(DistributionAgent.class);

        List<String> queues = new ArrayList<String>();
        queues.add("queueName");
        when(distributionAgent.getQueueNames()).thenReturn(queues);
        when(distributionAgent.getQueue(anyString())).thenReturn(queue);
        distributionQueueHealthCheck.bindDistributionAgent(distributionAgent);

        Result result = distributionQueueHealthCheck.execute();
        assertNotNull(result);
        assertTrue(result.isOk());
    }

    @Test
    public void testWithOneOkItemInTheQueue() throws Exception {
        DistributionQueueHealthCheck distributionQueueHealthCheck = new DistributionQueueHealthCheck();

        distributionQueueHealthCheck.activate(Collections.<String, Object>emptyMap());
        DistributionQueue queue = mock(DistributionQueue.class);
        DistributionQueueItem item = mock(DistributionQueueItem.class);
        DistributionQueueItemState status = mock(DistributionQueueItemState.class);
        when(status.getAttempts()).thenReturn(1);
        when(queue.getStatus(item)).thenReturn(status);
        when(queue.getHead()).thenReturn(item);
        DistributionAgent distributionAgent = mock(DistributionAgent.class);

        List<String> queues = new ArrayList<String>();
        queues.add("queueName");
        when(distributionAgent.getQueueNames()).thenReturn(queues);
        when(distributionAgent.getQueue(anyString())).thenReturn(queue);
        distributionQueueHealthCheck.bindDistributionAgent(distributionAgent);


        Result result = distributionQueueHealthCheck.execute();
        assertNotNull(result);
        assertTrue(result.isOk());
    }

    @Test
    public void testWithNotOkItemInTheQueue() throws Exception {
        DistributionQueueHealthCheck distributionQueueHealthCheck = new DistributionQueueHealthCheck();

        distributionQueueHealthCheck.activate(Collections.<String, Object>emptyMap());
        DistributionQueue queue = mock(DistributionQueue.class);
        DistributionQueueItem item = mock(DistributionQueueItem.class);
        DistributionQueueItemState status = mock(DistributionQueueItemState.class);
        when(status.getAttempts()).thenReturn(10);
        when(queue.getStatus(item)).thenReturn(status);
        when(queue.getHead()).thenReturn(item);
        DistributionAgent distributionAgent = mock(DistributionAgent.class);

        List<String> queues = new ArrayList<String>();
        queues.add("queueName");
        when(distributionAgent.getQueueNames()).thenReturn(queues);
        when(distributionAgent.getQueue(anyString())).thenReturn(queue);
        distributionQueueHealthCheck.bindDistributionAgent(distributionAgent);

        Result result = distributionQueueHealthCheck.execute();
        assertNotNull(result);
        assertFalse(result.isOk());
    }
}
