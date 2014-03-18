package org.apache.sling.replication.agent.impl;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.queue.ReplicationQueueProcessor;
import org.apache.sling.replication.queue.impl.jobhandling.ReplicationAgentJobConsumer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link org.apache.sling.replication.queue.impl.jobhandling.ReplicationAgentJobConsumer}
 */
public class ReplicationAgentJobConsumerTest {

    @Test
    public void testJobWithSuccessfulAgent() throws Exception {
        ReplicationQueueProcessor queueProcessor = mock(ReplicationQueueProcessor.class);
        when(queueProcessor.process(anyString(), any(ReplicationQueueItem.class))).thenReturn(true);

        ReplicationAgentJobConsumer replicationAgentJobConsumer = new ReplicationAgentJobConsumer(queueProcessor);
        Job job = mock(Job.class);
        JobConsumer.JobResult jobResult = replicationAgentJobConsumer.process(job);
        assertEquals(JobConsumer.JobResult.OK, jobResult);
    }

    @Test
    public void testJobWithUnsuccessfulAgent() throws Exception {
        ReplicationQueueProcessor queueProcessor = mock(ReplicationQueueProcessor.class);
        when(queueProcessor.process(anyString(), any(ReplicationQueueItem.class))).thenReturn(false);

        ReplicationAgentJobConsumer replicationAgentJobConsumer = new ReplicationAgentJobConsumer(queueProcessor);
        Job job = mock(Job.class);
        JobConsumer.JobResult jobResult = replicationAgentJobConsumer.process(job);
        assertEquals(JobConsumer.JobResult.FAILED, jobResult);
    }
}
