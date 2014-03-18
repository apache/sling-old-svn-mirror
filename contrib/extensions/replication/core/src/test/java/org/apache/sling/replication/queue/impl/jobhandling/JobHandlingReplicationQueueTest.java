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
package org.apache.sling.replication.queue.impl.jobhandling;

import java.util.Collections;
import java.util.Map;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.queue.ReplicationQueueItemState;
import org.apache.sling.replication.queue.ReplicationQueueItemState.ItemState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link JobHandlingReplicationQueue}
 */
public class JobHandlingReplicationQueueTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testPackageAddition() throws Exception {
        JobManager jobManager = mock(JobManager.class);
        JobBuilder builder = mock(JobBuilder.class);
        when(builder.properties(any(Map.class))).thenReturn(builder);
        Job job = mock(Job.class);
        when(job.getId()).thenReturn("id-123");
        when(builder.add()).thenReturn(job);
        String topic = JobHandlingReplicationQueue.REPLICATION_QUEUE_TOPIC + "/aname";
        when(jobManager.createJob(topic)).thenReturn(builder);
        when(jobManager.findJobs(JobManager.QueryType.ALL, topic, -1)).thenReturn(Collections.<Job>emptySet());
        when(builder.properties(any(Map.class))).thenReturn(builder);
        ReplicationQueue queue = new JobHandlingReplicationQueue("aname", topic, jobManager);
        ReplicationQueueItem pkg = mock(ReplicationQueueItem.class);
        assertTrue(queue.add(pkg));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPackageAdditionAndStatusCheck() throws Exception {
        JobManager jobManager = mock(JobManager.class);
        JobBuilder builder = mock(JobBuilder.class);
        when(builder.properties(any(Map.class))).thenReturn(builder);
        Job job = mock(Job.class);
        when(job.getId()).thenReturn("id-123");
        when(builder.add()).thenReturn(job);
        String topic = JobHandlingReplicationQueue.REPLICATION_QUEUE_TOPIC + "/aname";
        when(jobManager.createJob(topic)).thenReturn(builder);
        when(jobManager.findJobs(JobManager.QueryType.ALL, topic, -1)).thenReturn(Collections.<Job>emptySet());
        when(builder.properties(any(Map.class))).thenReturn(builder);
        ReplicationQueue queue = new JobHandlingReplicationQueue("aname", topic, jobManager);
        ReplicationQueueItem pkg = mock(ReplicationQueueItem.class);
        assertTrue(queue.add(pkg));
        ReplicationQueueItemState status = queue.getStatus(pkg);
        assertNotNull(status);
        assertFalse(status.isSuccessful());
        assertEquals(ItemState.DROPPED, status.getItemState());
    }

}
