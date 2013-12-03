package org.apache.sling.replication.agent.impl;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link ReplicationAgentJobConsumer}
 */
public class ReplicationAgentJobConsumerTest {

    @Test
    public void testJobWithSuccessfulAgent() throws Exception {
        ReplicationAgent replicationAgent = mock(ReplicationAgent.class);
        when(replicationAgent.process(any(ReplicationPackage.class))).thenReturn(true);
        ReplicationPackageBuilder packageBuilder = mock(ReplicationPackageBuilder.class);
        ReplicationAgentJobConsumer replicationAgentJobConsumer = new ReplicationAgentJobConsumer(replicationAgent, packageBuilder);
        Job job = mock(Job.class);
        JobConsumer.JobResult jobResult = replicationAgentJobConsumer.process(job);
        assertEquals(JobConsumer.JobResult.OK, jobResult);
    }

    @Test
    public void testJobWithUnsuccessfulAgent() throws Exception {
        ReplicationAgent replicationAgent = mock(ReplicationAgent.class);
        when(replicationAgent.process(any(ReplicationPackage.class))).thenReturn(false);
        ReplicationPackageBuilder packageBuilder = mock(ReplicationPackageBuilder.class);
        ReplicationAgentJobConsumer replicationAgentJobConsumer = new ReplicationAgentJobConsumer(replicationAgent, packageBuilder);
        Job job = mock(Job.class);
        JobConsumer.JobResult jobResult = replicationAgentJobConsumer.process(job);
        assertEquals(JobConsumer.JobResult.FAILED, jobResult);
    }
}
