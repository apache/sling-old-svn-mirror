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
package org.apache.sling.replication.queue.impl;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueException;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * abstract base implementation of a {@link ReplicationQueueProvider}
 */
public abstract class AbstractReplicationQueueProvider implements ReplicationQueueProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String, ReplicationQueue> queueMap = new ConcurrentHashMap<String, ReplicationQueue>();

    @Nonnull
    public ReplicationQueue getQueue(@Nonnull String agentName, @Nonnull String queueName)
            throws ReplicationQueueException {
        String key = agentName + queueName;

        ReplicationQueue queue = queueMap.get(key);
        if (queue == null) {
            log.info("creating a queue with key {}", key);
            queue = getInternalQueue(agentName, queueName);
            queueMap.put(key, queue);
            log.info("queue created {}", queue);
        }
        return queue;
    }

    @Nonnull
    public Collection<ReplicationQueue> getAllQueues() {
        return queueMap.values();
    }


    protected abstract ReplicationQueue getInternalQueue(String agentName, String queueName) throws ReplicationQueueException;

}
