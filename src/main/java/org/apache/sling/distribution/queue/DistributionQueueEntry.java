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

import javax.annotation.Nonnull;

/**
 * an entry in a {@link DistributionQueue}
 */
public final class DistributionQueueEntry {

    private final DistributionQueueItem item;
    private final DistributionQueueItemStatus status;

    public DistributionQueueEntry(@Nonnull DistributionQueueItem item, @Nonnull DistributionQueueItemStatus status) {
        this.item = item;
        this.status = status;
    }

    /**
     * get the status for this entry
     *
     * @return a {@link DistributionQueueStatus}
     */
    @Nonnull
    public DistributionQueueItemStatus getStatus() {
        return status;
    }

    /**
     * get the queue item in this entry
     *
     * @return a {@link DistributionQueueItem}
     */
    @Nonnull
    public DistributionQueueItem getItem() {
        return item;
    }

}
