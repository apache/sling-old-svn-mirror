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
package org.apache.sling.replication.agent.impl;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.sling.replication.agent.AgentReplicationException;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.queue.impl.jobhandling.JobHandlingUtils;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;

/**
 * {@link JobConsumer} for {@link ReplicationAgent}s using {@link org.apache.sling.replication.queue.impl.jobhandling.JobHandlingReplicationQueue}
 */
public class ReplicationAgentJobConsumer implements JobConsumer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ReplicationAgent agent;

    private final ReplicationPackageBuilder packageBuilder;

    public ReplicationAgentJobConsumer(ReplicationAgent agent,
                    ReplicationPackageBuilder packageBuilder) {
        this.agent = agent;
        this.packageBuilder = packageBuilder;
    }

    public JobResult process(Job job) {
        try {
            if (log.isInfoEnabled()) {
                log.info("processing job {}", job.getId());
            }
            ReplicationPackage item = JobHandlingUtils.getPackage(packageBuilder, job);
            if (item != null) {
                if (log.isInfoEnabled()) {
                    log.info("processing item {} ", item);
                }
                boolean processingResult = agent.process(item);
                JobResult jobResult = processingResult ? JobResult.OK : JobResult.FAILED;
                if (log.isInfoEnabled()) {
                    log.info("item {} processed {} ", item, jobResult);
                }
                return jobResult;
            } else {
                if (log.isInfoEnabled()) {
                    log.info("cannot get a replication package from job with id {}", job.getId());
                }
                return JobResult.FAILED;
            }
        } catch (AgentReplicationException e) {
            if (log.isErrorEnabled()) {
                log.error("agent failed processing job", e);
            }
            return JobResult.FAILED;
        }
    }

}
