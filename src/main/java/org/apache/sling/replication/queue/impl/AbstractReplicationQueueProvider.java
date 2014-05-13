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

    public ReplicationQueue getQueue(String agentName, String queueName)
                    throws ReplicationQueueException {
        String key = agentName + queueName;

        ReplicationQueue queue = queueMap.get(key);
        if (queue == null) {
            log.info("creating a queue with key {}", key);
            queue = getOrCreateQueue(agentName, queueName);
            queueMap.put(key, queue);
            log.info("queue created {}", queue);
        }
        return queue;
    }

    public ReplicationQueue getDefaultQueue(String agentName)
                    throws ReplicationQueueException {
        return getQueue(agentName, "");
    }

    public Collection<ReplicationQueue> getAllQueues() {
        return queueMap.values();
    }

    public void removeQueue(ReplicationQueue queue) throws ReplicationQueueException {
        deleteQueue(queue);
        // flush cache
        if (queueMap.containsValue(queue)) {
            if (!queueMap.values().remove(queue)) {
                throw new ReplicationQueueException("could not remove the queue " + queue);
            }
        }
    }

    protected abstract ReplicationQueue getOrCreateQueue(String agentName, String queueName) throws ReplicationQueueException;

    protected abstract void deleteQueue(ReplicationQueue queue);

}
