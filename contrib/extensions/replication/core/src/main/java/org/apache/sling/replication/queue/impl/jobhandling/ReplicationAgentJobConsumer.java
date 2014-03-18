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

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.queue.ReplicationQueueProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link JobConsumer} for {@link ReplicationAgent}s using {@link org.apache.sling.replication.queue.impl.jobhandling.JobHandlingReplicationQueue}
 */
public class ReplicationAgentJobConsumer implements JobConsumer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ReplicationQueueProcessor queueProcessor;

    public ReplicationAgentJobConsumer(ReplicationQueueProcessor queueProcessor) {
        this.queueProcessor = queueProcessor;
    }

    public JobResult process(Job job) {
        log.debug("processing job {}", job.getId());
        String queueName = JobHandlingUtils.getQueueName(job);
        ReplicationQueueItem info = JobHandlingUtils.getPackage(job);
        log.info("processing item {} in queue {}", info.getId(), queueName);
        boolean processingResult = queueProcessor.process(queueName, info);
        JobResult jobResult = processingResult ? JobResult.OK : JobResult.FAILED;
        log.info("item {} processed {} with result {}", new Object[]{info.getId(), jobResult, jobResult});
        return jobResult;
    }

}
