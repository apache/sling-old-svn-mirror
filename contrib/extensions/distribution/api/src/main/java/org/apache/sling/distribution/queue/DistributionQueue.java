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
package org.apache.sling.distribution.queue;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import aQute.bnd.annotation.ProviderType;

/**
 * A queue is responsible for collecting the {@link org.apache.sling.distribution.packaging.DistributionPackage}s
 * exported by a {@link org.apache.sling.distribution.agent.DistributionAgent} in
 * order to be able to process them also when there are multiple (concurrent)
 * {@link org.apache.sling.distribution.communication.DistributionRequest}s executed
 * on that same agent.
 * <p/>
 * The items (packages) in the queue can then get processed according to a FIFO
 * strategy or in parallel, or some other way, via {@link org.apache.sling.distribution.queue.DistributionQueueProcessor}s.
 */
@ProviderType
public interface DistributionQueue {

    /**
     * get this queue name
     *
     * @return the queue name
     */
    @Nonnull
    String getName();

    /**
     * add a distribution item to this queue
     *
     * @param item a distribution item representing a {@link org.apache.sling.distribution.packaging.DistributionPackage}
     *             to distribute
     * @return {@code true} if the distribution item was added correctly to the queue,
     * {@code false} otherwise
     */
    boolean add(@Nonnull DistributionQueueItem item);

    /**
     * get the status of a certain item in the queue
     *
     * @param item the distribution item to get the status for
     * @return the item status in the queue
     * @throws DistributionQueueException if any error occurs while getting the status
     */
    @Nonnull
    DistributionQueueItemStatus getStatus(@Nonnull DistributionQueueItem item)
            throws DistributionQueueException;

    /**
     * get the first item (in a FIFO strategy, the next to be processed) from the queue
     *
     * @return the first item into the queue or {@code null} if the queue is empty
     */
    @CheckForNull
    DistributionQueueItem getHead();

    /**
     * check if the queue is empty
     *
     * @return {@code true} if the queue is empty, {@code false} otherwise
     */
    boolean isEmpty();

    /**
     * get all the items in the queue
     *
     * @param queueItemSelector represents the criteria to filter queue items.
     *                          if null is passed then all items are returned.
     * @return a {@link java.lang.Iterable} of {@link DistributionQueueItem}s
     */
    @Nonnull
    Iterable<DistributionQueueItem> getItems(@Nullable DistributionQueueItemSelector queueItemSelector);

    /**
     * remove an item from the queue by specifying its id
     *
     * @param id an item's identifier
     * @return the removed item, or {@code null} if the item with the given id
     * doesn't exist
     */
    DistributionQueueItem remove(@Nonnull String id);
}
