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
package org.apache.sling.replication.queue.impl.simple;

import javax.annotation.Nonnull;

import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueException;
import org.apache.sling.replication.queue.ReplicationQueueProcessor;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * a queue provider {@link ReplicationQueueProvider} for simple in memory
 * {@link ReplicationQueue}s
 */
public class SimpleReplicationQueueProvider  implements ReplicationQueueProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());


    private final String name;
    private final Scheduler scheduler;

    private final Map<String, ReplicationQueue> queueMap = new ConcurrentHashMap<String, ReplicationQueue>();


    public SimpleReplicationQueueProvider(Scheduler scheduler, String name) {
        if (name == null || scheduler == null) {
            throw new IllegalArgumentException("all arguments are required");
        }

        this.scheduler = scheduler;
        this.name = name;

    }

    @Nonnull
    public ReplicationQueue getQueue(@Nonnull String queueName)
            throws ReplicationQueueException {
        String key = name + queueName;

        ReplicationQueue queue = queueMap.get(key);
        if (queue == null) {
            log.info("creating a queue with key {}", key);
            queue = new SimpleReplicationQueue(name, queueName);
            queueMap.put(key, queue);
            log.info("queue created {}", queue);
        }
        return queue;
    }


    protected Collection<ReplicationQueue> getQueues() {
        return queueMap.values();
    }

    public void enableQueueProcessing(@Nonnull ReplicationQueueProcessor queueProcessor) {
        ScheduleOptions options = scheduler.NOW(-1, 10)
                .canRunConcurrently(false)
                .name(getJobName());
        scheduler.schedule(new ScheduledReplicationQueueProcessorTask(this, queueProcessor), options);
    }

    public void disableQueueProcessing() {
        scheduler.unschedule(getJobName());
    }

    private String getJobName() {
        return "simple-queueProcessor-" + name;
    }
}
