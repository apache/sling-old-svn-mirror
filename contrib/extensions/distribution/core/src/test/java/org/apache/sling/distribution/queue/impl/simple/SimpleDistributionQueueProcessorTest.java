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
package org.apache.sling.distribution.queue.impl.simple;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueProcessor;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link SimpleDistributionQueueProcessor}
 */
public class SimpleDistributionQueueProcessorTest {

    @Test
    public void testRunWithNoQueue() throws Exception {
        DistributionQueue queue = mock(DistributionQueue.class);
        DistributionQueueProcessor queueProcessor = mock(DistributionQueueProcessor.class);
        SimpleDistributionQueueProcessor simpleDistributionQueueProcessor = new SimpleDistributionQueueProcessor(
                queue, queueProcessor);
        simpleDistributionQueueProcessor.run();
    }

    @Test
    public void testRunWithOneEmptyQueue() throws Exception {
        SimpleDistributionQueueProvider queueProvider = mock(SimpleDistributionQueueProvider.class);
        Collection<SimpleDistributionQueue> queues = new LinkedList<SimpleDistributionQueue>();
        SimpleDistributionQueue queue = mock(SimpleDistributionQueue.class);
        when(queue.getHead()).thenReturn(null);
        queues.add(queue);
        when(queueProvider.getQueues()).thenReturn(queues);
        DistributionQueueProcessor queueProcessor = mock(DistributionQueueProcessor.class);
        SimpleDistributionQueueProcessor simpleDistributionQueueProcessor = new SimpleDistributionQueueProcessor(
                queue, queueProcessor);
        simpleDistributionQueueProcessor.run();
    }

    @Test
    public void testRunWithOneNonEmptyQueue() throws Exception {
        SimpleDistributionQueueProvider queueProvider = mock(SimpleDistributionQueueProvider.class);
        Collection<SimpleDistributionQueue> queues = new LinkedList<SimpleDistributionQueue>();
        SimpleDistributionQueue queue = mock(SimpleDistributionQueue.class);
        DistributionQueueItem item = mock(DistributionQueueItem.class);
        when(queue.getHead()).thenReturn(new DistributionQueueEntry(null, item, null)).thenReturn(null);

        queues.add(queue);
        when(queueProvider.getQueues()).thenReturn(queues);
        DistributionQueueProcessor queueProcessor = mock(DistributionQueueProcessor.class);
        SimpleDistributionQueueProcessor simpleDistributionQueueProcessor = new SimpleDistributionQueueProcessor(
                queue, queueProcessor);
        simpleDistributionQueueProcessor.run();
    }
}