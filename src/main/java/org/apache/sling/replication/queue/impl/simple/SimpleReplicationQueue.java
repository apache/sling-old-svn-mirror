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
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.queue.ReplicationQueueItemSelector;
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

    private final String name;

    private final BlockingQueue<ReplicationQueueItem> queue;

    private final Map<ReplicationQueueItem, ReplicationQueueItemState> statusMap;

    public SimpleReplicationQueue(String agentName, String name) {
        log.info("starting a simple queue for agent {}", agentName);
        this.name = name;
        this.queue = new LinkedBlockingQueue<ReplicationQueueItem>();
        this.statusMap = new WeakHashMap<ReplicationQueueItem, ReplicationQueueItemState>(10);
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public boolean add(@Nonnull ReplicationQueueItem item) {
        ItemState itemState = ItemState.ERROR;
        boolean result = false;
        try {
            result = queue.offer(item, 10, TimeUnit.SECONDS);
            itemState = ItemState.QUEUED;
        } catch (InterruptedException e) {
            log.error("cannot add an item to the queue", e);
        } finally {
            statusMap.put(item, new ReplicationQueueItemState(Calendar.getInstance(), itemState, 0));
        }
        return result;
    }

    @Nonnull
    public ReplicationQueueItemState getStatus(@Nonnull ReplicationQueueItem queueItem) {
        ReplicationQueueItemState itemStatus = statusMap.get(queueItem);

        if (queue.contains(queueItem)) {
            return itemStatus;
        } else {
            return new ReplicationQueueItemState(itemStatus.getEntered(), ItemState.SUCCEEDED, itemStatus.getAttempts());
        }
    }

    public ReplicationQueueItem getHead() {
        ReplicationQueueItem element = queue.peek();
        if (element != null) {
            ReplicationQueueItemState itemState = statusMap.get(element);
            statusMap.put(element, new ReplicationQueueItemState(itemState.getEntered(),
                    itemState.getItemState(),
                    itemState.getAttempts()+1));
        }
        return element;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Nonnull
    public Iterable<ReplicationQueueItem> getItems(ReplicationQueueItemSelector queueItemSelector) {
        return queue;
    }



    public void remove(@Nonnull String id) {
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
        log.info("item with id {} removed from the queue: {}", id, removed);
    }

}
