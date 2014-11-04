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
package org.apache.sling.replication.queue.impl.simple;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.queue.ReplicationQueueProcessor;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link ScheduledReplicationQueueProcessorTask}
 */
public class ScheduledReplicationQueueProcessorTaskTest {

    @Test
    public void testRunWithNoQueue() throws Exception {
        SimpleReplicationQueueProvider queueProvider = mock(SimpleReplicationQueueProvider.class);
        ReplicationQueueProcessor queueProcessor = mock(ReplicationQueueProcessor.class);
        ScheduledReplicationQueueProcessorTask scheduledReplicationQueueProcessorTask = new ScheduledReplicationQueueProcessorTask(
                queueProvider, queueProcessor);
        scheduledReplicationQueueProcessorTask.run();
    }

    @Test
    public void testRunWithOneEmptyQueue() throws Exception {
        SimpleReplicationQueueProvider queueProvider = mock(SimpleReplicationQueueProvider.class);
        Collection<ReplicationQueue> queues = new LinkedList<ReplicationQueue>();
        ReplicationQueue queue = mock(ReplicationQueue.class);
        when(queue.isEmpty()).thenReturn(true);
        queues.add(queue);
        when(queueProvider.getAllQueues()).thenReturn(queues);
        ReplicationQueueProcessor queueProcessor = mock(ReplicationQueueProcessor.class);
        ScheduledReplicationQueueProcessorTask scheduledReplicationQueueProcessorTask = new ScheduledReplicationQueueProcessorTask(
                queueProvider, queueProcessor);
        scheduledReplicationQueueProcessorTask.run();
    }

    @Test
    public void testRunWithOneNonEmptyQueue() throws Exception {
        SimpleReplicationQueueProvider queueProvider = mock(SimpleReplicationQueueProvider.class);
        Collection<ReplicationQueue> queues = new LinkedList<ReplicationQueue>();
        ReplicationQueue queue = mock(ReplicationQueue.class);
        when(queue.isEmpty()).thenReturn(false).thenReturn(true);
        ReplicationQueueItem item = mock(ReplicationQueueItem.class);
        when(queue.getHead()).thenReturn(item);

        queues.add(queue);
        when(queueProvider.getAllQueues()).thenReturn(queues);
        ReplicationQueueProcessor queueProcessor = mock(ReplicationQueueProcessor.class);
        ScheduledReplicationQueueProcessorTask scheduledReplicationQueueProcessorTask = new ScheduledReplicationQueueProcessorTask(
                queueProvider, queueProcessor);
        scheduledReplicationQueueProcessorTask.run();
    }
}