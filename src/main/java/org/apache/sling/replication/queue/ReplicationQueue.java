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
package org.apache.sling.replication.queue;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * a queue for handling {@link org.apache.sling.replication.agent.ReplicationAgent}s' requests
 */
public interface ReplicationQueue {

    /**
     * get this queue name
     *
     * @return queue name as a <code>String</code>
     */
    @Nonnull
    String getName();

    /**
     * add a replication item to this queue
     *
     * @param item a replication item representing the package to replicate
     * @return <code>true</code> if the replication item was added correctly to the queue,
     * <code>false</code otherwise
     */
    boolean add(@Nonnull ReplicationQueueItem item);

    /**
     * get the status of a certain item in the queue
     *
     * @param item the replication item to get the status for
     * @return the item status in the queue
     * @throws ReplicationQueueException if any error occurs while getting the status
     */
    @Nonnull
    ReplicationQueueItemState getStatus(@Nonnull ReplicationQueueItem item)
            throws ReplicationQueueException;

    /**
     * get the first item (FIFO wise, the next to be processed) into the queue
     *
     * @return the first item into the queue or <code>null</code> if the queue is empty
     */
    @CheckForNull
    ReplicationQueueItem getHead();

    /**
     * check if the queue is empty
     *
     * @return <code>true</code> if the queue is empty, <code>false</code> otherwise
     */
    boolean isEmpty();

    /**
     * get the items in the queue
     *
     * @return a <code>Collection</code> of {@link org.apache.sling.replication.queue.ReplicationQueueItem}s
     */
    @Nonnull
    Collection<ReplicationQueueItem> getItems();

    /**
     * remove an item from the queue by specifying its id
     *
     * @param id <code>String</code> representing an item's identifier
     */
    void remove(@Nonnull String id);
}
