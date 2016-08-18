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
package org.apache.sling.distribution.agent.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DistributionPackageExporterProcessor}
 */
public class DistributionPackageExporterProcessorTest {

    @Test
    public void testGetAllResponses() throws Exception {
        String callingUser = "mr-who-cares";
        String requestId = "id231";
        long startTime = System.currentTimeMillis();
        DistributionEventFactory eventFactory = mock(DistributionEventFactory.class);
        DistributionQueueDispatchingStrategy scheduleQueueStrategy = mock(DistributionQueueDispatchingStrategy.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DefaultDistributionLog log = mock(DefaultDistributionLog.class);
        String agentName = "dummy";
        DistributionPackageExporterProcessor exporterProcessor = new DistributionPackageExporterProcessor(callingUser, requestId,
                startTime, eventFactory, scheduleQueueStrategy, queueProvider, log, agentName);

        List<DistributionResponse> allResponses = exporterProcessor.getAllResponses();
        assertNotNull(allResponses);
        assertEquals(0, allResponses.size());
    }

    @Test
    public void testGetPackagesCount() throws Exception {
        String callingUser = "mr-who-cares";
        String requestId = "id231";
        long startTime = System.currentTimeMillis();
        DistributionEventFactory eventFactory = mock(DistributionEventFactory.class);
        DistributionQueueDispatchingStrategy scheduleQueueStrategy = mock(DistributionQueueDispatchingStrategy.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DefaultDistributionLog log = mock(DefaultDistributionLog.class);
        String agentName = "dummy";
        DistributionPackageExporterProcessor exporterProcessor = new DistributionPackageExporterProcessor(callingUser, requestId,
                startTime, eventFactory, scheduleQueueStrategy, queueProvider, log, agentName);

        int packagesCount = exporterProcessor.getPackagesCount();
        assertEquals(0, packagesCount);

    }

    @Test
    public void testGetPackagesSize() throws Exception {
        String callingUser = "mr-who-cares";
        String requestId = "id231";
        long startTime = System.currentTimeMillis();
        DistributionEventFactory eventFactory = mock(DistributionEventFactory.class);
        DistributionQueueDispatchingStrategy scheduleQueueStrategy = mock(DistributionQueueDispatchingStrategy.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DefaultDistributionLog log = mock(DefaultDistributionLog.class);
        String agentName = "dummy";
        DistributionPackageExporterProcessor exporterProcessor = new DistributionPackageExporterProcessor(callingUser, requestId,
                startTime, eventFactory, scheduleQueueStrategy, queueProvider, log, agentName);

        long packagesSize = exporterProcessor.getPackagesSize();
        assertEquals(0L, packagesSize);
    }

    @Test
    public void testProcess() throws Exception {
        String callingUser = "mr-who-cares";
        String requestId = "id231";
        long startTime = System.currentTimeMillis();
        DistributionEventFactory eventFactory = mock(DistributionEventFactory.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionQueueDispatchingStrategy scheduleQueueStrategy = mock(DistributionQueueDispatchingStrategy.class);
        // assume scheduling works
        List<DistributionQueueItemStatus> statuses = new LinkedList<DistributionQueueItemStatus>();
        DistributionQueueItemStatus qis = new DistributionQueueItemStatus(DistributionQueueItemState.QUEUED, "queue-1");
        statuses.add(qis);
        when(scheduleQueueStrategy.add(distributionPackage, queueProvider)).thenReturn(statuses);

        DefaultDistributionLog log = mock(DefaultDistributionLog.class);
        String agentName = "dummy";
        DistributionPackageExporterProcessor exporterProcessor = new DistributionPackageExporterProcessor(callingUser, requestId,
                startTime, eventFactory, scheduleQueueStrategy, queueProvider, log, agentName);

        DistributionPackageInfo info = new DistributionPackageInfo("type-a", new HashMap<String, Object>());
        when(distributionPackage.getInfo()).thenReturn(info);
        exporterProcessor.process(distributionPackage);
    }
}