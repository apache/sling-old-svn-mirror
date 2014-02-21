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
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.queue.ReplicationQueueItemState;
import org.apache.sling.replication.queue.ReplicationQueueItemState.ItemState;
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

    private final String agentName;

    private final String name;

    private final BlockingQueue<ReplicationQueueItem> queue;

    private final Map<ReplicationQueueItem, ReplicationQueueItemState> statusMap;

    public SimpleReplicationQueue(String agentName, String name) {
        if (log.isInfoEnabled()) {
            log.info("starting a simple queue for agent {}", agentName);
        }
        this.agentName = agentName;
        this.name = name;
        this.queue = new LinkedBlockingQueue<ReplicationQueueItem>();
        this.statusMap = new WeakHashMap<ReplicationQueueItem, ReplicationQueueItemState>(10);
    }

    public String getName() {
        return name;
    }

    public boolean add(ReplicationQueueItem replicationPackage) {
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

    public ReplicationQueueItemState getStatus(ReplicationQueueItem replicationPackage) {
        ReplicationQueueItemState status = statusMap.get(replicationPackage);
        if (queue.contains(replicationPackage)) {
            status.setItemState(ItemState.QUEUED);
        } else {
            status.setItemState(ItemState.SUCCEEDED);
        }
        return status;
    }

    public ReplicationQueueItem getHead() {
        ReplicationQueueItem element = queue.peek();
        if (element != null) {
            ReplicationQueueItemState replicationQueueItemStatus = statusMap.get(element);
            replicationQueueItemStatus.setAttempts(replicationQueueItemStatus.getAttempts() + 1);
        }
        return element;
    }

    public void removeHead() {
        ReplicationQueueItem element = queue.remove();
        statusMap.get(element).setSuccessful(true);
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public Collection<ReplicationQueueItem> getItems() {
        return queue;
    }

    public void remove(String id) {
        ReplicationQueueItem toRemove = null;
        for (ReplicationQueueItem item : queue) {
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
