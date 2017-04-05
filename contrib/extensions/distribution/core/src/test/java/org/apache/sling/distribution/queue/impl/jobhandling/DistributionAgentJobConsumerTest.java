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

import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueProcessor;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link DistributionAgentJobConsumer}
 */
public class DistributionAgentJobConsumerTest {

    @Test
    public void testJobWithSuccessfulAgent() throws Exception {
        DistributionQueueProcessor queueProcessor = mock(DistributionQueueProcessor.class);
        when(queueProcessor.process(anyString(), any(DistributionQueueEntry.class))).thenReturn(true);

        DistributionAgentJobConsumer distributionAgentJobConsumer = new DistributionAgentJobConsumer(queueProcessor);
        Job job = mock(Job.class);
        JobConsumer.JobResult jobResult = distributionAgentJobConsumer.process(job);
        assertEquals(JobConsumer.JobResult.OK, jobResult);
    }

    @Test
    public void testJobWithUnsuccessfulAgent() throws Exception {
        DistributionQueueProcessor queueProcessor = mock(DistributionQueueProcessor.class);
        when(queueProcessor.process(anyString(), any(DistributionQueueEntry.class))).thenReturn(false);

        DistributionAgentJobConsumer distributionAgentJobConsumer = new DistributionAgentJobConsumer(queueProcessor);
        Job job = mock(Job.class);
        JobConsumer.JobResult jobResult = distributionAgentJobConsumer.process(job);
        assertEquals(JobConsumer.JobResult.FAILED, jobResult);
    }
}