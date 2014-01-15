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
package org.apache.sling.replication.monitor;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import org.apache.sling.hc.api.Result;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueItemState;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link org.apache.sling.replication.monitor.ReplicationQueueHealthCheck}
 */
public class ReplicationQueueHealthCheckTest {

    @Test
    public void testWithNoReplicationQueueProvider() throws Exception {
        ReplicationQueueHealthCheck replicationQueueHealthCheck = new ReplicationQueueHealthCheck();
        replicationQueueHealthCheck.activate(Collections.<String, Object>emptyMap());
        Result result = replicationQueueHealthCheck.execute();
        assertNotNull(result);
        assertTrue(result.isOk());
    }

    @Test
    public void testWithNoItemInTheQueue() throws Exception {
        ReplicationQueueHealthCheck replicationQueueHealthCheck = new ReplicationQueueHealthCheck();

        replicationQueueHealthCheck.activate(Collections.<String, Object>emptyMap());
        ReplicationQueue queue = mock(ReplicationQueue.class);
        when(queue.getHead()).thenReturn(null);
        ReplicationQueueProvider replicationQueueProvider = mock(ReplicationQueueProvider.class);
        Collection<ReplicationQueue> providers = new LinkedList<ReplicationQueue>();
        providers.add(queue);
        when(replicationQueueProvider.getAllQueues()).thenReturn(providers);
        replicationQueueHealthCheck.bindReplicationQueueProvider(replicationQueueProvider);

        Result result = replicationQueueHealthCheck.execute();
        assertNotNull(result);
        assertTrue(result.isOk());
    }

    @Test
    public void testWithOneOkItemInTheQueue() throws Exception {
        ReplicationQueueHealthCheck replicationQueueHealthCheck = new ReplicationQueueHealthCheck();

        replicationQueueHealthCheck.activate(Collections.<String, Object>emptyMap());
        ReplicationQueue queue = mock(ReplicationQueue.class);
        ReplicationPackage item = mock(ReplicationPackage.class);
        ReplicationQueueItemState status = mock(ReplicationQueueItemState.class);
        when(status.getAttempts()).thenReturn(1);
        when(queue.getStatus(item)).thenReturn(status);
        when(queue.getHead()).thenReturn(item);
        ReplicationQueueProvider replicationQueueProvider = mock(ReplicationQueueProvider.class);
        Collection<ReplicationQueue> providers = new LinkedList<ReplicationQueue>();
        providers.add(queue);
        when(replicationQueueProvider.getAllQueues()).thenReturn(providers);
        replicationQueueHealthCheck.bindReplicationQueueProvider(replicationQueueProvider);

        Result result = replicationQueueHealthCheck.execute();
        assertNotNull(result);
        assertTrue(result.isOk());
    }

    @Test
    public void testWithNotOkItemInTheQueue() throws Exception {
        ReplicationQueueHealthCheck replicationQueueHealthCheck = new ReplicationQueueHealthCheck();

        replicationQueueHealthCheck.activate(Collections.<String, Object>emptyMap());
        ReplicationQueue queue = mock(ReplicationQueue.class);
        ReplicationPackage item = mock(ReplicationPackage.class);
        ReplicationQueueItemState status = mock(ReplicationQueueItemState.class);
        when(status.getAttempts()).thenReturn(10);
        when(queue.getStatus(item)).thenReturn(status);
        when(queue.getHead()).thenReturn(item);
        ReplicationQueueProvider replicationQueueProvider = mock(ReplicationQueueProvider.class);
        Collection<ReplicationQueue> providers = new LinkedList<ReplicationQueue>();
        providers.add(queue);
        when(replicationQueueProvider.getAllQueues()).thenReturn(providers);
        replicationQueueHealthCheck.bindReplicationQueueProvider(replicationQueueProvider);

        Result result = replicationQueueHealthCheck.execute();
        assertNotNull(result);
        assertFalse(result.isOk());
    }
}
