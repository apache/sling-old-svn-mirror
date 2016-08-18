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

import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.junit.Test;

import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SimpleDistributionAgentQueueProcessor}
 */
public class SimpleDistributionAgentQueueProcessorTest {

    @Test
    public void testProcess() throws Exception {
        DistributionPackageExporter packageExporter = mock(DistributionPackageExporter.class);
        DistributionPackageImporter packageImporter = mock(DistributionPackageImporter.class);
        int retryAttempts = 3;
        DefaultDistributionLog log = mock(DefaultDistributionLog.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionEventFactory eventFactory = mock(DistributionEventFactory.class);
        SimpleDistributionAgentAuthenticationInfo authenticationInfo = mock(SimpleDistributionAgentAuthenticationInfo.class);
        String agentName = "dummy-a";
        SimpleDistributionAgentQueueProcessor queueProcessor = new SimpleDistributionAgentQueueProcessor(packageExporter,
                packageImporter, retryAttempts, null, log, queueProvider, eventFactory, authenticationInfo, agentName);

        String id = "123-456";
        DistributionQueueItem item = new DistributionQueueItem("pckg-123", new HashMap<String, Object>());
        String queueName = "queue-1";
        DistributionQueueItemStatus status = new DistributionQueueItemStatus(DistributionQueueItemState.QUEUED, queueName);
        DistributionQueueEntry entry = new DistributionQueueEntry(id, item, status);
        queueProcessor.process(queueName, entry);
    }
}