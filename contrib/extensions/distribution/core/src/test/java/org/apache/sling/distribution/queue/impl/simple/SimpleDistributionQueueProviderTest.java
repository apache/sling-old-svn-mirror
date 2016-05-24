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

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueProcessor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link SimpleDistributionQueueProvider}
 */
public class SimpleDistributionQueueProviderTest {

    @Test
    public void testGetOrCreateQueue() throws Exception {
        SimpleDistributionQueueProvider simpledistributionQueueProvider = new SimpleDistributionQueueProvider(mock(Scheduler.class), "agentName", false);
        DistributionQueue queue = simpledistributionQueueProvider.getQueue("default");
        assertNotNull(queue);
    }

    @Test
    public void testGetOrCreateQueueWithCheckpointing() throws Exception {
        SimpleDistributionQueueProvider simpledistributionQueueProvider = new SimpleDistributionQueueProvider(mock(Scheduler.class), "agentName", true);
        DistributionQueue queue = simpledistributionQueueProvider.getQueue("default");
        assertNotNull(queue);
    }

    @Test
    public void testEnableQueueProcessing() throws Exception {
        Scheduler scheduler = mock(Scheduler.class);
        ScheduleOptions options = mock(ScheduleOptions.class);
        when(scheduler.NOW(-1, 1)).thenReturn(options);
        when(options.canRunConcurrently(false)).thenReturn(options);
        when(options.name(any(String.class))).thenReturn(options);
        SimpleDistributionQueueProvider simpledistributionQueueProvider = new SimpleDistributionQueueProvider(scheduler, "dummy-agent", false);
        DistributionQueueProcessor processor = mock(DistributionQueueProcessor.class);
        simpledistributionQueueProvider.enableQueueProcessing(processor);
    }

    @Test
    public void testEnableQueueProcessingWithCheckpointRecovery() throws Exception {
        File checkpointDirectory = new File("dummy-agent-simple-queues-checkpoints");
        File file = new File(getClass().getResource("/dummy-agent-checkpoint").getFile());
        FileUtils.copyFileToDirectory(file, checkpointDirectory);

        Scheduler scheduler = mock(Scheduler.class);
        ScheduleOptions options = mock(ScheduleOptions.class);
        when(scheduler.NOW(-1, 1)).thenReturn(options);
        when(scheduler.NOW(-1, 15)).thenReturn(options);
        when(options.canRunConcurrently(false)).thenReturn(options);
        when(options.name(any(String.class))).thenReturn(options);
        SimpleDistributionQueueProvider simpledistributionQueueProvider = new SimpleDistributionQueueProvider(scheduler, "dummy-agent", true);
        DistributionQueueProcessor processor = mock(DistributionQueueProcessor.class);
        simpledistributionQueueProvider.enableQueueProcessing(processor, "dummy-agent");
        DistributionQueue queue = simpledistributionQueueProvider.getQueue("dummy-agent");
        assertNotNull(queue);
        assertEquals(1, queue.getStatus().getItemsCount());
    }

    @Test
    public void testEnableQueueProcessingWithCheckpointing() throws Exception {
        Scheduler scheduler = mock(Scheduler.class);
        ScheduleOptions options = mock(ScheduleOptions.class);
        when(scheduler.NOW(-1, 1)).thenReturn(options);
        when(options.canRunConcurrently(false)).thenReturn(options);
        when(options.name(any(String.class))).thenReturn(options);
        SimpleDistributionQueueProvider simpledistributionQueueProvider = new SimpleDistributionQueueProvider(scheduler, "dummy-agent", true);
        DistributionQueueProcessor processor = mock(DistributionQueueProcessor.class);
        simpledistributionQueueProvider.enableQueueProcessing(processor);
    }

    @Test
    public void testDisableQueueProcessing() throws Exception {
        Scheduler scheduler = mock(Scheduler.class);
        ScheduleOptions options = mock(ScheduleOptions.class);
        when(scheduler.NOW(-1, 10)).thenReturn(options);
        when(options.canRunConcurrently(false)).thenReturn(options);
        when(options.name(any(String.class))).thenReturn(options);
        SimpleDistributionQueueProvider simpledistributionQueueProvider = new SimpleDistributionQueueProvider(scheduler, "dummy-agent", false);
        simpledistributionQueueProvider.disableQueueProcessing();
    }

    @Test
    public void testDisableQueueProcessingWithCheckpointing() throws Exception {
        Scheduler scheduler = mock(Scheduler.class);
        ScheduleOptions options = mock(ScheduleOptions.class);
        when(scheduler.NOW(-1, 10)).thenReturn(options);
        when(options.canRunConcurrently(false)).thenReturn(options);
        when(options.name(any(String.class))).thenReturn(options);
        SimpleDistributionQueueProvider simpledistributionQueueProvider = new SimpleDistributionQueueProvider(scheduler, "dummy-agent", true);
        simpledistributionQueueProvider.disableQueueProcessing();
    }
}