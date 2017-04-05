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
package org.apache.sling.distribution.queue.impl.jobhandling;

import java.util.Collections;
import java.util.Map;

import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueType;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.JobManager;
import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link JobHandlingDistributionQueue}
 */
public class JobHandlingDistributionQueueTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testPackageAddition() throws Exception {
        JobManager jobManager = mock(JobManager.class);
        JobBuilder builder = mock(JobBuilder.class);
        when(builder.properties(any(Map.class))).thenReturn(builder);
        Job job = mock(Job.class);
        when(job.getId()).thenReturn("id-123");
        when(builder.add()).thenReturn(job);
        String topic = JobHandlingDistributionQueue.DISTRIBUTION_QUEUE_TOPIC + "/aname";
        when(jobManager.createJob(topic)).thenReturn(builder);
        when(jobManager.findJobs(JobManager.QueryType.ALL, topic, -1)).thenReturn(Collections.<Job>emptySet());
        when(builder.properties(any(Map.class))).thenReturn(builder);
        DistributionQueue queue = new JobHandlingDistributionQueue("aname", topic, jobManager, true, DistributionQueueType.ORDERED);
        DistributionPackageInfo packageInfo = new DistributionPackageInfo("type");
        packageInfo.put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, new String[]{"/foo"});
        packageInfo.put(DistributionPackageInfo.PROPERTY_REQUEST_TYPE, DistributionRequestType.ADD);

        DistributionQueueItem distributionQueueItem = new DistributionQueueItem("an-id", packageInfo);
        assertNotNull(queue.add(distributionQueueItem));
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
        String topic = JobHandlingDistributionQueue.DISTRIBUTION_QUEUE_TOPIC + "/aname";
        when(jobManager.createJob(topic)).thenReturn(builder);
        when(jobManager.getJobById(anyString())).thenReturn(job);
        when(builder.properties(any(Map.class))).thenReturn(builder);
        DistributionQueue queue = new JobHandlingDistributionQueue("aname", topic, jobManager, true, DistributionQueueType.ORDERED);
        DistributionPackageInfo packageInfo = new DistributionPackageInfo("type");
        packageInfo.put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, new String[]{"/foo"});
        packageInfo.put(DistributionPackageInfo.PROPERTY_REQUEST_TYPE, DistributionRequestType.ADD);
        DistributionQueueItem distributionQueueItem = new DistributionQueueItem("an-id", packageInfo);
        assertNotNull(queue.add(distributionQueueItem));
        DistributionQueueItemStatus status = queue.getItem(job.getId()).getStatus();
        assertNotNull(status);
        assertEquals(DistributionQueueItemState.QUEUED, status.getItemState());
    }

}
