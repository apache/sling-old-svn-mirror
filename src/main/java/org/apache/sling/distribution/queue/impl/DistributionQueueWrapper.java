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
package org.apache.sling.distribution.queue.impl;

import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueStatus;

import javax.annotation.Nonnull;

public abstract class DistributionQueueWrapper implements DistributionQueue {
    protected final DistributionQueue wrappedQueue;

    public DistributionQueueWrapper(DistributionQueue wrappedQueue) {

        this.wrappedQueue = wrappedQueue;
    }

    @Nonnull
    @Override
    public String getName() {
        return wrappedQueue.getName();
    }

    @Override
    public DistributionQueueEntry add(@Nonnull DistributionQueueItem item) {
        return wrappedQueue.add(item);
    }

    @Override
    public DistributionQueueEntry getHead() {
        return wrappedQueue.getHead();
    }

    @Nonnull
    @Override
    public Iterable<DistributionQueueEntry> getItems(int skip, int limit) {
        return wrappedQueue.getItems(skip, limit);
    }

    @Override
    public DistributionQueueEntry getItem(@Nonnull String itemId) {
        return wrappedQueue.getItem(itemId);
    }

    @Override
    public DistributionQueueEntry remove(@Nonnull String itemId) {
        return wrappedQueue.remove(itemId);
    }

    @Nonnull
    @Override
    public DistributionQueueStatus getStatus() {
        return wrappedQueue.getStatus();
    }
}
