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
import org.apache.sling.distribution.queue.DistributionQueueState;
import org.apache.sling.distribution.queue.DistributionQueueStatus;

import javax.annotation.Nonnull;

public class SimpleAgentDistributionQueue extends DistributionQueueWrapper {

    private final boolean isPaused;
    private final String agentName;

    public SimpleAgentDistributionQueue(DistributionQueue wrappedQueue, boolean isPaused, String agentName) {
        super(wrappedQueue);
        this.isPaused = isPaused;
        this.agentName = agentName;
    }

    @Nonnull
    @Override
    public DistributionQueueStatus getStatus() {
        return calculateStatus();
    }


    private DistributionQueueStatus calculateStatus() {
        DistributionQueueStatus status = super.getStatus();
        if (isPaused) {
            return new DistributionQueueStatus(status.getItemsCount(), DistributionQueueState.PAUSED);
        } else {
            return status;
        }
    }
}
