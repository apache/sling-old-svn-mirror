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

import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueItemState;
import org.apache.sling.replication.queue.ReplicationQueueItemState.ItemState;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple implementation of a {@link ReplicationQueue}.
 * <p/>
 * Note that, at the moment, this is a transient in memory queue not persisted on the repository and
 * therefore not usable for production.
 */
public class SimpleReplicationQueue implements ReplicationQueue {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ReplicationAgent agent;

    private final String name;

    private final BlockingQueue<ReplicationPackage> queue;

    private final Map<ReplicationPackage, ReplicationQueueItemState> statusMap;

    public SimpleReplicationQueue(ReplicationAgent agent, String name) {
        if (log.isInfoEnabled()) {
            log.info("starting a simple queue for agent {}", agent.getName());
        }
        this.agent = agent;
        this.name = name;
        this.queue = new LinkedBlockingQueue<ReplicationPackage>();
        this.statusMap = new WeakHashMap<ReplicationPackage, ReplicationQueueItemState>(10);
    }

    public String getName() {
        return name;
    }

    public boolean add(ReplicationPackage replicationPackage) {
        ReplicationQueueItemState status = new ReplicationQueueItemState();
        boolean result = false;
        try {
            result = queue.offer(replicationPackage, 10, TimeUnit.SECONDS);
            status.setEntered(Calendar.getInstance());
        } catch (InterruptedException e) {
            log.error("cannot add an item to the queue", e);
            status.setSuccessful(false);
        } finally {
            statusMap.put(replicationPackage, status);
        }
        return result;
    }

    public ReplicationQueueItemState getStatus(ReplicationPackage replicationPackage) {
        ReplicationQueueItemState status = statusMap.get(replicationPackage);
        if (queue.contains(replicationPackage)) {
            status.setItemState(ItemState.QUEUED);
        } else {
            status.setItemState(ItemState.SUCCEEDED);
        }
        return status;
    }

    public ReplicationAgent getAgent() {
        return agent;
    }

    public ReplicationPackage getHead() {
        ReplicationPackage element = queue.peek();
        if (element != null) {
            ReplicationQueueItemState replicationQueueItemStatus = statusMap.get(element);
            replicationQueueItemStatus.setAttempts(replicationQueueItemStatus.getAttempts() + 1);
        }
        return element;
    }

    public void removeHead() {
        ReplicationPackage element = queue.remove();
        statusMap.get(element).setSuccessful(true);
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public Collection<ReplicationPackage> getItems() {
        return queue;
    }

    public void remove(String id) {
        ReplicationPackage toRemove = null;
        for (ReplicationPackage item : queue) {
            if (id.equals(item.getId())) {
                toRemove = item;
            }
        }
        boolean removed = false;
        if (toRemove != null) {
            removed = queue.remove(toRemove);
        }
        if (log.isInfoEnabled()) {
            log.info("item with id {} removed from the queue: {}", id, removed);
        }
    }

}
