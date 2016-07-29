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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.junit.Test;

/**
 * Tests for {@link SimpleDistributionQueueCheckpoint}
 */
public class SimpleDistributionQueueCheckpointTest {

    @Test
    public void testRunWithNothingInQueue() throws Exception {
        DistributionQueue queue = mock(DistributionQueue.class);
        Iterable<DistributionQueueEntry> items = new LinkedList<DistributionQueueEntry>();
        when(queue.getItems(0, -1)).thenReturn(items);
        File checkpointDirectory = FileUtils.getTempDirectory();
        SimpleDistributionQueueCheckpoint simpleDistributionQueueCheckpoint = new SimpleDistributionQueueCheckpoint(queue,
                checkpointDirectory);
        simpleDistributionQueueCheckpoint.run();
    }

    @Test
    public void testRunWithOneItemInTheQueue() throws Exception {
        DistributionQueue queue = mock(DistributionQueue.class);
        String queueName = "sample-queue";
        when(queue.getName()).thenReturn(queueName);
        LinkedList<DistributionQueueEntry> entries = new LinkedList<DistributionQueueEntry>();
        Map<String, Object> base = new HashMap<String, Object>();
        base.put("here","there");
        base.put("foo","bar");
        base.put("multi", new String[]{"1", "2"});
        entries.add(new DistributionQueueEntry("123", new DistributionQueueItem("pid123", base),
                new DistributionQueueItemStatus(DistributionQueueItemState.QUEUED, queueName)));
        when(queue.getItems(0, -1)).thenReturn(entries);
        File checkpointDirectory = FileUtils.getTempDirectory();
        SimpleDistributionQueueCheckpoint simpleDistributionQueueCheckpoint = new SimpleDistributionQueueCheckpoint(queue,
                checkpointDirectory);
        simpleDistributionQueueCheckpoint.run();
        File checkpointFile = new File(checkpointDirectory, "sample-queue-checkpoint");
        assertTrue(checkpointFile.exists());
    }
}