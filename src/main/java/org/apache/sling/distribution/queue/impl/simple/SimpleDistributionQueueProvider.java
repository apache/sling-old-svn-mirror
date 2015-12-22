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
package org.apache.sling.distribution.queue.impl.simple;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueProcessor;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a queue provider {@link org.apache.sling.distribution.queue.DistributionQueueProvider} for simple in memory
 * {@link org.apache.sling.distribution.queue.DistributionQueue}s
 */
public class SimpleDistributionQueueProvider implements DistributionQueueProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String name;
    private final Scheduler scheduler;

    private final Map<String, DistributionQueue> queueMap = new ConcurrentHashMap<String, DistributionQueue>();

    public SimpleDistributionQueueProvider(Scheduler scheduler, String name) {
        if (name == null || scheduler == null) {
            throw new IllegalArgumentException("all arguments are required");
        }

        this.scheduler = scheduler;
        this.name = name;

    }

    @Nonnull
    public DistributionQueue getQueue(@Nonnull String queueName) {
        String key = name + queueName;

        DistributionQueue queue = queueMap.get(key);
        if (queue == null) {
            log.debug("creating a queue with key {}", key);
            queue = new SimpleDistributionQueue(name, queueName);
            queueMap.put(key, queue);
            log.debug("queue created {}", queue);
        }
        return queue;
    }


    Collection<DistributionQueue> getQueues() {
        return queueMap.values();
    }

    public void enableQueueProcessing(@Nonnull DistributionQueueProcessor queueProcessor, String... queueNames) {
        ScheduleOptions options = scheduler.NOW(-1, 10)
                .canRunConcurrently(false)
                .name(getJobName());
        scheduler.schedule(new ScheduledDistributionQueueProcessorTask(this, queueProcessor), options);
    }

    public void disableQueueProcessing() {
        scheduler.unschedule(getJobName());
    }

    private String getJobName() {
        return "simple-queueProcessor-" + name;
    }
}
