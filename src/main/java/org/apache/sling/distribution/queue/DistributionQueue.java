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

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.distribution.serialization.DistributionPackage;

/**
 * A queue is responsible for collecting the {@link DistributionPackage}s
 * exported by a {@link org.apache.sling.distribution.agent.DistributionAgent} in
 * order to be able to process them also when there are multiple (concurrent)
 * {@link org.apache.sling.distribution.DistributionRequest}s executed
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
     * @param item a distribution item, typically representing a {@link DistributionPackage}
     *             to distribute
     * @return the queue entry created for this item or {@code noll} if none is created
     */
    DistributionQueueEntry add(@Nonnull DistributionQueueItem item);


    /**
     * get the first item (in a FIFO strategy, the next to be processed) from the queue
     *
     * @return the first item into the queue or {@code null} if the queue is empty
     */
    @CheckForNull
    DistributionQueueEntry getHead();

    /**
     * get all the items in the queue
     *
     * @param skip the number of items to skip
     * @param limit the maximum number of items to return. use -1 to return all items.
     * @return a {@link java.lang.Iterable} of {@link DistributionQueueItem}s
     */
    @Nonnull
    Iterable<DistributionQueueEntry> getItems(int skip, int limit);


    /**
     * gets an item from the queue by specifying its id
     *
     * @param itemId the id of the item as returned by {@link DistributionQueueItem#getPackageId()}
     * @return the item, or {@code null} if the item with the given id
     * doesn't exist
     */
    @CheckForNull
    DistributionQueueEntry getItem(@Nonnull String itemId);

    /**
     * remove an item from the queue by specifying its id
     *
     * @param itemId the id the item as returned by {@link DistributionQueueItem#getPackageId()}
     * @return the removed item, or {@code null} if the item with the given id
     * doesn't exist
     */
    @CheckForNull
    DistributionQueueEntry remove(@Nonnull String itemId);


    /**
     * returns the status of the queue
     * @return the queue status
     */
    @Nonnull
    DistributionQueueStatus getStatus();
}
