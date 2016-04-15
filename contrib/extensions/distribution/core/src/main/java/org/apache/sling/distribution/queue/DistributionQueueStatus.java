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

/**
 * The status of a certain {@link DistributionQueue}
 */
public final class DistributionQueueStatus {
    private final int itemsCount;

    private final DistributionQueueState queueState;

    public DistributionQueueStatus(int itemsCount, DistributionQueueState queueState) {
        this.itemsCount = itemsCount;
        this.queueState = queueState;
    }


    /**
     * get the no. of items in the queue
     * @return the no. of items in the queue
     */
    public int getItemsCount() {
        return itemsCount;
    }

    /**
     * get the state associated with the status of this queue
     * @return a {@link DistributionQueueItemState}
     */
    public DistributionQueueState getState() {
        return queueState;
    }

    /**
     * check if the queue is empty
     * @return {@code true} if the queue is empty
     */
    public boolean isEmpty() {
        return itemsCount == 0;
    }
}
