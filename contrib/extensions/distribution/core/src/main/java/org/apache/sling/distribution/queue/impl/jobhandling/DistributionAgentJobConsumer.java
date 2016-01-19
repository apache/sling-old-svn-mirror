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
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueProcessor;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link JobConsumer} for {@link org.apache.sling.distribution.agent.DistributionAgent}s using {@link JobHandlingDistributionQueue}
 */
class DistributionAgentJobConsumer implements JobConsumer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DistributionQueueProcessor queueProcessor;

    public DistributionAgentJobConsumer(DistributionQueueProcessor queueProcessor) {
        this.queueProcessor = queueProcessor;
    }

    public JobResult process(Job job) {
        log.debug("processing job {}", job.getId());
        DistributionQueueEntry entry = JobHandlingUtils.getEntry(job);
        boolean processingResult = false;
        if (entry != null) {
            String queueName = entry.getStatus().getQueueName();
            DistributionQueueItem item = entry.getItem();
            log.debug("processing item {} in queue {}", item.getId(), queueName);
            processingResult = queueProcessor.process(queueName, entry);
            log.debug("item {} processed {}", item.getId());
        } else {
            log.warn("no entry for job {}", job.getId());
        }
        return processingResult ? JobResult.OK : JobResult.FAILED;
    }

}
