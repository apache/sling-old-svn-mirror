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
import java.util.Calendar;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus.ItemState;
import org.apache.sling.distribution.queue.DistributionQueueState;
import org.apache.sling.distribution.queue.impl.DistributionQueueUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple implementation of a {@link org.apache.sling.distribution.queue.DistributionQueue}.
 * <p/>
 * Note that, at the moment, this is a transient in memory queue not persisted on the repository and
 * therefore not usable for production.
 */
public class SimpleDistributionQueue implements DistributionQueue {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String name;

    private final BlockingQueue<DistributionQueueItem> queue;

    private final Map<DistributionQueueItem, DistributionQueueItemStatus> statusMap;

    public SimpleDistributionQueue(String agentName, String name) {
        log.info("starting a simple queue for agent {}", agentName);
        this.name = name;
        this.queue = new LinkedBlockingQueue<DistributionQueueItem>();
        this.statusMap = new WeakHashMap<DistributionQueueItem, DistributionQueueItemStatus>(10);
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public boolean add(@Nonnull DistributionQueueItem item) {
        ItemState itemState = ItemState.ERROR;
        boolean result = false;
        try {
            result = queue.offer(item, 10, TimeUnit.SECONDS);
            itemState = ItemState.QUEUED;
        } catch (InterruptedException e) {
            log.error("cannot add an item to the queue", e);
        } finally {
            statusMap.put(item, new DistributionQueueItemStatus(Calendar.getInstance(), itemState, 0, name));
        }
        return result;
    }

    @Nonnull
    public DistributionQueueItemStatus getStatus(@Nonnull DistributionQueueItem queueItem) {
        DistributionQueueItemStatus itemStatus = statusMap.get(queueItem);

        if (queue.contains(queueItem)) {
            return itemStatus;
        } else {
            return new DistributionQueueItemStatus(itemStatus.getEntered(), ItemState.SUCCEEDED, itemStatus.getAttempts(), name);
        }
    }

    public DistributionQueueItem getHead() {
        DistributionQueueItem element = queue.peek();
        if (element != null) {
            DistributionQueueItemStatus itemState = statusMap.get(element);
            statusMap.put(element, new DistributionQueueItemStatus(itemState.getEntered(),
                    itemState.getItemState(),
                    itemState.getAttempts() + 1, name));
        }
        return element;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int getItemsCount() {
        return queue.size();
    }

    public DistributionQueueState getState() {
        return DistributionQueueUtils.calculateState(this);
    }


    @Nonnull
    public Iterable<DistributionQueueItem> getItems(int skip, int limit) {
        return queue;
    }

    public DistributionQueueItem getItem(@Nonnull String id) {
        DistributionQueueItem toRemove = null;
        for (DistributionQueueItem item : queue) {
            if (id.equals(item.getId())) {
                return item;
            }
        }

        return null;
    }


    public DistributionQueueItem remove(@Nonnull String id) {
        DistributionQueueItem toRemove =getItem(id);

        boolean removed = false;
        if (toRemove != null) {
            removed = queue.remove(toRemove);
        }
        log.info("item with id {} removed from the queue: {}", id, removed);
        if (removed) {
            return toRemove;
        }
        else {
            return null;
        }
    }

}
